package com.example.speakerapp.features.enrollment.data

import android.util.Log
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.makeAudioPart
import com.example.speakerapp.network.makeImagePart
import com.example.speakerapp.network.toTextBody
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

sealed class EnrollmentResult<out T> {
    data class Success<T>(val data: T) : EnrollmentResult<T>()
    data class Error(val message: String, val code: Int? = null) : EnrollmentResult<Nothing>()
    object Loading : EnrollmentResult<Nothing>()
}

data class EnrolledSpeaker(
    val speakerId: String,
    val displayName: String,
    val samplesSaved: Int,
    val embeddingDim: Int,
    val voicedMs: Double?,
    val numSegments: Int?,
    val speechQualityPassed: Boolean?
)

data class SpeakerListItem(
    val id: String,
    val displayName: String,
    val sampleCount: Int,
    val profileImageUrl: String?,
    val createdAt: String,
    val updatedAt: String
)

class EnrollmentRepository @Inject constructor(
    private val apiService: ApiService
) {

    /**
     * Enroll a speaker by uploading a WAV file.
     * Exact endpoint: POST /enroll/speaker
     * Fields: display_name (required), speaker_id (optional), file (required as multipart)
     */
    suspend fun enrollSpeaker(
        displayName: String,
        audioFile: File,
        speakerId: String? = null
    ): Flow<EnrollmentResult<EnrolledSpeaker>> = flow {
        emit(EnrollmentResult.Loading)
        try {
            val displayNamePart = displayName.toTextBody()
            val speakerIdPart = speakerId?.toTextBody()
            val audioPart = makeAudioPart(audioFile, fieldName = "audio")

            val response = apiService.enrollSpeaker(
                displayName = displayNamePart,
                speakerId = speakerIdPart,
                audio = audioPart
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                Log.d(
                    "EnrollmentRepository",
                    "enroll success speaker_id=${body.speaker_id} samples_saved=${body.samples_saved}"
                )

                emit(EnrollmentResult.Success(
                    EnrolledSpeaker(
                        speakerId = body.speaker_id,
                        displayName = body.display_name,
                        samplesSaved = body.samples_saved,
                        embeddingDim = body.embedding_dim,
                        voicedMs = body.stages.vad?.voiced_ms,
                        numSegments = body.stages.vad?.num_segments,
                        speechQualityPassed = body.stages.speech_quality?.passed
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Enrollment failed"
                emit(EnrollmentResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(EnrollmentResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Get list of enrolled speakers for current parent.
     * Exact endpoint: GET /enroll/speakers
     */
    suspend fun listSpeakers(): Flow<EnrollmentResult<List<SpeakerListItem>>> = flow {
        emit(EnrollmentResult.Loading)
        try {
            val response = apiService.listSpeakers()

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                Log.d("EnrollmentRepository", "list speakers items.size=${body.items.size}")

                val speakers = body.items.map { item ->
                    SpeakerListItem(
                        id = item.id,
                        displayName = item.display_name,
                        sampleCount = item.sample_count,
                        profileImageUrl = item.profile_image_url,
                        createdAt = item.created_at,
                        updatedAt = item.updated_at
                    )
                }

                emit(EnrollmentResult.Success(speakers))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to load speakers"
                emit(EnrollmentResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(EnrollmentResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Update speaker display name.
     * Exact endpoint: PATCH /enroll/speakers/{speaker_id}
     */
    suspend fun updateSpeaker(
        speakerId: String,
        displayName: String
    ): Flow<EnrollmentResult<SpeakerListItem>> = flow {
        emit(EnrollmentResult.Loading)
        try {
            val response = apiService.updateSpeaker(
                speakerId = speakerId,
                request = com.example.speakerapp.network.dto.UpdateSpeakerRequest(display_name = displayName)
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")

                emit(EnrollmentResult.Success(
                    SpeakerListItem(
                        id = body.id,
                        displayName = body.display_name,
                        sampleCount = body.sample_count,
                        profileImageUrl = body.profile_image_url,
                        createdAt = body.created_at,
                        updatedAt = body.updated_at
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to update speaker"
                emit(EnrollmentResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(EnrollmentResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Update speaker avatar image.
     * Exact endpoint: POST /enroll/speakers/{speaker_id}/avatar
     */
    suspend fun updateSpeakerAvatar(
        speakerId: String,
        imageFile: File
    ): Flow<EnrollmentResult<SpeakerListItem>> = flow {
        emit(EnrollmentResult.Loading)
        try {
            val imagePart = makeImagePart(imageFile, fieldName = "profile_image")
            val response = apiService.updateSpeakerAvatar(
                speakerId = speakerId,
                image = imagePart
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                emit(EnrollmentResult.Success(
                    SpeakerListItem(
                        id = body.id,
                        displayName = body.display_name,
                        sampleCount = body.sample_count,
                        profileImageUrl = body.profile_image_url,
                        createdAt = body.created_at,
                        updatedAt = body.updated_at
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to update speaker avatar"
                emit(EnrollmentResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(EnrollmentResult.Error(message = e.message ?: "Network error"))
        }
    }

    /**
     * Delete a speaker.
     * Exact endpoint: DELETE /enroll/speakers/{speaker_id}
     */
    suspend fun deleteSpeaker(speakerId: String): Flow<EnrollmentResult<Unit>> = flow {
        emit(EnrollmentResult.Loading)
        try {
            val response = apiService.deleteSpeaker(speakerId = speakerId)

            if (response.isSuccessful) {
                emit(EnrollmentResult.Success(Unit))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Failed to delete speaker"
                emit(EnrollmentResult.Error(
                    message = errorDetail,
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            emit(EnrollmentResult.Error(message = e.message ?: "Network error"))
        }
    }
}

