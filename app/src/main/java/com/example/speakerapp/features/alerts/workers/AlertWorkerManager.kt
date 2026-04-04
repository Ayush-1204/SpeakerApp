package com.example.speakerapp.features.alerts.workers

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages recurring WorkManager tasks for alerts background refresh
 */
object AlertWorkerManager {

    private const val TAG = "SafeEar-AlertWorker"

    /**
    * Schedule periodic alert refresh to run every 15 minutes in background
     * Call this once from MainActivity or when app starts
     */
    fun startBackgroundAlertRefresh(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val alertRefreshRequest = PeriodicWorkRequestBuilder<AlertRefreshWorker>(
                repeatInterval = 15,  // Minimum WorkManager periodic interval
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(AlertRefreshWorker.WORK_TAG)
                .build()

            val immediateRefreshRequest = OneTimeWorkRequestBuilder<AlertRefreshWorker>()
                .setConstraints(constraints)
                .addTag(AlertRefreshWorker.WORK_TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                AlertRefreshWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,  // Keep existing work if already scheduled
                alertRefreshRequest
            )

            WorkManager.getInstance(context).enqueueUniqueWork(
                AlertRefreshWorker.WORK_NAME + "_immediate",
                ExistingWorkPolicy.REPLACE,
                immediateRefreshRequest
            )
            
            Log.d(TAG, "Background alert refresh scheduled: immediate + every 15 minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alert refresh: ${e.message}", e)
        }
    }

    /**
     * Stop background alert refresh
     */
    fun stopBackgroundAlertRefresh(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(AlertRefreshWorker.WORK_NAME)
            Log.d(TAG, "Background alert refresh cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alert refresh: ${e.message}", e)
        }
    }

    /**
     * Get current worker status for debugging
     * Note: This is an async operation, results appear in logcat
     */
    fun getWorkerStatus(context: Context) {
        try {
            Log.d(TAG, "Checking alert worker status...")
            // WorkManager status would require ListenableFuture handling
            // For now, just log that check was attempted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get worker status: ${e.message}")
        }
    }
}
