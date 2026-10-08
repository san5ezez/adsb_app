package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "planes_adsb_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        const val DEFAULT_SERVER = "192.168.88.68:8073"
        private val KEY_SERVER_ADDRESS = stringPreferencesKey("server_address")
    }

    val serverAddressFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SERVER_ADDRESS] ?: DEFAULT_SERVER
    }

    suspend fun saveServerAddress(address: String) {
        val cleanAddress = address.trim()
            .removePrefix("ws://")
            .removePrefix("http://")
            .removePrefix("wss://")
            .removePrefix("https://")
            .removeSuffix("/")
            .removeSuffix("/ws")
            .removeSuffix("/ws/")

        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_ADDRESS] = cleanAddress.ifBlank { DEFAULT_SERVER }
        }
    }
}
