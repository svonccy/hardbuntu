package com.hardbuntu.app.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.hardbuntu.app.data.model.CartItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Datos necesarios para armar una cotización del carrito. */
data class QuoteData(
    val storeName: String,
    val storeAddress: String?,
    val storePhone: String?,
    val items: List<CartItem>,
    val subtotal: Double,
    val igv: Double,
    val total: Double
)

/**
 * Cotización del carrito para compartir por WhatsApp (texto) o como PDF.
 * Es referencial: no reserva stock ni congela precios.
 */
object QuoteShare {
    private const val DISCLAIMER = "Cotización referencial: los precios y el stock pueden variar."

    private fun money(value: Double) = String.format(Locale.US, "S/. %.2f", value)

    private fun now(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE")).format(Date())

    fun buildText(data: QuoteData): String = buildString {
        appendLine("*Cotización - ${data.storeName}*")
        appendLine("Fecha: ${now()}")
        appendLine()
        data.items.forEach { item ->
            appendLine("• ${item.quantity} x ${item.product.name}")
            appendLine("   ${money(item.unitPrice)} c/u = ${money(item.subtotal)}")
        }
        appendLine()
        appendLine("Subtotal (sin IGV): ${money(data.subtotal)}")
        appendLine("IGV (18%): ${money(data.igv)}")
        appendLine("*Total: ${money(data.total)}* (IGV incluido)")
        appendLine()
        append(DISCLAIMER)
    }

    fun shareText(context: Context, data: QuoteData) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Cotización ${data.storeName}")
            putExtra(Intent.EXTRA_TEXT, buildText(data))
        }
        context.startActivity(Intent.createChooser(intent, "Compartir cotización"))
    }

    /** Genera el PDF en la caché de la app y abre el selector para compartirlo. */
    fun sharePdf(context: Context, data: QuoteData): Result<Unit> = runCatching {
        val file = createPdf(context, data)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Cotización ${data.storeName}")
            clipData = ClipData.newRawUri("Cotización", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir cotización"))
    }

    private fun createPdf(context: Context, data: QuoteData): File {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val right = pageWidth - margin
        val unitPriceX = 420f

        val brand = Color.rgb(0xF4, 0x51, 0x1E)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f
            color = brand
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = Color.BLACK }
        val bold = Paint(normal).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val muted = Paint(normal).apply { textSize = 10f; color = Color.DKGRAY }
        val normalRight = Paint(normal).apply { textAlign = Paint.Align.RIGHT }
        val boldRight = Paint(bold).apply { textAlign = Paint.Align.RIGHT }
        val totalRight = Paint(bold).apply { textAlign = Paint.Align.RIGHT; textSize = 14f; color = brand }
        val divider = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 18f

        fun fit(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            val count = paint.breakText(text, true, maxWidth - paint.measureText("…"), null)
            return text.take(count) + "…"
        }

        fun drawTableHeader() {
            canvas.drawText("Cant.", margin, y, bold)
            canvas.drawText("Descripción", margin + 45f, y, bold)
            canvas.drawText("P. unit.", unitPriceX, y, boldRight)
            canvas.drawText("Subtotal", right, y, boldRight)
            y += 6f
            canvas.drawLine(margin, y, right, y, divider)
            y += 16f
        }

        // Encabezado
        canvas.drawText(data.storeName, margin, y, titlePaint)
        y += 20f
        canvas.drawText("COTIZACIÓN", margin, y, bold)
        canvas.drawText(now(), right, y, normalRight)
        y += 14f
        listOfNotNull(data.storeAddress, data.storePhone?.let { "Tel. $it" })
            .filter { it.isNotBlank() }
            .forEach { line ->
                canvas.drawText(fit(line, muted, right - margin), margin, y, muted)
                y += 12f
            }
        y += 10f
        canvas.drawLine(margin, y, right, y, divider)
        y += 22f
        drawTableHeader()

        // Detalle (con salto de página si hace falta)
        data.items.forEach { item ->
            if (y > pageHeight - 160f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin + 18f
                drawTableHeader()
            }
            canvas.drawText(item.quantity.toString(), margin, y, normal)
            canvas.drawText(fit(item.product.name, normal, unitPriceX - margin - 110f), margin + 45f, y, normal)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), unitPriceX, y, normalRight)
            canvas.drawText(String.format(Locale.US, "%.2f", item.subtotal), right, y, normalRight)
            y += 18f
        }

        // Totales
        y += 4f
        canvas.drawLine(margin, y, right, y, divider)
        y += 20f
        canvas.drawText("Subtotal (sin IGV)", unitPriceX, y, boldRight)
        canvas.drawText(String.format(Locale.US, "%.2f", data.subtotal), right, y, normalRight)
        y += 16f
        canvas.drawText("IGV (18%)", unitPriceX, y, boldRight)
        canvas.drawText(String.format(Locale.US, "%.2f", data.igv), right, y, normalRight)
        y += 22f
        canvas.drawText("TOTAL (IGV incluido)", unitPriceX, y, totalRight)
        canvas.drawText(money(data.total), right, y, totalRight)
        y += 34f
        canvas.drawText(DISCLAIMER, margin, y, muted)

        document.finishPage(page)

        val dir = File(context.cacheDir, "quotes").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // solo se conserva la última cotización
        val file = File(dir, "cotizacion-${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
