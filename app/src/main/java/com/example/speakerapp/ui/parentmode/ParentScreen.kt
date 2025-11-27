package com.example.speakerapp.ui.parentmode

import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speakerapp.models.Alert
import com.example.speakerapp.network.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(
    goBack: () -> Unit,
    viewModel: ParentViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (isActive) {
            val success = clearTempAudioBackend()
            if (success) {
                Log.d("ParentScreen", "Temp audio cleared successfully from backend.")
            } else {
                Log.e("ParentScreen", "Failed to clear temp audio from backend.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to clear temp audio on backend.", Toast.LENGTH_LONG).show()
                }
            }
            delay(60_000) // Wait for 1 minute
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parent Dashboard") },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
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
                .padding(16.dp)
        ) {

            Text("Live Stranger Alerts", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                if (viewModel.alerts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No alerts yet", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(viewModel.alerts) { alert ->
                            AlertCard(
                                alert = alert,
                                onPlayClick = { playAudio(context, alert.audio) },
                                onEnrollRequest = { name ->
                                    scope.launch {
                                        val isSuccess = enrollVoiceBackend(alert.audio, name)
                                        Toast.makeText(
                                            context,
                                            if (isSuccess) "Saved as familiar: $name" else "Enrollment failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (isSuccess) {
                                            try {
                                                alert.audio.delete()
                                            } catch (e: Exception) {
                                                Log.e("ParentScreen", "Failed to delete audio file: ${e.message}")
                                            }
                                            viewModel.removeAlert(alert)
                                            viewModel.refreshFamiliarList()
                                        }
                                    }
                                },
                                onDismissRequest = {
                                    scope.launch {
                                        try {
                                            alert.audio.delete()
                                            viewModel.removeAlert(alert)
                                            Toast.makeText(context, "Alert dismissed", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Log.e("ParentScreen", "Failed to delete audio file: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("Familiar Voices", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                if (viewModel.familiarList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No familiar voices yet", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                        items(viewModel.familiarList) { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        Color(0xFFDFFFD6),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier.weight(1f),
                                    color = Color.Black.copy(alpha = 0.8f)
                                )
                                IconButton(onClick = { viewModel.deleteSpeaker(name) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Speaker", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { viewModel.refreshFamiliarList() }) {
                    Text("Refresh Familiar List")
                }
                Spacer(Modifier.height(16.dp))
                Text("Connection Status: ${viewModel.connectionStatus}")
                Button(onClick = { viewModel.testConnection() }) {
                    Text("Test Connection")
                }
            }
        }
    }
}


@Composable
fun AlertCard(
    alert: Alert,
    onPlayClick: () -> Unit,
    onEnrollRequest: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Enroll Familiar Voice") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Enter Speaker Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isNotEmpty()) {
                            onEnrollRequest(name.trim())
                            showDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss Alert")
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterStart)
            ) {
                Text("Time: ${formatTimestamp(alert.timestamp)}", style = MaterialTheme.typography.bodySmall)
                Text("Location: ${alert.location}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlayClick) { Text("Play Audio") }
                    Button(onClick = { showDialog = true }) { Text("Flag as Familiar") }
                }
            }
        }
    }
}


private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun playAudio(context: android.content.Context, file: File) {
    try {
        val player = MediaPlayer()
        player.setDataSource(file.absolutePath)
        player.prepare()
        player.start()
        Toast.makeText(context, "Playing audio...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to play audio", Toast.LENGTH_SHORT).show()
    }
}

suspend fun enrollVoiceBackend(file: File, speakerName: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val reqBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("name", speakerName)
                .addFormDataPart(
                    "file",
                    "voice.wav",
                    file.asRequestBody("audio/wav".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("${Constants.BASE_URL}enroll")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

suspend fun clearTempAudioBackend(): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val requestBody = ByteArray(0).toRequestBody(null, 0, 0)
            val request = Request.Builder()
                .url("${Constants.BASE_URL}clear_temp_audio")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ParentScreen", "Failed to clear temp audio. Response: ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("ParentScreen", "Exception when clearing temp audio: ${e.message}")
            false
        }
    }
