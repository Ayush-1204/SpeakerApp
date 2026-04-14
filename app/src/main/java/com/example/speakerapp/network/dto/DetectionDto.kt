package com.example.speakerapp.network.dto

import kotlinx.serialization.Serializable

// ===== Location Update Request =====
@Serializable
data class LocationUpdateRequest(
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val battery_percent: Int? = null,
    val battery: Int? = null
)

// ===== Detection Chunk Response =====
@Serializable
data class DetectionThresholds(
    val t_high: Double,
    val t_low: Double
)

@Serializable
data class DetectionStageInfo(
    val tier: String? = null
)

@Serializable
data class DetectionChunkResponse(
    val status: String, // "warming_up", "no_hop", or "ok"
    val decision: String? = null, // "familiar", "stranger_candidate", "uncertain", "hold"
    val stranger_streak: Int? = null,
    val score: Double? = null,
    val thresholds: DetectionThresholds? = null,
    val stage: DetectionStageInfo? = null,
    val alert_fired: Boolean? = null,
    val alert_id: String? = null,
    val idempotent_replay: Boolean? = null
)
