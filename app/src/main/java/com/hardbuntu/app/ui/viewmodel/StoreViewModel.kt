package com.hardbuntu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hardbuntu.app.data.model.CartItem
import com.hardbuntu.app.data.model.Category
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.data.model.OrderItem
import com.hardbuntu.app.data.model.Product
import com.hardbuntu.app.data.repository.HardbuntuRepository
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

data class StoreUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val cartItems: List<CartItem> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val isCheckingOut: Boolean = false,
    val checkoutSuccessOrder: Order? = null,
    // Estado de Autenticación Supabase
    val currentUser: UserInfo? = null,
    val isGuest: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null
) {
    val filteredProducts: List<Product>
        get() = products.filter { product ->
            val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
            val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.brand.contains(searchQuery, ignoreCase = true) ||
                product.sku.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }

    val cartTotalCount: Int
        get() = cartItems.sumOf { it.quantity }

    val subtotal: Double
        get() = round(cartItems.sumOf { it.subtotal } * 100.0) / 100.0

    // IGV 18% (Impuesto General a las Ventas)
    val igv: Double
        get() = round(subtotal * 0.18 * 100.0) / 100.0

    val total: Double
        get() = round((subtotal + igv) * 100.0) / 100.0
}

class StoreViewModel(
    private val repository: HardbuntuRepository = HardbuntuRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        val existingUser = repository.getCurrentUser()
        _uiState.update { it.copy(currentUser = existingUser) }
        loadInitialData()
    }

    // --- Métodos de Autenticación Supabase ---

    fun login(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Ingresa tu correo y contraseña") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null) }
            val result = repository.signIn(email.trim(), password)
            result.onSuccess { user ->
                _uiState.update { it.copy(currentUser = user, isGuest = false, authLoading = false, authError = null) }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(authLoading = false, authError = err.localizedMessage ?: "Credenciales inválidas") }
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Ingresa tu correo y contraseña") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(authError = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null) }
            val result = repository.signUp(email.trim(), password)
            result.onSuccess { user ->
                _uiState.update { it.copy(currentUser = user, isGuest = false, authLoading = false, authError = null) }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(authLoading = false, authError = err.localizedMessage ?: "Error en el registro") }
            }
        }
    }

    fun continueAsGuest() {
        _uiState.update { it.copy(isGuest = true, authError = null) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update { it.copy(currentUser = null, isGuest = false) }
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null) }
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val categoriesResult = repository.getCategories()
            val productsResult = repository.getProducts()
            val ordersResult = repository.getOrders()

            if (categoriesResult.isSuccess && productsResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        categories = categoriesResult.getOrDefault(emptyList()),
                        products = productsResult.getOrDefault(emptyList()),
                        recentOrders = ordersResult.getOrDefault(emptyList()),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                val error = categoriesResult.exceptionOrNull()?.localizedMessage
                    ?: productsResult.exceptionOrNull()?.localizedMessage
                    ?: "Error desconocido al conectar con Supabase"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst { it.product.id == product.id }
            val updatedList = if (existingIndex >= 0) {
                state.cartItems.toMutableList().apply {
                    val current = this[existingIndex]
                    this[existingIndex] = current.copy(quantity = current.quantity + quantity)
                }
            } else {
                state.cartItems + CartItem(product = product, quantity = quantity)
            }
            state.copy(cartItems = updatedList)
        }
    }

    fun incrementQuantity(productId: String) {
        _uiState.update { state ->
            val updated = state.cartItems.map {
                if (it.product.id == productId) it.copy(quantity = it.quantity + 1) else it
            }
            state.copy(cartItems = updated)
        }
    }

    fun decrementQuantity(productId: String) {
        _uiState.update { state ->
            val updated = state.cartItems.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null
                } else it
            }
            state.copy(cartItems = updated)
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            state.copy(cartItems = state.cartItems.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    fun checkout(
        customerName: String,
        customerPhone: String = "999888777",
        customerAddress: String,
        customerEmail: String? = null,
        notes: String? = null,
        onSuccess: (Order) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) {
            onError("El carrito de compras está vacío")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true, errorMessage = null) }

            val order = Order(
                customerName = customerName,
                customerEmail = customerEmail,
                customerPhone = customerPhone,
                deliveryAddress = customerAddress,
                subtotal = currentState.subtotal,
                tax = currentState.igv,
                total = currentState.total,
                status = "pending",
                notes = notes
            )

            val items = currentState.cartItems.map { cartItem ->
                OrderItem(
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    productPrice = cartItem.product.price,
                    quantity = cartItem.quantity,
                    subtotal = cartItem.subtotal
                )
            }

            val result = repository.createOrder(order, items)
            result.onSuccess { createdOrder ->
                _uiState.update {
                    it.copy(
                        isCheckingOut = false,
                        cartItems = emptyList(),
                        recentOrders = listOf(createdOrder) + it.recentOrders,
                        checkoutSuccessOrder = createdOrder
                    )
                }
                onSuccess(createdOrder)
            }.onFailure { error ->
                val msg = error.localizedMessage ?: "Error al registrar pedido en Supabase"
                _uiState.update { it.copy(isCheckingOut = false, errorMessage = msg) }
                onError(msg)
            }
        }
    }

    fun dismissCheckoutSuccessDialog() {
        _uiState.update { it.copy(checkoutSuccessOrder = null) }
    }
}
