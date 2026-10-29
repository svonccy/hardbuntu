package com.hardbuntu.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    /** Precio unitario según la cantidad (precios por mayor). */
    val unitPrice: Double
        get() = product.priceFor(quantity)

    val subtotal: Double
        get() = unitPrice * quantity
}
