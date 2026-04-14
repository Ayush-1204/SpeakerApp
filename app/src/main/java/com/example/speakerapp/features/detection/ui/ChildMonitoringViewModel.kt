package com.example.speakerapp.features.detection.ui

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.PackageManager
import android.os.SystemClock
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.example.speakerapp.core.audio.AudioRecorder
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.detection.data.DetectionRepository
import com.example.speakerapp.features.detection.data.DetectionResult
import com.example.speakerapp.features.devices.data.DeviceRepository
import com.example.speakerapp.features.devices.data.DeviceResult
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class DetectionLogEntry(
    val confidence: String,
    val streak: String,
    val decision: String,
    val alertId: String,
    val isSuccess: Boolean
)

data class ChildMonitoringUiState(
    val isRecording: Boolean = false,
    val isStopping: Boolean = false,
    val isUploading: Boolean = false,
    val lastDetectionStatus: String? = null,
    val lastDecision: String? = null,
    val confidenceScore: Double? = null,
    val lastAlertFired: Boolean = false,
    val lastAlertId: String? = null,
    val batteryPercent: Int? = null,
    val uploadLatencyMs: Long? = null,
    val remoteMonitoringEnabled: Boolean? = null,
    val isOffline: Boolean = false,
    val error: String? = null,
    val messages: List<String> = emptyList(),
    val activityLogs: List<DetectionLogEntry> = emptyList()
)

