package com.example.speakerapp.network

import com.example.speakerapp.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import retrofit2.Response

/**
 * SafeEar Backend API Service
 * Contracts exactly as specified by backend:
 * - All exact field names must be preserved
 * - All role values must match backend
 * - All endpoint paths must be exact
 * - All request/response shapes must match
 */
interface ApiService {

    // ===== AUTH ENDPOINTS (No auth required) =====
    
    /**
     * POST /auth/google
     * Dev mode supports: id_token = "dev:<parent_alias>"
     */
    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<GoogleAuthResponse>

    /**
     * POST /auth/register-email
     */
    @POST("auth/register-email")
    suspend fun registerEmail(@Body request: RegisterEmailRequest): Response<EmailAuthResponse>

    /**
     * POST /auth/login-email
     */
    @POST("auth/login-email")
    suspend fun loginEmail(@Body request: LoginEmailRequest): Response<EmailAuthResponse>

    /**
     * POST /auth/forgot-password
     */
    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericMessageResponse>

    /**
     * POST /auth/reset-password
     */
    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericMessageResponse>

    /**
     * POST /auth/refresh
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponse>

    /**
     * POST /auth/logout
     * Preferred logout endpoint for client compatibility.
     */
    @POST("auth/logout")
    suspend fun logoutPost(@Body request: LogoutRequest): Response<Unit>

    /**
     * DELETE /auth/logout?refresh_token=...
     * Fallback endpoint if POST is unavailable.
     */
    @DELETE("auth/logout")
    suspend fun logoutDeleteQuery(@Query("refresh_token") refreshToken: String): Response<Unit>

    // ===== DEVICE ENDPOINTS (Auth required) =====

    /**
     * POST /devices
     * Content-Type: multipart/form-data
     */
    @Multipart
    @POST("devices")
    suspend fun createDevice(
        @Part("device_name") deviceName: RequestBody?,
        @Part("role") role: RequestBody,
        @Part("device_token") deviceToken: RequestBody?
    ): Response<DeviceResponse>

    /**
     * POST /devices/upsert
     * Content-Type: multipart/form-data
     * Uses stable installation identity to return the same logical device record.
     */
    @Multipart
    @POST("devices/upsert")
    suspend fun upsertDevice(
        @Part("installation_id") installationId: RequestBody,
        @Part("device_name") deviceName: RequestBody?,
        @Part("role") role: RequestBody,
        @Part("device_token") deviceToken: RequestBody?
    ): Response<DeviceResponse>

    /**
     * GET /devices
     * List devices for current parent account.
     */
    @GET("devices")
    suspend fun listDevices(): Response<DeviceListResponse>

    /**
     * PATCH /devices/{device_id}/monitoring
     * Remotely enable/disable child monitoring.
     */
    @PATCH("devices/{device_id}/monitoring")
    suspend fun setDeviceMonitoring(
        @Path("device_id") deviceId: String,
        @Body request: DeviceMonitoringUpdateRequest
    ): Response<DeviceData>

    /**
     * PATCH /devices/{device_id}/token
     * Updates the registered FCM token for a device.
     */
    @PATCH("devices/{device_id}/token")
    suspend fun updateDeviceToken(
        @Path("device_id") deviceId: String,
        @Body request: UpdateDeviceTokenRequest
    ): Response<DeviceData>

    // ===== SPEAKER ENROLLMENT ENDPOINTS (Auth required) =====

    /**
     * POST /enroll/speaker
     * Fields: display_name (required), speaker_id (optional), file OR audio (one required)
     */
    @Multipart
    @POST("enroll/speaker")
    suspend fun enrollSpeaker(
        @Part("display_name") displayName: RequestBody,
        @Part("speaker_id") speakerId: RequestBody?,
        @Part audio: MultipartBody.Part
    ): Response<EnrollSpeakerResponse>

    /**
     * GET /enroll/speakers
     */
    @GET("enroll/speakers")
    suspend fun listSpeakers(): Response<SpeakersListResponse>

