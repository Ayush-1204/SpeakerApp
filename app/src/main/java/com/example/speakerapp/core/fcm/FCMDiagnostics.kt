package com.example.speakerapp.core.fcm

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

/**
 * FCM (Firebase Cloud Messaging) Diagnostic Utility
 * Use this to verify FCM is properly configured and receiving messages
 * 
 * HOW TO VERIFY FCM IS WORKING:
 * 1. Call FCMDiagnostics.diagnoseAll(context) after app launch
 * 2. Check Logcat for "SafeEar-FCM" tag
 * 3. Look for token logged in "FCM Token:"
 * 4. If no token appears, check Google Play Services
 * 5. Test by sending message from Firebase Console to your device token
 */
object FCMDiagnostics {

    private const val TAG = "SafeEar-FCM"

    /**
     * Run full FCM diagnostic check
     * Call this from MainActivity.onCreate() to verify setup
     */
    suspend fun diagnoseAll(context: Context) {
        Log.i(TAG, "=== STARTING FCM DIAGNOSTICS ===")
        
        checkFirebaseInitialized()
        checkPlayServices(context)
        checkToken()
        checkManifest(context)
        logEnvironment()
        
        Log.i(TAG, "=== FCM DIAGNOSTICS COMPLETE ===")
    }

    private fun checkFirebaseInitialized() {
        try {
            com.google.firebase.FirebaseApp.getInstance()
            Log.i(TAG, "✓ Firebase initialized")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Firebase NOT initialized: ${e.message}")
        }
    }

    private fun checkPlayServices(context: Context) {
        try {
            val result = com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context)
            
            if (result == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.i(TAG, "✓ Google Play Services available")
            } else {
                Log.e(TAG, "✗ Google Play Services NOT available (code: $result)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error checking Play Services: ${e.message}")
        }
    }

    /**
     * Get current FCM token and log it
     * This token should be sent to your backend during device registration
     */
    private suspend fun checkToken() {
        try {
            val token = Firebase.messaging.token.await()
            if (token.isNotEmpty()) {
                Log.i(TAG, "✓ FCM Token: $token")
                Log.i(TAG, "  (This should be sent to backend as device_token)")
                Log.i(TAG, "  Token length: ${token.length}")
            } else {
                Log.e(TAG, "✗ FCM Token is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to get FCM token: ${e.message}")
            Log.e(TAG, "  Cause: ${e.cause?.message}")
        }
    }

    private fun checkManifest(context: Context) {
        try {
            val manifestInfo = context.packageManager.getServiceInfo(
                android.content.ComponentName(
                    context,
                    "com.example.speakerapp.core.notifications.SafeEarFirebaseMessagingService"
                ),
                0
            )
            Log.i(TAG, "✓ SafeEarFirebaseMessagingService declared in manifest")
            Log.i(TAG, "  Service enabled: ${manifestInfo.enabled}")
        } catch (e: Exception) {
            Log.e(TAG, "✗ SafeEarFirebaseMessagingService NOT found in manifest")
            Log.e(TAG, "  Error: ${e.message}")
        }
    }

    private fun logEnvironment() {
        Log.i(TAG, "Device environment:")
        Log.i(TAG, "  - Device: ${android.os.Build.DEVICE}")
        Log.i(TAG, "  - Model: ${android.os.Build.MODEL}")
        Log.i(TAG, "  - Android: ${android.os.Build.VERSION.SDK_INT}")
    }

    /**
     * Test FCM by logging received messages
     * Call this from SafeEarFirebaseMessagingService.onMessageReceived()
     */
    fun logIncomingMessage(title: String?, body: String?, data: Map<String, String>) {
        Log.i(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.i(TAG, "Title: $title")
        Log.i(TAG, "Body: $body")
        Log.i(TAG, "Data keys: ${data.keys}")
        data.forEach { (k, v) ->
            Log.i(TAG, "  $k = $v")
        }
        Log.i(TAG, "========================")
    }

    /**
     * Log token refresh events
     */
    fun logTokenRefresh(newToken: String) {
        Log.i(TAG, "FCM TOKEN REFRESHED")
        Log.i(TAG, "New token: $newToken")
        Log.i(TAG, "Action: Update backend device_token field with this token")
    }
}

/**
 * MANUAL FCM VERIFICATION CHECKLIST:
 * 
 * 1. ANDROID MANIFEST:
 *    - ✓ Service declared with MESSAGING_EVENT filter
 *    - ✓ Permissions: POST_NOTIFICATIONS (Android 13+)
 *    
 * 2. GRADLE:
 *    - ✓ Firebase BOM added
 *    - ✓ firebase-messaging dependency included
 *    - ✓ Google Services plugin applied (google-services)
 *    
 * 3. FIREBASE CONFIG:
 *    - ✓ google-services.json in app/ folder
 *    - ✓ Valid Google Project ID in google-services.json
 *    - ✓ Firebase Console enabled for project
 *    - ✓ Cloud Messaging API enabled
 *    
 * 4. APP STARTUP:
 *    - ✓ Firebase initialized automatically (no manual init needed)
 *    - ✓ SafeEarFirebaseMessagingService loads at app start
 *    - ✓ Token available after ~2 seconds
 *    
 * 5. TESTING:
 *    - In logcat: adb logcat | grep "SafeEar-FCM"
 *    - Look for "FCM Token: eyJhbGc..." (long string)
 *    - Send test message from Firebase Console
 *    - Should see "FCM MESSAGE RECEIVED" in logcat
 *    
 * 6. COMMON ISSUES:
 *    - No token = Check Play Services installed
 *    - Service not called = Check manifest registration
 *    - Message not received = Check topic subscription on backend
 *    - Message in background = Make sure data fields populated (not just notification)
 */
