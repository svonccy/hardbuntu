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
    @SerialName("created_at")
    val createdAt: String? = null
)
