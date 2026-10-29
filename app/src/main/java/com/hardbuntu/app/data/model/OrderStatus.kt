package com.hardbuntu.app.data.model

/** Estados de un pedido tal como los entiende la app (el valor crudo vive en orders.status). */
enum class OrderStatus(val label: String) {
    PENDING("Pendiente"),
    PROCESSING("En proceso"),
    DELIVERED("Entregado"),
    CANCELLED("Cancelado");

    companion object {
        fun fromRaw(raw: String): OrderStatus = when (raw.trim().lowercase()) {
            "processing", "en_proceso" -> PROCESSING
            "delivered", "completed", "entregado" -> DELIVERED
            "cancelled", "canceled", "cancelado" -> CANCELLED
            else -> PENDING
        }
    }
}
