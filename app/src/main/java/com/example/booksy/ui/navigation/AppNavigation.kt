package com.example.booksy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.booksy.AppDependencies
import com.example.booksy.ui.screens.HomeScreen
import com.example.booksy.ui.screens.LoginScreen
import com.example.booksy.ui.screens.ProfileScreen
import com.example.booksy.ui.screens.RegisterScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"   // 👈 nueva ruta
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()

    var start by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val userId = AppDependencies.session.loadUserId()
        start = if (userId != null) Routes.HOME else Routes.LOGIN
    }

    if (start == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator() }
        return
    }

    NavHost(navController = nav, startDestination = start!!) {
        composable(Routes.LOGIN) {
            LoginScreen(
                goRegister = { nav.navigate(Routes.REGISTER) },
                onSuccess = { nav.navigate(Routes.HOME) { popUpTo(0); launchSingleTop = true } }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                goLogin = { nav.popBackStack() },
                onSuccess = { nav.navigate(Routes.HOME) { popUpTo(0); launchSingleTop = true } }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onLogout = { nav.navigate(Routes.LOGIN) { popUpTo(0); launchSingleTop = true } },
                goProfile = { nav.navigate(Routes.PROFILE) }  // 👈 navegación a Perfil
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen()
        }
    }
}
