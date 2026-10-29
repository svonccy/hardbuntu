package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String? = null,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("customer_email")
    val customerEmail: String? = null,
    @SerialName("customer_phone")
    val customerPhone: String,
    @SerialName("delivery_address")
    val deliveryAddress: String,
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val status: String = "pending",
    val notes: String? = null,
    @SerialName("fulfillment_type")
    val fulfillmentType: String = FULFILLMENT_DELIVERY,
    @SerialName("delivery_fee")
    val deliveryFee: Double = 0.0,
    @SerialName("payment_method")
    val paymentMethod: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    // Relación embebida de PostgREST: select("*, order_items(*)")
    @SerialName("order_items")
    val items: List<OrderItem> = emptyList()
) {
    val orderStatus: OrderStatus
        get() = OrderStatus.fromRaw(status)

    val isPickup: Boolean
        get() = fulfillmentType == FULFILLMENT_PICKUP

    companion object {
        const val FULFILLMENT_DELIVERY = "delivery"
        const val FULFILLMENT_PICKUP = "pickup"
    }
}
