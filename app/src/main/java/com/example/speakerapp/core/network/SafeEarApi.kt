package com.example.speakerapp.core.network

import com.example.speakerapp.core.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SafeEarApi {

    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleAuthRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<AuthResponse>

    @HTTP(method = "DELETE", path = "auth/logout", hasBody = true)
    suspend fun logout(@Body request: LogoutRequest): Response<StatusResponse>

    @Multipart
    @POST("devices")
    suspend fun registerDevice(
        @Part("device_name") deviceName: RequestBody,
        @Part("role") role: RequestBody,
        @Part("device_token") deviceToken: RequestBody? = null
    ): Response<DeviceResponse>

    @Multipart
    @POST("enroll/speaker")
    suspend fun enrollSpeaker(
        @Part("display_name") displayName: RequestBody,
        @Part("speaker_id") speakerId: RequestBody? = null,
        @Part file: MultipartBody.Part
    ): Response<EnrollmentResponse>

    @GET("enroll/speakers")
    suspend fun getSpeakers(): Response<SpeakerListResponse>

    @PATCH("enroll/speakers/{speaker_id}")
    suspend fun updateSpeaker(
        @Path("speaker_id") speakerId: String,
        @Body request: UpdateSpeakerRequest
    ): Response<SpeakerDto>

    @DELETE("enroll/speakers/{speaker_id}")
    suspend fun deleteSpeaker(@Path("speaker_id") speakerId: String): Response<Unit>

    @POST("detect/location")
    suspend fun updateLocation(@Body request: LocationRequest): Response<Unit>

    @Multipart
    @POST("detect/chunk")
    suspend fun uploadChunk(
        @Part("device_id") deviceId: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody? = null,
        @Part("longitude") longitude: RequestBody? = null
    ): Response<DetectionResponse>

    @DELETE("detect/session")
    suspend fun endSession(@Query("device_id") deviceId: String): Response<Unit>

    @GET("alerts")
    suspend fun getAlerts(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<AlertListResponse>

    @POST("alerts/{alert_id}/ack")
    suspend fun acknowledgeAlert(@Path("alert_id") alertId: String): Response<AlertDto>

    @Streaming
    @GET("alerts/{alert_id}/clip")
    suspend fun getAlertClip(@Path("alert_id") alertId: String): Response<ResponseBody>

    @GET("health")
    suspend fun healthCheck(): Response<Unit>
}
