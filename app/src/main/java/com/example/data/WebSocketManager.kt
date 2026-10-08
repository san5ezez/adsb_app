package com.example.data

import android.util.Log
import com.example.model.Aircraft
import com.example.model.ConnectionStatus
import com.example.model.RawMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class WebSocketManager {
    companion object {
        private const val TAG = "PlanesWebSocket"
        private const val HANDSHAKE_MESSAGE = "SERVER DE CLIENT client=openwebrx.js type=map"
        private const val MAX_RAW_MESSAGES = 20
        private const val RECONNECT_INTERVAL_MS = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var currentWebSocket: WebSocket? = null
    private var connectionLoopJob: Job? = null
    private var currentServerAddress: String = ""
    private var isIntentionalDisconnect = false

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _rawMessages = MutableStateFlow<List<RawMessage>>(emptyList())
    val rawMessages: StateFlow<List<RawMessage>> = _rawMessages.asStateFlow()

    private val _aircraftUpdates = MutableSharedFlow<List<Aircraft>>(extraBufferCapacity = 64)
    val aircraftUpdates: SharedFlow<List<Aircraft>> = _aircraftUpdates.asSharedFlow()

    private val messageCounter = AtomicLong(0)

    fun start(serverAddress: String) {
        if (currentServerAddress == serverAddress && connectionLoopJob?.isActive == true) {
            return
        }
        currentServerAddress = serverAddress
        isIntentionalDisconnect = false
        restartConnectionLoop()
    }

    fun reconnect() {
        isIntentionalDisconnect = false
        restartConnectionLoop()
    }

    fun stop() {
        isIntentionalDisconnect = true
        connectionLoopJob?.cancel()
        currentWebSocket?.close(1000, "Client stopped")
        currentWebSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    fun clearRawMessages() {
        _rawMessages.value = emptyList()
    }

    private fun restartConnectionLoop() {
        connectionLoopJob?.cancel()
        currentWebSocket?.cancel()
        currentWebSocket = null

        connectionLoopJob = scope.launch {
            while (isActive && !isIntentionalDisconnect) {
                if (currentServerAddress.isBlank()) {
                    delay(1000)
                    continue
                }

                _connectionStatus.value = ConnectionStatus.CONNECTING
                val url = formatWebSocketUrl(currentServerAddress)
                Log.d(TAG, "Connecting to WebSocket: $url")

                val connectedSignal = Job()

                val request = Request.Builder()
                    .url(url)
                    .build()

                currentWebSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket connected, sending handshake")
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        webSocket.send(HANDSHAKE_MESSAGE)
                        recordRawMessage("[SYSTEM] Подключено к $url", "system")
                        recordRawMessage("-> $HANDSHAKE_MESSAGE", "sent")
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleIncomingText(text)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closing: $code / $reason")
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        recordRawMessage("[SYSTEM] Закрытие соединения: $code $reason", "system")
                        webSocket.close(1000, null)
                        if (!connectedSignal.isCompleted) connectedSignal.complete()
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closed: $code / $reason")
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        if (!connectedSignal.isCompleted) connectedSignal.complete()
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "WebSocket failure: ${t.message}", t)
                        _connectionStatus.value = ConnectionStatus.DISCONNECTED
                        val errorDesc = t.localizedMessage ?: "Ошибка подключения"
                        recordRawMessage("[ERROR] $errorDesc", "error")
                        if (!connectedSignal.isCompleted) connectedSignal.complete()
                    }
                })

                // Wait until disconnected or failed
                connectedSignal.join()

                if (!isActive || isIntentionalDisconnect) break

                Log.d(TAG, "Waiting ${RECONNECT_INTERVAL_MS}ms before reconnecting...")
                delay(RECONNECT_INTERVAL_MS)
            }
        }
    }

    private fun handleIncomingText(text: String) {
        val parsed = OpenWebRxParser.parse(text)
        recordRawMessage(text, parsed.type)

        if (parsed.aircraftList.isNotEmpty()) {
            _aircraftUpdates.tryEmit(parsed.aircraftList)
        }
    }

    private fun recordRawMessage(text: String, type: String?) {
        val msg = RawMessage(
            id = messageCounter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            text = text,
            type = type
        )
        val currentList = _rawMessages.value
        val updated = (listOf(msg) + currentList).take(MAX_RAW_MESSAGES)
        _rawMessages.value = updated
    }

    private fun formatWebSocketUrl(server: String): String {
        var clean = server.trim()
        if (clean.startsWith("ws://") || clean.startsWith("wss://")) {
            if (!clean.endsWith("/ws/")) {
                clean = clean.trimEnd('/') + "/ws/"
            }
            return clean
        }
        clean = clean.removePrefix("http://").removePrefix("https://").trimEnd('/')
        return "ws://$clean/ws/"
    }
}
