package com.example.speakerapp.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val PARENT_ID = stringPreferencesKey("parent_id")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val DEVICE_ROLE = stringPreferencesKey("device_role")
        private val DEVICE_NAME = stringPreferencesKey("device_name")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[DEVICE_ID] }
    val deviceRole: Flow<String?> = context.dataStore.data.map { it[DEVICE_ROLE] }

    suspend fun saveTokens(access: String, refresh: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN] = access
            it[REFRESH_TOKEN] = refresh
        }
    }

    suspend fun saveParentInfo(parentId: String) {
        context.dataStore.edit {
            it[PARENT_ID] = parentId
        }
    }

    suspend fun saveDeviceInfo(id: String, role: String, name: String) {
        context.dataStore.edit {
            it[DEVICE_ID] = id
            it[DEVICE_ROLE] = role
            it[DEVICE_NAME] = name
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
