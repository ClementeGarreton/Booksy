package com.example.booksy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*        // Composable, remember, by, etc.
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel  // viewModel() EN EL Compose
import com.example.booksy.viewmodel.LoginViewModel // llamar al viewmodel de login screen (Ej: Screen = HTML, VirewmModel = JS)

// (Opcional) en cado de que si avisa API experimental de Material3: import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class) //error prescindible
@Composable
fun LoginScreen(
    goRegister: () -> Unit,
    onSuccess: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    if (ui.successUserId != null) onSuccess()

    Scaffold(topBar = { CenterAlignedTopAppBar({ Text("Booksy — Login") }) }) { p ->
        Column(Modifier.fillMaxSize().padding(p).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(ui.email, { vm.setEmail(it) }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ui.password, { vm.setPassword(it) }, label = { Text("Contraseña") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.submit(ctx) }, enabled = !ui.loading, modifier = Modifier.fillMaxWidth()) { Text("Entrar") }
            TextButton(onClick = goRegister) { Text("¿No tienes cuenta? Regístrate") }
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (ui.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
