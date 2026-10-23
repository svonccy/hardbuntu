package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val id: String? = null,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("product_id")
    val productId: String? = null,
    @SerialName("product_name")
    val productName: String,
    @SerialName("product_price")
    val productPrice: Double,
    val quantity: Int,
    val subtotal: Double,
    @SerialName("created_at")
    val createdAt: String? = null
)
