package com.example.speakerapp.network.dto

import kotlinx.serialization.Serializable

// ===== Speaker Enrollment Request/Response =====
@Serializable
data class EnrollmentStageVadInfo(
    val voiced_ms: Double,
    val num_segments: Int
)

@Serializable
data class EnrollmentStageSpeechQuality(
    val passed: Boolean
)

@Serializable
data class EnrollmentStages(
    val vad: EnrollmentStageVadInfo? = null,
    val speech_quality: EnrollmentStageSpeechQuality? = null
)

@Serializable
data class EnrollSpeakerResponse(
    val status: String,
    val speaker_id: String,
    val display_name: String,
    val samples_saved: Int,
    val embedding_dim: Int,
    val stages: EnrollmentStages
)

@Serializable
data class SpeakerInfo(
    val id: String,
    val parent_id: String,
    val display_name: String,
    val sample_count: Int,
    val profile_image_url: String? = null,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class SpeakersListResponse(
    val items: List<SpeakerInfo>
)

@Serializable
data class UpdateSpeakerRequest(
    val display_name: String
)
