package com.example.speakerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.models.Alert
import com.example.speakerapp.ui.AppNavHost
import com.example.speakerapp.ui.theme.SpeakerAppTheme
import com.example.speakerapp.utils.LocationHelper
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private var lastConsumedAlertId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeakerAppTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // Listen for raw stranger detection events
                LaunchedEffect(Unit) {
                    AlertBus.strangerEvents.collectLatest { event ->
                        // Enrich the event with location data
                        val location = LocationHelper.getLocation(context)
                        val completeAlert = Alert(
                            timestamp = System.currentTimeMillis(),
                            audio = event.audio,
                            location = location
                        )
                        // Post the complete alert for the rest of the app to use
                        AlertBus.sendAlert(completeAlert)
                    }
                }

                // Global listener for completed alerts (for navigation)
                LaunchedEffect(Unit) {
                    AlertBus.alerts.collectLatest { alert ->
                        if (alert.id != lastConsumedAlertId) {
                            lastConsumedAlertId = alert.id
                            val current = navController.currentBackStackEntry?.destination?.route
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
