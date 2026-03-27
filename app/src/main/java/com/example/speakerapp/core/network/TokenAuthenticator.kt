package com.example.speakerapp.core.network

import com.example.speakerapp.core.network.dto.RefreshRequest
import com.example.speakerapp.core.storage.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiProvider: Provider<SafeEarApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = runBlocking { sessionManager.refreshToken.first() } ?: return null

        synchronized(this) {
            val newTokens = runBlocking {
                try {
                    val api = apiProvider.get()
                    val refreshResponse = api.refreshToken(RefreshRequest(refreshToken))
                    if (refreshResponse.isSuccessful) {
                        refreshResponse.body()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            return if (newTokens != null) {
                runBlocking {
                    sessionManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                }
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                runBlocking { sessionManager.clear() }
                null
            }
        }
    }
}
