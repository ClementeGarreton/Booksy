package com.example.booksy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.* // importar componentes para compose
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp // dp para márgenes
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel() para Compose
import com.example.booksy.viewmodel.RegisterViewModel // Llamar al ViewModel de registro

@OptIn(ExperimentalMaterial3Api::class) // Evita warnings de Material3 experimental
@Composable
fun RegisterScreen(
    goLogin: () -> Unit,
    onSuccess: () -> Unit,
    vm: RegisterViewModel = viewModel()
) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    if (ui.successUserId != null) onSuccess()

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Booksy — Registro") }) }
    ) { p ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(p)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = ui.email,
                onValueChange = vm::setEmail,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.pass,
                onValueChange = vm::setPass,
                label = { Text("Contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ui.confirm,
                onValueChange = vm::setConfirm,
                label = { Text("Repite contraseña") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { vm.submit(ctx) },
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear cuenta")
            }

            TextButton(onClick = goLogin) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }

            ui.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}
