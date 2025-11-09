package com.example.booksy.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.booksy.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val successUserId: Long? = null
)

class LoginViewModel : ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState())
    val ui = _ui.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://x8ki-letl-twmt.n7.xano.io/api:Rfm_61dW"

    fun setEmail(v: String) { _ui.value = _ui.value.copy(email = v) }
    fun setPassword(v: String) { _ui.value = _ui.value.copy(password = v) }

    fun submit(ctx: Context) = viewModelScope.launch {
        val s = _ui.value
        if (s.email.isBlank() || s.password.isBlank()) { fail("Completa todos los campos"); return@launch }
        if (!s.email.contains("@")) { fail("Email no válido"); return@launch }
        if (s.password.length < 4) { fail("Contraseña demasiado corta (4+)"); return@launch }

        _ui.value = s.copy(loading = true, error = null)

        try {
            // 1) LOGIN
            val loginResp = withContext(Dispatchers.IO) {
                val json = JSONObject().put("email", s.email.trim()).put("password", s.password)
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url("$baseUrl/auth/login").post(body).build()
                client.newCall(req).execute()
            }

            val loginRaw = loginResp.use { it.body?.string().orEmpty() }
            if (!loginResp.isSuccessful) {
                fail("Login falló (${loginResp.code}): ${loginRaw.take(200)}")
                return@launch
            }

            val loginObj = JSONObject(loginRaw)
            val token = loginObj.optString("authToken", null)
            if (token == null) { fail("Respuesta sin token"); return@launch }

            val userObj = loginObj.optJSONObject("user")
            val pair: Pair<Long, String>? = if (userObj != null) {
                userObj.getLong("id") to userObj.optString("email", s.email.trim())
            } else {
                fetchMe(token)
            }

            if (pair == null) { fail("No se pudo obtener el perfil (me)"); return@launch }
            val (id, email) = pair

            AppDependencies.session.saveSession(id, email, token)
            _ui.value = _ui.value.copy(loading = false, successUserId = id)

        } catch (e: Exception) {
            fail("Error de red: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    private suspend fun fetchMe(token: String): Pair<Long, String>? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/auth/me")
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val raw = resp.body?.string().orEmpty()
            val obj = JSONObject(raw)
            val id = obj.optLong("id", -1L)
            val email = obj.optString("email", "")
            if (id <= 0 || email.isBlank()) null else (id to email)
        }
    }

    private fun fail(msg: String) {
        _ui.value = _ui.value.copy(loading = false, error = msg)
    }
}
