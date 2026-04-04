package com.example.speakerapp.core.realtime

import com.example.speakerapp.BuildConfig
import com.example.speakerapp.core.auth.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class RealtimeSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var shouldRun = false

    @Volatile
    private var refCount = 0

    @Volatile
    private var reconnectAttempt = 0

    fun acquire() {
        synchronized(this) {
            refCount += 1
            shouldRun = true
            if (refCount == 1) {
                connectInternal()
            }
        }
    }

    fun release() {
        synchronized(this) {
            refCount = (refCount - 1).coerceAtLeast(0)
            if (refCount == 0) {
                shouldRun = false
                reconnectAttempt = 0
                webSocket?.close(1000, "No active subscribers")
                webSocket = null
                _isConnected.value = false
            }
        }
    }

    private fun connectInternal() {
        scope.launch {
            val token = tokenManager.getAccessToken()
            if (token.isNullOrBlank()) {
                _isConnected.value = false
                return@launch
            }

            val request = Request.Builder()
                .url(buildWebSocketUrl())
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempt = 0
                    _isConnected.value = true
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parseAndEmit(text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _isConnected.value = false
                    scheduleReconnectIfNeeded()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    _isConnected.value = false
                    scheduleReconnectIfNeeded()
                }
            })
        }
    }

    private fun scheduleReconnectIfNeeded() {
        if (!shouldRun) return
        scope.launch {
            reconnectAttempt += 1
            val backoffMs = min(30_000L, 1_000L * (1 shl reconnectAttempt.coerceAtMost(5)))
            delay(backoffMs)
            if (shouldRun) {
                connectInternal()
            }
        }
    }

    private fun parseAndEmit(message: String) {
        runCatching {
            val root = json.parseToJsonElement(message).jsonObject
            val type = root["type"]?.jsonPrimitive?.content
                ?: root["event"]?.jsonPrimitive?.content
                ?: "unknown"

            val payload = root["payload"]?.jsonObject ?: root
            _events.tryEmit(RealtimeEvent(type = type, payload = payload))
        }
    }

    private fun buildWebSocketUrl(): String {
        val base = BuildConfig.BASE_URL
            .removeSuffix("/")
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://")

        return "$base/ws/events"
    }
}
