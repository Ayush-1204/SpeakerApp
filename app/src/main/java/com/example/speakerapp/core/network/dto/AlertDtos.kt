package com.example.speakerapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlertDto(
    @SerialName("id") val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("confidence_score") val confidenceScore: Float,
    @SerialName("audio_clip_path") val audioClipPath: String,
    @SerialName("latitude") val latitude: Double?,
    @SerialName("longitude") val longitude: Double?,
    @SerialName("acknowledged_at") val acknowledgedAt: String?,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class AlertListResponse(
    @SerialName("items") val items: List<AlertDto>
)
