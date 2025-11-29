package com.example.speakerapp.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.speakerapp.network.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpeakerApp") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to SpeakerApp",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Select your mode to get started.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))

            ModeButton(
                text = "Parent Mode",
                icon = Icons.Default.SupervisorAccount,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = {
                    scope.launch {
                        val deviceId = getDeviceID(context)
                        val canEnterParentMode = requestParentModeLock(deviceId)

                        if (canEnterParentMode) {
                            navController.navigate("parent")
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Parent mode is active on another device.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
            ModeButton(
                text = "Child Mode",
                icon = Icons.Default.ChildCare,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = {
                    scope.launch {
                        val deviceId = getDeviceID(context)
                        val canEnterChildMode = requestChildModeLock(deviceId)

                        if (canEnterChildMode) {
                            navController.navigate("child")
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Child mode is active on another device.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeButton(
    text: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

suspend fun requestParentModeLock(deviceId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("device_id", deviceId)
            .build()

        val request = Request.Builder()
            .url("${Constants.BASE_URL}start_parent_mode")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { 
            Log.d("HomeScreen", "Parent mode request for device $deviceId returned code: ${it.code}")
            it.isSuccessful 
        }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Exception requesting parent mode lock: ${e.message}")
        false
    }
}

suspend fun requestChildModeLock(deviceId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("device_id", deviceId)
            .build()

        val request = Request.Builder()
            .url("${Constants.BASE_URL}start_child_mode")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { 
            Log.d("HomeScreen", "Child mode request for device $deviceId returned code: ${it.code}")
            it.isSuccessful 
        }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Exception requesting child mode lock: ${e.message}")
        false
    }
}
