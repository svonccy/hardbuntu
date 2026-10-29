package com.hardbuntu.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.hardbuntu.app.data.model.Order
import java.util.Locale

/**
 * Confirmación de pedido registrado.
 * @param canViewOrders true si hay sesión iniciada (los pedidos de invitados no se pueden consultar luego).
 */
@Composable
fun OrderSuccessDialog(
    order: Order,
    canViewOrders: Boolean,
    paymentName: String?,
    paymentInstructions: String?,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = FeedbackType.SUCCESS.accentColor()
    val badgeScale = remember { Animatable(0f) }
    val ring = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        badgeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(durationMillis = 900))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 20.dp)
            ) {
                // Insignia animada: onda expansiva + check con rebote
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .graphicsLayer {
                                val progress = ring.value
                                scaleX = 1f + 0.35f * progress
                                scaleY = 1f + 0.35f * progress
                                alpha = (1f - progress) * 0.6f
                            }
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.35f))
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .scale(badgeScale.value)
                            .clip(CircleShape)
                            .background(accent)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "¡Pedido registrado!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (canViewOrders) {
                        "Puedes revisar el estado de tu pedido en tu perfil."
                    } else {
                        "Guarda tu código de pedido. Crea una cuenta para consultar tus próximos pedidos."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SummaryRow(
                            label = "Código",
                            value = "#${order.id?.take(8)?.uppercase(Locale.getDefault()).orEmpty()}",
                            valueColor = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        SummaryRow(
                            label = if (order.isPickup) "Recojo" else "Entrega",
                            value = order.deliveryAddress
                        )
                        if (!paymentName.isNullOrBlank()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            SummaryRow(label = "Pago", value = paymentName)
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        SummaryRow(
                            label = "Total (IGV inc.)",
                            value = String.format(Locale.US, "S/. %.2f", order.total),
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            emphasized = true
                        )
                    }
                }

                if (!paymentInstructions.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InlineMessage(
                        message = "Cómo pagar: $paymentInstructions",
                        type = FeedbackType.INFO
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onContinueShopping,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(text = "Seguir comprando", fontWeight = FontWeight.Bold)
                }

                if (canViewOrders) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onViewOrders,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(text = "Ver mis pedidos", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
