package com.example.speakerapp.features.devices.data

import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.toTextBody
import com.example.speakerapp.network.dto.DeviceMonitoringUpdateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class DeviceResult<out T> {
    data class Success<T>(val data: T) : DeviceResult<T>()
    data class Error(val message: String, val code: Int? = null) : DeviceResult<Nothing>()
    object Loading : DeviceResult<Nothing>()
}

data class RegisteredDevice(
    val id: String,
    val parentId: String,
    val deviceName: String,
    val role: String, // "child_device" or "parent_device"
    val deviceToken: String?
)

data class MonitoredDevice(
    val id: String,
    val deviceName: String,
    val role: String,
    val batteryPercent: Int?,
    val isOnline: Boolean?,
    val monitoringEnabled: Boolean
)

class DeviceRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Register a new device with specified role.
     * Valid roles: "child_device", "parent_device"
     * Exact endpoint: POST /devices
     * Exact role values required: "child_device" or "parent_device"
     */
    suspend fun registerDevice(
        deviceName: String? = null,
        role: String,
        deviceToken: String? = null
    ): Flow<DeviceResult<RegisteredDevice>> = flow {
        emit(DeviceResult.Loading)
        try {
            if (role != "parent_device" && role != "child_device") {
                emit(DeviceResult.Error(message = "Role must be parent_device or child_device", code = 422))
                return@flow
            }

            val deviceNamePart = deviceName
                ?.takeIf { it.isNotBlank() }
                ?.toTextBody()
            val rolePart = role.toTextBody()
            val deviceTokenPart = deviceToken?.toTextBody()

            val response = apiService.createDevice(
                deviceName = deviceNamePart,
                role = rolePart,
                deviceToken = deviceTokenPart
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val device = body.device

                // Save device info
                tokenManager.saveDeviceInfo(device.id, device.role)

                emit(DeviceResult.Success(
                    RegisteredDevice(
                        id = device.id,
                        parentId = device.parent_id,
                        deviceName = device.device_name,
                        role = device.role,
                        deviceToken = device.device_token
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Device registration failed"
                val friendly = when (response.code()) {
                    401 -> "Session expired. Please login again."
                    403 -> "Role is not allowed for this action."
                    422 -> "Invalid device data. Please check and retry."
                    else -> errorDetail
                }
                emit(DeviceResult.Error(
                    message = friendly,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(DeviceResult.Error(message = "Network error. Please retry."))
        }
    }

    suspend fun getDeviceId(): String? = tokenManager.getDeviceId()

    suspend fun getDeviceRole(): String? = tokenManager.getDeviceRole()

    suspend fun hasDeviceInfo(): Boolean = tokenManager.hasDeviceInfo()

    suspend fun listDevices(): Flow<DeviceResult<List<MonitoredDevice>>> = flow {
        emit(DeviceResult.Loading)
        try {
            val response = apiService.listDevices()
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val sourceItems = when {
                    body.items.isNotEmpty() -> body.items
                    body.devices.isNotEmpty() -> body.devices
                    else -> body.data
                }

                val devices = sourceItems.map { item ->
                    MonitoredDevice(
                        id = item.id,
                        deviceName = item.device_name,
                        role = item.role,
                        batteryPercent = item.battery_percent,
                        isOnline = item.is_online,
                        monitoringEnabled = item.monitoring_enabled ?: false
                    )
                }

                val childDevices = devices.filter { it.role == "child_device" }
                emit(DeviceResult.Success(if (childDevices.isNotEmpty()) childDevices else devices))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to load devices"
                emit(DeviceResult.Error(message = errorDetail, code = response.code()))
            }
        } catch (e: Exception) {
            emit(DeviceResult.Error(message = e.message ?: "Network error. Please retry."))
        }
    }

    suspend fun setMonitoringEnabled(
        deviceId: String,
        enabled: Boolean
    ): Flow<DeviceResult<MonitoredDevice>> = flow {
        emit(DeviceResult.Loading)
        try {
            val response = apiService.setDeviceMonitoring(
                deviceId = deviceId,
                request = DeviceMonitoringUpdateRequest(monitoring_enabled = enabled)
            )

            if (response.isSuccessful) {
                val item = response.body() ?: throw Exception("Empty response body")
                emit(
                    DeviceResult.Success(
                        MonitoredDevice(
                            id = item.id,
                            deviceName = item.device_name,
                            role = item.role,
                            batteryPercent = item.battery_percent,
                            isOnline = item.is_online,
                            monitoringEnabled = item.monitoring_enabled ?: enabled
                        )
                    )
                )
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to update monitoring"
                emit(DeviceResult.Error(message = errorDetail, code = response.code()))
            }
        } catch (e: Exception) {
            emit(DeviceResult.Error(message = e.message ?: "Network error. Please retry."))
        }
    }
}
