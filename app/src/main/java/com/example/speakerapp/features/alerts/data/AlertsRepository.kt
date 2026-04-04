package com.example.speakerapp.features.alerts.data

import com.example.speakerapp.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

sealed class AlertResult<out T> {
    data class Success<T>(val data: T) : AlertResult<T>()
    data class Error(val message: String, val code: Int? = null) : AlertResult<Nothing>()
    object Loading : AlertResult<Nothing>()
}

data class AlertItem(
    val id: String,
    val deviceId: String,
    val timestamp: String,
    val timestampMs: Long? = null,
    val confidenceScore: Double,
    val audioClipPath: String,
    val latitude: Double?,
    val longitude: Double?,
    val lat: Double?,
    val lng: Double?,
    val acknowledgedAt: String?,
    val isAcknowledged: Boolean
) {
    val mapLat: Double? get() = latitude ?: lat
    val mapLng: Double? get() = longitude ?: lng
}

class AlertsRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Get alerts for current parent.
     * Exact endpoint: GET /alerts?limit=50&offset=0
     */
    fun getAlerts(
        limit: Int = 50,
        offset: Int = 0
    ): Flow<AlertResult<List<AlertItem>>> = flow {
        emit(AlertResult.Loading)
        try {
            val response = apiService.getAlerts(limit = limit, offset = offset)

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")

                val alerts = body.items.map { item ->
                    AlertItem(
                        id = item.id,
                        deviceId = item.device_id,
                        timestamp = item.timestamp,
                        timestampMs = item.timestamp_ms,
                        confidenceScore = item.confidence_score,
                        audioClipPath = item.audio_clip_path,
                        latitude = item.latitude,
                        longitude = item.longitude,
                        lat = item.lat,
                        lng = item.lng,
                        acknowledgedAt = item.acknowledged_at,
                        isAcknowledged = item.acknowledged_at != null
                    )
                }

                emit(AlertResult.Success(alerts))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to load alerts"
                emit(AlertResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(AlertResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Acknowledge an alert.
     * Exact endpoint: POST /alerts/{alert_id}/ack
     */
    fun acknowledgeAlert(alertId: String): Flow<AlertResult<AlertItem>> = flow {
        emit(AlertResult.Loading)
        try {
            val response = apiService.acknowledgeAlert(alertId = alertId)

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")

                emit(AlertResult.Success(
                    AlertItem(
                        id = body.id,
                        deviceId = body.device_id,
                        timestamp = body.timestamp,
                        timestampMs = body.timestamp_ms,
                        confidenceScore = body.confidence_score,
                        audioClipPath = body.audio_clip_path,
                        latitude = body.latitude,
                        longitude = body.longitude,
                        lat = body.lat,
                        lng = body.lng,
                        acknowledgedAt = body.acknowledged_at,
                        isAcknowledged = body.acknowledged_at != null
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to acknowledge alert"
                emit(AlertResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(AlertResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Get alert audio clip.
     * Exact endpoint: GET /alerts/{alert_id}/clip
     * Returns audio/wav bytes
     */
    fun getAlertClip(alertId: String): Flow<AlertResult<ByteArray>> = flow {
        emit(AlertResult.Loading)
        try {
            val response = apiService.getAlertClip(alertId = alertId)

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                val bytes = body.bytes()
                emit(AlertResult.Success(bytes))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to load clip"
                emit(AlertResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(AlertResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Delete a specific alert.
     * Exact endpoint: DELETE /alerts/{alert_id}
     */
    fun deleteAlert(alertId: String): Flow<AlertResult<Unit>> = flow {
        emit(AlertResult.Loading)
        try {
            val response = apiService.deleteAlert(alertId = alertId)

            if (response.isSuccessful) {
                emit(AlertResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to delete alert"
                emit(AlertResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(AlertResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Delete all alerts.
     * Exact endpoint: DELETE /alerts
     */
    fun deleteAllAlerts(): Flow<AlertResult<Unit>> = flow {
        emit(AlertResult.Loading)
        try {
            val response = apiService.deleteAllAlerts()

            if (response.isSuccessful) {
                emit(AlertResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to delete all alerts"
                emit(AlertResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(AlertResult.Error(message = e.message ?: "Network error"))
        }
    }
}