    /**
     * PATCH /enroll/speakers/{speaker_id}
     */
    @PATCH("enroll/speakers/{speaker_id}")
    suspend fun updateSpeaker(
        @Path("speaker_id") speakerId: String,
        @Body request: UpdateSpeakerRequest
    ): Response<SpeakerInfo>

    /**
     * POST /enroll/speakers/{speaker_id}/avatar
     * Multipart field: profile_image
     */
    @Multipart
    @POST("enroll/speakers/{speaker_id}/avatar")
    suspend fun updateSpeakerAvatar(
        @Path("speaker_id") speakerId: String,
        @Part image: MultipartBody.Part
    ): Response<SpeakerInfo>

    /**
     * DELETE /enroll/speakers/{speaker_id}
     */
    @DELETE("enroll/speakers/{speaker_id}")
    suspend fun deleteSpeaker(@Path("speaker_id") speakerId: String): Response<Unit>

    // ===== DETECTION ENDPOINTS (Auth required, /detect/chunk only for child_device) =====

    /**
     * POST /detect/location
     */
    @POST("detect/location")
    suspend fun sendLocation(@Body request: LocationUpdateRequest): Response<Unit>

    /**
     * POST /detect/chunk
     * Fields: device_id (required), file OR audio (required), latitude/longitude (optional)
     * Audio constraints: 16kHz, mono preferred, WAV format
     * Responses: warming_up, no_hop, ok
     * Role check: only_child_devices_can_stream_audio (403)
     */
    @Multipart
    @POST("detect/chunk")
    suspend fun detectChunk(
        @Part("device_id") deviceId: RequestBody,
        @Part("chunk_id") chunkId: RequestBody?,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("battery_percent") batteryPercent: RequestBody?,
        @Part("battery") battery: RequestBody?,
        @Part audio: MultipartBody.Part
    ): Response<DetectionChunkResponse>

    /**
     * DELETE /detect/session
     */
    @HTTP(method = "DELETE", path = "detect/session")
    suspend fun deleteDetectionSession(
        @Query("device_id") deviceId: String
    ): Response<Unit>

    // ===== ALERTS ENDPOINTS (Auth required) =====

    /**
     * GET /alerts
     * Query params: limit (default 50), offset (default 0)
     */
    @GET("alerts")
    suspend fun getAlerts(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<AlertsListResponse>

    /**
     * POST /alerts/{alert_id}/ack
     * Returns acknowledged alert object
     */
    @POST("alerts/{alert_id}/ack")
    suspend fun acknowledgeAlert(@Path("alert_id") alertId: String): Response<AckAlertResponse>

    /**
     * POST /alerts/{alert_id}/flag-familiar
     * Flags an alert as familiar and returns updated familiar speakers list in items.
     */
    @POST("alerts/{alert_id}/flag-familiar")
    suspend fun flagFamiliar(
        @Path("alert_id") alertId: String,
        @Body request: FlagFamiliarRequest
    ): Response<FlagFamiliarResponse>

    /**
     * GET /alerts/{alert_id}/clip
     * Returns audio/wav bytes
     */
    @GET("alerts/{alert_id}/clip")
    suspend fun getAlertClip(@Path("alert_id") alertId: String): Response<okhttp3.ResponseBody>

    /**
     * DELETE /alerts/{alert_id}
     * Delete a specific alert
     */
    @DELETE("alerts/{alert_id}")
    suspend fun deleteAlert(@Path("alert_id") alertId: String): Response<Unit>

    /**
     * DELETE /alerts
     * Delete all alerts
     */
    @DELETE("alerts")
    suspend fun deleteAllAlerts(): Response<Unit>

    // ===== HEALTH CHECK (No auth required) =====

    /**
     * GET /health
     */
    @GET("health")
    suspend fun health(): Response<HealthResponse>

    /**
     * GET /
     */
    @GET("")
    suspend fun root(): Response<Unit>
}
