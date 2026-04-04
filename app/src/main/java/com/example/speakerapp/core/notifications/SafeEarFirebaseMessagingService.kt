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
import com.example.speakerapp.MainActivity
import com.example.speakerapp.R
import com.example.speakerapp.core.fcm.FCMDiagnostics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SafeEarFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Log token refresh for debugging
        Log.i(TAG, "FCM ONEW TOKEN EVENT ==================")
        Log.i(TAG, "Token: $token")
        Log.i(TAG, "Length: ${token.length}")
        Log.i(TAG, "Action: Send this token to backend as device_token")
        Log.i(TAG, "==========================================")
        
        FCMDiagnostics.logTokenRefresh(token)
        
        // TODO: Send token to backend for update
        // This would be a call to POST /devices/{deviceId}/token endpoint
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Log incoming message for debugging
        Log.i(TAG, "FCM MESSAGE RECEIVED ==================")
        Log.i(TAG, "From: ${remoteMessage.from}")
        Log.i(TAG, "MessageId: ${remoteMessage.messageId}")
        Log.i(TAG, "Sent time: ${remoteMessage.sentTime}")
        Log.i(TAG, "Data: ${remoteMessage.data}")
        Log.i(TAG, "Notification: ${remoteMessage.notification?.let { 
            "title=${it.title}, body=${it.body}" 
        } ?: "None"}")
        Log.i(TAG, "=========================================")
        
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
