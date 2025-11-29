package com.example.speakerapp.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.speakerapp.ui.childmode.ChildScreen
import com.example.speakerapp.ui.parentmode.ParentScreen
import com.example.speakerapp.ui.parentmode.ParentViewModel

@Composable
fun AppNavHost(navController: NavHostController) {

    val parentViewModel: ParentViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("parent") {
            ParentScreen(
                goBack = { navController.popBackStack() },
                viewModel = parentViewModel
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