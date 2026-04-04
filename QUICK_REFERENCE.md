# Background Services - Quick Verification Card

## Copy These Commands

### Verify All Services Running

```bash
# Complete background service status check
echo "=== BACKGROUND SERVICES VERIFICATION ===" && \
echo "" && \
echo "1. FCM Diagnostics:" && \
adb logcat | grep "SafeEar-FCM" | head -10 && \
echo "" && \
echo "2. Detection Service:" && \
adb logcat | grep "DetectionService\|Detection cycle" | head -5 && \
echo "" && \
echo "3. Alert Worker:" && \
adb logcat | grep "AlertRefreshWorker" | head -5
```

---

## What to Look For

### ✅ Child Mode Running in Background (DetectionService)

**When you START recording:**
```
D/DetectionService: START command received
D/DetectionService: Starting background detection stream...
D/DetectionService: Detection cycle: Checking audio...
```

**After closing app (you should STILL see):**
```
D/DetectionService: Detection cycle: Checking audio...
D/DetectionService: Detection cycle: Checking audio...
D/DetectionService: Detection cycle: Checking audio...
```

**When you STOP recording:**
```
D/DetectionService: STOP command received
D/DetectionService: Stopping background detection stream...
```

---

### ✅ Alerts Refreshing in Background (AlertRefreshWorker)

**At app startup:**
```
D/SafeEar-AlertWorker: Background alert refresh scheduled: Every 2 minutes
```

**After 2 minutes (even with app closed):**
```
D/SafeEar-AlertRefreshWorker: === AlertRefreshWorker: Starting background refresh ===
D/SafeEar-AlertRefreshWorker: Work execution #1
D/SafeEar-AlertRefreshWorker: Prerequisites OK - token present, parent device role confirmed
D/SafeEar-AlertRefreshWorker: === AlertRefreshWorker: Background refresh completed ===
```

**Every 2 minutes:**
```
D/SafeEar-AlertRefreshWorker: === AlertRefreshWorker: Starting background refresh ===
...
D/SafeEar-AlertRefreshWorker: === AlertRefreshWorker: Background refresh completed ===
```

---

### ✅ FCM Ready to Receive Notifications (FCMDiagnostics)

**At app startup:**
```
I/SafeEar-FCM: === STARTING FCM DIAGNOSTICS ===
I/SafeEar-FCM: ✓ Firebase initialized
I/SafeEar-FCM: ✓ Google Play Services available
I/SafeEar-FCM: ✓ FCM Token: <your_token_here>
I/SafeEar-FCM: (This should be sent to backend as device_token)
I/SafeEar-FCM: ✓ SafeEarFirebaseMessagingService declared in manifest
I/SafeEar-FCM: === FCM DIAGNOSTICS COMPLETE ===
```

**Copy the token string** and send to backend to test FCM message sending.

---

## Notification Verification

**Notification appears when message sent:**
```
I/SafeEarFirebaseMessagingService: FCM MESSAGE RECEIVED ==================
I/SafeEarFirebaseMessagingService: Title: Your Alert Title
I/SafEarFirebaseMessagingService: Body: Your Alert Message
```

---

## Complete Testing Scenario

### Scenario 1: Child Mode Background (5 minutes)

1. **Start app**
   ```bash
   adb logcat | grep "SafeEar"
   ```
   Look for: FCM diagnostics ✓

2. **Go to Child Monitoring**
   - Select child_device role if needed
   - Click "Start Recording"

3. **Verify service started**
   ```bash
   adb logcat | grep "DetectionService"
   ```
   Look for: `Detection cycle: Checking audio...`

4. **Close app**
   ```bash
   adb shell am start com.android.launcher
   ```

5. **Verify still running (wait 10 seconds)**
   ```bash
   adb logcat | grep "Detection cycle"
   ```
   Look for: Still seeing `Detection cycle` messages

6. **Stop from app** (open app again)
   - Click "Stop Recording"

