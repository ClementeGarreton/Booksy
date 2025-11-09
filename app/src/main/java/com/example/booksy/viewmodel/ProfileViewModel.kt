package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.booksy.AppDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Re-usa el mismo nombre de prefs para convivir con SessionManager
private val Context.dataStore by preferencesDataStore("booksy_prefs")
private val KEY_AVATAR = stringPreferencesKey("avatar_uri")

data class ProfileUiState(
    val email: String? = null,
    val avatarUri: String? = null,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {
    private val _ui = MutableStateFlow(ProfileUiState())
    val ui = _ui.asStateFlow()

    fun load(ctx: Context) = viewModelScope.launch {
        try {
            val email = try { AppDependencies.session.loadUserEmail() } catch (_: Throwable) { null }
            val avatar = ctx.dataStore.data.map { it[KEY_AVATAR] }.first()
            _ui.value = ProfileUiState(email = email, avatarUri = avatar, error = null)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message)
        }
    }

    fun setAvatar(ctx: Context, uri: String) = viewModelScope.launch {
        try {
            ctx.dataStore.edit { it[KEY_AVATAR] = uri }
            _ui.value = _ui.value.copy(avatarUri = uri, error = null)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message)
        }
    }

    fun clearAvatar(ctx: Context) = viewModelScope.launch {
        try {
            ctx.dataStore.edit { it.remove(KEY_AVATAR) }
            _ui.value = _ui.value.copy(avatarUri = null, error = null)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message)
        }
    }
}
