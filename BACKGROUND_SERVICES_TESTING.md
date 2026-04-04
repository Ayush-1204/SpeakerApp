# Background Services Testing Guide

This guide explains how to verify that all three requirements are working:
1. Child mode running in background until stop
2. Alerts refreshing in background
3. FCM notifications receiving

---

## Quick Verification (Without Device)

### Check FCM Setup
```bash
# Open logcat with SafeEar filter
adb logcat | grep "SafeEar"
```

Look for these messages in order on app startup:
```
SafeEar-FCM: === STARTING FCM DIAGNOSTICS ===
SafeEar-FCM: ✓ Firebase initialized
SafeEar-FCM: ✓ Google Play Services available
SafeEar-FCM: ✓ FCM Token: <long_token_string>
SafeEar-FCM: FCM TOKEN REFRESHED
```

If you see these ✓ marks, FCM is properly configured.

---

## Test 1: Child Mode Background Detection (Foreground Service)

### What Should Happen
- When child device starts monitoring, DetectionService creates a persistent notification
- Service continues recording/streaming even when app is closed or screen is off
- Service only stops when user clicks "Stop Recording"

### How to Test

**Step 1: Clear previous service instances**
```bash
adb shell am force-stop com.example.speakerapp
```

**Step 2: Start app on child device**
```bash
adb shell am start -n com.example.speakerapp/.MainActivity
```

**Step 3: Login and navigate to child monitoring**
- Make sure device role is "child_device"
- You should see the child monitoring screen

**Step 4: Click "Start Recording"**
```bash
# Watch logcat for service startup
adb logcat | grep "DetectionService\|Detection cycle"
```

Expected output:
```
DetectionService: START command received
DetectionService: Starting background detection stream...
DetectionService: Detection cycle: Checking audio...
```

**Step 5: Verify foreground notification**
```bash
# See active notifications
adb shell dumpsys notifications | grep -i safeear
```

You should see:
```
SafeEar Detection (Notification)
Active - listening for voices
```

**Step 6: Close the app**
```bash
adb shell am start com.android.launcher  # Go home
```

**Step 7: Verify service persists**
```bash
# Watch logcat - you should STILL see "Detection cycle:" messages
adb logcat | grep "Detection cycle"
```

Expected: Service continues running, notification remains visible

**Step 8: Stop recording from app**
- Bring app back to foreground
- Click "Stop Recording"
- Verify logcat shows:
```
DetectionService: STOP command received
DetectionService: Stopping background detection stream...
DetectionService: stopForeground + stopSelf called
```

---

## Test 2: Background Alert Refresh (WorkManager)

### What Should Happen
- Every 2 minutes, WorkManager fetches latest alerts from backend
- If new alerts exist, a notification appears
- Works even when app is closed

### How to Test

**Step 1: Stop app and verify WorkManager**
```bash
adb shell am force-stop com.example.speakerapp
```

**Step 2: Check WorkManager status**
```bash
# List all scheduled work
adb shell dumpsys jobscheduler | grep -i alert
```

**Step 3: Start app**
```bash
adb shell am start -n com.example.speakerapp/.MainActivity
```

**Step 4: Watch for alert refresh scheduling**
```bash
adb logcat | grep "AlertWorkerManager\|AlertRefreshWorker"
```

Expected at startup:
```
SafeEar-AlertWorker: Background alert refresh scheduled: Every 2 minutes
```

**Step 5: Close app and wait**
```bash
# Close app 
adb shell am start com.android.launcher

# Wait 2-3 minutes and watch logcat
adb logcat | grep "AlertRefreshWorker" &
sleep 120  # Wait 2 minutes
```

Expected every 2 minutes (even with app closed):
```
SafeEar-AlertRefreshWorker: AlertRefreshWorker: Starting background alert refresh...
SafeEar-AlertRefreshWorker: AlertRefreshWorker: Successfully completed background refresh
```

---

## Test 3: FCM Notifications (Firebase Cloud Messaging)

### What Should Happen
- Device receives FCM tokens from Firebase
- Token is sent to backend during registration
- When backend sends FCM message, notification appears even with app closed/screen off

### How to Test

**Step 1: Verify Token Generation on Device**
```bash
# Create new emulator or clear app data for fresh token
adb shell pm clear com.example.speakerapp
adb shell am start -n com.example.speakerapp/.MainActivity
```

**Step 2: Capture Token from Logcat**
```bash
# Watch for token generation
adb logcat | grep "SafeEar-FCM.*Token"
```

Expected output example:
```
SafeEar-FCM: ✓ FCM Token: eL3m...xK9z (87 char long string)
SafeEar-FCM: (This should be sent to backend as device_token)
```

**Copy this token** - you need it to test from backend.

**Step 3: Send Test Message from Firebase Console**

