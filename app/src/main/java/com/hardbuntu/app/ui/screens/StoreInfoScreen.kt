package com.hardbuntu.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hardbuntu.app.data.model.StoreSettings
import com.hardbuntu.app.ui.components.FeedbackHost
import com.hardbuntu.app.ui.components.FeedbackType
import com.hardbuntu.app.ui.components.showFeedback
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import com.hardbuntu.app.util.ContactActions
import kotlinx.coroutines.launch
import java.util.Locale

/** Ubicación, horario, contacto y condiciones de entrega del local (datos de la tabla store_settings). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreInfoScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.storeSettings
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun open(launched: Boolean, errorMessage: String) {
        if (!launched) scope.launch { snackbarHostState.showFeedback(errorMessage, FeedbackType.WARNING) }
    }

    Scaffold(
        snackbarHost = { FeedbackHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuestra tienda",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = settings.storeName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            if (!settings.hasPublicInfo) {
                item {
                    Text(
                        text = "El local aún no publicó sus datos de contacto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            settings.address?.takeIf { it.isNotBlank() }?.let { address ->
                item {
                    InfoCard(
                        icon = Icons.Outlined.Place,
                        title = "Dirección",
                        value = address,
                        actionLabel = "Cómo llegar",
                        onAction = { open(ContactActions.openMap(context, address), "No hay una app de mapas instalada") }
                    )
                }
            }
            settings.openingHours?.takeIf { it.isNotBlank() }?.let { hours ->
                item { InfoCard(icon = Icons.Outlined.AccessTime, title = "Horario de atención", value = hours) }
            }
            settings.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                item {
                    InfoCard(
                        icon = Icons.Outlined.Phone,
                        title = "Teléfono",
                        value = phone,
                        actionLabel = "Llamar",
                        onAction = { open(ContactActions.dial(context, phone), "No se pudo abrir el marcador") }
                    )
                }
            }
            settings.whatsapp?.takeIf { it.isNotBlank() }?.let { whatsapp ->
                item {
                    InfoCard(
                        icon = Icons.Outlined.Phone,
                        title = "WhatsApp",
                        value = whatsapp,
                        actionLabel = "Abrir chat",
                        onAction = {
                            open(
                                ContactActions.openWhatsApp(context, whatsapp, "Hola, tengo una consulta sobre ${settings.storeName}."),
                                "No se pudo abrir WhatsApp"
                            )
                        }
                    )
                }
            }
            settings.email?.takeIf { it.isNotBlank() }?.let { email ->
                item {
                    InfoCard(
                        icon = Icons.Outlined.Email,
                        title = "Correo",
                        value = email,
                        actionLabel = "Escribir",
                        onAction = {
                            open(
                                ContactActions.sendEmail(context, email, "Consulta ${settings.storeName}"),
                                "No hay una app de correo instalada"
                            )
                        }
                    )
                }
            }

            item {
                InfoCard(
                    icon = Icons.Outlined.LocalShipping,
                    title = "Entrega",
                    value = deliverySummary(settings)
                )
            }
        }
    }
}

/** Resumen de las opciones de entrega según la configuración del local. */
private fun deliverySummary(settings: StoreSettings): String {
    val lines = mutableListOf<String>()
    if (settings.deliveryEnabled) {
        val fee = settings.deliveryFee
        lines += if (fee > 0.0) {
            String.format(Locale.US, "Delivery: S/. %.2f", fee) +
                (settings.freeDeliveryFrom?.let { String.format(Locale.US, " (gratis desde S/. %.2f)", it) } ?: "")
        } else {
            "Delivery sin costo"
        }
    }
    if (settings.pickupEnabled) lines += "Recojo en tienda disponible"
    return lines.ifEmpty { listOf("Por ahora no hay opciones de entrega habilitadas") }.joinToString("\n")
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (actionLabel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
