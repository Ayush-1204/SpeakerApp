package com.example.speakerapp.features.auth.data

import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.dto.ForgotPasswordRequest
import com.example.speakerapp.network.dto.GoogleAuthRequest
import com.example.speakerapp.network.dto.LoginEmailRequest
import com.example.speakerapp.network.dto.LogoutRequest
import com.example.speakerapp.network.dto.RegisterEmailRequest
import com.example.speakerapp.network.dto.RefreshTokenRequest
import com.example.speakerapp.network.dto.ResetPasswordRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import java.io.IOException
import javax.inject.Inject

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val code: Int? = null) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

data class AuthUser(
    val parentId: String,
    val email: String?,
    val displayName: String?
)

class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val okHttpClient: OkHttpClient
) {
    private fun mapFriendlyError(code: Int?, fallback: String): String {
        return when (code) {
            401 -> "Invalid or expired credentials. Please login again."
            403 -> "You do not have permission for this action."
            422 -> "Please check your input and try again."
            else -> fallback
        }
    }

    private fun isRetryableNetworkError(e: Exception): Boolean =
        e is IOException


    /**
     * Login with Google ID token or dev token format (dev:<alias>)
     */
    suspend fun loginWithGoogle(idToken: String): Flow<AuthResult<AuthUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val response = apiService.googleAuth(GoogleAuthRequest(id_token = idToken))
            
            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                
                // Save tokens
                tokenManager.saveTokens(
                    accessToken = body.access_token,
                    refreshToken = body.refresh_token,
                    expiresInSeconds = body.expires_in
                )
                
                // Save parent ID
                tokenManager.saveParentId(body.parent.id)
                
                emit(AuthResult.Success(
                    AuthUser(
                        parentId = body.parent.id,
                        email = body.parent.email,
                        displayName = body.parent.display_name
                    )
                ))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Unknown error"
                emit(AuthResult.Error(
                    message = mapFriendlyError(response.code(), errorDetail),
                    code = response.code()
                ))
            }
        } catch (e: Exception) {
            val msg = if (isRetryableNetworkError(e)) {
                "Network error. Please retry."
            } else {
                e.message ?: "Network error"
            }
            emit(AuthResult.Error(message = msg))
        }
    }

    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Flow<AuthResult<AuthUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val response = apiService.registerEmail(
                RegisterEmailRequest(
                    email = email,
                    password = password,
                    display_name = displayName
                )
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                tokenManager.saveTokens(
                    accessToken = body.access_token,
                    refreshToken = body.refresh_token,
                    expiresInSeconds = body.expires_in
                )
                tokenManager.saveParentId(body.parent.id)

                emit(
                    AuthResult.Success(
                        AuthUser(
                            parentId = body.parent.id,
                            email = body.parent.email,
                            displayName = body.parent.display_name
                        )
                    )
                )
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Signup failed"
                emit(AuthResult.Error(mapFriendlyError(response.code(), errorDetail), response.code()))
            }
        } catch (e: Exception) {
            val msg = if (isRetryableNetworkError(e)) {
                "Network error. Please retry."
            } else {
                e.message ?: "Network error"
            }
            emit(AuthResult.Error(msg))
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Flow<AuthResult<AuthUser>> = flow {
        emit(AuthResult.Loading)
        try {
            val response = apiService.loginEmail(
                LoginEmailRequest(
                    email = email,
                    password = password
                )
            )

            if (response.isSuccessful) {
                val body = response.body() ?: throw Exception("Empty response body")
                tokenManager.saveTokens(
                    accessToken = body.access_token,
                    refreshToken = body.refresh_token,
                    expiresInSeconds = body.expires_in
                )
                tokenManager.saveParentId(body.parent.id)

                emit(
                    AuthResult.Success(
                        AuthUser(
                            parentId = body.parent.id,
                            email = body.parent.email,
                            displayName = body.parent.display_name
                        )
                    )
                )
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Login failed"
                emit(AuthResult.Error(mapFriendlyError(response.code(), errorDetail), response.code()))
            }
        } catch (e: Exception) {
            val msg = if (isRetryableNetworkError(e)) {
                "Network error. Please retry."
            } else {
                e.message ?: "Network error"
            }
            emit(AuthResult.Error(msg))
        }
    }

    suspend fun forgotPassword(email: String): Flow<AuthResult<String>> = flow {
        emit(AuthResult.Loading)
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email = email))
            if (response.isSuccessful) {
                emit(AuthResult.Success("If this email is registered, a reset link has been sent."))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Request failed"
                emit(AuthResult.Error(mapFriendlyError(response.code(), errorDetail), response.code()))
            }
        } catch (e: Exception) {
            val msg = if (isRetryableNetworkError(e)) {
                "Network error. Please retry."
            } else {
                e.message ?: "Network error"
            }
            emit(AuthResult.Error(msg))
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Flow<AuthResult<String>> = flow {
        emit(AuthResult.Loading)
        try {
            val response = apiService.resetPassword(
                ResetPasswordRequest(
                    token = token,
                    new_password = newPassword
                )
            )
            if (response.isSuccessful) {
                emit(AuthResult.Success("Password reset successful. Please login."))
            } else {
                val errorDetail = response.errorBody()?.string() ?: "Reset failed"
                emit(AuthResult.Error(mapFriendlyError(response.code(), errorDetail), response.code()))
            }
        } catch (e: Exception) {
            val msg = if (isRetryableNetworkError(e)) {
                "Network error. Please retry."
            } else {
                e.message ?: "Network error"
            }
            emit(AuthResult.Error(msg))
        }
    }

    suspend fun logout(): Flow<AuthResult<Unit>> = flow {
        emit(AuthResult.Loading)
        var logoutError: AuthResult.Error? = null
        val refreshToken = tokenManager.getRefreshToken()

        try {
            if (!refreshToken.isNullOrBlank()) {
                runCatching {
                    val postResponse = apiService.logoutPost(LogoutRequest(refresh_token = refreshToken))
                    if (!postResponse.isSuccessful) {
                        val fallbackResponse = apiService.logoutDeleteQuery(refreshToken)
                        if (!fallbackResponse.isSuccessful) {
                            val detail = fallbackResponse.errorBody()?.string()
                                ?: postResponse.errorBody()?.string()
                                ?: "Logout failed"
                            throw IllegalStateException(mapFriendlyError(fallbackResponse.code(), detail))
                        }
                    }
                }.onFailure {
                    logoutError = AuthResult.Error(message = it.message ?: "Logout error")
                }
            }
        } finally {
            tokenManager.clearAll()
            okHttpClient.cache?.evictAll()
        }

        if (logoutError != null) {
            emit(logoutError!!)
        } else {
            emit(AuthResult.Success(Unit))
        }
    }

    suspend fun refreshToken(): Flow<AuthResult<Unit>> = flow {
        emit(AuthResult.Loading)
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                emit(AuthResult.Error("Refresh token unavailable", 401))
                return@flow
            }

            val response = apiService.refreshToken(RefreshTokenRequest(refresh_token = refreshToken))
            if (response.isSuccessful) {
                val body = response.body() ?: throw IllegalStateException("Empty refresh response")
                if (body.access_token.isBlank() || body.refresh_token.isBlank()) {
                    throw IllegalStateException("Refresh response missing tokens")
                }
                tokenManager.saveTokens(
                    accessToken = body.access_token,
                    refreshToken = body.refresh_token,
                    expiresInSeconds = body.expires_in
                )
                emit(AuthResult.Success(Unit))
            } else {
                val detail = response.errorBody()?.string() ?: "Refresh failed"
                emit(AuthResult.Error(mapFriendlyError(response.code(), detail), response.code()))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error(e.message ?: "Refresh failed"))
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        val parentId = tokenManager.getParentId()
        val deviceId = tokenManager.getDeviceId()

        return !accessToken.isNullOrBlank() &&
            !refreshToken.isNullOrBlank() &&
            !parentId.isNullOrBlank() &&
            !deviceId.isNullOrBlank()
    }

    suspend fun getCurrentUser(): AuthUser? {
        val parentId = tokenManager.getParentId() ?: return null
        return AuthUser(
            parentId = parentId,
            email = null,
            displayName = null
        )
    }
}
