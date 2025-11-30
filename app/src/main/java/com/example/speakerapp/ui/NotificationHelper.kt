package com.example.speakerapp.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.speakerapp.MainActivity
import com.example.speakerapp.R

object NotificationHelper {

    private const val ALERT_CHANNEL_ID = "stranger_alert_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Stranger Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for stranger detection alerts."
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    fun showStrangerAlertNotification(context: Context, alertTimestamp: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigateTo", "parent") // Custom data to navigate to the parent screen
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            alertTimestamp.toInt(), // Use timestamp as a unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a suitable icon
            .setContentTitle("🚨 Stranger Detected! 🚨")
            .setContentText("A potential stranger has been detected near the child device.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Show on lock screen
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(alertTimestamp.toInt(), notification)
    }
}
