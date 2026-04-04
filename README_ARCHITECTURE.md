# SafeEar Android App - Quick Start & Project Status

## 📋 Project Status Summary

**Complete** ✅:
- All API DTOs matching exact backend contracts
- Retrofit service with all 16 endpoints
- Network, auth, and token management infrastructure
- Data layer repositories (auth, devices, enrollment, detection, alerts)
- All ViewModels with reactive UiState
- Audio recording (16kHz WAV) and playback
- Comprehensive documentation

**Ready for UI Build** ⚠️:
- LoginScreen (dev token input + Google Sign-In placeholder)
- DeviceRegistrationScreen (role selector)
- ParentDashboard with AlertsScreen and SpeakerEnrollmentScreen
- ChildMonitoringScreen
- Navigation graph wiring

**Not Implemented**:
- Google Sign-In SDK integration (dev token works for testing)
- Foreground service for background streaming
- WorkManager for background polling
- Push notifications (currently polling)
- Instrumentation tests

---

## 🚀 Quick Setup & First Launch

### Prerequisites
```bash
# 1. Clone repo (already done)
cd c:\Users\AYUSH VERMA\AndroidStudioProjects\SpeakerAppv2

# 2. Ensure backend running
curl http://localhost:8000/health
# Expected: {"status": "ok"}

# 3. Configure BASE_URL in app/build.gradle.kts
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")
# OR for physical device:
buildConfigField("String", "BASE_URL", "\"http://192.168.137.1:8000/\"")
```

### Build & Run
```bash
# Build APK
./gradlew assembleDebug

# Install on emulator
./gradlew installDebugAndroidTest

# Or install on physical device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Monitor logs
adb logcat | grep "SpeakerApp\|AuthViewModel\|ApiService"
```

---

## 🔐 Authentication Flow

### Dev Token Testing (No Google Sign-In needed)
```kotlin
// LoginScreen input field
Input: "dev:test_parent"
        or "dev:test_child"

// Sent as
POST /auth/google
{
  "id_token": "dev:test_parent"
}

// Response
{
  "access_token": "eyJhbGc...",
  "refresh_token": "refresh_...",
  "parent_id": "parent_123"
}

// Stored in DataStore (encrypted)
// Intercepted on next request → Authorization: Bearer eyJhbGc...
```

### Token Refresh (Automatic on 401)
```
Request fails with 401
  ↓
TokenAuthenticator.authenticate() triggered
  ↓
POST /auth/refresh with current refresh_token
  ↓
IF 200 → Save new tokens, retry original request
IF 401 → Clear tokens (logout), user sees login screen
```

---

## 📱 Role-Based Navigation

### Parent Device
```
Login → Device Registration (select "parent_device") 
  ↓
Parent Dashboard
  ├─ Alerts Tab
  │   ├─ List alerts (polling every 15s)
  │   ├─ Tap alert → Shows location + timestamp + confidence
  │   ├─ Play clip button → Downloads WAV, plays via ExoPlayer
  │   └─ Acknowledge button → POST /alerts/{id}/ack
  │
  ├─ Speakers Tab
  │   ├─ List enrolled speakers
  │   ├─ Tap + Record → Record speaker for enrollment
  │   ├─ Upload WAV → Receive embedding feedback
  │   ├─ Rename → PATCH /enroll/speakers/{id}
  │   └─ Delete → DELETE /enroll/speakers/{id}
  │
  └─ Settings Tab
      ├─ Show: Device ID, Parent ID, Role
      └─ Logout button
```

### Child Device
```
Login → Device Registration (select "child_device")
  ↓
Child Monitoring Screen
  ├─ Large toggle button: START/STOP
  │   └─ ON: Records 1.5s chunks @ 16kHz WAV
  │
  ├─ Upload loop (continuous while ON)
  │   ├─ Upload chunk
  │   ├─ Include latitude, longitude
  │   └─ Receive decision (familiar|stranger_candidate|uncertain|hold)
  │
  ├─ Status display
  │   ├─ Last detection: warming_up|no_hop|ok
  │   ├─ Last decision + confidence
  │   └─ Alert fired? (with alert ID)
  │
  └─ Location updates
      └─ Auto-refresh from FusedLocationProvider
```

