/**
 =================================================================
 * SAFEEAR ANDROID - COMPREHENSIVE IMPLEMENTATION GUIDE
 This file consolidates all remaining ViewModels, their state management,
 use cases, and critical UI infrastructure.
 =================================================================
 */

// ============= ENROLLMENT VIEWMODELS =============

file: app/src/main/java/com/example/speakerapp/features/enrollment/ui/SpeakerEnrollmentViewModel.kt
---
package com.example.speakerapp.features.enrollment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import com.example.speakerapp.features.enrollment.data.EnrollmentResult
import com.example.speakerapp.features.enrollment.data.EnrolledSpeaker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class SpeakerEnrollmentUiState(
    val isLoading: Boolean = false,
    val enrolledSpeaker: EnrolledSpeaker? = null,
    val error: String? = null,
    val voicedMs: Double? = null,
    val numSegments: Int? = null,
    val speechQualityPassed: Boolean? = null
)

@HiltViewModel
class SpeakerEnrollmentViewModel @Inject constructor(
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerEnrollmentUiState())
    val uiState: StateFlow<SpeakerEnrollmentUiState> = _uiState.asStateFlow()

    fun enrollSpeaker(displayName: String, audioFile: File, speakerId: String? = null) {
        viewModelScope.launch {
            enrollmentRepository.enrollSpeaker(
                displayName = displayName,
                audioFile = audioFile,
                speakerId = speakerId
            ).collect { result ->
                when (result) {
                    is EnrollmentResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is EnrollmentResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            enrolledSpeaker = result.data,
                            voicedMs = result.data.voicedMs,
                            numSegments = result.data.numSegments,
                            speechQualityPassed = result.data.speechQualityPassed,
                            error = null
                        )
                    }
                    is EnrollmentResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

---

file: app/src/main/java/com/example/speakerapp/features/enrollment/ui/SpeakerListViewModel.kt
---
package com.example.speakerapp.features.enrollment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import com.example.speakerapp.features.enrollment.data.EnrollmentResult
import com.example.speakerapp.features.enrollment.data.SpeakerListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SpeakerListUiState(
    val isLoading: Boolean = false,
    val speakers: List<SpeakerListItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SpeakerListViewModel @Inject constructor(
    private val enrollmentRepository: EnrollmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeakerListUiState())
    val uiState: StateFlow<SpeakerListUiState> = _uiState.asStateFlow()

    init {
        loadSpeakers()
    }

    fun loadSpeakers() {
        viewModelScope.launch {
            enrollmentRepository.listSpeakers().collect { result ->
                when (result) {
                    is EnrollmentResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is EnrollmentResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            speakers = result.data,
                            error = null
                        )
                    }
                    is EnrollmentResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun updateSpeaker(speakerId: String, newDisplayName: String) {
        viewModelScope.launch {
            enrollmentRepository.updateSpeaker(speakerId, newDisplayName).collect { result ->
                when (result) {
                    is EnrollmentResult.Success -> loadSpeakers()
                    is EnrollmentResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> {}
                }
            }
        }
    }

    fun deleteSpeaker(speakerId: String) {
        viewModelScope.launch {
            enrollmentRepository.deleteSpeaker(speakerId).collect { result ->
                when (result) {
                    is EnrollmentResult.Success -> loadSpeakers()
                    is EnrollmentResult.Error -> _uiState.value = _uiState.value.copy(error = result.message)
                    else -> {}
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

---

// ============= ALERTS VIEWMODELS =============

file: app/src/main/java/com/example/speakerapp/features/alerts/ui/AlertsViewModel.kt
---
package com.example.speakerapp.features.alerts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.alerts.data.AlertsRepository
import com.example.speakerapp.features.alerts.data.AlertResult
import com.example.speakerapp.features.alerts.data.AlertItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<AlertItem> = emptyList(),
    val error: String? = null,
    val ackingAlertId: String? = null
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
    }

    fun loadAlerts(limit: Int = 50, offset: Int = 0) {
        viewModelScope.launch {
            alertsRepository.getAlerts(limit, offset).collect { result ->
                when (result) {
                    is AlertResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is AlertResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            alerts = result.data,
                            error = null
                        )
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(ackingAlertId = alertId)
            alertsRepository.acknowledgeAlert(alertId).collect { result ->
                when (result) {
                    is AlertResult.Success -> {
                        // Update local alerts list
                        val updatedAlerts = _uiState.value.alerts.map { alert ->
                            if (alert.id == alertId) alert.copy(isAcknowledged = true) else alert
                        }
                        _uiState.value = _uiState.value.copy(alerts = updatedAlerts, ackingAlertId = null)
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            ackingAlertId = null
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun getAlertClip(alertId: String) {
        viewModelScope.launch {
            alertsRepository.getAlertClip(alertId).collect { result ->
                when (result) {
                    is AlertResult.Success -> {
                        // Clip fetched successfully - UI handles playback
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(error = "Failed to fetch clip: ${result.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

---

// ============= DETECTION VIEWMODELS =============

file: app/src/main/java/com/example/speakerapp/features/detection/ui/ChildMonitoringViewModel.kt
---
package com.example.speakerapp.features.detection.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.core.audio.AudioRecorder
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.detection.data.DetectionRepository
import com.example.speakerapp.features.detection.data.DetectionResult
import com.example.speakerapp.features.detection.data.DetectionResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class DetectionUiState(
    val isRecording: Boolean = false,
    val isUploading: Boolean = false,
    val lastDetectionStatus: String? = null,
    val lastDecision: String? = null,
    val confidenceScore: Double? = null,
    val lastAlertFired: Boolean = false,
    val lastAlertId: String? = null,
    val error: String? = null,
    val messages: List<String> = emptyList()
)

@HiltViewModel
class ChildMonitoringViewModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val audioRecorder: AudioRecorder,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetectionUiState())
    val uiState: StateFlow<DetectionUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var uploadJob: Job? = null
    private var chunkIndex = 0

    fun initialize() {
        if (!audioRecorder.initialize()) {
            _uiState.value = _uiState.value.copy(error = "Failed to initialize audio recorder")
        }
    }

    fun startDetectionStream() {
        viewModelScope.launch {
            val deviceId = tokenManager.getDeviceId()
            if (deviceId == null) {
                _uiState.value = _uiState.value.copy(error = "Device not registered")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isRecording = true, error = null)
            chunkIndex = 0

            recordingJob?.cancel()
            recordingJob = launch {
                while (_uiState.value.isRecording) {
                    try {
                        val chunkFile = File.createTempFile("chunk_$chunkIndex", ".wav")
                        chunkIndex++

                        // Record 1.5 second chunks
                        audioRecorder.recordAudio(1500, chunkFile)

                        // Upload chunk
                        _uiState.value = _uiState.value.copy(isUploading = true)
                        detectionRepository.uploadDetectionChunk(
                            deviceId = deviceId,
                            audioFile = chunkFile
                        ).collect { result ->
                            when (result) {
                                is DetectionResult.Success -> {
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        lastDetectionStatus = result.data.status,
                                        lastDecision = result.data.decision,
                                        confidenceScore = result.data.score,
                                        lastAlertFired = result.data.alertFired ?: false,
                                        lastAlertId = result.data.alertId
                                    )
                                    addMessage("Status: ${result.data.status}")
                                }
                                is DetectionResult.Error -> {
                                    _uiState.value = _uiState.value.copy(isUploading = false)
                                    if (result.code == 403) {
                                        stopDetectionStream()
                                        _uiState.value = _uiState.value.copy(
                                            error = "Only child devices can stream audio"
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }

                        chunkFile.delete()
                        delay(1000) // Brief delay between chunks
                    } catch (e: Exception) {
                        addMessage("Chunk error: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopDetectionStream() {
        viewModelScope.launch {
            recordingJob?.cancel()
            uploadJob?.cancel()
            audioRecorder.release()

            val deviceId = tokenManager.getDeviceId()
            if (deviceId != null) {
                detectionRepository.endDetectionSession(deviceId).collect { _ ->
                    _uiState.value = _uiState.value.copy(isRecording = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val deviceId = tokenManager.getDeviceId() ?: return@launch
            detectionRepository.updateLocation(deviceId, latitude, longitude).collect { result ->
                when (result) {
                    is DetectionResult.Error -> {
                        addMessage("Location update failed: ${result.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun addMessage(message: String) {
        _uiState.value = _uiState.value.copy(
            messages = (_uiState.value.messages + message).takeLast(10)
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        uploadJob?.cancel()
        audioRecorder.release()
    }
}

---

// ============= SETTINGS SCREEN =============

file: app/src/main/java/com/example/speakerapp/features/settings/ui/SettingsScreen.kt
---
package com.example.speakerapp.features.settings.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.speakerapp.core.auth.TokenManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    tokenManager: TokenManager = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var deviceRole by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            deviceRole = tokenManager.getDeviceRole()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Device Role
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Device Role", style = MaterialTheme.typography.labelMedium)
                    Text(
                        deviceRole ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // About
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://safeear.app"))
                    context.startActivity(intent)
                }
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("About SafeEar", style = MaterialTheme.typography.bodyMedium)
                    Text("v1.0.0", style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Filled.Info, contentDescription = "About")
            }
        }

        Spacer(Modifier.weight(1f))

        // Logout Button
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
            Spacer(Modifier.width(8.dp))
            Text("Logout")
        }
    }
}

---

// ============= HILT AUTH MODULE =============

file: app/src/main/java/com/example/speakerapp/features/auth/di/AuthModule.kt
---
package com.example.speakerapp.features.auth.di

import com.example.speakerapp.features.auth.data.AuthRepository
import com.example.speakerapp.features.auth.domain.IsLoggedInUseCase
import com.example.speakerapp.features.auth.domain.LoginUseCase
import com.example.speakerapp.features.auth.domain.LogoutUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideLoginUseCase(authRepository: AuthRepository): LoginUseCase {
        return LoginUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideLogoutUseCase(authRepository: AuthRepository): LogoutUseCase {
        return LogoutUseCase(authRepository)
    }

    @Provides
    @Singleton
    fun provideIsLoggedInUseCase(authRepository: AuthRepository): IsLoggedInUseCase {
        return IsLoggedInUseCase(authRepository)
    }
}

---

// ============= NAVIGATION SETUP =============

file: app/src/main/java/com/example/speakerapp/navigation/Screen.kt
---
package com.example.speakerapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DeviceRegistration : Screen("device_registration")
    object ParentDashboard : Screen("parent_dashboard")
    object ChildMonitoring : Screen("child_monitoring")
    object SpeakerEnrollment : Screen("speaker_enrollment")
    object SpeakerList : Screen("speaker_list")
    object Alerts : Screen("alerts")
    object Settings : Screen("settings")
}

---

// ============= ANDROID MANIFEST =============

file: app/src/main/AndroidManifest.xml
---
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required Permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application>
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>

---

