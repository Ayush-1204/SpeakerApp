package com.example.speakerapp.features.alerts.ui

import android.content.Context
import com.example.speakerapp.core.audio.AudioPlayer
import com.example.speakerapp.core.realtime.RealtimeSocketManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.enrollment.data.EnrollmentRepository
import com.example.speakerapp.features.enrollment.data.EnrollmentResult
import com.example.speakerapp.features.alerts.data.AlertsRepository
import com.example.speakerapp.features.alerts.data.AlertResult
import com.example.speakerapp.features.alerts.data.AlertItem
import com.example.speakerapp.features.devices.data.DeviceRepository
import com.example.speakerapp.features.devices.data.DeviceResult
import com.example.speakerapp.features.devices.data.MonitoredDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.time.Instant
import javax.inject.Inject

data class AlertsUiState(
    val isLoading: Boolean = false,
    val alerts: List<AlertItem> = emptyList(),
    val devices: List<MonitoredDevice> = emptyList(),
    val isLoadingDevices: Boolean = false,
    val togglingDeviceId: String? = null,
    val error: String? = null,
    val ackingAlertId: String? = null,
    val flaggingAlertId: String? = null,
    val deletingAlertId: String? = null,
    val isDeletingAll: Boolean = false
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val deviceRepository: DeviceRepository,
    private val realtimeSocketManager: RealtimeSocketManager,
    private val audioPlayer: AudioPlayer,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()
    private var realtimeJob: Job? = null

    init {
        loadAlerts()
        loadDevices()
        startRealtimeUpdates()
    }

    private fun startRealtimeUpdates() {
        realtimeSocketManager.acquire()
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            realtimeSocketManager.events.collect { event ->
                when (event.type.lowercase()) {
                    "alert_created", "stranger_detected", "alert.new" -> {
                        event.payload.toAlertItemOrNull()?.let { incoming ->
                            val current = _uiState.value.alerts
                            val deduped = current.filterNot { it.id == incoming.id }
                            _uiState.value = _uiState.value.copy(alerts = listOf(incoming) + deduped)
                        }
                    }
                    "device_status_changed", "device_updated", "monitoring_changed", "device.monitoring" -> {
                        event.payload.toMonitoredDeviceOrNull()?.let { incoming ->
                            val current = _uiState.value.devices
                            val exists = current.any { it.id == incoming.id }
                            val updated = if (exists) {
                                current.map { old -> if (old.id == incoming.id) old.merge(incoming) else old }
                            } else {
                                current + incoming
                            }
                            _uiState.value = _uiState.value.copy(devices = updated)
                        }
                    }
                }
            }
        }
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

    fun playAlertClip(alertId: String) {
        viewModelScope.launch {
            alertsRepository.getAlertClip(alertId).collect { result ->
                when (result) {
                    is AlertResult.Success -> {
                        audioPlayer.playFromBytes(result.data, fileName = "alert_$alertId.wav")
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(error = "Failed to fetch clip: ${result.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun flagAsFamiliar(alertId: String, displayName: String) {
        if (displayName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Display name is required")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(flaggingAlertId = alertId)

            alertsRepository.getAlertClip(alertId).collect { clipResult ->
                when (clipResult) {
                    is AlertResult.Success -> {
                        val clipFile = File(appContext.cacheDir, "enroll_from_alert_$alertId.wav")
                        clipFile.writeBytes(clipResult.data)

                        enrollmentRepository.enrollSpeaker(
                            displayName = displayName,
                            audioFile = clipFile
                        ).collect { enrollResult ->
                            when (enrollResult) {
                                is EnrollmentResult.Success -> {
                                    _uiState.value = _uiState.value.copy(flaggingAlertId = null, error = null)
                                }
                                is EnrollmentResult.Error -> {
                                    _uiState.value = _uiState.value.copy(
                                        flaggingAlertId = null,
                                        error = "Failed to enroll familiar: ${enrollResult.message}"
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            flaggingAlertId = null,
                            error = "Failed to fetch alert clip: ${clipResult.message}"
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.listDevices().collect { result ->
                when (result) {
                    is DeviceResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoadingDevices = true)
                    }
                    is DeviceResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDevices = false,
                            devices = result.data,
                            error = null
                        )
                    }
                    is DeviceResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingDevices = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun setMonitoringEnabled(deviceId: String, enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(togglingDeviceId = deviceId)
            deviceRepository.setMonitoringEnabled(deviceId, enabled).collect { result ->
                when (result) {
                    is DeviceResult.Success -> {
                        val updated = _uiState.value.devices.map { device ->
                            if (device.id == deviceId) {
                                device.copy(monitoringEnabled = result.data.monitoringEnabled)
                            } else {
                                device
                            }
                        }
                        _uiState.value = _uiState.value.copy(
                            devices = updated,
                            togglingDeviceId = null,
                            error = null
                        )
                    }
                    is DeviceResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            togglingDeviceId = null,
                            error = result.message
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(deletingAlertId = alertId)
            alertsRepository.deleteAlert(alertId).collect { result ->
                when (result) {
                    is AlertResult.Success -> {
                        // Remove from local list
                        val updatedAlerts = _uiState.value.alerts.filter { it.id != alertId }
                        _uiState.value = _uiState.value.copy(alerts = updatedAlerts, deletingAlertId = null)
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            deletingAlertId = null
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    fun deleteAllAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingAll = true)
            alertsRepository.deleteAllAlerts().collect { result ->
                when (result) {
                    is AlertResult.Success -> {
                        _uiState.value = _uiState.value.copy(alerts = emptyList(), isDeletingAll = false)
                    }
                    is AlertResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = result.message,
                            isDeletingAll = false
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        realtimeJob?.cancel()
        realtimeSocketManager.release()
    }
}

private fun JsonObject.toAlertItemOrNull(): AlertItem? {
    val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
    val deviceId = this["device_id"]?.jsonPrimitive?.contentOrNull ?: return null
    val timestamp = this["timestamp"]?.jsonPrimitive?.contentOrNull ?: Instant.now().toString()

    return AlertItem(
        id = id,
        deviceId = deviceId,
        timestamp = timestamp,
        timestampMs = this["timestamp_ms"]?.jsonPrimitive?.longOrNull,
        confidenceScore = this["confidence_score"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
        audioClipPath = this["audio_clip_path"]?.jsonPrimitive?.contentOrNull ?: "",
        latitude = this["latitude"]?.jsonPrimitive?.doubleOrNull,
        longitude = this["longitude"]?.jsonPrimitive?.doubleOrNull,
        lat = this["lat"]?.jsonPrimitive?.doubleOrNull,
        lng = this["lng"]?.jsonPrimitive?.doubleOrNull,
        acknowledgedAt = this["acknowledged_at"]?.jsonPrimitive?.contentOrNull,
        isAcknowledged = this["acknowledged_at"]?.jsonPrimitive?.contentOrNull != null
    )
}

private fun JsonObject.toMonitoredDeviceOrNull(): MonitoredDevice? {
    val id = this["id"]?.jsonPrimitive?.contentOrNull ?: return null
    return MonitoredDevice(
        id = id,
        deviceName = this["device_name"]?.jsonPrimitive?.contentOrNull ?: "Child Device",
        role = this["role"]?.jsonPrimitive?.contentOrNull ?: "child_device",
        batteryPercent = this["battery_percent"]?.jsonPrimitive?.intOrNull,
        isOnline = this["is_online"]?.jsonPrimitive?.booleanOrNull,
        monitoringEnabled = this["monitoring_enabled"]?.jsonPrimitive?.booleanOrNull ?: false
    )
}

private fun MonitoredDevice.merge(incoming: MonitoredDevice): MonitoredDevice {
    return copy(
        deviceName = incoming.deviceName.ifBlank { deviceName },
        role = incoming.role.ifBlank { role },
        batteryPercent = incoming.batteryPercent ?: batteryPercent,
        isOnline = incoming.isOnline ?: isOnline,
        monitoringEnabled = incoming.monitoringEnabled
    )
}