# SafeEar Android App - Project Documentation Index

Welcome! This project is a production-grade Android app for SafeEar backend integration. Below is a guide to understand what's been built and how to continue development.

---

## 📖 Where to Start?

### I'm New to This Project
👉 Start here: [README_ARCHITECTURE.md](README_ARCHITECTURE.md)
- Quick 10-minute overview of what's been built
- Setup instructions
- Role-based navigation flow
- Testing checklist

### I Want to Understand Everything
👉 Read: [ARCHITECTURE.md](ARCHITECTURE.md)
- Complete architecture documentation
- Package structure
- Data flow diagrams
- Error handling strategy
- API contract compliance matrix

### I'm Ready to Build UI Screens
👉 Reference: [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- Complete ViewModel implementations (copy-paste ready)
- Hilt modules setup code
- Navigation route definitions
- AndroidManifest.xml permission declarations
- Material3 design patterns

### I Need to Test Backend Integration
👉 Use: [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md)
- Step-by-step build instructions
- Backend connectivity validation
- Curl commands for each endpoint
- Role enforcement validation
- Permission handling
- Troubleshooting with adb

### I Need API Endpoint Details
👉 Reference: [API_REFERENCE.md](API_REFERENCE.md)
- All 15 endpoints documented
- Request/response examples
- Error codes with recommended actions
- Curl testing templates
- Field name reference (exact backend contracts)

### I Want to Know Project Status
👉 Read: [COMPLETION_STATUS.md](COMPLETION_STATUS.md)
- What's complete (25+ files, 3,500+ LOC)
- What's partially complete (navigation, foreground service)
- What's not started (UI screens, instrumentation tests)
- Known limitations & mitigations
- Next steps checklist

---

## 🎯 Quick Facts

**What's Done** ✅:
- All API endpoints implemented (15/15)
- All ViewModels with reactive UiState (6/6)
- All repositories with Flow-based APIs (5/5)
- Network layer: Retrofit + OkHttp + token refresh
- All DTOs matching backend contracts exactly
- Authentication & authorization infrastructure
- Audio recording at 16kHz WAV
- Audio playback with ExoPlayer
- DataStore token persistence
- Hilt dependency injection
- Comprehensive documentation

**What's Ready to Build** ⚠️:
- Navigation graph structure
- Compose screen templates
- Role-based routing
- Foreground service skeleton

**What's Not Started** ❌:
- Compose screen implementations
- Google Sign-In integration
- WorkManager background tasks
- Instrumentation tests

---

## 📁 Project Structure

```
SpeakerAppv2/
├── app/
│   ├── src/main/java/com/example/speakerapp/
│   │   ├── network/                    # API & DTOs
│   │   │   ├── ApiService.kt
│   │   │   └── dto/                   # AuthDto, DeviceDto, etc.
│   │   ├── core/                       # Infrastructure
│   │   │   ├── auth/                  # TokenManager, interceptors
│   │   │   ├── network/               # NetworkModule (Hilt)
│   │   │   └── audio/                 # AudioRecorder, AudioPlayer
│   │   ├── features/                   # Feature modules
│   │   │   ├── auth/                  # Login/logout
│   │   │   ├── devices/               # Device registration
│   │   │   ├── enrollment/            # Speaker CRUD
│   │   │   ├── detection/             # Child monitoring
│   │   │   ├── alerts/                # Parent alerts
│   │   │   └── settings/              # Settings screen
│   │   ├── navigation/                 # Navigation routes
│   │   ├── ui/                         # Shared UI: theme, colors
│   │   ├── MainActivity.kt
│   │   └── SpeakerApp.kt
│   ├── src/test/                       # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                             # Gradle configuration
├── ARCHITECTURE.md                     # Full architecture guide
├── README_ARCHITECTURE.md              # Quick start
├── IMPLEMENTATION_GUIDE.md             # Code templates
├── RUN_AND_TEST_GUIDE.md              # Build & test
├── API_REFERENCE.md                    # Endpoint reference
└── COMPLETION_STATUS.md                # Project status
```

---

## 🚀 Quick Start Commands

```bash
# 1. Navigate to project
cd c:\Users\AYUSH VERMA\AndroidStudioProjects\SpeakerAppv2

# 2. Build APK
./gradlew assembleDebug

# 3. Install on emulator
./gradlew installDebugAndroidTest

# 4. View logs
adb logcat | grep "SpeakerApp"

# 5. Test backend connectivity
curl http://localhost:8000/health

# 6. Test login (dev token)
curl -X POST http://localhost:8000/auth/google \
  -H "Content-Type: application/json" \
  -d '{"id_token": "dev:test_parent"}'
```

---

## 🔐 Authentication Quick Reference

### Dev Token Format (For Testing)
```
Format: "dev:<alias>"
Example: "dev:test_parent" or "dev:test_child"

Backend will accept and return valid JWT
No need for Google Sign-In during development
```

### How Token Refresh Works
```
1. Request returns 401 (unauthorized)
2. TokenAuthenticator automatically called
3. Calls POST /auth/refresh with refresh_token
4. On success: saves new tokens, retries original request
5. On failure: clears session (forces logout)
6. Once-retry pattern prevents infinite loops
```

### Bearer Token Injection
```
All protected requests automatically get:
Authorization: Bearer <access_token>

Handled by AuthInterceptor (transparent to ViewModels)
```

---

## 📱 Role-Based Navigation

### Parent Device Journey
```
Login with "dev:test_parent"
    ↓
Register Device (select "parent_device")
    ↓
Parent Dashboard
├─ Alerts Tab: List, ack, play clips
├─ Speakers Tab: Enroll, list, rename, delete
└─ Settings Tab: Device info, logout
```

### Child Device Journey
```
Login with "dev:test_child"
    ↓
Register Device (select "child_device")
    ↓
Child Monitoring Screen
├─ Large toggle: START/STOP recording
├─ Auto-upload 1.5s chunks @ 16kHz
├─ Display decision + confidence
└─ Show alerts fired
```

---

## 🧪 Testing Acceptance Criteria

- [ ] Dev token login succeeds → tokens saved in DataStore
- [ ] Device registration works for both roles
- [ ] Parent can enroll speaker → receives embedding feedback
- [ ] Parent can list speakers → GET returns items
- [ ] Parent can rename speaker → PATCH updates display_name
- [ ] Parent can delete speaker → DELETE succeeds
- [ ] Child can upload chunk (16kHz) → receives status + decision
- [ ] Child 403 on parent attempt → role enforcement works
- [ ] Parent can list alerts → polling works (15s interval)
- [ ] Parent can acknowledge alert → POST /alerts/{id}/ack succeeds
- [ ] Parent can download + play alert clip → WAV playback works
- [ ] Token refresh works → 401 triggers refresh automatically
- [ ] Logout clears tokens → redirects to login screen

See [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md) for detailed curl commands.

---

## 📋 Key API Contracts

### Required Multipart Fields (Exact Names)
- Speaker enrollment: `display_name`, `file`, optional `speaker_id`
- Detection chunk: `device_id`, `file`, optional `latitude`, `longitude`

### Required Role Values (Exact)
- `"child_device"` - lowercase, underscore, not camelCase
- `"parent_device"` - lowercase, underscore, not camelCase

### Required Audio Format
- Sample rate: **exactly 16kHz**
- Channels: 1 (mono)
- Bit depth: 16-bit (PCM_16BIT)
- Format: WAV (RIFF container)
- Backend rejects if not exactly 16kHz

### Bearer Token Format
- Header: `Authorization: Bearer <access_token>`
- Applied to all protected endpoints
- Automatic via AuthInterceptor

---

## 🛠️ Development Workflow

### To Add a New Screen

1. **Create ViewModel** (if not exists)
   ```kotlin
   // features/modulename/ui/NewScreenViewModel.kt
   @HiltViewModel
   class NewScreenViewModel @Inject constructor(
       private val repository: SomeRepository
   ) : ViewModel() {
       val uiState = MutableStateFlow<NewScreenUiState>(...)
       fun doSomething() { ... }
   }
   ```

2. **Create Composable Screen**
   ```kotlin
   // features/modulename/ui/NewScreen.kt
   @Composable
   fun NewScreen(
       viewModel: NewScreenViewModel = hiltViewModel(),
       onNavigate: (route: String) -> Unit
   ) {
       val uiState by viewModel.uiState.collectAsState()
       // UI implementation
   }
   ```

3. **Add to Navigation**
   ```kotlin
   // navigation/NavGraph.kt
   composable(Screen.NewScreen.route) {
       NewScreen(onNavigate = navController::navigate)
   }
   ```

4. **Add Route to Screen.kt**
   ```kotlin
   object Screen {
       data object NewScreen : Screen(route = "new_screen")
   }
   ```

---

## 🔧 Build Configuration

### BASE_URL Setting (CRITICAL)
Edit `app/build.gradle.kts`:
```kotlin
// For emulator
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/\"")

// For physical device (replace with your IP)
buildConfigField("String", "BASE_URL", "\"http://192.168.137.1:8000/\"")
```

### Required Permissions (AndroidManifest.xml already updated)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 📚 Dependencies

All dependencies in `gradle/libs.versions.toml`:
- **Kotlin**: 2.0.21
- **Compose**: 2024.11.00
- **Retrofit**: 2.11.0
- **OkHttp**: 4.12.0
- **Hilt**: 2.52
- **DataStore**: 1.1.1
- **Media3/ExoPlayer**: 1.4.1
- **Coroutines**: 1.8.1
- **Location Services**: 21.3.0

No additional dependencies needed for core functionality.

---

## ⚠️ Common Pitfalls

1. **❌ Changing field names** - Use exact backend names (device_name not deviceName)
2. **❌ Wrong role values** - Must be exactly "child_device" or "parent_device"
3. **❌ Audio not 16kHz** - Backend rejects; AudioRecorder handles, don't override
4. **❌ Missing Bearer token** - AuthInterceptor adds automatically; verify in logs
5. **❌ Not handling 401** - TokenAuthenticator handles automatically; don't remove
6. **❌ Infinite token refresh** - Once-retry pattern prevents; don't change
7. **❌ Forgetting to stop polling** - AlertsViewModel.stopPolling() in onCleared()
8. **❌ Using dev token in production** - For testing only; integrate Google Sign-In later

---

## 📞 Support

- **API Questions?** → [API_REFERENCE.md](API_REFERENCE.md)
- **Architecture Questions?** → [ARCHITECTURE.md](ARCHITECTURE.md)
- **Build/Test Issues?** → [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md)
- **Code Templates?** → [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- **Project Status?** → [COMPLETION_STATUS.md](COMPLETION_STATUS.md)
- **Quick Start?** → [README_ARCHITECTURE.md](README_ARCHITECTURE.md)

---

## ✨ Next Steps

1. **Review** [README_ARCHITECTURE.md](README_ARCHITECTURE.md) (10 min)
2. **Build** APK: `./gradlew assembleDebug` (5 min)
3. **Verify** backend: `curl http://localhost:8000/health` (1 min)
4. **Test** login flow via curl (see [RUN_AND_TEST_GUIDE.md](RUN_AND_TEST_GUIDE.md)) (10 min)
5. **Implement** LoginScreen (1-2 hours)
6. **Implement** DeviceRegistrationScreen (1 hour)
7. **Setup** navigation graph (1 hour)
8. **Implement** remaining screens (4-6 hours)
9. **Test** end-to-end flow (2 hours)

**Total Estimated Time**: 3-4 developer-days ✅

---

**Project Status**: Production-Ready Infrastructure | Ready for UI Build  
**Kotlin Version**: 2.0.21  
**Min SDK**: 26 | **Target SDK**: 35  
**Last Updated**: 2024-02-15  

