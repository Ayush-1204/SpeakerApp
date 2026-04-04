package com.example.speakerapp.features.enrollment.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakerEnrollmentScreen(
    viewModel: SpeakerEnrollmentViewModel,
    onEnrollmentSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val speakerName = remember { mutableStateOf("") }
    val selectedFileUri = remember { mutableStateOf<Uri?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedFileUri.value = uri
    }

    LaunchedEffect(uiState.enrolledSpeaker) {
        if (uiState.enrolledSpeaker != null) {
            onEnrollmentSuccess()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6FAFA),
        topBar = {
            TopAppBar(
                title = { Text("Enroll Speaker", color = Color(0xFF004650)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6FAFA))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "VOICE REGISTRATION",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF50686D),
                letterSpacing = 2.sp
            )
            Text(
                "Create Profile",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF181C1D)
            )
            Text(
                "Record a fresh sample or upload a clear voice clip.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF3F4949)
            )

            OutlinedTextField(
                value = speakerName.value,
                onValueChange = { speakerName.value = it },
                label = { Text("Speaker Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF135F6B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Quick Enroll",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        "Capture a 5-second sample directly in app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Button(
                        onClick = { viewModel.recordAndEnroll(speakerName.value, durationMs = 5000L) },
                        enabled = speakerName.value.isNotBlank() && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF004650))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Record 5s", color = Color(0xFF004650))
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Upload Existing Audio", style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = { filePicker.launch("audio/*") },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFileUri.value == null) "Pick Audio File" else "Change Audio File")
                    }

                    if (selectedFileUri.value != null) {
                        Text("Audio selected", style = MaterialTheme.typography.bodySmall, color = Color(0xFF006518))
                    }

                    Button(
                        onClick = {
                            val uri = selectedFileUri.value ?: return@Button
                            CoroutineScope(Dispatchers.IO).launch {
                                val input = context.contentResolver.openInputStream(uri) ?: return@launch
                                val tempFile = File.createTempFile("enroll_", ".wav", context.cacheDir)
                                input.use { stream -> tempFile.outputStream().use { stream.copyTo(it) } }
                                viewModel.enrollSpeaker(speakerName.value, tempFile)
                            }
                        },
                        enabled = speakerName.value.isNotBlank() && selectedFileUri.value != null && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enroll from File")
                    }
                }
            }

            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            if (uiState.error != null) {
                Text(uiState.error ?: "Enrollment failed", color = MaterialTheme.colorScheme.error)
            }

            if (uiState.enrolledSpeaker != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7EA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Enrollment successful", color = Color(0xFF005312), style = MaterialTheme.typography.titleSmall)
                        Text("Saved samples: ${uiState.enrolledSpeaker?.samplesSaved}")
                        Text("Quality passed: ${uiState.speechQualityPassed ?: false}")
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
