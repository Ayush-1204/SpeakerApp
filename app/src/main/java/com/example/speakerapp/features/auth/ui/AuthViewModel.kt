package com.example.speakerapp.features.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.features.auth.data.AuthResult
import com.example.speakerapp.features.auth.data.AuthUser
import com.example.speakerapp.features.auth.domain.EmailLoginUseCase
import com.example.speakerapp.features.auth.domain.EmailRegisterUseCase
import com.example.speakerapp.features.auth.domain.ForgotPasswordUseCase
import com.example.speakerapp.features.auth.domain.IsLoggedInUseCase
import com.example.speakerapp.features.auth.domain.LoginUseCase
import com.example.speakerapp.features.auth.domain.LogoutUseCase
import com.example.speakerapp.features.auth.domain.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: AuthUser? = null,
    val error: String? = null,
    val infoMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val retryable: Boolean = false,
    val lastAction: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val emailLoginUseCase: EmailLoginUseCase,
    private val emailRegisterUseCase: EmailRegisterUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val resetPasswordUseCase: ResetPasswordUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val isLoggedInUseCase: IsLoggedInUseCase,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val isLoggedIn = isLoggedInUseCase()
            _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
        }
    }

    /**
     * Login with Google ID token or dev token format.
     * Dev format: "dev:<parent_alias>"
     * Google format: actual JWT token from Google Sign-In
     */
    fun login(idToken: String) {
        viewModelScope.launch {
            loginUseCase(idToken).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null,
                            retryable = false,
                            lastAction = "google"
                        )
                    }
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = result.data,
                            isLoggedIn = true,
                            error = null,
                            retryable = false
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            isLoggedIn = false,
                            retryable = result.message.contains("Network", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            emailLoginUseCase(email.trim(), password).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null,
                            retryable = false,
                            lastAction = "email_login"
                        )
                    }
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = result.data,
                            isLoggedIn = true,
                            error = null,
                            retryable = false
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            retryable = result.message.contains("Network", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String, displayName: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }
        if (password.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters")
            return
        }
        if (displayName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Display name is required")
            return
        }

        viewModelScope.launch {
            emailRegisterUseCase(email.trim(), password, displayName.trim()).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null,
                            retryable = false,
                            lastAction = "email_register"
                        )
                    }
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = result.data,
                            isLoggedIn = true,
                            error = null,
                            retryable = false
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            retryable = result.message.contains("Network", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    fun forgotPassword(email: String) {
        if (!isValidEmail(email)) {
            _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            forgotPasswordUseCase(email.trim()).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null,
                            retryable = false,
                            lastAction = "forgot"
                        )
                    }
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            infoMessage = result.data,
                            error = null,
                            retryable = false
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            retryable = result.message.contains("Network", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    fun resetPassword(token: String, newPassword: String) {
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Reset token is required")
            return
        }
        if (newPassword.length < 8) {
            _uiState.value = _uiState.value.copy(error = "Password must be at least 8 characters")
            return
        }

        viewModelScope.launch {
            resetPasswordUseCase(token.trim(), newPassword).collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null,
                            infoMessage = null,
                            retryable = false,
                            lastAction = "reset"
                        )
                    }
                    is AuthResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            infoMessage = result.data,
                            error = null,
                            retryable = false
                        )
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message,
                            retryable = result.message.contains("Network", ignoreCase = true)
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase().collect { result ->
                when (result) {
                    is AuthResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                    }
                    is AuthResult.Success -> {
                        _uiState.value = AuthUiState()
                    }
                    is AuthResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Logout failed: ${result.message}",
                            isLoggedIn = false
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, infoMessage = null, retryable = false)
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }
}