7. **Verify stopped**
   ```bash
   adb logcat | grep "DetectionService: STOP"
   ```

---

### Scenario 2: Alert Refresh Background (4 minutes)

1. **Start app**
   ```bash
   adb logcat | grep "AlertRefreshWorker"
   ```
   Look for: `Background alert refresh scheduled`

2. **Close app**
   ```bash
   adb shell am force-stop com.example.speakerapp
   ```

3. **Wait 2 minutes 30 seconds**

4. **Check for worker execution**
   ```bash
   adb logcat | grep "AlertRefreshWorker: Starting"
   ```
   Look for: Worker ran automatically

5. **Wait another 2 minutes**
   ```bash
   adb logcat | grep "AlertRefreshWorker"
   ```
   Look for: Another execution exactly 2 minutes later

---

### Scenario 3: FCM Message Reception (5 minutes)

1. **Start app and capture token**
   ```bash
   adb logcat -c && \
   adb shell am start -n com.example.speakerapp/.MainActivity && \
   sleep 3 && \
   adb logcat | grep "SafeEar-FCM: ✓ FCM Token"
   ```
   Copy the token string

2. **Send test message from Firebase Console:**
   - Go to https://console.firebase.google.com/
   - Select project → Cloud Messaging
   - Click "Send your first message"
   - Title: "Test Alert"
   - Body: "Testing FCM"
   - Click "Send to a specific device" → paste token

3. **Verify message received**
   ```bash
   adb logcat | grep "FCM MESSAGE RECEIVED\|SafeEarFirebaseMessagingService"
   ```
   Look for: Message title and body logged

4. **Check device notification**
   - Should see push notification appear
   - If app closed, still appears on lock screen

---

## Troubleshooting Quick Fixes

| Problem | Quick Fix |
|---------|-----------|
| No FCM Token | `adb shell pm clear com.example.speakerapp` + restart |
| Service not persisting | Check device battery settings (disable battery saver) |
| Worker not running | Restart app + wait exact 2 minutes |
| Token is empty | Verify google-services.json is in `app/` folder |
| Message not received | Send to specific token (not topic) |

---

## What Each Component Does

| Component | Purpose | When | Still Active When Closed |
|-----------|---------|------|-------------------------|
| **DetectionService** | Child device recording | Click start | ✅ Yes (foreground) |
| **AlertRefreshWorker** | Parent device updates | Every 2 min | ✅ Yes (WorkManager) |
| **FCMDiagnostics** | Verify FCM setup | App startup | ❌ No (one-time) |
| **FCM Message Handler** | Receive notifications | Backend sends | ✅ Yes (service) |

---

## Success Criteria

✅ All three working when:
1. Child recording persists 10+ minutes with app closed
2. Alert refresh executes every exactly 2 minutes consistently
3. FCM token appears on startup in logcat
4. Backend can send FCM message and notification appears on device

---

## Share This With Backend Team

**Required for FCM message sending:**

```
1. Get device_token from "FCM_DIAGNOSTICS" startup log
2. Send POST request to Firebase Cloud Messaging API:
   {
     "token": "<device_token_from_log>",
     "notification": {
       "title": "Alert",
       "body": "New voice detected"
     },
     "data": {
       "alertId": "123",
       "latitude": "40.7128",
       "longitude": "-74.0060"
     }
   }
3. Within 5 seconds, notification should appear on device
```

---

## Reference Files

- `BACKGROUND_SERVICES_SUMMARY.md` - Architecture overview
- `BACKGROUND_SERVICES_TESTING.md` - Detailed testing procedures
- `FCM_DEBUGGING_GUIDE.md` - FirebaseCloud Messaging troubleshooting
- `app/src/main/java/com/example/speakerapp/service/DetectionService.kt` - Service code
- `app/src/main/java/com/example/speakerapp/features/alerts/workers/AlertRefreshWorker.kt` - Worker code
- `app/src/main/java/com/example/speakerapp/core/fcm/FCMDiagnostics.kt` - FCM verification code
