package com.hardbuntu.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Acciones de contacto con el local. Cada una devuelve false si el dispositivo no tiene una app que la atienda. */
object ContactActions {

    private fun Context.launch(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    fun dial(context: Context, phone: String): Boolean {
        val number = phone.filter { it.isDigit() || it == '+' }
        return context.launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    /** [phone] con código de país (ej. 51987654321). */
    fun openWhatsApp(context: Context, phone: String, message: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")))
    }

    fun sendEmail(context: Context, email: String, subject: String, body: String = ""): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return context.launch(intent)
    }

    fun openMap(context: Context, address: String): Boolean =
        context.launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}")))
}
