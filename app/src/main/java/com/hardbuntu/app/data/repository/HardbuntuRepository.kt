package com.hardbuntu.app.data.repository

import com.hardbuntu.app.data.model.Address
import com.hardbuntu.app.data.model.AddressUpsert
import com.hardbuntu.app.data.model.CartItem
import com.hardbuntu.app.data.model.Category
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.data.model.PaymentMethod
import com.hardbuntu.app.data.model.Product
import com.hardbuntu.app.data.model.Profile
import com.hardbuntu.app.data.model.ProfileUpsert
import com.hardbuntu.app.data.model.StoreSettings
import com.hardbuntu.app.data.remote.SupabaseClient
import io.github.jan.supabase.SupabaseClient as SClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException

/** Resultado del registro: Supabase exige confirmar el correo antes de iniciar sesión. */
enum class SignUpResult { SIGNED_IN, CONFIRMATION_REQUIRED }

class HardbuntuRepository(
    private val client: SClient = SupabaseClient.instance
) {
    /** Ejecuta [block] en IO y devuelve el resultado con errores traducidos a mensajes legibles. */
    private suspend fun <T> call(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        runCatching { block() }.mapError()
    }

    // --- Autenticación ---

    /** Estado de sesión de Supabase (incluye la restauración de la sesión guardada al abrir la app). */
    val sessionStatus: StateFlow<SessionStatus>
        get() = client.auth.sessionStatus

    suspend fun signIn(email: String, password: String): Result<UserInfo> = call {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        client.auth.currentUserOrNull() ?: throw IllegalStateException("No se pudo obtener el usuario autenticado")
    }

    suspend fun signUp(email: String, password: String): Result<SignUpResult> = call {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        // Sin sesión activa = el proyecto requiere confirmación por correo
        if (client.auth.currentSessionOrNull() != null) SignUpResult.SIGNED_IN else SignUpResult.CONFIRMATION_REQUIRED
    }

    suspend fun signOut(): Result<Unit> = call {
        client.auth.signOut()
    }

    /** Envía el correo de recuperación; el enlace abre la app (hardbuntu://auth). */
    suspend fun requestPasswordReset(email: String): Result<Unit> = call {
        client.auth.resetPasswordForEmail(email)
    }

    /** Cambia la contraseña del usuario con sesión activa (también tras abrir el enlace de recuperación). */
    suspend fun updatePassword(newPassword: String): Result<Unit> = call {
        client.auth.updateUser { password = newPassword }
        Unit
    }

    // --- Catálogo y datos del local ---

    suspend fun getCategories(): Result<List<Category>> = call {
        client.from("categories")
            .select()
            .decodeList<Category>()
    }

    suspend fun getProducts(categoryId: String? = null): Result<List<Product>> = call {
        client.from("products")
            .select {
                filter {
                    if (!categoryId.isNullOrBlank()) {
                        eq("category_id", categoryId)
                    }
                    eq("is_active", true)
                }
            }
            .decodeList<Product>()
    }

    suspend fun getStoreSettings(): Result<StoreSettings> = call {
        client.from("store_settings")
            .select()
            .decodeSingleOrNull<StoreSettings>()
            ?: StoreSettings()
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = call {
        client.from("payment_methods")
            .select { order("sort_order", SortOrder.ASCENDING) }
            .decodeList<PaymentMethod>()
    }

    // --- Pedidos ---

    /**
     * Registra el pedido mediante la función transaccional `create_order` de Supabase.
     * El servidor valida stock, calcula montos (precios por mayor, envío, IGV) con sus propios datos,
     * descuenta el stock y vincula el pedido al usuario autenticado (si lo hay).
     */
    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        items: List<CartItem>,
        fulfillmentType: String,
        paymentMethod: String?,
        customerEmail: String? = null,
        notes: String? = null
    ): Result<Order> = call {
        val params = buildJsonObject {
            put("p_customer_name", customerName)
            put("p_customer_phone", customerPhone)
            put("p_delivery_address", deliveryAddress)
            put("p_customer_email", customerEmail)
            put("p_notes", notes)
            put("p_fulfillment_type", fulfillmentType)
            put("p_payment_method", paymentMethod)
            put("p_items", buildJsonArray {
                items.forEach { item ->
                    add(buildJsonObject {
                        put("product_id", item.product.id)
                        put("quantity", item.quantity)
                    })
                }
            })
        }

        client.postgrest.rpc("create_order", params).decodeAs<Order>()
    }

    /** Pedidos del usuario autenticado (RLS solo devuelve los propios), del más reciente al más antiguo. */
    suspend fun getMyOrders(): Result<List<Order>> = call {
        client.from("orders")
            .select(Columns.raw("*, order_items(*)")) {
                order("created_at", SortOrder.DESCENDING)
            }
            .decodeList<Order>()
    }

    /** Cancela un pedido propio que siga pendiente; el servidor devuelve el stock. */
    suspend fun cancelOrder(orderId: String): Result<Order> = call {
        client.postgrest.rpc(
            "cancel_order",
            buildJsonObject { put("p_order_id", orderId) }
        ).decodeAs<Order>()
    }

    // --- Perfil y direcciones ---

    suspend fun getProfile(): Result<Profile?> = call {
        client.from("profiles")
            .select()
            .decodeSingleOrNull<Profile>()
    }

    suspend fun saveProfile(fullName: String?, phone: String?): Result<Profile> = call {
        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Debes iniciar sesión para guardar tus datos")
        client.from("profiles")
            .upsert(ProfileUpsert(userId, fullName, phone)) { select() }
            .decodeSingle<Profile>()
    }

    suspend fun getAddresses(): Result<List<Address>> = call {
        client.from("addresses")
            .select {
                order("is_default", SortOrder.DESCENDING)
                order("created_at", SortOrder.ASCENDING)
            }
            .decodeList<Address>()
    }

    /** Crea la dirección (id nulo) o actualiza una existente. */
    suspend fun saveAddress(address: AddressUpsert): Result<Address> = call {
        client.from("addresses")
            .upsert(address) { select() }
            .decodeSingle<Address>()
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> = call {
        client.from("addresses").delete {
            filter { eq("id", addressId) }
        }
        Unit
    }

    suspend fun setDefaultAddress(addressId: String): Result<Unit> = call {
        client.postgrest.rpc(
            "set_default_address",
            buildJsonObject { put("p_address_id", addressId) }
        )
        Unit
    }

    /** Convierte excepciones técnicas de Supabase en mensajes legibles para el usuario. */
    private fun <T> Result<T>.mapError(): Result<T> = recoverCatching { error ->
        throw IllegalStateException(error.toUserMessage(), error)
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is AuthSessionMissingException -> "El enlace venció o ya fue usado. Solicita uno nuevo con \"¿Olvidaste tu contraseña?\"."
        is AuthRestException -> when (errorCode) {
            AuthErrorCode.InvalidCredentials -> "Correo o contraseña incorrectos"
            AuthErrorCode.EmailNotConfirmed -> "Debes confirmar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada."
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> "Ya existe una cuenta registrada con este correo"
            AuthErrorCode.WeakPassword -> "La contraseña es demasiado débil (mínimo 6 caracteres)"
            AuthErrorCode.OverEmailSendRateLimit -> "Se enviaron demasiados correos. Intenta nuevamente en unos minutos."
            AuthErrorCode.SignupDisabled -> "El registro de nuevas cuentas está deshabilitado"
            AuthErrorCode.ValidationFailed -> "Revisa el formato del correo electrónico"
            AuthErrorCode.SamePassword -> "La nueva contraseña debe ser distinta a la anterior"
            else -> errorDescription
        }
        // Mensaje de la base de datos (ej. "Stock insuficiente para ...") sin el volcado HTTP
        is RestException -> error
        is HttpRequestException, is IOException -> "Sin conexión con el servidor. Verifica tu internet e inténtalo de nuevo."
        else -> message ?: "Ocurrió un error inesperado"
    }
}
