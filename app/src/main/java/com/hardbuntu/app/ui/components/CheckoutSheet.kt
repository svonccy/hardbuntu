package com.hardbuntu.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.ui.viewmodel.StoreUiState
import com.hardbuntu.app.util.isValidPhone
import java.util.Locale

/** Datos que el cliente confirmó en el panel de pago. */
data class CheckoutRequest(
    val name: String,
    val phone: String,
    val address: String,
    val fulfillmentType: String,
    val paymentMethod: String?,
    val notes: String?,
    val saveAddress: Boolean
)

private fun money(value: Double) = String.format(Locale.US, "S/. %.2f", value)

/**
 * Panel de finalización del pedido: entrega o recojo, datos de contacto, forma de pago y resumen con envío.
 * Las validaciones y errores se muestran dentro del panel (un snackbar quedaría detrás).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CheckoutSheet(
    state: StoreUiState,
    onDismiss: () -> Unit,
    onSubmit: (request: CheckoutRequest, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val settings = state.storeSettings
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fulfillmentOptions = buildList {
        if (settings.deliveryEnabled) add(Order.FULFILLMENT_DELIVERY)
        if (settings.pickupEnabled) add(Order.FULFILLMENT_PICKUP)
    }
    var fulfillment by remember { mutableStateOf(fulfillmentOptions.firstOrNull() ?: Order.FULFILLMENT_DELIVERY) }

    var name by remember {
        mutableStateOf(
            state.profile?.fullName?.takeIf { it.isNotBlank() }
                ?: state.currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: ""
        )
    }
    var phone by remember { mutableStateOf(state.profile?.phone.orEmpty()) }
    var address by remember {
        mutableStateOf(state.addresses.firstOrNull { it.isDefault }?.address ?: state.addresses.firstOrNull()?.address ?: "")
    }
    var notes by remember { mutableStateOf("") }
    var saveAddress by remember { mutableStateOf(true) }
    var paymentCode by remember { mutableStateOf(state.paymentMethods.firstOrNull()?.code) }

    // El perfil, las direcciones y las formas de pago pueden terminar de cargar con el panel ya abierto
    LaunchedEffect(state.profile) {
        if (name.isBlank()) name = state.profile?.fullName.orEmpty()
        if (phone.isBlank()) phone = state.profile?.phone.orEmpty()
    }
    LaunchedEffect(state.addresses) {
        if (address.isBlank()) {
            address = state.addresses.firstOrNull { it.isDefault }?.address ?: state.addresses.firstOrNull()?.address ?: ""
        }
    }
    LaunchedEffect(state.paymentMethods) {
        if (paymentCode == null) paymentCode = state.paymentMethods.firstOrNull()?.code
    }

    var message by remember { mutableStateOf<String?>(null) }
    var messageType by remember { mutableStateOf(FeedbackType.WARNING) }
    var showFieldErrors by remember { mutableStateOf(false) }

    val isDelivery = fulfillment == Order.FULFILLMENT_DELIVERY
    val deliveryFee = if (isDelivery) settings.deliveryFeeFor(state.total) else 0.0
    val grandTotal = state.total + deliveryFee
    val noFulfillment = fulfillmentOptions.isEmpty()
    val addressIsSaved = state.addresses.any { it.address.equals(address.trim(), ignoreCase = true) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )

    fun submit() {
        showFieldErrors = true
        val error = when {
            noFulfillment -> "Por ahora la tienda no tiene entregas habilitadas"
            name.isBlank() || phone.isBlank() || (isDelivery && address.isBlank()) ->
                "Completa los campos obligatorios marcados con *"
            !isValidPhone(phone) -> "Ingresa un número de teléfono válido"
            state.paymentMethods.isNotEmpty() && paymentCode == null -> "Selecciona una forma de pago"
            else -> null
        }
        if (error != null) {
            messageType = FeedbackType.WARNING
            message = error
            return
        }
        message = null
        onSubmit(
            CheckoutRequest(
                name = name,
                phone = phone,
                // En recojo no hay dirección: el servidor registra "Recojo en tienda"
                address = if (isDelivery) address else "",
                fulfillmentType = fulfillment,
                paymentMethod = paymentCode,
                notes = notes.ifBlank { null },
                saveAddress = saveAddress && isDelivery && state.currentUser != null && !addressIsSaved
            ),
            { onDismiss() },
            { error ->
                messageType = FeedbackType.ERROR
                message = error
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Finalizar pedido",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // --- Entrega o recojo ---
            SectionLabel("¿Cómo lo recibes?")
            if (noFulfillment) {
                InlineMessage(
                    message = "Por ahora la tienda no tiene entregas ni recojo habilitados. Intenta más tarde.",
                    type = FeedbackType.WARNING
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (Order.FULFILLMENT_DELIVERY in fulfillmentOptions) {
                    val fee = settings.deliveryFeeFor(state.total)
                    OptionCard(
                        selected = isDelivery,
                        title = "Delivery",
                        subtitle = when {
                            fee <= 0.0 -> "Sin costo de envío"
                            else -> "Envío ${money(fee)}"
                        } + (settings.freeDeliveryFrom?.takeIf { fee > 0.0 }?.let { " · Gratis desde ${money(it)}" } ?: ""),
                        icon = Icons.Outlined.LocalShipping,
                        onClick = {
                            fulfillment = Order.FULFILLMENT_DELIVERY
                            message = null
                        }
                    )
                }
                if (Order.FULFILLMENT_PICKUP in fulfillmentOptions) {
                    OptionCard(
                        selected = !isDelivery,
                        title = "Recojo en tienda",
                        subtitle = settings.address?.takeIf { it.isNotBlank() } ?: "Sin costo de envío",
                        icon = Icons.Outlined.Storefront,
                        onClick = {
                            fulfillment = Order.FULFILLMENT_PICKUP
                            message = null
                        }
                    )
                }
            }

            if (isDelivery) {
                Spacer(modifier = Modifier.height(12.dp))
                if (state.addresses.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.addresses.forEach { saved ->
                            FilterChip(
                                selected = saved.address.equals(address.trim(), ignoreCase = true),
                                onClick = {
                                    address = saved.address
                                    message = null
                                },
                                label = { Text(saved.label) },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = {
                        address = it
                        message = null
                    },
                    label = { Text("Dirección de entrega / obra *") },
                    leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
                    isError = showFieldErrors && address.isBlank(),
                    supportingText = if (showFieldErrors && address.isBlank()) {
                        { Text("Ingresa la dirección de entrega") }
                    } else null,
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.currentUser != null && address.isNotBlank() && !addressIsSaved) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = saveAddress, onCheckedChange = { saveAddress = it })
                        Text(
                            text = "Guardar esta dirección en mi cuenta",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val pickupDetails = listOfNotNull(
                    settings.address?.takeIf { it.isNotBlank() },
                    settings.openingHours?.takeIf { it.isNotBlank() }?.let { "Horario: $it" }
                )
                if (pickupDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    InlineMessage(
                        message = "Recoge tu pedido en:\n" + pickupDetails.joinToString("\n"),
                        type = FeedbackType.INFO
                    )
                }
            }

            // --- Contacto ---
            SectionLabel("Datos de contacto")
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    message = null
                },
                label = { Text("Nombre completo / Razón social *") },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                isError = showFieldErrors && name.isBlank(),
                supportingText = if (showFieldErrors && name.isBlank()) {
                    { Text("Ingresa tu nombre o razón social") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    message = null
                },
                label = { Text("Teléfono de contacto (WhatsApp) *") },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                isError = showFieldErrors && !isValidPhone(phone),
                supportingText = if (showFieldErrors && !isValidPhone(phone)) {
                    { Text(if (phone.isBlank()) "Ingresa un teléfono de contacto" else "Usa entre 7 y 15 dígitos, ej. 987654321") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Instrucciones (opcional)") },
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            // --- Forma de pago ---
            if (state.paymentMethods.isNotEmpty()) {
                SectionLabel("Forma de pago")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.paymentMethods.forEach { method ->
                        OptionCard(
                            selected = paymentCode == method.code,
                            title = method.name,
                            subtitle = method.instructions?.takeIf { it.isNotBlank() },
                            icon = paymentIcon(method.code),
                            onClick = {
                                paymentCode = method.code
                                message = null
                            }
                        )
                    }
                }
            }

            // --- Resumen ---
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryLine("Productos", money(state.total))
                    SummaryLine(
                        label = if (isDelivery) "Envío" else "Recojo en tienda",
                        value = if (isDelivery && deliveryFee > 0.0) money(deliveryFee) else "Gratis"
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Total (IGV inc.)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = money(grandTotal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedInlineMessage(
                message = message,
                type = messageType,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
            )

            Button(
                onClick = { submit() },
                enabled = !state.isCheckingOut && !noFulfillment,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isCheckingOut) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Confirmar y enviar pedido",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun paymentIcon(code: String): ImageVector = when (code) {
    "cash" -> Icons.Outlined.Payments
    "transfer" -> Icons.Outlined.AccountBalance
    "yape", "plin" -> Icons.Outlined.PhoneAndroid
    else -> Icons.Outlined.CreditCard
}

/** Tarjeta seleccionable (entrega, forma de pago). */
@Composable
private fun OptionCard(
    selected: Boolean,
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}
