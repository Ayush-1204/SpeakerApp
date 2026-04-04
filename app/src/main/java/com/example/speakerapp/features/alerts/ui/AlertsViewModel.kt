package com.example.speakerapp.features.alerts.ui

import android.content.Context
import com.example.speakerapp.core.audio.AudioPlayer
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
import kotlinx.coroutines.launch
import java.io.File
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
    private val audioPlayer: AudioPlayer,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        loadAlerts()
        loadDevices()
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
}