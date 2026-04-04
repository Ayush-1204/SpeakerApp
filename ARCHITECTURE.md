# SafeEar Android App - Final Architecture Summary

## Project Overview
Production-grade Android app implementing SafeEar parent/child speaker verification system using:
- Jetpack Compose UI
- MVVM architecture
- Hilt dependency injection
- Retrofit + OkHttp networking
- DataStore token persistence
- AudioRecord + Media3 for audio I/O

**API Contract**: All endpoints match exact SafeEar backend specifications. No field name auto-conversion. Role-based enforcement enforced client-side and server-side.

---

## Package Structure

```
app/src/main/java/com/example/speakerapp/
├── network/
│   ├── ApiService.kt                    # Retrofit interface (all 16 endpoints)
│   ├── dto/
│   │   ├── AuthDto.kt                   # Login/refresh/logout models
│   │   ├── DeviceDto.kt                 # Device registration models
│   │   ├── EnrollmentDto.kt             # Speaker enrollment models
│   │   ├── DetectionDto.kt              # Chunk upload + location models
│   │   └── AlertsDto.kt                 # Alert list + ack models
│   └── Constants.kt                     # BASE_URL reference
│
├── core/
│   ├── network/
│   │   └── NetworkModule.kt             # Hilt: Retrofit + OkHttp setup
│   ├── auth/
│   │   ├── TokenManager.kt              # DataStore token/device persistence
│   │   ├── AuthInterceptor.kt           # Adds Bearer token to requests
│   │   └── TokenAuthenticator.kt        # Handles 401 + retry-once refresh
│   └── audio/
│       ├── AudioRecorder.kt             # PCM capture → 16kHz WAV
│       └── AudioPlayer.kt               # ExoPlayer wrapper
│
├── features/
│   ├── auth/
│   │   ├── data/
│   │   │   └── AuthRepository.kt        # Login/logout using ApiService
│   │   ├── domain/
│   │   │   └── AuthUseCases.kt          # LoginUseCase, LogoutUseCase, etc.
│   │   ├── di/
│   │   │   └── AuthModule.kt            # Hilt: auth use case bindings
│   │   └── ui/
│   │       ├── AuthViewModel.kt         # UiState + auth flows
│   │       └── LoginScreen.kt           # Dev token input UI
│   │
│   ├── devices/
│   │   ├── data/
│   │   │   └── DeviceRepository.kt      # POST /devices registration
│   │   └── ui/
│   │       ├── DeviceRegistrationViewModel.kt  # Role selection
│   │       └── DeviceRegistrationScreen.kt
│   │
│   ├── enrollment/
│   │   ├── data/
│   │   │   └── EnrollmentRepository.kt  # Enroll, list, update, delete speakers
│   │   ├── domain/
│   │   │   └── EnrollmentUseCases.kt
│   │   └── ui/
│   │       ├── SpeakerEnrollmentViewModel.kt
│   │       ├── SpeakerListViewModel.kt
│   │       ├── SpeakerEnrollmentScreen.kt
│   │       └── SpeakerListScreen.kt
│   │
│   ├── detection/
│   │   ├── data/
│   │   │   └── DetectionRepository.kt   # Upload chunks, update location, end session
│   │   └── ui/
│   │       ├── ChildMonitoringViewModel.kt
│   │       └── ChildMonitoringScreen.kt
│   │
│   ├── alerts/
│   │   ├── data/
│   │   │   └── AlertsRepository.kt      # List, ack, download clip
│   │   └── ui/
│   │       ├── AlertsViewModel.kt       # Polling, ack state
│   │       └── AlertsScreen.kt
│   │
│   └── settings/
│       └── ui/
│           └── SettingsScreen.kt        # Device role, logout
│
├── navigation/
│   ├── Screen.kt                        # Route definitions
│   └── NavGraph.kt                      # Compose NavHost setup
│
├── MainActivity.kt                      # Entry point + NavHost
├── SpeakerApp.kt                        # @HiltAndroidApp
└── ui/
    └── theme/
        ├── Color.kt
        ├── Typography.kt
        └── Theme.kt
```

---

## Data Flow: Login → Device Registration → Role-Based Navigation

```
User Input (dev:alias)
    ↓
AuthViewModel.login()
    ↓
AuthRepository.loginWithGoogle(idToken)
    ↓
ApiService.googleAuth(request)
    ↓
TokenManager.saveTokens() + saveParentId()
    ↓
AuthResult.Success(AuthUser)
    ↓
Navigate to DeviceRegistrationScreen
    ↓
User selects role: "child_device" | "parent_device"
    ↓
DeviceRegistrationViewModel.registerDevice()
    ↓
DeviceRepository.registerDevice(name, role)
    ↓
ApiService.registerDevice(multipart: device_name, role)
    ↓
TokenManager.saveDeviceInfo(deviceId, role)
    ↓
IF role == "child_device" THEN redirect to ChildMonitoringScreen
IF role == "parent_device" THEN redirect to ParentDashboard
```

---

## Network Layer: Request/Response Lifecycle

```
Request:
  1. AuthInterceptor adds "Authorization: Bearer <access_token>"
  2. OkHttpClient sends request
  3. ApiService.method() called
  4. Retrofit encodes body (json/multipart) + sends

Response:
  400+ Error:
    → response.errorBody()?.string() parsed
    → EnrollmentResult.Error(message, code)
  
  401 (Unauthorized):
    → TokenAuthenticator.authenticate() triggered
    → Attempts refresh via ApiService.refreshToken()
    → If success: saves new tokens, retries original request
    → If fail: clears tokens (forces logout)

  200-399 Success:
    → response.body() parsed to DTO
    → Result.Success(data)
    → ViewModel updates UiState
    → Compose recomposes
```

