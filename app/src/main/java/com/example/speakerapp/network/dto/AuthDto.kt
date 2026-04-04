package com.example.speakerapp.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ===== Google Auth Request/Response =====
@Serializable
data class GoogleAuthRequest(
    val id_token: String
)

@Serializable
data class ParentResponse(
    val id: String,
    val email: String? = null,
    val display_name: String? = null
)

@Serializable
data class GoogleAuthResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Long,
    val parent: ParentResponse
)

// ===== Email Auth Request/Response =====
@Serializable
data class RegisterEmailRequest(
    val email: String,
    val password: String,
    val display_name: String
)

@Serializable
data class LoginEmailRequest(
    val email: String,
    val password: String
)

@Serializable
data class EmailAuthResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Long,
    val parent: ParentResponse
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)

@Serializable
data class GenericMessageResponse(
    val message: String? = null,
    val status: String? = null
)

// ===== Refresh Token Request/Response =====
@Serializable
data class RefreshTokenRequest(
    val refresh_token: String
)

@Serializable
data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Long
)

// ===== Logout Request/Response =====
@Serializable
data class LogoutRequest(
    val refresh_token: String
)

@Serializable
data class LogoutResponse(
    val status: String
)
