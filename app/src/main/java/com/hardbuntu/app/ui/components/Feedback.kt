package com.hardbuntu.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hardbuntu.app.ui.theme.SuccessStockDark
import com.hardbuntu.app.ui.theme.SuccessStockLight
import com.hardbuntu.app.ui.theme.WarningStockDark
import com.hardbuntu.app.ui.theme.WarningStockLight

/** Tipo de mensaje al usuario: define color e icono de forma consistente en toda la app. */
enum class FeedbackType { SUCCESS, ERROR, WARNING, INFO }

@Composable
fun FeedbackType.accentColor(): Color {
    val dark = isSystemInDarkTheme()
    return when (this) {
        FeedbackType.SUCCESS -> if (dark) SuccessStockDark else SuccessStockLight
        FeedbackType.ERROR -> MaterialTheme.colorScheme.error
        FeedbackType.WARNING -> if (dark) WarningStockDark else WarningStockLight
        FeedbackType.INFO -> MaterialTheme.colorScheme.secondary
    }
}

val FeedbackType.icon: ImageVector
    get() = when (this) {
        FeedbackType.SUCCESS -> Icons.Rounded.CheckCircle
        FeedbackType.ERROR -> Icons.Rounded.Error
        FeedbackType.WARNING -> Icons.Rounded.Warning
        FeedbackType.INFO -> Icons.Rounded.Info
    }

/** Contenido de un snackbar tipado (se entrega a [SnackbarHostState.showSnackbar]). */
class FeedbackVisuals(
    override val message: String,
    val type: FeedbackType,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

/**
 * Muestra un mensaje tipado reemplazando el que esté visible, para que no se acumulen
 * avisos en cola cuando el usuario toca varias veces seguidas.
 */
suspend fun SnackbarHostState.showFeedback(
    message: String,
    type: FeedbackType = FeedbackType.INFO,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(FeedbackVisuals(message, type, actionLabel, duration = duration))
}

/** Reemplazo de [SnackbarHost] que dibuja los mensajes con el estilo de Hardbuntu. */
@Composable
fun FeedbackHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        FeedbackSnackbar(data)
    }
}

@Composable
private fun FeedbackSnackbar(data: SnackbarData) {
    val visuals = data.visuals
    val type = (visuals as? FeedbackVisuals)?.type ?: FeedbackType.INFO
    val accent = type.accentColor()

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 560.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f))
            ) {
                Icon(
                    imageVector = type.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
            )

            visuals.actionLabel?.let { label ->
                TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/** Mensaje en línea (dentro del contenido) para validaciones y errores de formularios. */
@Composable
fun InlineMessage(
    message: String,
    type: FeedbackType,
    modifier: Modifier = Modifier
) {
    val accent = type.accentColor()
    val shape = RoundedCornerShape(14.dp)

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .clip(shape)
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.30f), shape)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Igual que [InlineMessage] pero aparece y desaparece con animación cuando [message] cambia a/desde null. */
@Composable
fun AnimatedInlineMessage(
    message: String?,
    type: FeedbackType,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Conserva el último texto para que se vea durante la animación de salida
    var lastMessage by remember { mutableStateOf("") }
    if (message != null && message != lastMessage) lastMessage = message

    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        InlineMessage(
            message = lastMessage,
            type = type,
            modifier = Modifier.padding(contentPadding)
        )
    }
}
