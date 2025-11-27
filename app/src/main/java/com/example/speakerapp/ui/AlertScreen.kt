package com.example.speakerapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.speakerapp.core.AlertBus
import com.example.speakerapp.models.Alert

@Composable
fun AlertScreen(navController: NavHostController) {

    // Read last alert ONCE
    val latest = remember { AlertBus.alerts.replayCache.lastOrNull() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (latest == null) {
            Text("No new alerts.", color = MaterialTheme.colorScheme.onSurface)
        } else {
            StrangerPopup(
                alert = latest,
                onGoParent = {
                    navController.navigate("parent") {
                        popUpTo("home")
                    }
                }
            )
        }
    }
}

@Composable
fun StrangerPopup(
    alert: Alert,
    onGoParent: () -> Unit
) {
    Card(
        modifier = Modifier.padding(32.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {

        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Stranger Detected!",
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "An unrecognized voice was detected near the child's device.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onGoParent,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Parent Dashboard", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
