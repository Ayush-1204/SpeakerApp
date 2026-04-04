# SafeEar FCM & Background Monitoring - Complete Debugging Guide

## Quick Status Check

### How to Verify FCM is Working

Run this command in a terminal after app starts:
```bash
adb logcat | grep "SafeEar-FCM"
```

You should see output like:
```
I/SafeEar-FCM: ✓ Firebase initialized
I/SafeEar-FCM: ✓ Google Play Services available
I/SafeEar-FCM: ✓ FCM Token: eyJhbGc...
I/SafeEar-FCM: (Token should be sent to backend as device_token)
```

If you see this, FCM is properly initialized.

---

## Issue 1: Background Child Monitoring Not Working

### Root Cause
The app now uses a **Foreground Service** (`DetectionService`) to continue audio streaming even when app is in background.

### How It Works
1. When child clicks **"Start Recording"** → `DetectionService` starts as foreground service
2. Foreground service shows persistent notification "SafeEar Monitoring - Active"
3. Service continues even when:
   - User closes the app
   - Phone screen turns off
   - App is in background

### To Test
```
1. Open app → Child Monitoring screen
2. Click "Start Recording"
3. **Press home button** (app goes to background)
4. in Terminal: adb logcat | grep "SafeEar-DetectionService"
5. You should see "Detection cycle:" messages continuing
6. Click "Stop Recording" to terminate
```

### If Not Working
Check logs for:
```bash
adb logcat | grep "SafeEar"
```

Look for errors like:
- `Failed to start DetectionService` - Permission issue
- `Detection job cancelled` - Service was killed
- No logs at all - Check if class path is correct

---

## Issue 2: Background Alert Refresh Not Running

### Root Cause
Background alert refresh runs via **WorkManager** (every 2 minutes).

### How It Works
1. At app startup → `AlertWorkerManager.startBackgroundAlertRefresh()` schedules work
2. WorkManager runs every 2 minutes, fetches alerts
3. **Parent device** gets alerts even while app is closed or in background

### To Test
```bash
# Check if work is scheduled
adb shell dumpsys jobscheduler

# View WorkManager logs
adb logcat | grep "AlertRefreshWorker"
```

Expected output:
```
D/SafeEar-AlertWorker: Background alert refresh scheduled: Every 2 minutes
D/SafeEar-AlertRefreshWorker: Starting background alert refresh...
```

### If Not Working
1. Check if WorkManager added to build.gradle ✓ (Already added)
2. Verify Android 6+ device (required for WorkManager)
3. Try manually triggering:
   ```bash
   adb shell cmd workmanager run --name alert_refresh_periodic
   ```

---

## Issue 3: Notifications Not Received

### Complete Verification Checklist

#### Step 1: Check FCM Service Declaration
```bash
adb shell pm dump com.example.speakerapp | grep SafeEarFirebaseMessagingService
```

Should output:
```
Service SafeEar...FirebaseMessagingService:
  android.intent.action.MESSAGING_EVENT
```

#### Step 2: Check FCM Token is Being Logged
```bash
adb logcat -c
# Launch app
sleep 5
adb logcat | grep "SafeEar-FCM" | head -20
```

Look for:
```
I/SafeEar-FCM: ✓ FCM Token: eyJ...
```

If you see `✗` messages instead, FCM is not initialized.

#### Step 3: Test FCM Message Delivery

**Option A: Using Firebase Console**
1. Go to: https://console.firebase.google.com
2. Select your project → Cloud Messaging
3. Create new campaign → Send test message
4. Select device by FCM token (from logs)
5. Send

Watch logcat:
```bash
adb logcat | grep "SafeEar-FCM"
```

Should see:
```
I/SafeEar-FCM: FCM MESSAGE RECEIVED
I/SafeEar-FCM: From: ...
I/SafeEar-FCM: Data: {...}
I/SafeEar-FCM: Title: Test
I/SafeEar-FCM: Body: Test message
```

**Option B: Using Backend API (curl)**
```bash
# First, get the FCM token from logs
# Then send via backend (backend must implement FCM API call)

# Example backend code needed:
from firebase_admin import messaging

message = messaging.Message(
    notification=messaging.Notification(
        title="Stranger Alert",
        body="Unknown voice detected"
    ),
    data={
        "deviceId": "...",
        "alertId": "...",
        "latitude": "...",
        "longitude": "..."
    },
    token="DEVICE_FCM_TOKEN_HERE"  # From logcat
)

response = messaging.send(message)
print(response)
```

#### Step 4: If Message Received But No Notification

Check permissions:
```bash
adb shell pm dump com.example.speakerapp | grep PERMISSIONS
```

