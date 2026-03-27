package com.example.speakerapp.features.enrollment.data

import com.example.speakerapp.core.network.Resource
import com.example.speakerapp.core.network.SafeEarApi
import com.example.speakerapp.core.network.dto.EnrollmentResponse
import com.example.speakerapp.core.network.dto.SpeakerDto
import com.example.speakerapp.core.network.dto.UpdateSpeakerRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrollmentRepository @Inject constructor(
    private val api: SafeEarApi
) {
    suspend fun enrollSpeaker(
        displayName: String,
        audioFile: File,
        speakerId: String? = null
    ): Resource<EnrollmentResponse> {
        return try {
            val namePart = displayName.toRequestBody("text/plain".toMediaTypeOrNull())
            val speakerIdPart = speakerId?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val requestFile = audioFile.asRequestBody("audio/wav".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            val response = api.enrollSpeaker(namePart, speakerIdPart, filePart)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Enrollment failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun getSpeakers(): Resource<List<SpeakerDto>> {
        return try {
            val response = api.getSpeakers()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.items)
            } else {
                Resource.Error(response.message() ?: "Failed to fetch speakers")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun updateSpeaker(speakerId: String, newName: String): Resource<SpeakerDto> {
        return try {
            val response = api.updateSpeaker(speakerId, UpdateSpeakerRequest(newName))
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Failed to update speaker")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun deleteSpeaker(speakerId: String): Resource<Unit> {
        return try {
            val response = api.deleteSpeaker(speakerId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(response.message() ?: "Failed to delete speaker")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
