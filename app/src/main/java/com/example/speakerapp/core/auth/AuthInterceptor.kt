package com.example.speakerapp.core.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp interceptor that adds Authorization header with Bearer token to all requests.
 */
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // If token exists, add Authorization header
        val token = runBlocking { tokenManager.getAccessToken() }
        if (token != null && token.isNotEmpty()) {
            request = request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        return chain.proceed(request)
    }
}
