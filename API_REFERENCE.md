# SafeEar API Endpoints Reference

Complete endpoint specifications matching backend contract exactly. Use this for verification before UI implementation.

---

## Authentication Endpoints

### 1. Google Sign-In / Dev Token Login
```
POST /auth/google
Authorization: None (public)
Content-Type: application/json

Request:
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjEyMyJ9..." 
    OR 
  "id_token": "dev:test_parent"  (for dev testing)
}

Response (200):
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "refresh_token_xyz...",
  "parent_id": "parent_123456"
}

Response (400):
{
  "detail": "Invalid token format"
}

Notes:
- Dev token format: "dev:<alias>" accepted by backend
- No period in dev tokens (valid for testing)
- access_token valid for ~1 hour
- Must store both tokens in DataStore for future requests
```

### 2. Refresh Token
```
POST /auth/refresh
Authorization: Bearer <refresh_token>
Content-Type: application/json

Request: (empty body)

Response (200):
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "refresh_token_new..."
}

Response (401):
{
  "detail": "Invalid refresh token"
}

Notes:
- Called by TokenAuthenticator when main token returns 401
- Returns new access_token + refresh_token
- Must update both in DataStore
- If 401, clear session (force logout)
```

### 3. Logout
```
POST /auth/logout
Authorization: Bearer <access_token>
Content-Type: application/json

Request:
{
  "refresh_token": "refresh_token_xyz..."
}

Response (200):
{
  "message": "Logged out successfully"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Clear both tokens + device info from DataStore
- User returned to login screen
- No token refresh attempted on logout
```

---

## Device Registration

### 4. Register Device
```
POST /devices
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

Fields:
  device_name: string (required)       "My iPhone"
  role: string (required)              "child_device" or "parent_device"
  device_token?: string (optional)     "FCM token" (for push notifications)

Response (200):
{
  "device_id": "dev_7f8a9b1c2d3e",
  "device_name": "My iPhone",
  "role": "child_device",
  "parent_id": "parent_123456"
}

Response (400):
{
  "detail": "Invalid role. Must be 'child_device' or 'parent_device'"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- EXACT role values: "child_device" or "parent_device" (case-sensitive)
- Store device_id + role in DataStore for later API calls
- Role determines UI navigation and access to certain endpoints
- Call once per device (platform-specific setup)
```

---

## Speaker Enrollment (Parent Only)

### 5. Enroll Speaker
```
POST /enroll/speaker
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

Fields:
  display_name: string (required)      "Grandma"
  file: binary (required)              16kHz WAV audio file
  speaker_id?: string (optional)       "sp_existing_id" (for re-enrollment)

Response (200):
{
  "speaker_id": "sp_1f2a3b4c5d6e",
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

Response (400):
{
  "detail": "audio_chunk_must_be_16khz"
    OR "missing_audio_chunk"
    OR "invalid_audio_chunk"
}

Response (403):
{
  "detail": "only_parent_devices_can_enroll_speakers"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Audio file MUST be 16kHz mono WAV (AudioRecorder ensures this)
- Responses include VAD (voice activity) and speech quality feedback
- speaker_id not needed for first enrollment (backend generates)
- Re-enrollment updates existing speaker (add more samples)
- No field renaming: display_name not displayName
```

### 6. List Speakers
```
GET /enroll/speakers
Authorization: Bearer <access_token>
Content-Type: application/json

Query Parameters: (none)

Response (200):
{
  "items": [
    {
      "id": "sp_1f2a3b4c5d6e",
      "display_name": "Grandma",
      "sample_count": 12,
      "created_at": "2024-02-15T10:30:45Z",
      "updated_at": "2024-02-15T10:35:22Z"
    },
    {
      "id": "sp_2g3b4c5d6e7f",
      "display_name": "Mom",
      "sample_count": 8,
      "created_at": "2024-02-14T09:15:30Z",
      "updated_at": "2024-02-14T09:18:10Z"
    }
  ],
  "total": 2
}

Response (403):
{
  "detail": "only_parent_devices_can_list_speakers"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Returns all speakers enrolled by this parent device
- Use id for update/delete operations
- sample_count shows how many audio samples stored for speaker
```

### 7. Update Speaker (Rename)
```
PATCH /enroll/speakers/{speaker_id}
Authorization: Bearer <access_token>
Content-Type: application/json

Path Parameter:
  speaker_id: string  "sp_1f2a3b4c5d6e"

Request:
{
  "display_name": "Grandmother"
}

Response (200):
{
  "id": "sp_1f2a3b4c5d6e",
  "display_name": "Grandmother",
  "sample_count": 12,
  "created_at": "2024-02-15T10:30:45Z",
  "updated_at": "2024-02-15T10:40:00Z"
}

Response (404):
{
  "detail": "speaker_not_found"
}

Response (403):
{
  "detail": "only_parent_devices_can_update_speakers"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Only updates display_name (not samples/embedding)
- Returns updated speaker with new updated_at timestamp
```