---

## 📡 API Contract Overview

### Endpoints Implemented

| Method | Path | Purpose |
|--------|------|---------|
| POST | /auth/google | Login with id_token |
| POST | /auth/refresh | Refresh access token |
| POST | /auth/logout | Revoke tokens |
| **POST** | **/devices** | **Register device (child/parent)** |
| POST | /enroll/speaker | Enroll new speaker (parent only) |
| GET | /enroll/speakers | List enrolled speakers (parent) |
| PATCH | /enroll/speakers/{id} | Rename speaker |
| DELETE | /enroll/speakers/{id} | Delete speaker |
| **POST** | **/detect/chunk** | **Upload audio chunk (child only)** |
| POST | /detect/location | Update location |
| DELETE | /detect/session | End detection session |
| GET | /alerts | List parent's alerts |
| **POST** | **/alerts/{id}/ack** | **Acknowledge alert** |
| GET | /alerts/{id}/clip | Download alert audio clip |
| GET | /health | Server health check |

**Bold** = Most frequently used

### Request/Response Examples

#### Parent Enrolls Speaker
```bash
curl -X POST http://localhost:8000/enroll/speaker \
  -H "Authorization: Bearer <token>" \
  -F "display_name=Grandma" \
  -F "file=@/path/to/audio.wav"

# Response
{
  "speaker_id": "sp_123",
  "display_name": "Grandma",
  "samples_saved": 12,
  "embedding_dim": 128,
  "stages": {
    "vad": {
      "voiced_ms": 3450
    },
    "speech_quality": {
      "passed": true,
      "score": 0.92
    }
  }
}
```

#### Child Uploads Chunk
```bash
curl -X POST http://localhost:8000/detect/chunk \
  -H "Authorization: Bearer <token>" \
  -F "device_id=dev_456" \
  -F "file=@/path/to/chunk.wav" \
  -F "latitude=37.7749" \
  -F "longitude=-122.4194"

# Response
{
  "status": "ok",
  "decision": "familiar",
  "score": 0.95,
  "stranger_streak": 0,
  "thresholds": {
    "t_high": 0.8,
    "t_low": 0.5
  },
  "alert_fired": false,
  "alert_id": null
}
```

#### Parent Gets Alerts
```bash
curl -X GET "http://localhost:8000/alerts?limit=50&offset=0" \
  -H "Authorization: Bearer <token>"

# Response
{
  "items": [
    {
      "id": "alert_789",
      "timestamp": "2024-02-15T10:30:45Z",
      "confidence_score": 0.72,
      "audio_clip_path": "/clips/alert_789.wav",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "acknowledged_at": null
    }
  ],
  "total": 5,
  "offset": 0,
  "limit": 50
}
```

---

## 🎵 Audio Handling

### Recording (Child → Detection Streaming)
```kotlin
// AudioRecorder.kt
val audioFile = File.createTempFile("chunk", ".wav")
audioRecorder.recordAudio(
  durationMs = 1500,    // 1.5 seconds
  outputFile = audioFile
)

// Output format
// - PCM input: Android system sample rate (44.1kHz, 48kHz, etc.)
// - Resampler: Downsamples to 16kHz mono
// - Encoder: WAV with RIFF header (PCM_16BIT)
// - Upload: Single multipart field "file"
// - Backend constraint: MUST be 16kHz (rejects with "audio_chunk_must_be_16khz" if not)
```

### Playback (Parent ← Alert Clip)
```kotlin
// AudioPlayer.kt
alertsViewModel.playAlertClip(alertId = "alert_789")
  ↓
AlertsRepository.getAlertClip()
  ↓
Receives audio/wav bytes
  ↓
AudioPlayer.playFromBytes(bytes)
  ↓
ExoPlayer renders to device speaker
  ↓
UI shows isPlaying, currentPosition, duration
```

---

## 🏗️ Architecture Layers

```
┌─────────────────────────────────────────┐
│  PRESENTATION (Compose UI)              │
│  Screens + ViewModels + UiState         │
└──────────────────┬──────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│  DOMAIN (Use Cases)                     │
│  LoginUseCase, EnrollUseCase, etc.      │
└──────────────────┬──────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│  DATA (Repositories)                    │
│  AuthRepository, EnrollmentRepository   │
└──────────────────┬──────────────────────┘
                   ↓
┌─────────────────────────────────────────┐
│  NETWORK (Retrofit)                     │
│  ApiService + Interceptors + Auth       │
└──────────────────┬──────────────────────┘
                   ↓
        Backend API (SafeEar)
```

