/**
 * SAFEEAR ANDROID APP - PRODUCTION SETUP & RUN GUIDE
 * 
 * This guide provides step-by-step instructions to build, configure, and run
 * the SafeEar Android application with exact backend contract compliance.
 */

# TABLE OF CONTENTS
1. Environment Setup
2. Project Configuration
3. Build & Deploy
4. Backend Connection
5. First Launch Checklist
6. Testing Procedures
7. Troubleshooting
8. Known Limitations

---

# 1. ENVIRONMENT SETUP

## Prerequisites
- Android Studio 2024.1+
- Android SDK 35 (compileSdk)
- Kotlin 2.0.21+
- Java 17 JDK
- Git

## Step 1: Clone/Verify Project Structure
```bash
cd /path/to/SpeakerAppv2
git status

Required directories:
- app/src/main/java/com/example/speakerapp/
  ├── features/      # Feature modules
  ├── core/          # Core infrastructure
  ├── network/       # API layer
  └── MainActivity.kt
```

## Step 2: Configure Local Properties
```bash
# local.properties (auto-detected or create manually)
sdk.dir=/path/to/Android/sdk
```

---

# 2. PROJECT CONFIGURATION

## Step A: BuildConfig and Base URL

Edit `app/build.gradle.kts` and ensure buildConfigField for BASE_URL:

```kotlin
android {
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")  // Emulator
        // For device on local network:
        // buildConfigField("String", "BASE_URL", "\"http://192.168.137.1:8000/\"")
    }
}
```

For **emulator**: Use `http://10.0.2.2:8000/`
For **device on Wi-Fi**: Use `http://192.168.137.1:8000/` (where host PC is 192.168.137.1)

## Step B: Permissions & Manifest

Verify `app/src/main/AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

# 3. BUILD & DEPLOY

## Build Command
```bash
cd /path/to/SpeakerAppv2

# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build and install to connected emulator/device
./gradlew installDebug

# Run on emulator
./gradlew runDebug
```

## Expected Output
```
> Task :app:assembleDebug
...
> Task :app:installDebug
Installing APK 'app/build/outputs/apk/debug/app-debug.apk'
...
BUILD SUCCESSFUL
```

---

# 4. BACKEND CONNECTION

## Start SafeEar Backend

Before launching the app, ensure backend is running:

```bash
# From SafeEar backend repo
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Verify backend health:
```bash
curl http://localhost:8000/health
# Expected response: {"status": "ok"} or similar
```

## Network Connectivity Test

**For Emulator:**
```bash
adb shell
ping 10.0.2.2:8000
curl http://10.0.2.2:8000/health
```

**For Device:**
```bash
ping 192.168.137.1:8000
```

---

# 5. FIRST LAUNCH CHECKLIST

✅ **Pre-Launch Verification:**
1. Backend is running on correct port (default: 8000)
2. BuildConfig.BASE_URL matches backend address
3. Emulator/device is on same network (or emulator bridge configured)
4. APK built with `assembleDebug` and installed
5. Permissions granted in app settings after first launch

## Runtime Permissions Granting

App will request permissions on first use:
- **RECORD_AUDIO** - for child device detection streaming
- **ACCESS_FINE_LOCATION** - for location tracking
- **POST_NOTIFICATIONS** - for alert notifications (Android 13+)

Grant all permissions for full functionality.

## First Run Flow

1. **Login Screen**: Dev token login (for testing)
   - Input: `dev:parent_test`
   - Expected: Navigate to device registration after successful auth

2. **Device Registration**:
   - Device Name: e.g., "My Phone"
   - Role: Choose "Parent Device" or "Child Device"
   - Expected: Device registered, navigate to home based on role

3. **Role-Based Routing**:
   - **Parent Device** → Parent Dashboard (Alerts + Enrollment)
   - **Child Device** → Child Monitoring (Detection Streaming)

---

# 6. TESTING PROCEDURES

## Unit Test Suite

Navigate to test directory and run with Android Studio or:

```bash
./gradlew testDebug

# Individual test modules
./gradlew :app:testDebugUnitTest
```

## Integration Test: API Contract Validation

