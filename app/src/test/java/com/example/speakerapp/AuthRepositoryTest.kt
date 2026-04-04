package com.example.speakerapp

import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.auth.data.AuthRepository
import com.example.speakerapp.features.auth.data.AuthResult
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.network.dto.GoogleAuthResponse
import com.example.speakerapp.network.dto.ParentResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import retrofit2.Response

class AuthRepositoryTest {

    @Mock
    lateinit var api: ApiService

    @Mock
    lateinit var tokenManager: TokenManager

    @Mock
    lateinit var okHttpClient: OkHttpClient

    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = AuthRepository(api, tokenManager, okHttpClient)
    }

    @Test
    fun `login success saves tokens and parent info`() = runBlocking {
        val response = GoogleAuthResponse(
            access_token = "access",
            refresh_token = "refresh",
            token_type = "bearer",
            expires_in = 3600,
            parent = ParentResponse("uuid", "test@test.com", "Tester")
        )

        `when`(api.googleAuth(any())).thenReturn(Response.success(response))

        val results = repository.loginWithGoogle("dev:test").toList()

        assertTrue(results[0] is AuthResult.Loading)
        assertTrue(results[1] is AuthResult.Success)
    }

    @Test
    fun `login failure returns error resource`() = runBlocking {
        `when`(api.googleAuth(any())).thenReturn(Response.error(401, "".toResponseBody()))

        val results = repository.loginWithGoogle("invalid").toList()

        assertTrue(results[0] is AuthResult.Loading)
        assertTrue(results[1] is AuthResult.Error)
    }
}
