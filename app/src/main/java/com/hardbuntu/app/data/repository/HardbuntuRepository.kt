package com.hardbuntu.app.data.repository

import com.hardbuntu.app.data.model.Category
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.data.model.OrderItem
import com.hardbuntu.app.data.model.Product
import com.hardbuntu.app.data.remote.SupabaseClient
import io.github.jan.supabase.SupabaseClient as SClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HardbuntuRepository(
    private val client: SClient = SupabaseClient.instance
) {
    suspend fun signIn(email: String, password: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        runCatching {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            client.auth.currentUserOrNull() ?: throw IllegalStateException("No se pudo obtener el usuario autenticado")
        }
    }

    suspend fun signUp(email: String, password: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        runCatching {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            client.auth.currentUserOrNull() ?: throw IllegalStateException("Registro completado. Revisa tu correo de confirmación.")
        }
    }

    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.auth.signOut()
        }
    }

    fun getCurrentUser(): UserInfo? {
        return client.auth.currentUserOrNull()
    }

    suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("categories")
                .select()
                .decodeList<Category>()
        }
    }

    suspend fun getProducts(categoryId: String? = null): Result<List<Product>> = withContext(Dispatchers.IO) {
        runCatching {
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
    }

    suspend fun createOrder(order: Order, items: List<OrderItem>): Result<Order> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Insertar orden y obtener registro generado con su ID
            val insertedOrder = client.from("orders")
                .insert(order) {
                    select()
                }
                .decodeSingle<Order>()

            val orderId = insertedOrder.id
                ?: throw IllegalStateException("No se pudo obtener el ID de la orden generada")

            // 2. Insertar items vinculados al order_id
            if (items.isNotEmpty()) {
                val itemsWithOrderId = items.map { it.copy(orderId = orderId) }
                client.from("order_items").insert(itemsWithOrderId)
            }

            insertedOrder
        }
    }

    suspend fun getOrders(): Result<List<Order>> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("orders")
                .select()
                .decodeList<Order>()
        }
    }
}
