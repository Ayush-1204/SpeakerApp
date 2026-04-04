package com.example.speakerapp.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== Alert Models =====
@Serializable
data class AlertInfo(
    val id: String,
    val parent_id: String,
    val device_id: String,
    val timestamp: String,
    @SerialName("timestamp_ms") val timestamp_ms: Long? = null,
    val confidence_score: Double,
    val audio_clip_path: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val acknowledged_at: String? = null,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class AlertsListResponse(
    val items: List<AlertInfo>
)

@Serializable
data class AckAlertResponse(
    val id: String,
    val parent_id: String,
    val device_id: String,
    val timestamp: String,
    @SerialName("timestamp_ms") val timestamp_ms: Long? = null,
    val confidence_score: Double,
    val audio_clip_path: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val acknowledged_at: String? = null,
    val created_at: String,
    val updated_at: String
)

// ===== Health Check =====
@Serializable
data class HealthResponse(
    val status: String? = null
)