**Test Case 1: POST /auth/google (Dev Token)**
```bash
# Request
curl -X POST http://localhost:8000/auth/google \
  -H "Content-Type: application/json" \
  -d '{"id_token":"dev:parent_alias"}'

# Expected Response (200)
{
  "access_token": "...",
  "refresh_token": "...",
  "token_type": "bearer",
  "expires_in": 604800,
  "parent": {
    "id": "uuid-string",
    "email": null,
    "display_name": null  
  }
}
```

**Test Case 2: POST /devices (Device Registration)**
```bash
curl -X POST http://localhost:8000/devices \
  -H "Authorization: Bearer <access_token>" \
  -F "device_name=TestDevice" \
  -F "role=child_device"

# Expected Response (200)
{
  "status": "created",
  "device": {
    "id": "device-uuid",
    "parent_id": "parent-uuid",
    "device_name": "TestDevice",
    "role": "child_device",
    "device_token": null
  }
}
```

**Test Case 3: POST /detect/chunk (Child Only, Audio 16kHz)**
```bash
# Create 16kHz WAV file first (using ffmpeg or similar)
ffmpeg -f lavfi -i sine=frequency=440:duration=1 -ar 16000 -ac 1 temp.wav

curl -X POST http://localhost:8000/detect/chunk \
  -H "Authorization: Bearer <access_token>" \
  -F "device_id=child-device-uuid" \
  -F "file=@temp.wav"

# Possible Response Status (200 OK):
{
  "status": "ok",
  "decision": "familiar",
  "score": 0.85,
  "alert_fired": false
}
```

**Test Case 4: Parent Cannot Call /detect/chunk (Role Gating)**
```bash
# Parent device attempts /detect/chunk
curl -X POST http://localhost:8000/detect/chunk \
  -H "Authorization: Bearer <parent_token>"
  -F "device_id=parent-device-uuid"
  -F "file=@temp.wav"

# Expected (403 Forbidden):
{
  "detail": "only_child_devices_can_stream_audio"
}
```

**Test Case 5: GET /alerts (Pagination)**
```bash
curl -X GET "http://localhost:8000/alerts?limit=10&offset=0" \
  -H "Authorization: Bearer <access_token>"

# Expected Response (200):
{
  "items": [
    {
      "id": "alert-uuid",
      "parent_id": "parent-uuid",
      "device_id": "device-uuid",
      "timestamp": "2026-03-27T12:00:00Z",
      "confidence_score": 0.75,
      "audio_clip_path": "s3://bucket/path/to/clip.wav",
      "latitude": 40.7128,
      "longitude": -74.0060,
      "acknowledged_at": null,
      "created_at": "2026-03-27T12:00:00Z",
      "updated_at": "2026-03-27T12:00:00Z"
    }
  ]
}
```

## Manual App Testing: Parent Flow

1. **Launch App** → Logs in with dev token
2. **Register as Parent** → Device registered with role: parent_device
3. **Navigate to Enrollment** → Tap "Add Speaker"
4. **Enroll Speaker** → Record/select WAV file
   - Display name: "John"
   - Expected: Speaker enrolled with samples and quality feedback
5. **View Speakers** → List all enrolled speakers
6. **Rename Speaker** → Update display name
7. **Alerts Dashboard** → View any fired alerts (if available from backend)
8. **Acknowledge Alert** → Mark alert as acknowledged
9. **Settings** → Verify device role displayed, logout works

## Manual App Testing: Child Flow

1. **Launch App** → Logs in with dev token
2. **Register as Child** → Device registered with role: child_device
3. **Child Monitoring Screen** → Toggle detection ON
   - Expected: Recording starts, chunks uploaded every 1-2 seconds
   - Status shows server response: "warming_up", "no_hop", or "ok"
4. **Location Integration** (if FusedLocationProvider enabled)
   - Location updates included with chunks
5. **Stop Detection** → Toggle OFF
   - Expected: Session ended via DELETE /detect/session
6. **Logout** → Verify tokens cleared

---

# 7. TROUBLESHOOTING

## Common Issues

### Issue: "Connection Refused" on Emulator
**Cause**: Backend not running or wrong IP
**Fix**:
```bash
# Verify backend is running
curl http://localhost:8000/health

# Check emulator can reach host
adb shell ping 10.0.2.2

# If needed, restart emulator with network bridge
emulator -avd <device> -no-snapshot-load
```

### Issue: "only_child_devices_can_stream_audio" when child attempts /detect/chunk
**Cause**: Child device not registered with "child_device" role
**Fix**: Re-register device, ensure exact role name: "child_device" (lowercase, underscore)

