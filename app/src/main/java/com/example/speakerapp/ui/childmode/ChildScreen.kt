package com.example.speakerapp.ui.childmode

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.speakerapp.service.RecorderService
import com.example.speakerapp.ui.getDeviceID
import com.example.speakerapp.ui.releaseModeLock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildScreen(navController: NavHostController) {

    val context = LocalContext.current
    var statusText by remember { mutableStateOf("Not Recording") }
    var isRecording by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                val deviceId = getDeviceID(context)
                if (releaseModeLock(deviceId)) {
                    Log.d("ChildScreen", "Successfully released child mode lock.")
                } else {
                    Log.e("ChildScreen", "Failed to release child mode lock.")
                }

                // Stop the recording service when leaving the screen
                val intent = Intent(context, RecorderService::class.java).apply { action = "STOP" }
                context.startService(intent)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                val intent = Intent(context, RecorderService::class.java).apply { action = "START" }
                context.startService(intent)
                isRecording = true
                statusText = "Listening…"
            } else {
                statusText = "Location Permission Denied"
            }
        }
    )

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                statusText = "Microphone Permission Denied"
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Child Mode") },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            val deviceId = getDeviceID(context)
                            releaseModeLock(deviceId)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Microphone",
                modifier = Modifier.size(100.dp),
                tint = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (!isRecording) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        val intent = Intent(context, RecorderService::class.java).apply { action = "STOP" }
                        context.startService(intent)
                        isRecording = false
                        statusText = "Stopped"
                    }
                },
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
                    .animateContentSize(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isRecording) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(text = if (!isRecording) "Start Listening" else "Stop Listening")
            }
        }
    }
}
