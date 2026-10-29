package com.hardbuntu.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.util.Patterns
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hardbuntu.app.ui.components.AnimatedInlineMessage
import com.hardbuntu.app.ui.components.FeedbackType
import com.hardbuntu.app.ui.components.InlineMessage
import com.hardbuntu.app.ui.theme.OrangeOnPrimaryContainerDark
import com.hardbuntu.app.ui.theme.OrangePrimaryContainerDark
import com.hardbuntu.app.ui.theme.OrangePrimaryLight
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import kotlin.math.roundToInt

@Composable
fun LoginScreen(
    viewModel: StoreViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotSheet by remember { mutableStateOf(false) }

    // Tras registrarse con confirmación pendiente, volver al modo de ingreso
    LaunchedEffect(uiState.authInfo) {
        if (uiState.authInfo != null) {
            isRegisterMode = false
            password = ""
        }
    }

    // Pequeña sacudida del formulario cuando aparece un error
    val shake = remember { Animatable(0f) }
    LaunchedEffect(uiState.authError) {
        if (uiState.authError != null) {
            repeat(3) {
                shake.animateTo(10f, tween(durationMillis = 45))
                shake.animateTo(-10f, tween(durationMillis = 45))
            }
            shake.animateTo(0f, tween(durationMillis = 45))
        }
    }

    fun submit() {
        focusManager.clearFocus()
        if (isRegisterMode) {
            viewModel.register(email, password, onLoginSuccess)
        } else {
            viewModel.login(email, password, onLoginSuccess)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    if (showForgotSheet) {
        ForgotPasswordSheet(
            initialEmail = email,
            onSend = { address, onResult -> viewModel.requestPasswordReset(address, onResult) },
            onDismiss = { showForgotSheet = false }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        LoginHero()

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp)
                .offset { IntOffset(shake.value.roundToInt(), 0) }
        ) {
            AnimatedContent(targetState = isRegisterMode, label = "authHeading") { registering ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (registering) "Crea tu cuenta" else "Bienvenido de vuelta",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (registering) {
                            "Regístrate para guardar tus pedidos y comprar más rápido."
                        } else {
                            "Ingresa para ver tu historial de pedidos y comprar más rápido."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (uiState.authError != null) viewModel.clearAuthError()
                },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                singleLine = true,
                isError = uiState.authError != null,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (uiState.authError != null) viewModel.clearAuthError()
                },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                },
                supportingText = if (isRegisterMode) {
                    { Text("Mínimo 6 caracteres") }
                } else null,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                isError = uiState.authError != null,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth()
            )

            if (!isRegisterMode) {
                TextButton(
                    onClick = { showForgotSheet = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "¿Olvidaste tu contraseña?",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Errores del servidor o validaciones (rojo) y avisos informativos (azul-verdoso)
            AnimatedInlineMessage(message = uiState.authError, type = FeedbackType.ERROR)
            AnimatedInlineMessage(message = uiState.authInfo, type = FeedbackType.SUCCESS)

            Button(
                onClick = { submit() },
                enabled = !uiState.authLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (uiState.authLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cambio entre ingresar y registrarse
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isRegisterMode) "¿Ya tienes cuenta?" else "¿Eres nuevo en Hardbuntu?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        viewModel.clearAuthError()
                    },
                    enabled = !uiState.authLoading
                ) {
                    Text(
                        text = if (isRegisterMode) "Inicia sesión" else "Regístrate",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            TextButton(
                onClick = {
                    viewModel.continueAsGuest()
                    onLoginSuccess()
                },
                enabled = !uiState.authLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Explorar como invitado",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/** Cabecera de marca: degradado naranja con círculos decorativos y borde inferior curvo. */
@Composable
internal fun LoginHero(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val gradientStart = if (dark) OrangePrimaryContainerDark else OrangePrimaryLight
    val gradientEnd = if (dark) Color(0xFF2A0E00) else OrangePrimaryContainerDark
    val onHero = if (dark) OrangeOnPrimaryContainerDark else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 44.dp, bottomEnd = 44.dp))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.95f, size.height * 0.02f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.width * 0.36f,
                    center = Offset(size.width * 0.02f, size.height * 0.98f)
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 36.dp, bottom = 40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Build,
                    contentDescription = "Logo Hardbuntu",
                    tint = onHero,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Hardbuntu",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = onHero
            )
            Text(
                text = "Ferretería Industrial & Construcción",
                style = MaterialTheme.typography.bodyMedium,
                color = onHero.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Panel para pedir el correo de recuperación de contraseña. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordSheet(
    initialEmail: String,
    onSend: (email: String, onResult: (Result<Unit>) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var email by remember { mutableStateOf(initialEmail) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    fun send() {
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error = "Ingresa un correo electrónico válido"
            return
        }
        loading = true
        error = null
        onSend(email) { result ->
            loading = false
            result
                .onSuccess { sent = true }
                .onFailure { error = it.message ?: "No se pudo enviar el correo" }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Recupera tu contraseña",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (sent) {
                InlineMessage(
                    message = "Si ${email.trim()} tiene una cuenta, te llegará un correo con el enlace para crear una " +
                        "nueva contraseña. Revisa también la carpeta de spam.",
                    type = FeedbackType.SUCCESS
                )
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Entendido", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Ingresa el correo de tu cuenta y te enviaremos un enlace para crear una nueva contraseña.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = null
                    },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { send() }),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedInlineMessage(message = error, type = FeedbackType.ERROR)
                Button(
                    onClick = { send() },
                    enabled = !loading,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar enlace", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
