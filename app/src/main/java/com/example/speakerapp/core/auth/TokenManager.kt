package com.example.speakerapp.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "tokens")

@Singleton
class TokenManager @Inject constructor(@ApplicationContext private val context: Context) {
    
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val PARENT_ID_KEY = stringPreferencesKey("parent_id")
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val DEVICE_ROLE_KEY = stringPreferencesKey("device_role")
        private val INSTALLATION_ID_KEY = stringPreferencesKey("installation_id")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        private val TOKEN_EXPIRY_KEY = stringPreferencesKey("token_expiry")
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresInSeconds: Long = 604800) {
        val expiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds)
        context.tokenDataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[REFRESH_TOKEN_KEY] = refreshToken
            preferences[TOKEN_EXPIRY_KEY] = expiryTime.toString()
        }
    }

    suspend fun getAccessToken(): String? {
        return context.tokenDataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun getRefreshToken(): String? {
        return context.tokenDataStore.data.first()[REFRESH_TOKEN_KEY]
    }

    suspend fun saveParentId(parentId: String) {
        context.tokenDataStore.edit { preferences ->
            preferences[PARENT_ID_KEY] = parentId
        }
    }

    suspend fun getParentId(): String? {
        return context.tokenDataStore.data.first()[PARENT_ID_KEY]
    }

    suspend fun saveDeviceInfo(deviceId: String, deviceRole: String) {
        context.tokenDataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
            preferences[DEVICE_ROLE_KEY] = deviceRole
        }
    }

    suspend fun clearDeviceInfo() {
        context.tokenDataStore.edit { preferences ->
            preferences.remove(DEVICE_ID_KEY)
            preferences.remove(DEVICE_ROLE_KEY)
        }
    }

    suspend fun saveFcmToken(fcmToken: String) {
        context.tokenDataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = fcmToken
        }
    }

    suspend fun getFcmToken(): String? {
        return context.tokenDataStore.data.first()[FCM_TOKEN_KEY]
    }

    suspend fun clearFcmToken() {
        context.tokenDataStore.edit { preferences ->
            preferences.remove(FCM_TOKEN_KEY)
        }
    }

    suspend fun getDeviceId(): String? {
        return context.tokenDataStore.data.first()[DEVICE_ID_KEY]
    }

    suspend fun getDeviceRole(): String? {
        return context.tokenDataStore.data.first()[DEVICE_ROLE_KEY]
    }

    suspend fun getInstallationId(): String? {
        return context.tokenDataStore.data.first()[INSTALLATION_ID_KEY]
    }

    suspend fun getOrCreateInstallationId(): String {
        val existing = getInstallationId()
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        context.tokenDataStore.edit { preferences ->
            preferences[INSTALLATION_ID_KEY] = generated
        }
        return generated
    }

    suspend fun clearAll() {
        context.tokenDataStore.edit { preferences ->
            val installationId = preferences[INSTALLATION_ID_KEY]
            preferences.clear()
            if (!installationId.isNullOrBlank()) {
                preferences[INSTALLATION_ID_KEY] = installationId
            }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val token = getAccessToken()
        return token != null && token.isNotEmpty()
    }

    suspend fun hasDeviceInfo(): Boolean {
        return getDeviceId() != null && getDeviceRole() != null
    }
}
