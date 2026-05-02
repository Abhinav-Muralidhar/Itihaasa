package com.itihaasa.nammakathey

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.itihaasa.nammakathey.ui.map.MapScreen
import com.itihaasa.nammakathey.ui.profile.ProfileScreen

sealed class Screen(val route: String) {
    data object Map : Screen("map")
    data object Profile : Screen("profile")
}

@Composable
fun NammaKatheyRoot() {
    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Map.route) {
                MapScreen(
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
        }
    }
}
