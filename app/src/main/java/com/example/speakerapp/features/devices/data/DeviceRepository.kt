package com.example.speakerapp.features.devices.data

import com.example.speakerapp.core.network.Resource
import com.example.speakerapp.core.network.SafeEarApi
import com.example.speakerapp.core.network.dto.DeviceDto
import com.example.speakerapp.core.storage.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val api: SafeEarApi,
    private val sessionManager: SessionManager
) {
    suspend fun registerDevice(name: String, role: String, token: String? = null): Resource<DeviceDto> {
        return try {
            val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val rolePart = role.toRequestBody("text/plain".toMediaTypeOrNull())
            val tokenPart = token?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.registerDevice(namePart, rolePart, tokenPart)
            if (response.isSuccessful && response.body() != null) {
                val device = response.body()!!.device
                sessionManager.saveDeviceInfo(device.id, device.role, device.deviceName)
                Resource.Success(device)
            } else {
                Resource.Error(response.message() ?: "Failed to register device")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
