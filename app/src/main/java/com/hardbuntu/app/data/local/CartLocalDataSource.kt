package com.hardbuntu.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hardbuntu.app.data.model.CartItem
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.cartDataStore by preferencesDataStore(name = "cart")

/**
 * Persiste el carrito en el dispositivo para que sobreviva al cierre de la app.
 */
class CartLocalDataSource(context: Context) {
    private val dataStore = context.applicationContext.cartDataStore
    private val cartKey = stringPreferencesKey("cart_items")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): List<CartItem> = runCatching {
        val raw = dataStore.data.first()[cartKey] ?: return emptyList()
        json.decodeFromString<List<CartItem>>(raw)
    }.getOrDefault(emptyList())

    suspend fun save(items: List<CartItem>) {
        runCatching {
            dataStore.edit { prefs -> prefs[cartKey] = json.encodeToString(items) }
        }
    }
}
