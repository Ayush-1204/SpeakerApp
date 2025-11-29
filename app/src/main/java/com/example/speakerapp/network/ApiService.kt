package com.example.speakerapp.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Response

// Data class to match the server's JSON response for alerts
data class ServerAlert(
    val timestamp: Long, // Corrected to Long to match server response parsing
    val location: String, // This is a URL
    val audio_url: String // This is a relative URL path
)

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

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

interface ApiService {

    @GET("/get_alerts")
    suspend fun getAlerts(@Query("since") since: Long): Response<List<ServerAlert>>

    @POST("/update_location")
    suspend fun updateLocation(@Body location: LocationData): Response<Unit>

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
