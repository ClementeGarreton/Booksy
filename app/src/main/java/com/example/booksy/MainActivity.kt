package com.example.booksy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.booksy.ui.navigation.AppNavigation //no encuentra el recurso, crear carpeta navigation
import com.example.booksy.ui.theme.BooksyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iniciar dependencias DB + Session
        AppDependencies.init(application)

        setContent {
            BooksyTheme {
                AppNavigation()   // navegar entre Login / Register / Home / Profile // no encuentra el recurso
            }
        }
    }
}
