package com.hardbuntu.app.util

private val PHONE_REGEX = Regex("^\+?[0-9]{7,15}$")

/** Mismo criterio que la validación del servidor (create_order): 7 a 15 dígitos, "+" opcional. */
fun isValidPhone(raw: String): Boolean =
    raw.replace(" ", "").replace("-", "").matches(PHONE_REGEX)
