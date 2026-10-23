package com.hardbuntu.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.hardbuntu.app.ui.screens.CartScreen
import com.hardbuntu.app.ui.screens.HomeScreen
import com.hardbuntu.app.ui.screens.ProductDetailScreen
import com.hardbuntu.app.ui.viewmodel.StoreViewModel
import kotlinx.serialization.Serializable

// Rutas tipadas (Type-Safe Navigation oficial de Navigation Compose 2.8+)
@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val productId: String)

@Serializable
data object CartRoute

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    storeViewModel: StoreViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<HomeRoute> {
            HomeScreen(
                viewModel = storeViewModel,
                onNavigateToDetail = { productId ->
                    navController.navigate(DetailRoute(productId))
                },
                onNavigateToCart = {
                    navController.navigate(CartRoute)
                }
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
                    navController.navigate(CartRoute)
                }
            )
        }

        composable<CartRoute> {
            CartScreen(
                viewModel = storeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate(HomeRoute) {
                        popUpTo<HomeRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}
