package com.hardbuntu.app.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import com.hardbuntu.app.util.ContactActions
import com.hardbuntu.app.data.model.OrderStatus
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import com.hardbuntu.app.ui.components.FeedbackHost
import com.hardbuntu.app.ui.components.FeedbackType
import com.hardbuntu.app.ui.components.InlineMessage
import com.hardbuntu.app.ui.components.showFeedback
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: StoreViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToStoreInfo: () -> Unit,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.currentUser
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settings = uiState.storeSettings
    var cancellingOrderId by remember { mutableStateOf<String?>(null) }

    fun notify(message: String, type: FeedbackType) {
        scope.launch { snackbarHostState.showFeedback(message, type) }
    }

    Scaffold(
        snackbarHost = { FeedbackHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mi Perfil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (user != null) {
                // Tarjeta de Usuario Autenticado M3
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = user.email ?: "Cliente Hardbuntu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val isVerified = user.emailConfirmedAt != null
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isVerified) "Correo verificado" else "Correo sin verificar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isVerified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "ID: ${user.id.take(8).uppercase(Locale.getDefault())}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                // Mi cuenta: datos personales y direcciones guardadas
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            ProfileOptionRow(
                                icon = Icons.Outlined.Person,
                                title = "Mis datos y direcciones",
                                subtitle = if (uiState.addresses.isEmpty()) {
                                    "Guarda tu nombre, teléfono y direcciones de entrega"
                                } else {
                                    "${uiState.addresses.size} dirección(es) guardada(s)"
                                },
                                onClick = onNavigateToEditProfile
                            )
                        }
                    }
                }

                // Sección: Historial de Pedidos y Cotizaciones
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Mis Pedidos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp, top = 4.dp)
                        )
                        if (uiState.ordersLoading) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            IconButton(onClick = viewModel::loadMyOrders) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Actualizar pedidos",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (uiState.ordersError != null) {
                    item {
                        InlineMessage(
                            message = "No pudimos cargar tus pedidos. ${uiState.ordersError}",
                            type = FeedbackType.ERROR
                        )
                    }
                } else if (uiState.recentOrders.isEmpty() && !uiState.ordersLoading) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Aún no registras pedidos o cotizaciones.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentOrders, key = { it.id ?: it.hashCode().toString() }) { order ->
                        ExpandableOrderCard(
                            order = order,
                            paymentName = uiState.paymentMethodByCode(order.paymentMethod)?.name,
                            cancelling = cancellingOrderId == order.id,
                            onCancel = {
                                val orderId = order.id
                                if (orderId != null) {
                                    cancellingOrderId = orderId
                                    viewModel.cancelOrder(orderId) { result ->
                                        cancellingOrderId = null
                                        result
                                            .onSuccess { notify("Pedido cancelado", FeedbackType.INFO) }
                                            .onFailure { notify(it.message ?: "No se pudo cancelar el pedido", FeedbackType.ERROR) }
                                    }
                                }
                            },
                            onReorder = {
                                val result = viewModel.reorder(order)
                                scope.launch {
                                    if (result.added > 0) {
                                        val skippedNote = if (result.skipped > 0) " (${result.skipped} sin stock)" else ""
                                        val action = snackbarHostState.showFeedback(
                                            message = "Agregamos ${result.added} producto(s) a tu carrito$skippedNote",
                                            type = FeedbackType.SUCCESS,
                                            actionLabel = "Ver carrito"
                                        )
                                        if (action == SnackbarResult.ActionPerformed) onNavigateToCart()
                                    } else {
                                        snackbarHostState.showFeedback(
                                            "Los productos de este pedido ya no tienen stock disponible",
                                            FeedbackType.WARNING
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Opciones y Acciones Rápidas
                item {
                    Text(
                        text = "Tienda y ayuda",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            ProfileOptionRow(
                                icon = Icons.Outlined.Storefront,
                                title = "Nuestra tienda",
                                subtitle = "Ubicación, horario y formas de contacto",
                                onClick = onNavigateToStoreInfo
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            ProfileOptionRow(
                                icon = Icons.Outlined.VerifiedUser,
                                title = "Garantía Industrial Hardbuntu",
                                subtitle = "Términos oficiales, cambio de repuestos y soporte técnico",
                                onClick = {
                                    val body = "Cuenta: ${user.email.orEmpty()}\nPedido: \nProducto: \nDetalle del problema: "
                                    val storeEmail = settings.email
                                    val storeWhatsApp = settings.whatsapp
                                    val launched = when {
                                        !storeEmail.isNullOrBlank() ->
                                            ContactActions.sendEmail(context, storeEmail, "Consulta de garantía", body)
                                        !storeWhatsApp.isNullOrBlank() ->
                                            ContactActions.openWhatsApp(context, storeWhatsApp, "Consulta de garantía.\n$body")
                                        else -> null
                                    }
                                    when (launched) {
                                        null -> notify("El local aún no configuró un canal de contacto", FeedbackType.WARNING)
                                        false -> notify("No hay una app disponible para contactar al local", FeedbackType.WARNING)
                                        true -> Unit
                                    }
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            ProfileOptionRow(
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                title = "Asistencia Ferretera Especializada",
                                subtitle = "Contacto directo con asesores de venta industrial",
                                onClick = {
                                    val storeWhatsApp = settings.whatsapp
                                    val storePhone = settings.phone
                                    val launched = when {
                                        !storeWhatsApp.isNullOrBlank() -> ContactActions.openWhatsApp(
                                            context,
                                            storeWhatsApp,
                                            "Hola, necesito asesoría de ${settings.storeName}. Mi cuenta: ${user.email.orEmpty()}"
                                        )
                                        !storePhone.isNullOrBlank() -> ContactActions.dial(context, storePhone)
                                        else -> null
                                    }
                                    when (launched) {
                                        null -> notify("El local aún no configuró un canal de contacto", FeedbackType.WARNING)
                                        false -> notify("No hay una app disponible para contactar al local", FeedbackType.WARNING)
                                        true -> Unit
                                    }
                                }
                            )
                        }
                    }
                }

                // Botón de Cerrar Sesión
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onNavigateToLogin()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cerrar sesión",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Tarjeta de Modo Invitado M3
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Navegando como Invitado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Inicia sesión con tu cuenta de Hardbuntu para guardar tu historial de cotizaciones, vincular pedidos y agilizar tus compras en obra.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = onNavigateToLogin,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = "Iniciar Sesión o Registrarse",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        elevation = CardDefaults.elevatedCardElevation(1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            ProfileOptionRow(
                                icon = Icons.Outlined.Storefront,
                                title = "Nuestra tienda",
                                subtitle = "Ubicación, horario y formas de contacto",
                                onClick = onNavigateToStoreInfo
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de pedido expandible: seguimiento por estados, detalle, volver a comprar y cancelar (si está pendiente).
 */
@Composable
private fun ExpandableOrderCard(
    order: Order,
    paymentName: String?,
    cancelling: Boolean,
    onCancel: () -> Unit,
    onReorder: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    val status = order.orderStatus

    val statusColor = when (status) {
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondary
        OrderStatus.PROCESSING -> MaterialTheme.colorScheme.primary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
        OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiary
    }
    val statusBg = when (status) {
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        OrderStatus.PROCESSING -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
        OrderStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancelar pedido", fontWeight = FontWeight.Bold) },
            text = { Text("¿Quieres cancelar este pedido? Los productos volverán a estar disponibles.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onCancel()
                }) {
                    Text("Sí, cancelar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text("No") }
            }
        )
    }

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Orden #${order.id?.take(8)?.uppercase(Locale.getDefault()) ?: "NUEVA"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = String.format(Locale.US, "S/. %.2f", order.total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = status.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 14.dp)
                    )

                    OrderTimeline(status = status)

                    Spacer(modifier = Modifier.height(14.dp))

                    order.createdAt?.let { createdAt ->
                        Text(
                            text = "Fecha: ${formatOrderDate(createdAt)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        text = if (order.isPickup) "Entrega: Recojo en tienda" else "Entrega: Delivery",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Destinatario: ${order.customerName}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Teléfono: ${order.customerPhone}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (!order.isPickup) {
                        Text(
                            text = "Dirección: ${order.deliveryAddress}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (!paymentName.isNullOrBlank()) {
                        Text(
                            text = "Pago: $paymentName",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    order.notes?.let {
                        Text(
                            text = "Notas: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (order.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Productos",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        order.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "${item.quantity} x ${item.productName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = String.format(Locale.US, "S/. %.2f", item.subtotal),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "Subtotal: S/. %.2f | IGV: S/. %.2f", order.subtotal, order.tax),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.deliveryFee > 0.0) {
                        Text(
                            text = String.format(Locale.US, "Envío incluido: S/. %.2f", order.deliveryFee),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (order.items.isNotEmpty()) {
                            OutlinedButton(
                                onClick = onReorder,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Volver a comprar", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (status == OrderStatus.PENDING) {
                            TextButton(
                                onClick = { confirmCancel = true },
                                enabled = !cancelling
                            ) {
                                if (cancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        text = "Cancelar pedido",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Línea de tiempo del pedido: Pendiente, En proceso y Entregado (los cancelados muestran un aviso). */
@Composable
private fun OrderTimeline(status: OrderStatus) {
    if (status == OrderStatus.CANCELLED) {
        InlineMessage(message = "Este pedido fue cancelado.", type = FeedbackType.ERROR)
        return
    }

    val steps = listOf(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.DELIVERED)
    val reached = steps.indexOf(status)
    val activeLine = MaterialTheme.colorScheme.primary
    val idleLine = MaterialTheme.colorScheme.outlineVariant

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        steps.forEachIndexed { index, step ->
            val done = index <= reached
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                when {
                                    index == 0 -> Color.Transparent
                                    index <= reached -> activeLine
                                    else -> idleLine
                                }
                            )
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (done) activeLine else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, if (done) activeLine else idleLine, CircleShape)
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                when {
                                    index == steps.lastIndex -> Color.Transparent
                                    index < reached -> activeLine
                                    else -> idleLine
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = step.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == reached) FontWeight.Bold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// "2026-10-29T16:05:09.123+00:00" → "29/10/2026 11:05" (hora local del dispositivo)
private fun formatOrderDate(isoDate: String): String = runCatching {
    OffsetDateTime.parse(isoDate)
        .atZoneSameInstant(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}.getOrDefault(isoDate.take(10))

@Composable
private fun ProfileOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
    }
}