### 8. Delete Speaker
```
DELETE /enroll/speakers/{speaker_id}
Authorization: Bearer <access_token>

Path Parameter:
  speaker_id: string  "sp_1f2a3b4c5d6e"

Response (204):
(empty body)

Response (404):
{
  "detail": "speaker_not_found"
}

Response (403):
{
  "detail": "only_parent_devices_can_delete_speakers"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Returns 204 on success (empty body)
- Deletes all samples + embeddings for speaker
- Role checked: parent_device only
```

---

## Detection (Child Only)

### 9. Upload Detection Chunk
```
POST /detect/chunk
Authorization: Bearer <access_token>
Content-Type: multipart/form-data

Fields:
  device_id: string (required)         "dev_7f8a9b1c2d3e"
  file: binary (required)              16kHz mono WAV audio
  latitude?: float (optional)          37.7749
  longitude?: float (optional)         -122.4194

Response (200):
{
  "status": "warming_up" | "no_hop" | "ok",
  "decision": "familiar" | "stranger_candidate" | "uncertain" | "hold" | null,
  "score": 0.95,
  "stranger_streak": 0,
  "thresholds": {
    "t_high": 0.8,
    "t_low": 0.5
  },
  "alert_fired": false,
  "alert_id": null
}

Response (400):
{
  "detail": "audio_chunk_must_be_16khz"
    OR "invalid_audio_chunk"
    OR "missing_device_id"
}

Response (403):
{
  "detail": "only_child_devices_can_stream_audio"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- CRITICAL: Audio must be 16kHz (backend 400 if not)
- device_id must match registered device
- status values: "warming_up" (initial), "no_hop" (no speakers enrolled), "ok" (ready)
- decision: null if status != "ok"; populated when ready
- thresholds are confidence thresholds for decision boundary
- alert_fired true + alert_id populated if stranger_candidate + high confidence
- latitude/longitude optional but recommended for parent tracking
- Call continuously (1.5s chunks, 1s delay) for continuous monitoring
```

### 10. Update Detection Location
```
POST /detect/location
Authorization: Bearer <access_token>
Content-Type: application/json

Request:
{
  "device_id": "dev_7f8a9b1c2d3e",
  "latitude": 37.7749,
  "longitude": -122.4194
}

Response (200):
{
  "message": "Location updated successfully"
}

Response (400):
{
  "detail": "invalid_location" | "missing_device_id"
}

Response (403):
{
  "detail": "only_child_devices_can_update_location"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Called periodically or on meaningful movement
- Updates location for next alert (if fired)
- device_id required in both path + body for verification
```

### 11. End Detection Session
```
DELETE /detect/session
Authorization: Bearer <access_token>

Query Parameter:
  device_id: string  "dev_7f8a9b1c2d3e"

Response (200):
{
  "message": "Session ended"
}

Response (400):
{
  "detail": "missing_device_id"
}

Response (403):
{
  "detail": "only_child_devices_can_end_session"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Called when child stops monitoring
- Cleanup: clears state on server for this device
- Role checked: child_device only
```

---

## Alerts (Parent Only)

### 12. List Alerts
```
GET /alerts
Authorization: Bearer <access_token>

Query Parameters:
  limit?: integer (default: 50)        50
  offset?: integer (default: 0)        0

Response (200):
{
  "items": [
    {
      "id": "alert_1a2b3c4d5e6f",
      "timestamp": "2024-02-15T10:30:45Z",
      "confidence_score": 0.92,
      "audio_clip_path": "/clips/alert_1a2b3c4d5e6f.wav",
      "latitude": 37.7749,
      "longitude": -122.4194,
      "acknowledged_at": null
    },
    {
      "id": "alert_2b3c4d5e6f7g",
      "timestamp": "2024-02-15T10:20:15Z",
      "confidence_score": 0.75,
      "audio_clip_path": "/clips/alert_2b3c4d5e6f7g.wav",
      "latitude": 37.7760,
      "longitude": -122.4200,
      "acknowledged_at": "2024-02-15T10:22:00Z"
    }
  ],
  "total": 245,
  "offset": 0,
  "limit": 50
}

Response (403):
{
  "detail": "only_parent_devices_can_list_alerts"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Paginated response (limit + offset)
- acknowledged_at null = unacknowledged; populated = ack time
- Sorted by timestamp descending (newest first)
- Use for polling mechanism (every 15 seconds)
```