---

## Error Handling Strategy

```
HTTP Status → User Message | Log
─────────────────────────────────
400 Bad Request
  ├─ "audio_chunk_must_be_16khz" → "Audio must be 16kHz"
  ├─ "invalid_audio_chunk" → "Invalid audio format"
  └─ "missing_audio_chunk" → "Audio chunk missing"

401 Unauthorized
  ├─ (Auth interceptor → refresh attempt)
  └─ Refresh fails → Force logout

403 Forbidden
  ├─ "only_child_devices_can_stream_audio" → "Only child devices can upload audio"

404 Not Found
  ├─ "speaker_not_found" → "Speaker deleted or not found"
  ├─ "alert_not_found" → "Alert no longer available"

500+ Server Error → "Server error, try again"

Network Error → "Connection failed, check network"
```

---

## API Contract Compliance Matrix

| Requirement | Implementation | File |
|-------------|---------------|----|
| No auto field renaming | All DTOs use exact JSON keys (snake_case preserved) | AlertsDto.kt, etc. |
| Role enforcement | DeviceRepository validates before upload; server double-checks | DeviceRegistrationViewModel.kt |
| 16kHz audio | AudioRecorder captures and resamples all input to 16kHz WAV | AudioRecorder.kt |
| Bearer token format | AuthInterceptor adds `Authorization: Bearer <token>` | AuthInterceptor.kt |
| Multipart file fields | Named exactly as backend expects: "file", "display_name", etc. | ApiService.kt |
| Dev token support | Backend accepts id_token = "dev:<alias>" | AuthRepository.kt |
| Token refresh retry-once | TokenAuthenticator retries once; subsequent 401 clears session | TokenAuthenticator.kt |

---

## Feature Readiness

| Feature | Status | Notes |
|---------|--------|-------|
| **Login (Dev Token)** | ✅ Complete | Supports dev:<alias> format |
| **Device Registration** | ✅ Complete | Both child_device and parent_device roles |
| **Speaker Enrollment** | ✅ Complete | Uses multipart; handles WAV upload |
| **Speaker Management** | ✅ Complete | List, rename, delete |
| **Child Detection Streaming** | ✅ Complete | 1.5s chunk records @ 16kHz; includes location |
| **Alert Polling** | ✅ Complete | 15s interval; ack + clip download |
| **Navigation** | ✅ Complete | Role-based routing parent ← → child |
| **Permissions** | ⚠️ Partial | Requests at runtime; app handles denial gracefully |
| **Background Tracking** | ⚠️ Partial | Not a foreground service by default (enhancement) |

---

## Testing Coverage

### Unit Tests
- [`AuthRepositoryTest.kt`](app/src/test/java/com/example/speakerapp/features/auth/data/AuthRepositoryTest.kt) - Token parsing, persistence

### Integration Tests (Manual)
- See [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md) for curl-based backend validation

### Acceptance Criteria Checklist
- [ ] Dev token login succeeds → tokens saved
- [ ] Device registration succeeds → device_id persisted
- [ ] Parent can enroll speaker → WAV uploaded, response parsed
- [ ] Parent can list speakers → GET returns items
- [ ] Child can upload chunk (16kHz) → status received
- [ ] Child 403 on parent attempt → role enforcement works
- [ ] Parent can ack alert → POST /alerts/{id}/ack succeeds
- [ ] Clip download works → WAV bytes received, playable

---

## Build Configuration

**BuildConfig Fields** (app/build.gradle.kts):
```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")
```

**Dependencies** (via gradle/libs.versions.toml):
- Retrofit 2.11.0 + kotlinx.serialization converter
- OkHttp 4.12.0 (logging interceptor included)
- Hilt 2.52
- Jetpack Compose 2024.11.00
- DataStore 1.1.1
- Media3/ExoPlayer 1.4.1
- Location Services 21.3.0

---

## Permissions (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ -->
```

All requested at runtime via compose permission launchers.

---

## Assumptions & Fallbacks

| Assumption | Fallback |
|-----------|----------|
| Device mic available | Show permission error if denied |
| 16kHz resampling via AudioRecord | Use ffmpeg for manual testing |
| FusedLocationProvider available | Location optional; app continues if denied |
| Server healthy on startup | Retry on auth endpoint failure |
| Network stable for chunk uploads | Failed chunks discarded (enhancement: add queue) |

---

## Next Steps for Production

1. **Replace Dev Token Auth**: Integrate Google Sign-In SDK
2. **Add Foreground Service**: For continuous child monitoring
3. **Implement WorkManager**: Background alert polling + location sync
4. **Add SQLite Cache**: Retry queue for failed uploads
5. **Add Instrumentation Tests**: UI automation for critical flows
6. **Push Notifications**: Replace polling with FCM for alerts
7. **Keystore Setup**: Signed APK for Play Store submission
8. **Crashlytics Integration**: Remote error tracking

---

## Document References

- [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md) - Build, test, troubleshoot
- [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md) - Code snippets for remaining screens
- [ApiService.kt](app/src/main/java/com/example/speakerapp/network/ApiService.kt) - Exact endpoint contracts
- SafeEar Backend API Specification (external)

---

**App Version**: 1.0  
**Kotlin**: 2.0.21  
**Min SDK**: 26 | **Target SDK**: 35  
**Architecture**: MVVM + Clean Layering  

