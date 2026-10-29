package com.hardbuntu.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hardbuntu.app.data.local.CartLocalDataSource
import com.hardbuntu.app.data.local.FavoritesLocalDataSource
import com.hardbuntu.app.data.model.Address
import com.hardbuntu.app.data.model.AddressUpsert
import com.hardbuntu.app.data.model.CartItem
import com.hardbuntu.app.data.model.Category
import com.hardbuntu.app.data.model.Order
import com.hardbuntu.app.data.model.PaymentMethod
import com.hardbuntu.app.data.model.Product
import com.hardbuntu.app.data.model.Profile
import com.hardbuntu.app.data.model.StoreSettings
import com.hardbuntu.app.data.repository.HardbuntuRepository
import com.hardbuntu.app.data.repository.SignUpResult
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

enum class ProductSort(val label: String) {
    RELEVANCE("Relevancia"),
    PRICE_ASC("Precio: menor a mayor"),
    PRICE_DESC("Precio: mayor a menor"),
    NAME_ASC("Nombre: A - Z")
}

/** Resultado de volver a comprar un pedido anterior. */
data class ReorderResult(val added: Int, val skipped: Int)

data class StoreUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val selectedCategoryId: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    // Error de carga del catálogo (los errores de checkout se notifican por callback)
    val errorMessage: String? = null,

    // Filtros y orden
    val sort: ProductSort = ProductSort.RELEVANCE,
    val selectedBrands: Set<String> = emptySet(),
    // null = sin límite de precio
    val priceRange: ClosedFloatingPointRange<Float>? = null,
    val onlyInStock: Boolean = false,
    val onlyFavorites: Boolean = false,
    val favoriteIds: Set<String> = emptySet(),

    // Carrito y pedidos
    val cartItems: List<CartItem> = emptyList(),
    val recentOrders: List<Order> = emptyList(),
    val ordersLoading: Boolean = false,
    val ordersError: String? = null,
    val isCheckingOut: Boolean = false,
    val checkoutSuccessOrder: Order? = null,

    // Datos del local (editables desde Supabase)
    val storeSettings: StoreSettings = StoreSettings(),
    val paymentMethods: List<PaymentMethod> = emptyList(),

    // Cuenta del usuario
    val currentUser: UserInfo? = null,
    val profile: Profile? = null,
    val addresses: List<Address> = emptyList(),
    val profileSaving: Boolean = false,
    val isGuest: Boolean = false,
    // false mientras Supabase restaura la sesión guardada en el dispositivo
    val authReady: Boolean = false,
    val authLoading: Boolean = false,
    val authError: String? = null,
    val authInfo: String? = null,
    // true cuando el usuario abrió el enlace de "recuperar contraseña" del correo
    val passwordRecovery: Boolean = false
) {
    val availableBrands: List<String>
        get() = products.map { it.brand }.distinct().sortedBy { it.lowercase() }

    val priceBounds: ClosedFloatingPointRange<Float>
        get() = if (products.isEmpty()) {
            0f..0f
        } else {
            floor(products.minOf { it.price }).toFloat()..ceil(products.maxOf { it.price }).toFloat()
        }

    val activeFilterCount: Int
        get() = (if (selectedBrands.isNotEmpty()) 1 else 0) +
            (if (priceRange != null) 1 else 0) +
            (if (onlyInStock) 1 else 0) +
            (if (onlyFavorites) 1 else 0)

    val filteredProducts: List<Product>
        get() {
            val filtered = products.filter { product ->
                val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
                val matchesSearch = searchQuery.isBlank() ||
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.brand.contains(searchQuery, ignoreCase = true) ||
                    product.sku.contains(searchQuery, ignoreCase = true)
                val matchesBrand = selectedBrands.isEmpty() || product.brand in selectedBrands
                val matchesPrice = priceRange == null || product.price.toFloat() in priceRange
                val matchesStock = !onlyInStock || product.stock > 0
                val matchesFavorite = !onlyFavorites || product.id in favoriteIds
                matchesCategory && matchesSearch && matchesBrand && matchesPrice && matchesStock && matchesFavorite
            }
            return when (sort) {
                ProductSort.RELEVANCE -> filtered
                ProductSort.PRICE_ASC -> filtered.sortedBy { it.price }
                ProductSort.PRICE_DESC -> filtered.sortedByDescending { it.price }
                ProductSort.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            }
        }

    val cartTotalCount: Int
        get() = cartItems.sumOf { it.quantity }

    // Los precios del catálogo incluyen IGV: el total es la suma de los ítems
    val total: Double
        get() = round(cartItems.sumOf { it.subtotal } * 100.0) / 100.0

    // Base imponible (sin IGV)
    val subtotal: Double
        get() = round(total / 1.18 * 100.0) / 100.0

    // IGV 18% (Impuesto General a las Ventas) contenido en el total
    val igv: Double
        get() = round((total - subtotal) * 100.0) / 100.0

    fun quantityInCart(productId: String): Int =
        cartItems.firstOrNull { it.product.id == productId }?.quantity ?: 0

    fun isFavorite(productId: String): Boolean = productId in favoriteIds

    fun paymentMethodByCode(code: String?): PaymentMethod? = paymentMethods.firstOrNull { it.code == code }
}

