package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String = "",
    val name: String,
    val slug: String,
    val description: String? = null,
    val icon: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
