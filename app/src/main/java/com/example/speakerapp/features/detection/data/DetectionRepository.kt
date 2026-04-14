package com.example.speakerapp.features.detection.data

import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.makeAudioPart
import com.example.speakerapp.network.toTextBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

sealed class DetectionResult<out T> {
    data class Success<T>(val data: T) : DetectionResult<T>()
    data class Error(val message: String, val code: Int? = null) : DetectionResult<Nothing>()
    object Loading : DetectionResult<Nothing>()
}

data class DetectionResponse(
    val status: String, // "warming_up", "no_hop", or "ok"
    val decision: String?, // "familiar", "stranger_candidate", "uncertain", "hold"
    val score: Double?,
    val strangerStreak: Int?,
    val thresholds: DetectionThresholds?,
    val alertFired: Boolean?,
    val alertId: String?,
    val idempotentReplay: Boolean?
)

data class DetectionThresholds(
    val tHigh: Double,
    val tLow: Double
)

class DetectionRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Upload audio chunk for detection/verification.
     * Exact endpoint: POST /detect/chunk
     * Fields: device_id (required), file (required), latitude/longitude (optional)
     * Audio constraints: 16kHz mono, WAV format
     * Role requirement: Callable only by child_device (403 if parent attempts)
     * Status responses: "warming_up", "no_hop", "ok"
     */
    suspend fun uploadDetectionChunk(
        deviceId: String,
        audioFile: File,
        latitude: Double? = null,
        longitude: Double? = null,
        batteryPercent: Int? = null
    ): Flow<DetectionResult<DetectionResponse>> = flow {
        emit(DetectionResult.Loading)
        try {
            val sampleRate = 16000
            check(sampleRate == 16000) { "Audio must be 16kHz WAV" }

            val deviceIdPart = deviceId.toTextBody()
            val chunkIdPart = buildChunkId(deviceId, audioFile).toTextBody()
            val latitudePart = latitude?.toString()?.toTextBody()
            val longitudePart = longitude?.toString()?.toTextBody()
            val batteryPart = batteryPercent?.toString()?.toTextBody()
            val audioPart = makeAudioPart(audioFile, fieldName = "audio")

            val response = apiService.detectChunk(
                deviceId = deviceIdPart,
                chunkId = chunkIdPart,
                latitude = latitudePart,
                longitude = longitudePart,
                batteryPercent = batteryPart,
                battery = batteryPart,
                audio = audioPart,
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")

                val thresholds = if (body.thresholds != null) {
                    DetectionThresholds(
                        tHigh = body.thresholds.t_high,
                        tLow = body.thresholds.t_low
                    )
                } else {
                    null
                }

                emit(DetectionResult.Success(
                    DetectionResponse(
                        status = body.status,
                        decision = body.decision,
                        score = body.score,
                        strangerStreak = body.stranger_streak,
                        thresholds = thresholds,
                        alertFired = body.alert_fired,
                        alertId = body.alert_id,
                        idempotentReplay = body.idempotent_replay
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Detection upload failed"
                // Handle specific error codes
                val message = when (response.code()) {
                    403 -> "Only child devices can upload audio chunks"
                    400 -> if (errorDetail.contains("audio_chunk_must_be_16khz")) {
                        "Audio must be 16kHz"
                    } else {
                        errorDetail
                    }
                    else -> errorDetail
                }
                emit(DetectionResult.Error(
                    message = message,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(DetectionResult.Error(message = e.message ?: "Network error"))
        }
    }

    suspend fun uploadDetectionChunkStrict(
        deviceId: String,
        audioFile: File,
        latitude: Double? = null,
        longitude: Double? = null,
        batteryPercent: Int? = null,
        chunkId: String = buildChunkId(deviceId, audioFile)
    ) {
        val deviceIdPart = deviceId.toTextBody()
        val chunkIdPart = chunkId.toTextBody()
        val latitudePart = latitude?.toString()?.toTextBody()
        val longitudePart = longitude?.toString()?.toTextBody()
        val batteryPart = batteryPercent?.toString()?.toTextBody()
        val audioPart = makeAudioPart(audioFile, fieldName = "audio")

        val response = apiService.detectChunk(
            deviceId = deviceIdPart,
            chunkId = chunkIdPart,
            latitude = latitudePart,
            longitude = longitudePart,
            batteryPercent = batteryPart,
            battery = batteryPart,
            audio = audioPart,
        )

        if (!response.isSuccessful) {
            throw HttpException(response)
        }
    }

    private fun buildChunkId(deviceId: String, audioFile: File): String {
        val bytes = audioFile.readBytes()
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(deviceId.toByteArray(Charsets.UTF_8))
        digest.update(bytes)
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        return hash.take(40)
    }

    /**
     * Update device location.
     * Exact endpoint: POST /detect/location
     */
    suspend fun updateLocation(
        deviceId: String,
        latitude: Double,
        longitude: Double,
        batteryPercent: Int? = null
    ): Flow<DetectionResult<Unit>> = flow {
        emit(DetectionResult.Loading)
        try {
            val locationRequest = com.example.speakerapp.network.dto.LocationUpdateRequest(
                device_id = deviceId,
                latitude = latitude,
                longitude = longitude,
                battery_percent = batteryPercent,
                battery = batteryPercent
            )

            val response = apiService.sendLocation(locationRequest)

            if (response.isSuccessful) {
                emit(DetectionResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Location update failed"
                emit(DetectionResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(DetectionResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * End detection session.
     * Exact endpoint: DELETE /detect/session?device_id=<id>
     */
    suspend fun endDetectionSession(deviceId: String): Flow<DetectionResult<Unit>> = flow {
        emit(DetectionResult.Loading)
        try {
            val response = apiService.deleteDetectionSession(deviceId = deviceId)

            if (response.isSuccessful) {
                emit(DetectionResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to end session"
                emit(DetectionResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(DetectionResult.Error(message = e.message ?: "Network error"))
        }
    }
}

