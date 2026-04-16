package com.example.speakerapp.features.devices.data

import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.toTextBody
import com.example.speakerapp.network.dto.DeviceMonitoringUpdateRequest
import com.example.speakerapp.network.dto.UpdateDeviceTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException
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
    private val tokenManager: TokenManager,
    @ApplicationContext private val appContext: Context
) {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _deviceCache = MutableStateFlow<List<MonitoredDevice>>(emptyList())
    val deviceCache: StateFlow<List<MonitoredDevice>> = _deviceCache.asStateFlow()
    private val pollingLock = Any()
    @Volatile private var pollJob: Job? = null
    private var pollSubscribers = 0

    /**
     * Register a new device with specified role.
     * Valid roles: "child_device", "parent_device"
        * Exact endpoint: POST /devices/upsert
     * Exact role values required: "child_device" or "parent_device"
     */
    suspend fun registerDevice(
        deviceName: String? = null,
        role: String,
        deviceToken: String? = null
    ): Flow<DeviceResult<RegisteredDevice>> = flow {
        emit(DeviceResult.Loading)
        try {
            val normalizedRole = role.trim().lowercase()

            if (normalizedRole != "parent_device" && normalizedRole != "child_device") {
                emit(DeviceResult.Error(message = "Role must be parent_device or child_device", code = 422))
                return@flow
            }

            val resolvedDeviceName = deviceName
                ?.takeIf { it.isNotBlank() }
                ?: buildStableDeviceName()

            val deviceNamePart = resolvedDeviceName
                ?.toTextBody()
            val rolePart = normalizedRole.toTextBody()
            val resolvedDeviceToken = deviceToken?.takeIf { it.isNotBlank() } ?: tokenManager.getFcmToken()
            val deviceTokenPart = resolvedDeviceToken?.toTextBody()
            val installationIdPart = tokenManager.getOrCreateInstallationId(normalizedRole).toTextBody()

            val response = apiService.upsertDevice(
                installationId = installationIdPart,
                deviceName = deviceNamePart,
                role = rolePart,
                deviceToken = deviceTokenPart
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val device = body.device

                val previousDeviceId = tokenManager.getDeviceId()
                val previousRole = tokenManager.getDeviceRole()

                if (
                    previousRole == "child_device" &&
                    previousDeviceId != null &&
                    previousDeviceId != device.id
                ) {
                    runCatching {
                        apiService.setDeviceMonitoring(
                            deviceId = previousDeviceId,
                            request = DeviceMonitoringUpdateRequest(monitoring_enabled = false)
                        )
                    }
                }

                // Preserve the user's selected role locally even if backend returns a stale role.
                tokenManager.clearDeviceInfo()
                tokenManager.saveDeviceInfo(device.id, normalizedRole)

                emit(DeviceResult.Success(
                    RegisteredDevice(
                        id = device.id,
                        parentId = device.parent_id,
                        deviceName = device.device_name,
                        role = normalizedRole,
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

    suspend fun syncFcmTokenWithRetry(
        initialDeviceId: String,
        token: String,
        maxRetries: Int = 3
    ): DeviceResult<Unit> {
        var currentDeviceId = initialDeviceId

        repeat(maxRetries) { attempt ->
            try {
                val response = apiService.updateDeviceToken(
                    deviceId = currentDeviceId,
                    request = UpdateDeviceTokenRequest(device_token = token)
                )

                if (response.isSuccessful) {
                    return DeviceResult.Success(Unit)
                }

                if (response.code() == 409) {
                    val remapped = resolveLatestDeviceMapping()
                    if (!remapped.isNullOrBlank()) {
                        currentDeviceId = remapped
                        return@repeat
                    }
                    return DeviceResult.Error(
                        message = "Token conflict and device remap failed",
                        code = 409
                    )
                }

                if (response.code() in 500..599 && attempt < maxRetries - 1) {
                    delay((attempt + 1) * 1000L)
                    return@repeat
                }

                val errorDetail = response.errorBody()?.string() ?: "Failed to update FCM token"
                return DeviceResult.Error(message = errorDetail, code = response.code())
            } catch (e: IOException) {
                if (attempt < maxRetries - 1) {
                    delay((attempt + 1) * 1000L)
                } else {
                    return DeviceResult.Error(message = "Network error. Please retry.")
                }
            } catch (e: Exception) {
                return DeviceResult.Error(message = e.message ?: "Failed to update FCM token")
            }
        }

        return DeviceResult.Error(message = "Failed to update FCM token")
    }

    suspend fun updateFcmToken(deviceId: String, token: String): Flow<DeviceResult<Unit>> = flow {
        emit(DeviceResult.Loading)
        try {
            val response = apiService.updateDeviceToken(
                deviceId = deviceId,
                request = UpdateDeviceTokenRequest(device_token = token)
            )

            if (response.isSuccessful) {
                emit(DeviceResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to update FCM token"
                emit(DeviceResult.Error(message = errorDetail, code = response.code()))
            }
        } catch (e: Exception) {
            emit(DeviceResult.Error(message = e.message ?: "Network error. Please retry."))
        }
    }

    private suspend fun resolveLatestDeviceMapping(): String? {
        return try {
            val response = apiService.listDevices()
            if (!response.isSuccessful) return null

            val body = response.body() ?: return null
            val sourceItems = when {
                body.items.isNotEmpty() -> body.items
                body.devices.isNotEmpty() -> body.devices
                else -> body.data
            }

            val currentRole = tokenManager.getDeviceRole()
            val stableDeviceName = buildStableDeviceName()
            val candidate = sourceItems.firstOrNull {
                it.device_name == stableDeviceName && (currentRole == null || it.role == currentRole)
            }

            candidate?.let {
                val resolvedRole = currentRole ?: it.role
                tokenManager.saveDeviceInfo(it.id, resolvedRole)
                it.id
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildStableDeviceName(): String {
        val androidId = runCatching {
            Settings.Secure.getString(appContext.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"

        val model = Build.MODEL?.replace("\\s+".toRegex(), "_")?.lowercase() ?: "android"
        return "speakerapp_${model}_${androidId}"
    }

    suspend fun getDeviceId(): String? = tokenManager.getDeviceId()

    suspend fun getDeviceRole(): String? = tokenManager.getDeviceRole()

    suspend fun hasDeviceInfo(): Boolean = tokenManager.hasDeviceInfo()

    fun startDevicePolling(pollIntervalMs: Long = 15_000L) {
        synchronized(pollingLock) {
            pollSubscribers += 1
            if (pollJob?.isActive == true) {
                return
            }

            pollJob = repositoryScope.launch {
                while (isActive) {
                    try {
                        val response = apiService.listDevices()
                        if (response.isSuccessful) {
                            val devices = response.body().toMonitoredDevices()
                            _deviceCache.value = filterDevicesForCurrentRole(devices)
                        }
                    } catch (_: Exception) {
                        // Keep the last known cache and retry on the next loop.
                    }

                    delay(pollIntervalMs)
                }
            }
        }
    }

    fun stopDevicePolling() {
        synchronized(pollingLock) {
            pollSubscribers = (pollSubscribers - 1).coerceAtLeast(0)
            if (pollSubscribers > 0) {
                return
            }

            pollJob?.cancel()
            pollJob = null
        }
    }

    suspend fun listDevices(): Flow<DeviceResult<List<MonitoredDevice>>> = flow {
        emit(DeviceResult.Loading)
        try {
            val response = apiService.listDevices()
            if (response.isSuccessful) {
                val devices = response.body().toMonitoredDevices()
                val filteredForRole = filterDevicesForCurrentRole(devices)
                _deviceCache.value = filteredForRole
                emit(DeviceResult.Success(filteredForRole))
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

    private suspend fun filterDevicesForCurrentRole(devices: List<MonitoredDevice>): List<MonitoredDevice> {
        val childDevices = devices.filter { it.role == "child_device" }
        val currentRole = tokenManager.getDeviceRole()
        val currentDeviceId = tokenManager.getDeviceId()
        val currentStableDeviceName = buildStableDeviceName()

        return if (currentRole == "parent_device") {
            childDevices.filterNot { device ->
                device.id == currentDeviceId || device.deviceName == currentStableDeviceName
            }
        } else {
            childDevices
        }
    }

    private fun com.example.speakerapp.network.dto.DeviceListResponse?.toMonitoredDevices(): List<MonitoredDevice> {
        val sourceItems = when {
            this == null -> emptyList()
            items.isNotEmpty() -> items
            devices.isNotEmpty() -> devices
            else -> data
        }

        return sourceItems.map { item ->
            MonitoredDevice(
                id = item.id,
                deviceName = item.device_name,
                role = item.role,
                batteryPercent = item.battery_percent,
                isOnline = item.is_online,
                monitoringEnabled = item.monitoring_enabled ?: false
            )
        }
    }
}
