package com.hardbuntu.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/**
 * Guarda en el dispositivo los ids de los productos favoritos (funciona también para invitados).
 */
class FavoritesLocalDataSource(context: Context) {
    private val dataStore = context.applicationContext.favoritesDataStore
    private val favoritesKey = stringSetPreferencesKey("favorite_product_ids")

    suspend fun load(): Set<String> = runCatching {
        dataStore.data.first()[favoritesKey] ?: emptySet()
    }.getOrDefault(emptySet())

    suspend fun save(ids: Set<String>) {
        runCatching {
            dataStore.edit { prefs -> prefs[favoritesKey] = ids }
        }
    }
}
