package com.example.speakerapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.speakerapp.ui.childmode.ChildScreen
import com.example.speakerapp.ui.parentmode.ParentScreen

@Composable
fun AppNavHost(navController: NavHostController) {

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("parent") {
            ParentScreen(
                goBack = { navController.popBackStack() }
            )
        }

        composable("child") {
            ChildScreen(navController = navController)
        }

        composable("alert") {
            AlertScreen(navController)
        }
    }
}