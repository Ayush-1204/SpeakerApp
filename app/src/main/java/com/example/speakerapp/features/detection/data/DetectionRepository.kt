package com.example.speakerapp.features.detection.data

import com.example.speakerapp.core.network.Resource
import com.example.speakerapp.core.network.SafeEarApi
import com.example.speakerapp.core.network.dto.DetectionResponse
import com.example.speakerapp.core.network.dto.LocationRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetectionRepository @Inject constructor(
    private val api: SafeEarApi
) {
    suspend fun uploadChunk(
        deviceId: String,
        audioFile: File,
        latitude: Double? = null,
        longitude: Double? = null
    ): Resource<DetectionResponse> {
        return try {
            val deviceIdPart = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            
            val latPart = latitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lngPart = longitude?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = api.uploadChunk(deviceIdPart, filePart, latPart, lngPart)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Chunk upload failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun updateLocation(deviceId: String, lat: Double, lng: Double): Resource<Unit> {
        return try {
            val response = api.updateLocation(LocationRequest(deviceId, lat, lng))
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.message() ?: "Location update failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun endSession(deviceId: String): Resource<Unit> {
        return try {
            val response = api.endSession(deviceId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.message() ?: "Failed to end session")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
