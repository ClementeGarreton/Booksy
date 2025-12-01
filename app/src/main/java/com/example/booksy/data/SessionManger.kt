package com.example.booksy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Ya tenemos appDataStore definido en AppDataStore.kt
// val Context.appDataStore by preferencesDataStore("booksy_prefs")

private val KEY_USER_ID = stringPreferencesKey("user_id")
private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
private val KEY_TOKEN = stringPreferencesKey("auth_token")

class SessionManager(private val context: Context) {

    // atajo local, solo para no repetir context.appDataStore todo el rato
    private val dataStore = context.appDataStore

    suspend fun saveSession(userId: Long, email: String, token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId.toString()
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_TOKEN] = token
        }
    }

    suspend fun loadUserId(): Long? =
        dataStore.data
            .map { it[KEY_USER_ID] }
            .first()
            ?.toLongOrNull()

    suspend fun loadUserEmail(): String? =
        dataStore.data
            .map { it[KEY_USER_EMAIL] }
            .first()

    suspend fun loadToken(): String? =
        dataStore.data
            .map { it[KEY_TOKEN] }
            .first()

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
