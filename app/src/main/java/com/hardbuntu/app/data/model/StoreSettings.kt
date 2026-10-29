package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Datos del local, editables desde Supabase (tabla store_settings). */
@Serializable
data class StoreSettings(
    @SerialName("store_name")
    val storeName: String = "Hardbuntu",
    val address: String? = null,
    val phone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    @SerialName("opening_hours")
    val openingHours: String? = null,
    @SerialName("pickup_enabled")
    val pickupEnabled: Boolean = true,
    @SerialName("delivery_enabled")
    val deliveryEnabled: Boolean = true,
    @SerialName("delivery_fee")
    val deliveryFee: Double = 0.0,
    @SerialName("free_delivery_from")
    val freeDeliveryFrom: Double? = null
) {
    /** Costo de envío para un pedido de [itemsTotal] soles (gratis desde el monto configurado). */
    fun deliveryFeeFor(itemsTotal: Double): Double {
        val threshold = freeDeliveryFrom
        return if (threshold != null && itemsTotal >= threshold) 0.0 else deliveryFee
    }

    /** true si hay algún dato de contacto o ubicación para mostrar en "Nuestra tienda". */
    val hasPublicInfo: Boolean
        get() = listOf(address, phone, whatsapp, email, openingHours).any { !it.isNullOrBlank() }
}
