package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class PreferencesManager(private val context: Context) {
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tautulli_prefs")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_USER_SERVER_TYPE = stringPreferencesKey("server_type")
        private val KEY_USE_DEMO_MODE = booleanPreferencesKey("use_demo_mode")
    }

    val serverUrl: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_SERVER_URL] ?: ""
        }

    val apiKey: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_API_KEY] ?: ""
        }

    val serverType: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_SERVER_TYPE] ?: "tautulli"
        }

    val useDemoMode: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Default to true so the user is immediately greeted by a beautiful, populated dashboard
            preferences[KEY_USE_DEMO_MODE] ?: true
        }

    suspend fun saveServerCredentials(url: String, key: String, type: String = "tautulli") {
        context.dataStore.edit { preferences ->
            preferences[KEY_SERVER_URL] = url
            preferences[KEY_API_KEY] = key
            preferences[KEY_USER_SERVER_TYPE] = type
        }
    }

    suspend fun setDemoMode(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USE_DEMO_MODE] = active
        }
    }
}
