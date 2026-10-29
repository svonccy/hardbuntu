package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Forma de pago disponible (la base solo devuelve las activas). */
@Serializable
data class PaymentMethod(
    val code: String,
    val name: String,
    val instructions: String? = null,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)
