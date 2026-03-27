package com.example.speakerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.speakerapp.common.AlertEventBus
import com.example.speakerapp.ui.AppNavHost
import com.example.speakerapp.ui.NotificationHelper
import com.example.speakerapp.ui.theme.SpeakerAppTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create notification channels on app startup
        NotificationHelper.createNotificationChannels(this)

        setContent {
            SpeakerAppTheme {
                val navController = rememberNavController()

                // Global listener for alert events (for navigation)
                LaunchedEffect(Unit) {
                    AlertEventBus.events.collectLatest { _ ->
                        val current = navController.currentBackStackEntry?.destination?.route
                        if (current != "alert") {
                            navController.navigate("alert") {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                AppNavHost(navController)
            }
        }
    }
}
