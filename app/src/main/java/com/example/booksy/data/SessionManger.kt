package com.example.booksy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("booksy_prefs")
private val KEY_USER_ID = stringPreferencesKey("user_id")
private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
private val KEY_TOKEN = stringPreferencesKey("auth_token")

class SessionManager(private val context: Context) {

    suspend fun saveSession(userId: Long, email: String, token: String) {
        context.dataStore.edit {
            it[KEY_USER_ID] = userId.toString()
            it[KEY_USER_EMAIL] = email
            it[KEY_TOKEN] = token
        }
    }

    suspend fun loadUserId(): Long? =
        context.dataStore.data.map { it[KEY_USER_ID] }.first()?.toLongOrNull()

    suspend fun loadUserEmail(): String? =
        context.dataStore.data.map { it[KEY_USER_EMAIL] }.first()

    suspend fun loadToken(): String? =
        context.dataStore.data.map { it[KEY_TOKEN] }.first()

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