### 13. Acknowledge Alert
```
POST /alerts/{alert_id}/ack
Authorization: Bearer <access_token>
Content-Type: application/json

Path Parameter:
  alert_id: string  "alert_1a2b3c4d5e6f"

Request: (empty body)

Response (200):
{
  "id": "alert_1a2b3c4d5e6f",
  "acknowledged_at": "2024-02-15T10:35:22Z"
}

Response (404):
{
  "detail": "alert_not_found"
}

Response (403):
{
  "detail": "only_parent_devices_can_acknowledge_alerts"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Updates alert to mark as acknowledged
- Returns acknowledged_at timestamp
- Used by parent to dismiss notification
```

### 14. Download Alert Audio Clip
```
GET /alerts/{alert_id}/clip
Authorization: Bearer <access_token>

Path Parameter:
  alert_id: string  "alert_1a2b3c4d5e6f"

Response (200):
audio/wav binary data (1KB - 100KB depending on duration)

Response (404):
{
  "detail": "alert_not_found" | "clip_not_found"
}

Response (403):
{
  "detail": "only_parent_devices_can_download_clips"
}

Response (401):
{
  "detail": "Unauthorized"
}

Notes:
- Returns WAV file (audio/wav content-type)
- Save to temp cache file, pass to AudioPlayer.playFromBytes()
- Content-Length header shows file size
- Retry on error (network timeout possible for large files)
```

---

## Health Check

### 15. Health Check
```
GET /health
Authorization: None (public)

Response (200):
{
  "status": "ok"
}

Response (503):
{
  "status": "unhealthy"
}

Notes:
- Use to verify backend connectivity before attempting login
- No auth required
- Should respond in <1 second
```

---

## Error Code Quick Reference

| Code | Error Detail | Meaning | Action |
|------|--------------|---------|--------|
| 400 | audio_chunk_must_be_16khz | Audio not 16kHz | Re-record with AudioRecorder |
| 400 | invalid_audio_chunk | Corrupt audio data | Check file format |
| 400 | missing_audio_chunk | No file uploaded | Ensure file attached |
| 400 | missing_device_id | device_id param missing | Check query/body params |
| 400 | invalid_role | Role not child/parent | Use exact values |
| 401 | Unauthorized / Invalid token | Token expired/invalid | Refresh token or logout |
| 403 | only_child_devices_can_stream_audio | Parent on /detect/chunk | Check device role |
| 403 | only_parent_devices_can_enroll_speakers | Child on enroll | Check device role |
| 404 | speaker_not_found | Speaker deleted | Reload list |
| 404 | alert_not_found | Alert deleted/expired | Reload list |
| 500 | Server error | Backend crash | Retry, contact support |

---

## Curl Testing Commands

### Test Auth Flow
```bash
# 1. Login
curl -X POST http://localhost:8000/auth/google \
  -H "Content-Type: application/json" \
  -d '{"id_token": "dev:test_parent"}'

# Save access_token from response

TOKEN="eyJhbGciOiJI..." # copy from response above

# 2. Register device
curl -X POST http://localhost:8000/devices \
  -H "Authorization: Bearer $TOKEN" \
  -F "device_name=TestPhone" \
  -F "role=parent_device"

# 3. List alerts (parent)
curl -X GET "http://localhost:8000/alerts?limit=10&offset=0" \
  -H "Authorization: Bearer $TOKEN"
```

### Test Child Device Flow
```bash
# 1. Login as child
curl -X POST http://localhost:8000/auth/google \
  -H "Content-Type: application/json" \
  -d '{"id_token": "dev:test_child"}'

# Save access_token

TOKEN="eyJhbGciOiJI..." 

# 2. Register as child
curl -X POST http://localhost:8000/devices \
  -H "Authorization: Bearer $TOKEN" \
  -F "device_name=ChildPhone" \
  -F "role=child_device"

# Save device_id

DEVICE_ID="dev_xyz"

# 3. Upload chunk (create test.wav first)
ffmpeg -f lavfi -i "sine=frequency=440:duration=1.5" -acodec pcm_s16le \
  -ar 16000 -ac 1 test.wav

curl -X POST http://localhost:8000/detect/chunk \
  -H "Authorization: Bearer $TOKEN" \
  -F "device_id=$DEVICE_ID" \
  -F "file=@test.wav" \
  -F "latitude=37.7749" \
  -F "longitude=-122.4194"
```

---

**API Version**: 1.0  
**Last Updated**: 2024-02-15  
**Backend**: SafeEar  

