package com.hardbuntu.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String = "",
    @SerialName("category_id")
    val categoryId: String? = null,
    val name: String,
    val slug: String,
    val brand: String,
    val sku: String,
    val description: String? = null,
    val price: Double,
    val stock: Int = 0,
    val unit: String = "unidad",
    val specs: Map<String, String> = emptyMap(),
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
