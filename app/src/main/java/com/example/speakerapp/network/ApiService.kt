package com.example.speakerapp.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Response

data class SpeakerListResponse(
    val speakers: List<String>
)

data class EnrollResponse(
    val status: String,
    val name: String
)

data class PredictResponse(
    val prediction: String,
    val distance: Float,
    val closest_speaker: String?
)

interface ApiService {

    @GET("/list_speakers")
    suspend fun listSpeakers(): Response<SpeakerListResponse>

    @Multipart
    @POST("/enroll")
    suspend fun enrollSpeaker(
        @Part("name") name: RequestBody,
        @Part audio: MultipartBody.Part
    ): Response<EnrollResponse>

    @Multipart
    @POST("/predict")
    suspend fun predictSpeaker(
        @Part audio: MultipartBody.Part
    ): Response<PredictResponse>

    @POST("/flag_familiar")
    suspend fun flagFamiliar(
        @Query("speaker") speaker: String
    ): Response<Map<String, String>>

    @DELETE("/delete_speaker/{name}")
    suspend fun deleteSpeaker(@Path("name") name: String): Response<Map<String, String>>

    @GET("/test_connection")
    suspend fun testConnection(): Response<Map<String, String>>
}
