package com.example.speakerapp.features.auth.data

import com.example.speakerapp.core.network.Resource
import com.example.speakerapp.core.network.SafeEarApi
import com.example.speakerapp.core.network.dto.GoogleAuthRequest
import com.example.speakerapp.core.network.dto.LogoutRequest
import com.example.speakerapp.core.storage.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SafeEarApi,
    private val sessionManager: SessionManager
) {
    suspend fun login(idToken: String): Resource<Unit> {
        return try {
            val response = api.googleLogin(GoogleAuthRequest(idToken))
            if (response.isSuccessful && response.body() != null) {
                val authBody = response.body()!!
                sessionManager.saveTokens(authBody.accessToken, authBody.refreshToken)
                authBody.parent?.id?.let { sessionManager.saveParentInfo(it) }
                Resource.Success(Unit)
            } else {
                Resource.Error(response.message() ?: "Login failed")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            val refreshToken = sessionManager.refreshToken.firstOrNull()
            if (refreshToken != null) {
                api.logout(LogoutRequest(refreshToken))
            }
            sessionManager.clear()
            Resource.Success(Unit)
        } catch (e: Exception) {
            sessionManager.clear()
            Resource.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
