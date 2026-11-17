package com.harunmaul.epelatihan.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.harunmaul.epelatihan.feature.home.HomeRoute
import kotlinx.serialization.Serializable

@Serializable
private data object HomeDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HomeDestination,
    ) {
        composable<HomeDestination> {
            HomeRoute()
        }
    }
}