@HiltViewModel
class ChildMonitoringViewModel @Inject constructor(
    private val detectionRepository: DetectionRepository,
    private val audioRecorder: AudioRecorder,
    private val tokenManager: TokenManager,
    private val deviceRepository: DeviceRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val MIN_VALID_WAV_BYTES = 44L
        private const val MONITOR_PREFS = "safeear_monitoring"
        private const val KEY_FOREGROUND_OWNER_ACTIVE = "foreground_owner_active"
        private const val CONNECTIVITY_PREFS = "safeear_connectivity"
        private const val KEY_IS_OFFLINE = "is_offline"
    }

    private val _uiState = MutableStateFlow(ChildMonitoringUiState())
    val uiState: StateFlow<ChildMonitoringUiState> = _uiState.asStateFlow()

    private var recordingJob: Job? = null
    private var chunkIndex = 0
    private var latestLatitude: Double? = null
    private var latestLongitude: Double? = null
    private var monitoringSyncJob: Job? = null
    private var telemetryJob: Job? = null
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(appContext) }
    private var locationCallback: LocationCallback? = null
    private val stopGuard = AtomicBoolean(false)

    init {
        initialize()
        startDeviceTelemetryRefresh()
        startRemoteMonitoringSync()
        deviceRepository.startDevicePolling()
    }

    fun initialize() {
        if (!audioRecorder.initialize()) {
            _uiState.value = _uiState.value.copy(error = "Failed to initialize audio recorder")
        }
    }

    fun toggleMonitoring() {
        if (_uiState.value.isRecording) {
            stopDetectionStream(syncRemoteState = true)
        } else {
            startDetectionStream(syncRemoteState = true)
        }
    }

    fun startDetectionStream(syncRemoteState: Boolean = true) {
        viewModelScope.launch {
            if (_uiState.value.isStopping) return@launch
            if (_uiState.value.isRecording || recordingJob?.isActive == true) return@launch

            if (!hasRecordPermission()) {
                _uiState.value = _uiState.value.copy(error = "Microphone permission is required")
                return@launch
            }

            if (!ensureRecorderReady()) {
                _uiState.value = _uiState.value.copy(error = "Audio recorder is not ready")
                return@launch
            }

            val deviceId = tokenManager.getDeviceId()
            val savedRole = tokenManager.getDeviceRole()
            if (deviceId == null) {
                _uiState.value = _uiState.value.copy(error = "Device not registered")
                return@launch
            }
            if (savedRole != "child_device") {
                _uiState.value = _uiState.value.copy(error = "Device role must be child_device")
                return@launch
            }

            if (syncRemoteState) {
                updateRemoteMonitoring(deviceId = deviceId, enabled = true)
            }

            Log.d("ChildMonitoringViewModel", "Starting detection stream for device: $deviceId")
            
            val serviceIntent = android.content.Intent(appContext, com.example.speakerapp.service.DetectionService::class.java).apply {
                action = "START"
            }
            try {
                appContext.startService(serviceIntent)
                Log.d("ChildMonitoringViewModel", "DetectionService started")
            } catch (e: Exception) {
                Log.e("ChildMonitoringViewModel", "Failed to start DetectionService: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Failed to start background service")
                return@launch
            }

            startLocationTracking(deviceId)

            _uiState.value = _uiState.value.copy(isRecording = true, error = null)
            setForegroundMonitoringOwnerActive(true)
            chunkIndex = 0
            stopGuard.set(false)

            recordingJob?.cancel()
            recordingJob = launch {
                while (_uiState.value.isRecording && !stopGuard.get()) {
                    try {
                        val chunkFile = File.createTempFile("chunk_$chunkIndex", ".wav")
                        chunkIndex++

                        val recorded = audioRecorder.recordAudio(1500, chunkFile)
                        val fileBytes = chunkFile.length()
                        val hasValidAudio = recorded != null && fileBytes > MIN_VALID_WAV_BYTES

                        if (!hasValidAudio) {
                            Log.w(
                                "ChildMonitoringViewModel",
                                "Skip uploading invalid/empty chunk: ${chunkFile.name}, bytes=$fileBytes"
                            )
                            chunkFile.delete()
                            ensureRecorderReady()
                            delay(350)
                            continue
                        }

                        _uiState.value = _uiState.value.copy(isUploading = true)
                        val requestStartedAt = SystemClock.elapsedRealtime()
                        detectionRepository.uploadDetectionChunk(
                            deviceId = deviceId,
                            audioFile = chunkFile,
                            latitude = latestLatitude,
                            longitude = latestLongitude,
                            batteryPercent = readBatteryPercent()
                        ).collect { result ->
                            when (result) {
                                is DetectionResult.Success -> {
                                    val data = result.data
                                    val latency = (SystemClock.elapsedRealtime() - requestStartedAt).coerceAtLeast(0L)
                                    val newLog = DetectionLogEntry(
                                        confidence = if (data.score != null) "${(data.score * 100).toInt()}%" else "--",
                                        streak = if (data.strangerStreak != null) "${data.strangerStreak} Samples" else "--",
                                        decision = data.decision?.replaceFirstChar { it.uppercase() } ?: "Hold",
                                        alertId = data.alertId ?: if (data.decision == "hold") "HOLD" else "OK",
                                        isSuccess = data.decision != "stranger_candidate"
                                    )

                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        lastDetectionStatus = data.status,
                                        lastDecision = data.decision,
                                        confidenceScore = data.score,
                                        uploadLatencyMs = latency,
                                        lastAlertFired = data.alertFired ?: false,
                                        lastAlertId = data.alertId,
                                        activityLogs = (listOf(newLog) + _uiState.value.activityLogs).take(10)
                                    )
                                }
                                is DetectionResult.Error -> {
                                    val latency = (SystemClock.elapsedRealtime() - requestStartedAt).coerceAtLeast(0L)
                                    _uiState.value = _uiState.value.copy(
                                        isUploading = false,
                                        uploadLatencyMs = latency
                                    )
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
                        delay(500)
                    } catch (e: Exception) {
                        Log.e("ChildMonitoringViewModel", "Recording loop error: ${e.message}")
                        ensureRecorderReady()
                        delay(350)
                    }
                }
            }
        }
    }

    fun stopDetectionStream(syncRemoteState: Boolean = true) {
        viewModelScope.launch {
            if (!stopGuard.compareAndSet(false, true)) {
                Log.d("ChildMonitoringViewModel", "stopDetectionStream ignored: already stopping")
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isStopping = true,
                isRecording = false,
                isUploading = false
            )
            setForegroundMonitoringOwnerActive(false)

            val deviceId = tokenManager.getDeviceId()

            if (syncRemoteState && deviceId != null) {
                updateRemoteMonitoring(deviceId = deviceId, enabled = false)
            }

            val job = recordingJob
            recordingJob = null
            if (job != null) {
                runCatching { job.cancelAndJoin() }
            }

            stopLocationTracking()

            withContext(Dispatchers.IO) {
                runCatching { audioRecorder.stopSafely() }
                runCatching { audioRecorder.release() }
            }

            _uiState.value = _uiState.value.copy(
                isRecording = false,
                isUploading = false,
                isStopping = false
            )

            if (deviceId != null) {
                launch(Dispatchers.IO) {
                    detectionRepository.endDetectionSession(deviceId).collect { }
                }
            }

            stopGuard.set(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking(deviceId: String) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    latestLatitude = loc.latitude
                    latestLongitude = loc.longitude
                    pushLocation(deviceId, loc.latitude, loc.longitude)
                }
            }

        val req = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            20_000L
        ).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                latestLatitude = loc.latitude
                latestLongitude = loc.longitude
                pushLocation(deviceId, loc.latitude, loc.longitude)
            }
        }
        locationCallback = callback
        fusedLocationClient.requestLocationUpdates(req, callback, Looper.getMainLooper())
    }

    private fun stopLocationTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun pushLocation(deviceId: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            detectionRepository.updateLocation(
                deviceId = deviceId,
                latitude = latitude,
                longitude = longitude,
                batteryPercent = readBatteryPercent()
            ).collect { }
        }
    }

    private fun startDeviceTelemetryRefresh() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (true) {
                val offline = appContext.getSharedPreferences(CONNECTIVITY_PREFS, Context.MODE_PRIVATE)
                    .getBoolean(KEY_IS_OFFLINE, false)
                _uiState.value = _uiState.value.copy(
                    batteryPercent = readBatteryPercent(),
                    isOffline = offline
                )
                delay(20_000L)
            }
        }
    }

    private fun startRemoteMonitoringSync() {
        monitoringSyncJob?.cancel()
        monitoringSyncJob = viewModelScope.launch {
            while (true) {
                syncRemoteMonitoringState()
                delay(8_000L)
            }
        }
    }

    private suspend fun syncRemoteMonitoringState() {
        val deviceId = tokenManager.getDeviceId() ?: return

        val selfDevice = deviceRepository.deviceCache.value.firstOrNull { it.id == deviceId }
            ?: return

        _uiState.value = _uiState.value.copy(
            remoteMonitoringEnabled = selfDevice.monitoringEnabled,
            batteryPercent = selfDevice.batteryPercent ?: _uiState.value.batteryPercent
        )

        applyRemoteMonitoringState(selfDevice.monitoringEnabled)
    }

    private fun applyRemoteMonitoringState(enabled: Boolean) {
        val state = _uiState.value
        if (enabled && !state.isRecording && !state.isStopping) {
            startDetectionStream(syncRemoteState = false)
            return
        }
        if (!enabled && state.isRecording && !state.isStopping) {
            stopDetectionStream(syncRemoteState = false)
        }
    }

    private fun readBatteryPercent(): Int? {
        val batteryIntent = appContext.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return null

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null

        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureRecorderReady(): Boolean {
        runCatching { audioRecorder.stopSafely() }
        return audioRecorder.initialize()
    }

    private suspend fun updateRemoteMonitoring(deviceId: String, enabled: Boolean) {
        deviceRepository.setMonitoringEnabled(deviceId, enabled).collect { result ->
            if (result is DeviceResult.Error) {
                _uiState.value = _uiState.value.copy(error = result.message)
            }
        }
    }

    private fun setForegroundMonitoringOwnerActive(active: Boolean) {
        appContext
            .getSharedPreferences(MONITOR_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FOREGROUND_OWNER_ACTIVE, active)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        stopGuard.set(true)
        setForegroundMonitoringOwnerActive(false)
        monitoringSyncJob?.cancel()
        telemetryJob?.cancel()
        recordingJob?.cancel()
        stopLocationTracking()
        audioRecorder.release()
    }
}
