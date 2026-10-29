package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    val phone: String? = null
)

/** Sin valores por defecto a propósito: así los nulos se envían y permiten borrar un dato. */
@Serializable
data class ProfileUpsert(
    val id: String,
    @SerialName("full_name")
    val fullName: String?,
    val phone: String?
)