### Issue: "audio_chunk_must_be_16khz" error
**Cause**: WAV file not resampled to 16kHz
**Fix**: AudioRecorder handles resampling. If manual testing:
```bash
ffmpeg -i input.wav -ar 16000 -ac 1 output_16khz.wav
```

### Issue: Permissions Not Requested
**Cause**: Targeting Android 12+ requires runtime permission grants
**Fix**: Grant permissions in Settings > Apps > SafeEar > Permissions

### Issue: Token Refresh Fails
**Cause**: Refresh token expired or invalid
**Fix**: Complete logout and re-login with fresh dev token

### Issue: Audio Recording Fails
**Cause**: RECORD_AUDIO permission not granted or mic not available
**Check**:
```bash
adb shell pm list permissions | grep RECORD
adb shell settings list global | grep audio
```

---

# 8. KNOWN LIMITATIONS & FUTURE ENHANCEMENTS

## Limitations

1. **Audio Resampling**
   - Assumption: Device mic may capture at 44.1kHz or 48kHz, resampled to 16kHz
   - Fallback: If resampling not supported, use external tool (ffmpeg)

2. **Location Tracking**
   - Requires FusedLocationProvider setup (GooglePlay services)
   - May not work on devices without Google Play Services

3. **Token Refresh**
   - Retries refresh only once; subsequent 401 forces logout
   - No automatic re-login; user must manually re-authenticate

4. **Background Detection**
   - Child streaming stops if app backgrounded (not a foreground service in basic impl)
   - Enhancement: Implement WorkManager + ForegroundService for continuous monitoring

5. **Offline Mode**
   - No offline queue; failed uploads discarded
   - Enhancement: Add local SQLite cache for retry on reconnect

## Future Enhancements

- [ ] Foreground service for continuous child streaming
- [ ] WorkManager periodic sync for background polling
- [ ] Local cache + retry queue for network resilience
- [ ] Audio quality assessment before upload
- [ ] Geofencing support for location-triggered actions
- [ ] Rich notifications with clip preview
- [ ] Multi-device support (multiple children)
- [ ] Settings UI for interval customization

---

# 9. QUICK REFERENCE: IMPORTANT API CONSTRAINTS

| Endpoint | Auth | Role | Input | Output |
|----------|------|------|-------|--------|
| POST /auth/google | No | N/A | id_token (dev:alias format) | access_token, refresh_token |
| POST /devices | Yes | N/A | device_name, role (child_device \| parent_device) | DeviceResponse |
| POST /detect/chunk | Yes | child_device only | file (16kHz WAV), device_id | status, decision, alert_id |
| POST /enroll/speaker | Yes | N/A | display_name, file (WAV) | speaker_id, stages |
| GET /alerts | Yes | N/A | limit, offset | AlertsListResponse |
| POST /alerts/{id}/ack | Yes | N/A | - | AckAlertResponse |

**Role Enforcement**:
- `POST /detect/chunk`: Returns 403 if parent_device attempts upload
- `POST /devices`: Creates device with specified role; persists role to child/parent

## Networking Rules

- Token interceptor for Bearer auth
- 401 refresh retry with /auth/refresh once
- Fail to login if refresh fails
- Exact field names in DTOs (no renaming)

## DataStore & Auth State Management (CRITICAL)

### On Login Success
```kotlin
// Save to DataStore
dataStore.edit { prefs ->
  prefs[ACCESS_TOKEN_KEY] = response.access_token
  prefs[REFRESH_TOKEN_KEY] = response.refresh_token
  prefs[PARENT_ID_KEY] = response.parent.id
  prefs[DEVICE_ID_KEY] = response.device.id  // if returned
}
```

### On Logout (MUST DO ALL STEPS)
```kotlin
// Step 1: Call backend logout endpoint FIRST
val logoutResult = apiService.logout(DeleteLogoutRequest(refreshToken = storedRefreshToken))

// Step 2: Clear ALL DataStore keys (do this even if logout call fails or throws)
dataStore.edit { prefs ->
  prefs.remove(ACCESS_TOKEN_KEY)
  prefs.remove(REFRESH_TOKEN_KEY)
  prefs.remove(PARENT_ID_KEY)
  prefs.remove(DEVICE_ID_KEY)
  prefs.remove(DEVICE_ROLE_KEY)
  // Clear ANY other auth-related cached data
}

// Step 3: Clear OkHttp cache (if using cache interceptor)
okHttpClient.cache?.evictAll()

// Step 4: Navigate to login screen WITHOUT caching/remembering state
navController.navigate(LOGIN_ROUTE) {
  popUpTo(0) // Clear entire nav backstack
}
```

