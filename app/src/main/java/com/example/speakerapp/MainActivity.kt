package com.example.speakerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.ui.AppNavHost
import com.example.speakerapp.ui.theme.SpeakerAppTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private var lastConsumedAlertId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeakerAppTheme {
                val navController = rememberNavController()

                // GLOBAL ALERT LISTENER
                LaunchedEffect(Unit) {
                    AlertBus.alerts.collectLatest { alert ->

                        // 👉 Avoid infinite loop by consuming only new alerts
                        if (alert.id != lastConsumedAlertId) {
                            lastConsumedAlertId = alert.id

                            // Navigate only if not already on alert page
                            val current = navController.currentBackStackEntry
                                ?.destination?.route

                            if (current != "alert") {
                                navController.navigate("alert") {
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }

                AppNavHost(navController)
            }
        }
    }
}
