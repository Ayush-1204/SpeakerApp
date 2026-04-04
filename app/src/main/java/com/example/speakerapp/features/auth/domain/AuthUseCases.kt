package com.example.speakerapp.features.auth.domain

import com.example.speakerapp.features.auth.data.AuthRepository
import com.example.speakerapp.features.auth.data.AuthResult
import com.example.speakerapp.features.auth.data.AuthUser
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Flow<AuthResult<AuthUser>> {
        return authRepository.loginWithGoogle(idToken)
    }
}

class EmailLoginUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Flow<AuthResult<AuthUser>> {
        return authRepository.loginWithEmail(email, password)
    }
}

class EmailRegisterUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String
    ): Flow<AuthResult<AuthUser>> {
        return authRepository.registerWithEmail(email, password, displayName)
    }
}

class ForgotPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Flow<AuthResult<String>> {
        return authRepository.forgotPassword(email)
    }
}

class ResetPasswordUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Flow<AuthResult<String>> {
        return authRepository.resetPassword(token, newPassword)
    }
}

class LogoutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Flow<AuthResult<Unit>> {
        return authRepository.logout()
    }
}

class IsLoggedInUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Boolean {
        return authRepository.isLoggedIn()
    }
}
