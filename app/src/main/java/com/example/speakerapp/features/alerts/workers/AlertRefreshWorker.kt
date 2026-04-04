package com.example.speakerapp.features.alerts.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.speakerapp.network.ApiService
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.network.dto.AlertInfo
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Background worker to refresh alerts periodically
 * Runs every 2 minutes to fetch new alerts even when app is in background
 * Shows notification for unacknowledged alerts
 */
class AlertRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "=== AlertRefreshWorker: Starting background refresh ===")
            Log.d(TAG, "Work execution #${runAttemptCount + 1}")

            val tokenManager = TokenManager(applicationContext)
            val accessToken = tokenManager.getAccessToken()
            val deviceId = tokenManager.getDeviceId()
            val deviceRole = tokenManager.getDeviceRole()
            
            if (accessToken.isNullOrEmpty()) {
                Log.w(TAG, "No access token found, skipping refresh (user likely logged out)")
                return Result.success()  // Don't retry - just stop silently
            }
            
            if (deviceRole != "parent_device") {
                Log.w(TAG, "Not parent device (role: $deviceRole), skipping alert refresh")
                return Result.success()
            }
            
            Log.d(TAG, "Prerequisites OK - token present, parent device role confirmed")

            val json = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val apiService = Retrofit.Builder()
                .baseUrl(com.example.speakerapp.BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(ApiService::class.java)

            val response = apiService.getAlerts(limit = 20, offset = 0)
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to refresh alerts. HTTP ${response.code()}")
                return Result.retry()
            }

            val items = response.body()?.items.orEmpty()
            val unacknowledged = items.count { it.acknowledged_at == null }
            Log.d(TAG, "Device ID: $deviceId, alerts fetched=${items.size}, unacknowledged=$unacknowledged")

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastNotifiedAtMs = prefs.getLong(KEY_LAST_NOTIFIED_ALERT_MS, 0L)

            val unackAlerts = items.filter { it.acknowledged_at == null }
            val newestAlertMs = unackAlerts.maxOfOrNull { it.toEpochMs() } ?: 0L
            val newAlertCount = if (lastNotifiedAtMs == 0L) {
                unackAlerts.size
            } else {
                unackAlerts.count { it.toEpochMs() > lastNotifiedAtMs }
            }

            if (newAlertCount > 0) {
                showAlertNotification(
                    title = "SafeEar Alerts",
                    body = "You have $newAlertCount new alert${if (newAlertCount == 1) "" else "s"}"
                )
                if (newestAlertMs > 0L) {
                    prefs.edit().putLong(KEY_LAST_NOTIFIED_ALERT_MS, newestAlertMs).apply()
                }
            }
            
            Log.d(TAG, "=== AlertRefreshWorker: Background refresh completed ===")
            Result.success()
            
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error ${e.code()}: ${e.message()}")
            Log.e(TAG, "Will retry after backoff")
            Result.retry()  // WorkManager will retry with exponential backoff
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            Log.e(TAG, "Will retry after backoff")
            Result.retry()
        }
    }

    private fun showAlertNotification(title: String, body: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) 
            as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            "alert_refresh_channel",
            "Background Alerts",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
        
        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "alert_refresh_channel")
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "AlertRefreshWorker: Showed notification with ID $notificationId")
    }

    companion object {
        private const val TAG = "SafeEar-AlertRefreshWorker"
        const val WORK_NAME = "alert_refresh_periodic"
        const val WORK_TAG = "background_alert_refresh"
        private const val PREFS_NAME = "safeear_alert_worker"
        private const val KEY_LAST_NOTIFIED_ALERT_MS = "last_notified_alert_ms"
    }
}

private fun AlertInfo.toEpochMs(): Long {
    timestamp_ms?.let { return it }

    return runCatching { Instant.parse(timestamp).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(timestamp).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(timestamp).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { Instant.parse(created_at).toEpochMilli() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(created_at).toInstant().toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(created_at).atZone(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        ?: 0L
}
