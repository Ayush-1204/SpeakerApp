package com.example.speakerapp.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.EntryPointAccessors
import com.example.speakerapp.MainActivity
import com.example.speakerapp.R
import com.example.speakerapp.core.auth.TokenManager
import com.example.speakerapp.core.fcm.FCMDiagnostics
import com.example.speakerapp.features.devices.data.DeviceResult
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class SafeEarFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM ONEW TOKEN EVENT ==================")
        Log.i(TAG, "Token: $token")
        Log.i(TAG, "Length: ${token.length}")
        Log.i(TAG, "Action: Send this token to backend as device_token")
        Log.i(TAG, "==========================================")
        
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    applicationContext,
                    FcmServiceEntryPoint::class.java
                )
                val tokenManager = entryPoint.tokenManager()
                val deviceRepository = entryPoint.deviceRepository()
                val deviceId = tokenManager.getDeviceId()

                if (deviceId.isNullOrBlank()) {
                    tokenManager.saveFcmToken(token)
                    Log.i(TAG, "Saved FCM token locally; device not registered yet")
                    return@launch
                }

                when (val result = deviceRepository.syncFcmTokenWithRetry(deviceId, token)) {
                    is DeviceResult.Success<*> -> {
                        tokenManager.saveFcmToken(token)
                        Log.i(TAG, "FCM token rotated successfully")
                    }
                    is DeviceResult.Error -> {
                        Log.e(TAG, "FCM token rotation failed: ${result.message}")
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM token rotation failed: ${e.message}", e)
            } finally {
                FCMDiagnostics.logTokenRefresh(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Log incoming message for debugging
        Log.i(TAG, "FCM MESSAGE RECEIVED ==================")
        Log.i(TAG, "From: ${remoteMessage.from}")
        Log.i(TAG, "MessageId: ${remoteMessage.messageId}")
        Log.i(TAG, "Sent time: ${remoteMessage.sentTime}")
        Log.i(TAG, "Data: ${remoteMessage.data}")
        val incomingNotification = remoteMessage.notification
        Log.i(
            TAG,
            "Notification: ${if (incomingNotification != null) "title=${incomingNotification.title}, body=${incomingNotification.body}" else "None"}"
        )
        Log.i(TAG, "=========================================")
        
        val localRole = runBlocking { TokenManager(applicationContext).getDeviceRole() }
        if (localRole != "parent_device") {
            Log.i(TAG, "Dropping FCM alert for non-parent role: $localRole")
            return
        }

        val targetRole = remoteMessage.data["target_role"]
            ?: remoteMessage.data["recipient_role"]
            ?: remoteMessage.data["audience_role"]
        if (!targetRole.isNullOrBlank() && targetRole != "parent_device") {
            Log.i(TAG, "Dropping FCM alert targeted for role=$targetRole")
            return
        }

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "SafeEar Alert"
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Potential stranger detected near child device"

        val channelId = remoteMessage.data["notification_channel_id"]
            ?: remoteMessage.data["android_channel_id"]
            ?: remoteMessage.data["channel_id"]
            ?: ALERT_CHANNEL_ID

        showHighAlertNotification(title, body, channelId)
        FCMDiagnostics.logIncomingMessage(title, body, remoteMessage.data)
    }

    private fun showHighAlertNotification(title: String, body: String, channelId: String) {
        createNotificationChannel(channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(this).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(channelId)
        if (existing != null) return

        val channel = NotificationChannel(
            channelId,
            "SafeEar Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High priority stranger alerts"
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val ALERT_CHANNEL_ID = "safeear_alerts"
        private const val ALERT_NOTIFICATION_ID = 2001
        private const val TAG = "SafeEarFCM"

        fun ensureAlertChannel(context: Context, channelId: String = ALERT_CHANNEL_ID) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(channelId)
            if (existing != null) return

            val channel = NotificationChannel(
                channelId,
                "SafeEar Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority stranger alerts"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }
}