Option A - Firebase Console (User-friendly):
1. Go to https://console.firebase.google.com/
2. Select your project
3. Cloud Messaging → Send your first message
4. Title: "Test Alert"
5. Body: "This is a test notification"
6. Send to: Select "Send to a topic or condition"
7. Set notification topic: (ask backend team for topic, or use device token)
8. Click "Send"

Option B - Backend API (What backend will do):
```python
from firebase_admin import messaging

message = messaging.Message(
    notification=messaging.Notification(
        title="Alert Test",
        body="This is a test notification"
    ),
    data={
        "alertId": "TEST123",
        "latitude": "40.7128",
        "longitude": "-74.0060"
    },
    token="<PASTE_TOKEN_HERE>"  # Use captured token from Step 2
)

response = messaging.send(message)
print(f"Message sent: {response}")
```

**Step 4: Verify Message Reception**
```bash
# With app in foreground
adb logcat | grep "FCM MESSAGE RECEIVED"
```

Expected:
```
SafeEarFirebaseMessagingService: FCM MESSAGE RECEIVED ==================
SafeEarFirebaseMessagingService: Title: Alert Test
SafEarFirebaseMessagingService: Body: This is a test notification
```

**Step 5: Test with App Closed/Background**
```bash
# Close app
adb shell am force-stop com.example.speakerapp

# Send another test message from Firebase Console
# (from Option A above)

# Wait 5 seconds and check device notifications
# You should see push notification appear on lock screen or notification tray

# Verify in logcat:
adb logcat | grep "SafeEar"
```

Expected: Notification appears even with app stopped

---

## Troubleshooting

### Issue: No FCM Token appears
**Solution:**
1. Ensure Google Play Services installed on device/emulator
2. Check internet connectivity: `adb shell ping 8.8.8.8`
3. Verify google-services.json in app/
4. Check Firebase Console has project properly configured

### Issue: Service doesn't persist in background
**Solution:**
1. Verify Android 12+: `adb shell getprop ro.build.version.sdk`
2. Check notification permission granted: `adb shell dumpsys permissions | grep POST_NOTIFICATIONS`
3. Ensure manifest has FOREGROUND_SERVICE_MICROPHONE permission
4. Device battery not in extreme power saving mode

### Issue: AlertRefreshWorker not triggering every 2 minutes
**Solution:**
1. Check if constraints prevent execution (charging, network)
2. Verify app has permission to schedule background work
3. Disable battery optimization: Settings → Battery → Disable "SpeakerApp"

---

## Expected Logcat Output (Full Session)

```
# App startup
D/SafeEar: FCM diagnostics started
I/SafeEar-FCM: === STARTING FCM DIAGNOSTICS ===
I/SafeEar-FCM: ✓ Firebase initialized
I/SafeEar-FCM: ✓ Google Play Services available
I/SafeEar-FCM: ✓ FCM Token: eL3m...xK9z
I/SafeEar-AlertWorker: Background alert refresh scheduled: Every 2 minutes

# Child monitoring started
D/ChildMonitoringViewModel: Starting detection stream
D/ChildMonitoringViewModel: DetectionService started
D/DetectionService: START command received
D/DetectionService: Starting background detection stream...
D/DetectionService: Detection cycle: Checking audio...

# After 2 minutes (background alert refresh)
D/SafeEar-AlertRefreshWorker: AlertRefreshWorker: Starting background alert refresh...

# FCM message reception
I/SafeEarFirebaseMessagingService: FCM MESSAGE RECEIVED ==================
I/SafEarFirebaseMessagingService: Title: New Alert
```

---

## Quick Command Reference

```bash
# Start app
adb shell am start -n com.example.speakerapp/.MainActivity

# Close app
adb shell am force-stop com.example.speakerapp

# Watch FCM logs
adb logcat | grep "SafeEar-FCM"

# Watch Detection Service
adb logcat | grep "DetectionService"

# Watch Alert Refresh Worker
adb logcat | grep "AlertRefreshWorker"

# Check notifications
adb shell dumpsys notifications | grep -i safeear

# Clear app data (fresh token)
adb shell pm clear com.example.speakerapp
```

---

## Backend Integration Checklist

For full FCM functionality, backend needs to:

- [ ] Store `device_token` when device registers
- [ ] When alert created, call `messaging.send()` with:
  - `token`: recipient device_token
  - `notification.title`: Alert title
  - `notification.body`: Alert message
  - `data.alertId`: ID for tracking
  - `data.latitude`: Location (if available)
  - `data.longitude`: Location (if available)
- [ ] Test sending message to specific device token
- [ ] Implement retry logic for failed sends

---

## Expected Timeline

1. **App Startup** (0s): FCM diagnostics run, alert refresh scheduled
2. **Start Recording** (variable): DetectionService starts, notification appears
3. **2 min intervals**: AlertRefreshWorker automatic execution
4. **Stop Recording**: Service shuts down cleanly
5. **App Closed**: Services continue in background (detectable via logcat)
