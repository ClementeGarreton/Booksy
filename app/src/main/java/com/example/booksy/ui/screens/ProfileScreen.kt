package com.example.booksy.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.booksy.R
import com.example.booksy.viewmodel.ProfileUiState // import no usado, evaluar alcance
import com.example.booksy.viewmodel.ProfileViewModel
import java.io.File

// Compose no Reconocido por falta de una dependencia llamada viewmodel helper y helper compose activity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: ProfileViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    // Cargar email + avatar guardado
    LaunchedEffect(Unit) { vm.load(ctx) }

    // ----- Launchers -----

    // Galería: Photo Picker moderno (no requiere permisos en Android 13+)
    val pickFromGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) vm.setAvatar(ctx, uri.toString())
    }

    // Preparamos un archivo temporal para la cámara
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) tempCameraUri?.let { vm.setAvatar(ctx, it.toString()) }
    }

    // Permiso de cámara (solo si hace falta)
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createImageUri(ctx)
            tempCameraUri = uri
            takePhoto.launch(uri)
        }
    }

    fun onCameraClick() {
        val hasPerm = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            val uri = createImageUri(ctx)
            tempCameraUri = uri
            takePhoto.launch(uri)
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Booksy — Perfil") }) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ----- Avatar -----
            val avatarModifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)

            if (ui.avatarUri != null) {
                AsyncImage(
                    model = ui.avatarUri,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = avatarModifier
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_avatar_default),
                    contentDescription = "Avatar por defecto",
                    contentScale = ContentScale.Crop,
                    modifier = avatarModifier
                )
            }

            // Email (si lo guardaste en SessionManager)
            ui.email?.let {
                Text(it, style = MaterialTheme.typography.titleMedium)
            }

            // ----- Botones -----
            Button(
                onClick = {
                    pickFromGallery.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Elegir de galería") }

            Button(
                onClick = { onCameraClick() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tomar foto con cámara") }

            OutlinedButton(
                onClick = { vm.clearAvatar(ctx) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Quitar foto") }

            // Errorcito si ocurre algo
            ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

/** Crea una Uri segura en cache/images para guardar la foto tomada por la cámara */
private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "camera_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider", // Debe coincidir con Manifest
        imageFile
    )
}
