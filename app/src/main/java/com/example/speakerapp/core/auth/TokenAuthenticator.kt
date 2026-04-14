package com.example.speakerapp.core.auth

import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

/**
 * OkHttp authenticator that handles token refresh on 401 response.
 * Attempts to refresh token once; if refresh fails, clears tokens for logout.
 */
class TokenAuthenticator @Inject constructor(
    private val apiServiceProvider: Provider<ApiService>,
    private val tokenManager: TokenManager
) : Authenticator {

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if already retried for this response
        if (response.request.header("X-Retry") != null) {
            return null
        }

        if (responseCount(response) >= 2) {
            return null
        }

        // If not a 401, don't handle
        if (response.code != 401) {
            return null
        }

        return runBlocking {
            try {
                val refreshToken = tokenManager.getRefreshToken() ?: return@runBlocking null
                
                // Attempt to refresh
                val refreshResponse = apiServiceProvider.get().refreshToken(
                    RefreshTokenRequest(refresh_token = refreshToken)
                )

                if (refreshResponse.isSuccessful) {
                    refreshResponse.body()?.let { body ->
                        // Save new tokens
                        tokenManager.saveTokens(
                            accessToken = body.access_token,
                            refreshToken = body.refresh_token,
                            expiresInSeconds = body.expires_in
                        )

                        // Retry original request with new token
                        return@runBlocking response.request
                            .newBuilder()
                            .header("Authorization", "Bearer ${body.access_token}")
                            .header("X-Retry", "true")
                            .build()
                    }
                } else {
                    // Refresh failed - logout user
                    tokenManager.clearAll()
                }
            } catch (e: Exception) {
                // Log but don't crash
                e.printStackTrace()
                // Clear tokens on any error
                runBlocking { tokenManager.clearAll() }
            }
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
