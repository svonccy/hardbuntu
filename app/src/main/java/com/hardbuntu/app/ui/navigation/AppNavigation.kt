package com.hardbuntu.app.ui.navigation

import androidx.compose.animation.Crossfade
import com.hardbuntu.app.ui.screens.StoreInfoScreen
import com.hardbuntu.app.ui.screens.ResetPasswordScreen
import com.hardbuntu.app.ui.screens.EditProfileScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hardbuntu.app.ui.screens.CartScreen
import com.hardbuntu.app.ui.screens.CategoriesScreen
import com.hardbuntu.app.ui.screens.HomeScreen
import com.hardbuntu.app.ui.screens.LoginScreen
import com.hardbuntu.app.ui.screens.ProductDetailScreen
import com.hardbuntu.app.ui.screens.ProfileScreen
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import kotlinx.serialization.Serializable

// Rutas tipadas (Type-Safe Navigation oficial de Navigation Compose 2.8+)
@Serializable
data object LoginRoute

@Serializable
data class MainShellRoute(val initialTab: Int = 0)

@Serializable
data class DetailRoute(val productId: String)

@Serializable
data object ResetPasswordRoute

@Serializable
data object EditProfileRoute

@Serializable
data object StoreInfoRoute

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    storeViewModel: StoreViewModel = viewModel(factory = StoreViewModel.Factory),
    modifier: Modifier = Modifier
) {
    val uiState by storeViewModel.uiState.collectAsStateWithLifecycle()

    // Esperar a que Supabase restaure la sesión guardada antes de elegir la pantalla inicial
    if (!uiState.authReady) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // Se calcula una sola vez: los cambios posteriores de sesión se manejan navegando
    val startDestination: Any = remember {
        if (uiState.currentUser != null || uiState.isGuest) MainShellRoute(0) else LoginRoute
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<LoginRoute> {
            LoginScreen(
                viewModel = storeViewModel,
                onLoginSuccess = {
                    navController.navigate(MainShellRoute(0)) {
                        popUpTo<LoginRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<MainShellRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<MainShellRoute>()
            MainStoreShell(
                initialTab = route.initialTab,
                storeViewModel = storeViewModel,
                navController = navController
            )
        }

        composable<ResetPasswordRoute> {
            // Salir sin cambiar la contraseña también cierra el modo de recuperación
            BackHandler {
                storeViewModel.dismissPasswordRecovery()
                navController.popBackStack()
            }
            ResetPasswordScreen(
                viewModel = storeViewModel,
                onDone = {
                    navController.navigate(MainShellRoute(0)) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }

        composable<EditProfileRoute> {
            EditProfileScreen(
                viewModel = storeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<StoreInfoRoute> {
            StoreInfoScreen(
                viewModel = storeViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            ProductDetailScreen(
                productId = route.productId,
                viewModel = storeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCart = {
                    navController.navigate(MainShellRoute(initialTab = 2)) {
                        popUpTo<MainShellRoute> { inclusive = true }
                    }
                }
            )
        }
    }

    // El usuario abrió el enlace de "recuperar contraseña" del correo: pedir la nueva contraseña
    LaunchedEffect(uiState.passwordRecovery) {
        if (uiState.passwordRecovery) {
            navController.navigate(ResetPasswordRoute) { launchSingleTop = true }
        }
    }

    // Cuenta confirmada desde el correo: la sesión llega estando en el login, así que se entra a la tienda
    LaunchedEffect(uiState.currentUser?.id) {
        val onLogin = navController.currentBackStackEntry?.destination?.hasRoute<LoginRoute>() == true
        if (uiState.currentUser != null && !uiState.passwordRecovery && onLogin) {
            navController.navigate(MainShellRoute(0)) {
                popUpTo<LoginRoute> { inclusive = true }
            }
        }
    }
}

/**
 * Shell Principal Adaptativo con Material 3:
 * - Teléfonos (< 600dp): Barra inferior NavigationBar con elevación tonal.
 * - Tablets / Paisaje (>= 600dp): Barra lateral NavigationRail anclada.
 * - Transiciones animadas suaves (Crossfade) entre pestañas.
 */
@Composable
fun MainStoreShell(
    initialTab: Int,
    storeViewModel: StoreViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val uiState by storeViewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Diseño para Pantallas Grandes / Tablets: NavigationRail Lateral
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    header = {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(44.dp)
                                .padding(2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = "Hardbuntu Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Inicio"
                            )
                        },
                        label = { Text("Inicio") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.Category else Icons.Outlined.Category,
                                contentDescription = "Categorías"
                            )
                        },
                        label = { Text("Categorías") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationRailItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.cartTotalCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(uiState.cartTotalCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                    contentDescription = "Carrito"
                                )
                            }
                        },
                        label = { Text("Carrito") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )

                    NavigationRailItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Perfil"
                            )
                        },
                        label = { Text("Perfil") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                // Contenido principal en pantalla grande con animación de transición
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TabContent(
                        selectedTab = selectedTab,
                        storeViewModel = storeViewModel,
                        navController = navController,
                        onTabChange = { selectedTab = it }
                    )
                }
            }
        } else {
            // Diseño para Teléfonos Móviles: NavigationBar Inferior
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 3.dp
                    ) {
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Inicio"
                                )
                            },
                            label = { Text("Inicio") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.Category else Icons.Outlined.Category,
                                    contentDescription = "Categorías"
                                )
                            },
                            label = { Text("Categorías") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.cartTotalCount > 0) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ) {
                                                Text(uiState.cartTotalCount.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedTab == 2) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                        contentDescription = "Carrito"
                                    )
                                }
                            },
                            label = { Text("Carrito") },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person,
                                    contentDescription = "Perfil"
                                )
                            },
                            label = { Text("Perfil") },
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    TabContent(
                        selectedTab = selectedTab,
                        storeViewModel = storeViewModel,
                        navController = navController,
                        onTabChange = { selectedTab = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Int,
    storeViewModel: StoreViewModel,
    navController: NavHostController,
    onTabChange: (Int) -> Unit
) {
    Crossfade(
        targetState = selectedTab,
        animationSpec = tween(durationMillis = 240),
        label = "tabCrossfade"
    ) { tab ->
        when (tab) {
            0 -> HomeScreen(
                viewModel = storeViewModel,
                onNavigateToDetail = { productId ->
                    navController.navigate(DetailRoute(productId))
                },
                onNavigateToCart = {
                    onTabChange(2)
                }
            )
            1 -> CategoriesScreen(
                viewModel = storeViewModel,
                onCategorySelected = {
                    onTabChange(0)
                }
            )
            2 -> CartScreen(
                viewModel = storeViewModel,
                onNavigateBack = {
                    onTabChange(0)
                },
                onNavigateHome = {
                    onTabChange(0)
                },
                onNavigateToOrders = {
                    onTabChange(3)
                }
            )
            3 -> ProfileScreen(
                viewModel = storeViewModel,
                onNavigateToEditProfile = { navController.navigate(EditProfileRoute) },
                onNavigateToStoreInfo = { navController.navigate(StoreInfoRoute) },
                onNavigateToCart = { onTabChange(2) },
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo<MainShellRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}
