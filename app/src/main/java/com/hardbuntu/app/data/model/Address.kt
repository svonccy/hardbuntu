package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: String,
    val label: String = "Casa",
    val address: String,
    val reference: String? = null,
    @SerialName("is_default")
    val isDefault: Boolean = false
)

/** [id] nulo = dirección nueva (se omite del JSON y la base genera el id). */
@Serializable
data class AddressUpsert(
    val id: String? = null,
    val label: String,
    val address: String,
    val reference: String?,
    @SerialName("is_default")
    val isDefault: Boolean
)