class StoreViewModel(
    private val repository: HardbuntuRepository = HardbuntuRepository(),
    private val cartLocalDataSource: CartLocalDataSource? = null,
    private val favoritesLocalDataSource: FavoritesLocalDataSource? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoreUiState())
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        restoreCart()
        restoreFavorites()
        loadInitialData()
    }

    // --- Autenticación Supabase ---

    private fun observeSession() {
        viewModelScope.launch {
            repository.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val userChanged = user?.id != _uiState.value.currentUser?.id
                        _uiState.update { it.copy(currentUser = user, isGuest = false, authReady = true) }
                        if (userChanged) {
                            loadMyOrders()
                            loadProfileData()
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _uiState.update {
                            it.copy(
                                currentUser = null,
                                profile = null,
                                addresses = emptyList(),
                                recentOrders = emptyList(),
                                ordersError = null,
                                authReady = true
                            )
                        }
                    }
                    // Sin conexión para refrescar el token: se mantiene el estado actual
                    is SessionStatus.RefreshFailure -> _uiState.update { it.copy(authReady = true) }
                    SessionStatus.Initializing -> Unit
                }
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Ingresa tu correo y contraseña", authInfo = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, authInfo = null) }
            val result = repository.signIn(email.trim(), password)
            result.onSuccess { user ->
                _uiState.update { it.copy(currentUser = user, isGuest = false, authLoading = false) }
                onSuccess()
            }.onFailure { err ->
                _uiState.update { it.copy(authLoading = false, authError = err.message ?: "Credenciales inválidas") }
            }
        }
    }

    fun register(email: String, password: String, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(authError = "Ingresa tu correo y contraseña", authInfo = null) }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(authError = "La contraseña debe tener al menos 6 caracteres", authInfo = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, authError = null, authInfo = null) }
            val result = repository.signUp(email.trim(), password)
            result.onSuccess { signUpResult ->
                when (signUpResult) {
                    SignUpResult.SIGNED_IN -> {
                        _uiState.update { it.copy(isGuest = false, authLoading = false) }
                        onSuccess()
                    }
                    SignUpResult.CONFIRMATION_REQUIRED -> _uiState.update {
                        it.copy(
                            authLoading = false,
                            authInfo = "Te enviamos un correo de confirmación a ${email.trim()}. " +
                                "Ábrelo, confirma tu cuenta y luego inicia sesión aquí."
                        )
                    }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(authLoading = false, authError = err.message ?: "Error en el registro") }
            }
        }
    }

    fun continueAsGuest() {
        _uiState.update { it.copy(isGuest = true, authError = null, authInfo = null) }
    }

    fun logout() {
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(currentUser = null, isGuest = false, profile = null, addresses = emptyList(), recentOrders = emptyList())
            }
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authError = null, authInfo = null) }
    }

    /** Envía el correo para restablecer la contraseña. El enlace abre la app (hardbuntu://auth). */
    fun requestPasswordReset(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.requestPasswordReset(email.trim()))
        }
    }

    /** La app se abrió desde el enlace de recuperación: pedir la nueva contraseña. */
    fun onPasswordRecoveryLink() {
        _uiState.update { it.copy(passwordRecovery = true) }
    }

    fun updatePassword(newPassword: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.updatePassword(newPassword)
            result.onSuccess { _uiState.update { it.copy(passwordRecovery = false) } }
            onResult(result)
        }
    }

    fun dismissPasswordRecovery() {
        _uiState.update { it.copy(passwordRecovery = false) }
    }

    // --- Catálogo y datos del local ---

    fun loadInitialData() {
        loadStoreData()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val categoriesResult = repository.getCategories()
            val productsResult = repository.getProducts()

            if (categoriesResult.isSuccess && productsResult.isSuccess) {
                val products = productsResult.getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        categories = categoriesResult.getOrDefault(emptyList()),
                        products = products,
                        cartItems = reconcileCart(it.cartItems, products),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                val error = categoriesResult.exceptionOrNull()?.message
                    ?: productsResult.exceptionOrNull()?.message
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

    /** Datos del local y formas de pago (públicos, no requieren sesión). */
    private fun loadStoreData() {
        viewModelScope.launch {
            repository.getStoreSettings().onSuccess { settings ->
                _uiState.update { it.copy(storeSettings = settings) }
            }
            repository.getPaymentMethods().onSuccess { methods ->
                _uiState.update { it.copy(paymentMethods = methods) }
            }
        }
    }

    /** Actualiza precios y stock del catálogo sin mostrar el estado de carga. */
    private fun refreshProducts() {
        viewModelScope.launch {
            repository.getProducts().onSuccess { products ->
                _uiState.update {
                    it.copy(products = products, cartItems = reconcileCart(it.cartItems, products))
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

    // --- Filtros, orden y favoritos ---

    fun setSort(sort: ProductSort) {
        _uiState.update { it.copy(sort = sort) }
    }

    fun toggleBrand(brand: String) {
        _uiState.update {
            val brands = if (brand in it.selectedBrands) it.selectedBrands - brand else it.selectedBrands + brand
            it.copy(selectedBrands = brands)
        }
    }

    fun setPriceRange(range: ClosedFloatingPointRange<Float>) {
        _uiState.update { state ->
            val bounds = state.priceBounds
            // Cubrir todo el rango equivale a no filtrar por precio
            val covers = range.start <= bounds.start && range.endInclusive >= bounds.endInclusive
            state.copy(priceRange = if (covers) null else range)
        }
    }

    fun setOnlyInStock(enabled: Boolean) {
        _uiState.update { it.copy(onlyInStock = enabled) }
    }

    fun setOnlyFavorites(enabled: Boolean) {
        _uiState.update { it.copy(onlyFavorites = enabled) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                sort = ProductSort.RELEVANCE,
                selectedBrands = emptySet(),
                priceRange = null,
                onlyInStock = false,
                onlyFavorites = false
            )
        }
    }

    fun toggleFavorite(productId: String) {
        _uiState.update {
            val favorites = if (productId in it.favoriteIds) it.favoriteIds - productId else it.favoriteIds + productId
            it.copy(favoriteIds = favorites)
        }
    }

    private fun restoreFavorites() {
        val localDataSource = favoritesLocalDataSource ?: return
        viewModelScope.launch {
            val saved = localDataSource.load()
            _uiState.update { it.copy(favoriteIds = it.favoriteIds + saved) }

            _uiState
                .map { it.favoriteIds }
                .distinctUntilChanged()
                .collect { localDataSource.save(it) }
        }
    }

    // --- Carrito ---

    private fun restoreCart() {
        val localDataSource = cartLocalDataSource ?: return
        viewModelScope.launch {
            val savedItems = localDataSource.load()
            _uiState.update { state ->
                val items = if (state.cartItems.isEmpty()) savedItems else state.cartItems
                val reconciled = if (state.products.isEmpty()) items else reconcileCart(items, state.products)
                state.copy(cartItems = reconciled)
            }

            // Guardar cada cambio del carrito
            _uiState
                .map { it.cartItems }
                .distinctUntilChanged()
                .collect { localDataSource.save(it) }
        }
    }

    /**
     * Sincroniza el carrito con el catálogo vigente: actualiza precio y stock,
     * descarta productos inexistentes o agotados y ajusta cantidades al stock disponible.
     */
    private fun reconcileCart(items: List<CartItem>, products: List<Product>): List<CartItem> {
        val productsById = products.associateBy { it.id }
        return items.mapNotNull { item ->
            val fresh = productsById[item.product.id] ?: return@mapNotNull null
            val quantity = item.quantity.coerceAtMost(fresh.stock)
            if (quantity <= 0) null else CartItem(product = fresh, quantity = quantity)
        }
    }

    /**
     * Agrega el producto respetando el stock disponible.
     * @return `true` si se agregó al menos una unidad, `false` si no hay stock suficiente.
     */
    fun addToCart(product: Product, quantity: Int = 1): Boolean {
        var added = false
        _uiState.update { state ->
            val existingIndex = state.cartItems.indexOfFirst { it.product.id == product.id }
            val currentQuantity = if (existingIndex >= 0) state.cartItems[existingIndex].quantity else 0
            val newQuantity = (currentQuantity + quantity).coerceAtMost(product.stock)
            added = newQuantity > currentQuantity
            if (!added) return@update state

            val updatedList = if (existingIndex >= 0) {
                state.cartItems.toMutableList().apply {
                    this[existingIndex] = this[existingIndex].copy(quantity = newQuantity)
                }
            } else {
                state.cartItems + CartItem(product = product, quantity = newQuantity)
            }
            state.copy(cartItems = updatedList)
        }
        return added
    }

    fun incrementQuantity(productId: String) {
        _uiState.update { state ->
            val updated = state.cartItems.map {
                if (it.product.id == productId && it.quantity < it.product.stock) {
                    it.copy(quantity = it.quantity + 1)
                } else it
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

    // --- Checkout y Pedidos ---

    fun checkout(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        fulfillmentType: String,
        paymentMethod: String?,
        customerEmail: String? = null,
        notes: String? = null,
        saveAddress: Boolean = false,
        onSuccess: (Order) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState.cartItems.isEmpty()) {
            onError("El carrito de compras está vacío")
            return
        }
        if (currentState.isCheckingOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true) }

            // Los montos, el envío y el stock se validan y calculan en el servidor (función create_order)
            val result = repository.createOrder(
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                deliveryAddress = customerAddress.trim(),
                items = currentState.cartItems,
                fulfillmentType = fulfillmentType,
                paymentMethod = paymentMethod,
                customerEmail = customerEmail?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null }
            )
            result.onSuccess { createdOrder ->
                _uiState.update {
                    it.copy(
                        isCheckingOut = false,
                        cartItems = emptyList(),
                        checkoutSuccessOrder = createdOrder
                    )
                }
                if (_uiState.value.currentUser != null) {
                    loadMyOrders()
                    rememberCheckoutData(customerName, customerPhone, customerAddress, fulfillmentType, saveAddress)
                }
                onSuccess(createdOrder)
            }.onFailure { error ->
                _uiState.update { it.copy(isCheckingOut = false) }
                onError(error.message ?: "Error al registrar pedido en Supabase")
            }

            // Stock actualizado tras el pedido (o tras un rechazo por stock insuficiente)
            refreshProducts()
        }
    }

    /** Guarda en la cuenta los datos usados en el pedido para no volver a escribirlos. */
    private suspend fun rememberCheckoutData(
        name: String,
        phone: String,
        address: String,
        fulfillmentType: String,
        saveAddress: Boolean
    ) {
        val state = _uiState.value
        val profile = state.profile
        if (profile == null || (profile.fullName.isNullOrBlank() && profile.phone.isNullOrBlank())) {
            repository.saveProfile(name.trim().ifBlank { null }, phone.trim().ifBlank { null })
                .onSuccess { saved -> _uiState.update { it.copy(profile = saved) } }
        }

        val alreadySaved = state.addresses.any { it.address.equals(address.trim(), ignoreCase = true) }
        if (saveAddress && fulfillmentType == Order.FULFILLMENT_DELIVERY && address.isNotBlank() && !alreadySaved) {
            val isFirst = state.addresses.isEmpty()
            repository.saveAddress(
                AddressUpsert(
                    label = "Dirección ${state.addresses.size + 1}",
                    address = address.trim(),
                    reference = null,
                    isDefault = isFirst
                )
            )
            reloadAddresses()
        }
    }

    fun loadMyOrders() {
        if (_uiState.value.currentUser == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(ordersLoading = true, ordersError = null) }
            repository.getMyOrders()
                .onSuccess { orders ->
                    _uiState.update { it.copy(recentOrders = orders, ordersLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(ordersLoading = false, ordersError = error.message) }
                }
        }
    }

    fun cancelOrder(orderId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.cancelOrder(orderId)
            result.onSuccess { cancelled ->
                // La función devuelve el pedido sin ítems: se conservan los ya cargados
                _uiState.update { state ->
                    state.copy(
                        recentOrders = state.recentOrders.map {
                            if (it.id == cancelled.id) it.copy(status = cancelled.status) else it
                        }
                    )
                }
                refreshProducts() // el stock vuelve al catálogo
            }
            onResult(result.map { })
        }
    }

    /** Vuelve a agregar al carrito los productos de un pedido anterior (respetando el stock actual). */
    fun reorder(order: Order): ReorderResult {
        var added = 0
        var skipped = 0
        order.items.forEach { item ->
            val product = _uiState.value.products.firstOrNull { it.id == item.productId }
            if (product != null && addToCart(product, item.quantity)) added++ else skipped++
        }
        return ReorderResult(added = added, skipped = skipped)
    }

    fun dismissCheckoutSuccessDialog() {
        _uiState.update { it.copy(checkoutSuccessOrder = null) }
    }

    // --- Perfil y direcciones ---

    fun loadProfileData() {
        if (_uiState.value.currentUser == null) return
        viewModelScope.launch {
            repository.getProfile().onSuccess { profile ->
                _uiState.update { it.copy(profile = profile) }
            }
            reloadAddresses()
        }
    }

    private suspend fun reloadAddresses() {
        repository.getAddresses().onSuccess { addresses ->
            _uiState.update { it.copy(addresses = addresses) }
        }
    }

    fun saveProfile(fullName: String, phone: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileSaving = true) }
            val result = repository.saveProfile(
                fullName = fullName.trim().ifBlank { null },
                phone = phone.trim().ifBlank { null }
            )
            result
                .onSuccess { saved -> _uiState.update { it.copy(profile = saved, profileSaving = false) } }
                .onFailure { _uiState.update { it.copy(profileSaving = false) } }
            onResult(result.map { })
        }
    }

    /** Crea (id nulo) o actualiza una dirección guardada. */
    fun saveAddress(
        id: String?,
        label: String,
        address: String,
        reference: String,
        isDefault: Boolean,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileSaving = true) }
            val makeDefault = isDefault || _uiState.value.addresses.isEmpty()
            val result = repository.saveAddress(
                AddressUpsert(
                    id = id,
                    label = label.trim().ifBlank { "Casa" },
                    address = address.trim(),
                    reference = reference.trim().ifBlank { null },
                    isDefault = makeDefault
                )
            )
            result.onSuccess { saved ->
                if (makeDefault) repository.setDefaultAddress(saved.id)
                reloadAddresses()
            }
            _uiState.update { it.copy(profileSaving = false) }
            onResult(result.map { })
        }
    }

    fun deleteAddress(addressId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteAddress(addressId)
            result.onSuccess { reloadAddresses() }
            onResult(result)
        }
    }

    fun setDefaultAddress(addressId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.setDefaultAddress(addressId)
            result.onSuccess { reloadAddresses() }
            onResult(result)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                StoreViewModel(
                    repository = HardbuntuRepository(),
                    cartLocalDataSource = CartLocalDataSource(application),
                    favoritesLocalDataSource = FavoritesLocalDataSource(application)
                )
            }
        }
    }
}
