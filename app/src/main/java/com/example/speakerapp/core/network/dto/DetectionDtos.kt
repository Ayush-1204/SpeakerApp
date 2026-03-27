package com.example.speakerapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double
)

@Serializable
data class DetectionResponse(
    @SerialName("status") val status: String,
    @SerialName("decision") val decision: String? = null,
    @SerialName("stranger_streak") val strangerStreak: Int? = null,
    @SerialName("score") val score: Float? = null,
    @SerialName("thresholds") val thresholds: ThresholdsDto? = null,
    @SerialName("alert_fired") val alertFired: Boolean? = null,
    @SerialName("alert_id") val alertId: String? = null
)

@Serializable
data class ThresholdsDto(
    @SerialName("t_high") val tHigh: Float,
    @SerialName("t_low") val tLow: Float
)