Should contain:
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECORD_AUDIO`

If missing, manually grant in app:
```bash
adb shell pm grant com.example.speakerapp android.permission.POST_NOTIFICATIONS
```

#### Step 5: Check Notification Channel

View notification channels:
```bash
adb shell cmd notification list_configs --package=com.example.speakerapp
```

Should show:
```
safeear_alerts: IMPORTANCE_HIGH
```

---

## How to Know FCM Setup is Complete

| Item | Status | How to Check |
|------|--------|------------|
| Firebase initialized | ✓ | Run `adb logcat \| grep "SafeEar-FCM"` |
| Play Services available | ✓ | Same logs as above |
| FCM token obtained | ✓ | Look for "FCM Token: eyJ..." |
| SafeEarMessagingService running | ✓ | Send test message, see logs |
| onMessageReceived() called | ✓ | "FCM MESSAGE RECEIVED" in logs |
| Notification posted | ✓ | See notification on device |
| Background refresh scheduled | ✓ | Check WorkManager jobs |
| DetectionService running | ✓ | Persistent notification shows |

---

## Commands Summary

```bash
# View all SafeEar logs
adb logcat -c && adb logcat | grep "SafeEar"

# View only FCM logs
adb logcat | grep "SafeEar-FCM"

# View only service logs
adb logcat | grep "SafeEar-Detection"

# View only alert refresh logs
adb logcat | grep "AlertRefresh"

# Check if services are declared
adb shell pm dump com.example.speakerapp | grep "Service"

# Fine-grained permission check
adb shell pm dump com.example.speakerapp | grep POST_NOTIFICATIONS

# Manually send WorkManager job
adb shell cmd workmanager run --name alert_refresh_periodic

# Clear app data and restart
adb shell pm clear com.example.speakerapp && adb shell am start -n com.example.speakerapp/.MainActivity

# View notification configuration
adb shell cmd notification list_configs
```

---

## Backend Requirements for Full Integration

Your backend must implement:

### 1. Accept FCM Token on Device Registration
```
POST /devices
{
  "device_name": "...",
  "role": "parent_device",
  "device_token": "eyJhbGc..." ← FCM token from app
}
```

Currently implemented in app ✓

### 2. Handle Token Refresh
```
POST /devices/{deviceId}/token
{
  "device_token": "eyJhbGc..." ← New token when FCM refreshes
}
```

Backend logs here will show when tokens are refreshed.

### 3. Send Alert Notifications
```python
from firebase_admin import messaging

def notify_parent_device(parent_device_token, alert):
    message = messaging.Message(
        notification=messaging.Notification(
            title="Stranger Alert",
            body=f"Unknown voice detected at {alert.created_at}"
        ),
        data={
            "alertId": str(alert.id),
            "latitude": str(alert.location.latitude),
            "longitude": str(alert.location.longitude),
            "timestamp": str(alert.created_at)
        },
        token=parent_device_token
    )
    response = messaging.send(message)
    return response
```

### 4. Initialize Firebase in Backend
```python
import firebase_admin
from firebase_admin import credentials

# Load credentials from Firebase Console → Service Accounts → Generate Key
cred = credentials.Certificate("firebase_credentials.json")
firebase_admin.initialize_app(cred)
```

---

## Testing Scenarios

### Scenario 1: Child Records in Background
```
1. Open app → Child Monitoring
2. Click "Start Recording"
3. See: "SafeEar Monitoring - Active" persistent notification
4. Close app / press home
5. Wait 5 seconds
6. Check: adb logcat | grep "DetectionService" → should see "Detection cycle"
7. Click notification to return to app
8. Click "Stop Recording"
```

### Scenario 2: Parent Gets Alert While App Closed
```
1. Open app → Parent Dashboard
2. Close app
3. Trigger stranger detection (from child device)
4. Backend sends FCM message
5. Device shows notification (even though app closed)
6. Click notification → app opens to Alerts screen
7. Alert is visible
```

### Scenario 3: Background Alert Refresh
```
1. Open app as parent
2. Note current alerts
3. Close app
4. In separate window, trigger new stranger detection
5. Wait 2-3 minutes
6. Reopen app
7. New alert should be visible (fetched in background)
```

---

## Troubleshooting Quick Links

- **No FCM token**: Check Google Play Services installed on device
- **Service not starting**: Check `FOREGROUND_SERVICE_MICROPHONE` permission granted
- **Notification not showing**: Check `POST_NOTIFICATIONS` permission (Android 13+)
- **Background job not running**: Check WorkManager on Android 6+
- **Message received but no action**: Check notification channel created with IMPORTANCE_HIGH

---

Generated: 2026-03-28
For latest logs, run: `adb logcat | grep SafeEar`