### On App Startup (Check Auth State)
```kotlin
// DO NOT auto-login from cached tokens
// ONLY proceed if ALL of these are present:
// - ACCESS_TOKEN non-empty
// - REFRESH_TOKEN non-empty  
// - PARENT_ID non-empty
// - DEVICE_ID non-empty

// If any are missing → show login screen
```

If logout silently crashes the app, ensure:
1. You're NOT throwing uncaught exceptions in logout handler
2. You're clearing DataStore in a try/catch block
3. You're clearing cache before navigation

## Multipart Rules (CRITICAL)

### Rule 1: Text + File Combined
When uploading a file + text fields together (e.g., display_name + audio file):
```kotlin
@Multipart
@POST("/enroll/speaker")
suspend fun enrollSpeaker(
  @Part("display_name") displayName: RequestBody,
  @Part("speaker_id") speakerId: RequestBody?,  // optional
  @Part audio: MultipartBody.Part  // NO parameter name here; name embedded in Part
): EnrollSpeakerResponse

// Caller usage:
val displayNameBody = RequestBody.create("text/plain".toMediaType(), name)
val partId = RequestBody.create("text/plain".toMediaType(), speakerId ?: "")
val audioPart = MultipartBody.Part.createFormData("audio", filename, audioBody)
enrollSpeaker(displayNameBody, partId, audioPart)
```

### Rule 2: File-Only (No Text Fields)
```kotlin
@Multipart
@POST("/detect/chunk")
suspend fun detectChunk(
  @Part("device_id") deviceId: RequestBody,
  @Part("latitude") latitude: RequestBody?,
  @Part("longitude") longitude: RequestBody?,
  @Part audio: MultipartBody.Part  // NO param name for MultipartBody.Part
): DetectResponse
```

### Rule 3: NEVER Do This
```kotlin
// WRONG - Causes "must not include a part name" error:
@Part("audio") audio: MultipartBody.Part  // ❌ WRONG

// CORRECT:
@Part audio: MultipartBody.Part  // ✓ Correct
```

Key: MultipartBody.Part parameters must NOT have @Part("name") annotation. The name is embedded when you call `MultipartBody.Part.createFormData("name", filename, body)`.

## Debugging Child Monitoring (If "Nothing Happens")

If child monitoring doesn't produce alerts, verify in this order:

1. **Device Role Check**
   - After onboarding, verify persisted role = "child_device" in DataStore
   - If role is "parent_device", monitoring won't work (endpoint rejects non-child roles)

2. **Chunk Submission**
   - Add logging to see if detectChunk() is being called
   - Log device_id, audio size, sample rate
   - Check for 16kHz WAV requirement (if SampleRate != 16000 → error)

3. **Backend Response Status**
   - Log the DetectResponse.status field (should be "warming_up", "no_hop", or "ok")
   - "ok" means stranger detected → alert should be created
   - Still no alert? Check: Is familiar speaker list empty? If empty, all audio = stranger alert

4. **Alerts List**
   - Call GET /alerts after detection
   - Should show new Alert with confidence_score > 0.5
   - Location lat/lon should match submitted coordinates

5. **Common Failures**
   - 403 Forbidden + "only_child_devices_can_stream_audio" → fix device role
   - 422 "audio_chunk_must_be_16khz" → resample audio to 16kHz before upload
   - 200 response but status="no_hop" → normal operation, no stranger detected
   - 200 response but status="warming_up" → model still warming up, try again

## Acceptance Criteria

App is accepted only if:

- Login works with Google/dev token flow and persists auth state safely
- Device registration persists exact role (`child_device` or `parent_device`)
- Child monitoring uploads valid 16kHz chunks and produces detectable statuses
- Parent alerts screen can list, play, and acknowledge alerts
- Logout clears auth state and returns to login without stale cache/backstack

---

# CONTACT & SUPPORT

For backend contract issues, refer to the SafeEar backend specification.
For Android-specific issues, check logcat:

```bash
adb logcat | grep SafeEar
adb logcat | grep "E/.*"  # All errors
```

---
