package com.example.speakerapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EnrollmentResponse(
    @SerialName("status") val status: String,
    @SerialName("speaker_id") val speakerId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("samples_saved") val samplesSaved: Int,
    @SerialName("embedding_dim") val embeddingDim: Int,
    @SerialName("stages") val stages: EnrollmentStagesDto
)

@Serializable
data class EnrollmentStagesDto(
    @SerialName("vad") val vad: VadDto,
    @SerialName("speech_quality") val speechQuality: SpeechQualityDto
)

@Serializable
data class VadDto(
    @SerialName("voiced_ms") val voicedMs: Float,
    @SerialName("num_segments") val numSegments: Int
)

@Serializable
data class SpeechQualityDto(
    @SerialName("passed") val passed: Boolean
)

@Serializable
data class SpeakerDto(
    @SerialName("id") val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("sample_count") val sampleCount: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SpeakerListResponse(
    @SerialName("items") val items: List<SpeakerDto>
)

@Serializable
data class UpdateSpeakerRequest(
    @SerialName("display_name") val displayName: String
)
