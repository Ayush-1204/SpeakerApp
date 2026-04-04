package com.example.speakerapp.features.devices.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.features.devices.data.DeviceRepository
import com.example.speakerapp.features.devices.data.DeviceResult
import com.example.speakerapp.features.devices.data.RegisteredDevice
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeviceRegistrationUiState(
    val isLoading: Boolean = false,
    val device: RegisteredDevice? = null,
    val error: String? = null
)

@HiltViewModel
class DeviceRegistrationViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceRegistrationUiState())
    val uiState: StateFlow<DeviceRegistrationUiState> = _uiState.asStateFlow()

    /**
     * Register device with specified role.
     * Valid roles: "child_device", "parent_device"
     */
    fun registerDevice(role: String) {
        if (role != "parent_device" && role != "child_device") {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Role must be exactly parent_device or child_device"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                val fcmToken = if (task.isSuccessful) task.result else null
                viewModelScope.launch {
                    deviceRepository.registerDevice(
                        deviceName = null,
                        role = role,
                        deviceToken = fcmToken
                    ).collect { result ->
                        when (result) {
                            is DeviceResult.Loading -> {
                                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                            }
                            is DeviceResult.Success -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    device = result.data,
                                    error = null
                                )
                            }
                            is DeviceResult.Error -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = result.message
                                )
                            }
                        }
                    }
                }
            }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