---

## ✅ Testing Checklist

See [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md) for detailed commands:

- [ ] Build APK successfully
- [ ] Backend responds to /health
- [ ] Login with dev token succeeds
- [ ] Device registration succeeds (both roles)
- [ ] Parent can enroll speaker
- [ ] Parent can list, rename, delete speakers
- [ ] Child can upload detection chunks (1.5s @ 16kHz)
- [ ] Child 403 when parent attempts /detect/chunk
- [ ] Parent can list alerts
- [ ] Parent can ack alert
- [ ] Parent can download + play alert clip
- [ ] Token refresh on 401 works
- [ ] Logout clears tokens + shows login screen

---

## 📚 Key Files Reference

| File | Purpose | Lines |
|------|---------|-------|
| [ApiService.kt](app/src/main/java/com/example/speakerapp/network/ApiService.kt) | All 16 endpoints | ~200 |
| [AuthRepository.kt](app/src/main/java/com/example/speakerapp/features/auth/data/AuthRepository.kt) | Login/logout | ~80 |
| [TokenManager.kt](app/src/main/java/com/example/speakerapp/core/auth/TokenManager.kt) | DataStore persistence | ~90 |
| [TokenAuthenticator.kt](app/src/main/java/com/example/speakerapp/core/auth/TokenAuthenticator.kt) | 401 refresh logic | ~60 |
| [AuthViewModel.kt](app/src/main/java/com/example/speakerapp/features/auth/ui/AuthViewModel.kt) | Auth UiState | ~100 |
| [AudioRecorder.kt](app/src/main/java/com/example/speakerapp/core/audio/AudioRecorder.kt) | 16kHz WAV encoding | ~180 |
| [ChildMonitoringViewModel.kt](app/src/main/java/com/example/speakerapp/features/detection/ui/ChildMonitoringViewModel.kt) | Chunk recording loop | ~150 |
| [AlertsViewModel.kt](app/src/main/java/com/example/speakerapp/features/alerts/ui/AlertsViewModel.kt) | Alert polling (15s) | ~100 |

---

## 🐛 Troubleshooting

### "Connection failed"
```bash
# Check backend address (app/build.gradle.kts)
# For emulator: http://10.0.2.2:8000/
# For device: http://192.168.x.x:8000/
adb logcat | grep "BASE_URL"
```

### "Invalid audio format" (400 from /detect/chunk)
```bash
# Verify AudioRecorder outputs 16kHz WAV
# Check: AudioRecorder.kt line ~100 (sample rate config)
# Re-run: audioRecorder.recordAudio(1500, file)
```

### "Token expired" (401)
```bash
# TokenAuthenticator will retry once automatically
# If still 401, check:
# - refresh_token not expired
# - Backend /auth/refresh endpoint working
# - Tokens saved in DataStore
```

### "Only child devices can upload" (403)
```bash
# Parent device attempted POST /detect/chunk
# This is role enforcement working
# Verify device role: Settings tab shows "parent_device"
```

---

## 📖 Documentation Files

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Full architecture overview + layer explanations
- **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** - Code snippets for ViewModels + Screens + Hilt setup
- **[RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md)** - Build, test, curl examples, troubleshooting
- **API Specification** - In conversation history (not in repo)

---

## 🎯 Next Phase: UI Implementation

1. Implement Compose screens from IMPLEMENTATION_GUIDE.md
2. Wire screens to ViewModels (collectors on UiState)
3. Setup Navigation graph (role-based routing)
4. Test happy path: login → registration → role dashboard
5. Integrate Google Sign-In (optional, dev token works)
6. Add background service + WorkManager (enhancement)

All infrastructure is ready. UI layer is templated and ready to build.

---

**Last Updated**: Today  
**Status**: Infrastructure Complete | UI Ready for Build  
**Kotlin**: 2.0.21 | **Compose**: 2024.11.00 | **Min SDK**: 26

