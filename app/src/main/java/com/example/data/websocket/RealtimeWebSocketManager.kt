package com.example.data.websocket

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

sealed class WebSocketRealtimeEvent {
    data class ChatMessage(
        val messageId: Long,
        val channelId: String,
        val senderName: String,
        val senderRole: String,
        val messageText: String,
        val timestampText: String,
        val attachmentFileName: String? = null,
        val attachmentFileSize: String? = null,
        val audioPath: String? = null,
        val audioDurationSeconds: Int = 0,
        val isVoiceMessage: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketRealtimeEvent()

    data class AdminNotification(
        val broadcastId: String,
        val title: String,
        val message: String,
        val audience: String,
        val topic: String,
        val priority: String,
        val category: String,
        val actionRoute: String,
        val senderName: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketRealtimeEvent()

    data class EmojiReaction(
        val messageId: Long,
        val emoji: String,
        val userName: String,
        val userRole: String,
        val channelId: String,
        val isAdded: Boolean,
        val timestamp: Long
    ) : WebSocketRealtimeEvent()

    data class DynamicFeedUpdate(
        val feedId: Long,
        val authorName: String,
        val authorRole: String,
        val title: String,
        val content: String,
        val category: String,
        val timestampText: String
    ) : WebSocketRealtimeEvent()

    data class DynamicFeedLike(
        val feedId: Long,
        val userName: String,
        val newLikesCount: Int
    ) : WebSocketRealtimeEvent()

    data class GenericBroadcast(
        val eventType: String,
        val payload: String
    ) : WebSocketRealtimeEvent()
}

/**
 * RealtimeWebSocketManager manages persistent full-duplex WebSocket connections
 * for immediate broadcast of chat emoji reactions, user-generated dynamic feeds,
 * and live active presence across all active users.
 */
object RealtimeWebSocketManager {
    private const val TAG = "RealtimeWebSocket"
    private const val DEFAULT_WS_URL = "wss://ws.postman-echo.com/raw"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var reconnectJob: Job? = null

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    private val _events = MutableSharedFlow<WebSocketRealtimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WebSocketRealtimeEvent> = _events.asSharedFlow()

    fun connect(wsUrl: String = DEFAULT_WS_URL) {
        if (_connectionState.value == WebSocketConnectionState.CONNECTED ||
            _connectionState.value == WebSocketConnectionState.CONNECTING
        ) {
            return
        }

        _connectionState.value = WebSocketConnectionState.CONNECTING
        Log.d(TAG, "Initiating WebSocket connection to $wsUrl...")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to $wsUrl")
                _connectionState.value = WebSocketConnectionState.CONNECTED
                reconnectJob?.cancel()

                // Announce connection handshake
                val handshakeJson = JSONObject().apply {
                    put("type", "HANDSHAKE")
                    put("client", "MakingBrands-Android")
                    put("timestamp", System.currentTimeMillis())
                }
                webSocket.send(handshakeJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleIncomingMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket connection failure: ${t.message}. Will auto-reconnect.")
                _connectionState.value = WebSocketConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(5000)
            if (_connectionState.value != WebSocketConnectionState.CONNECTED) {
                Log.d(TAG, "Reconnecting WebSocket...")
                connect()
            }
        }
    }

    private fun handleIncomingMessage(payload: String) {
        try {
            val json = JSONObject(payload)
            val type = json.optString("type")
            when (type) {
                "CHAT_MESSAGE" -> {
                    val event = WebSocketRealtimeEvent.ChatMessage(
                        messageId = json.optLong("messageId"),
                        channelId = json.optString("channelId"),
                        senderName = json.optString("senderName"),
                        senderRole = json.optString("senderRole"),
                        messageText = json.optString("messageText"),
                        timestampText = json.optString("timestampText"),
                        attachmentFileName = if (json.has("attachmentFileName") && !json.isNull("attachmentFileName")) json.optString("attachmentFileName") else null,
                        attachmentFileSize = if (json.has("attachmentFileSize") && !json.isNull("attachmentFileSize")) json.optString("attachmentFileSize") else null,
                        audioPath = if (json.has("audioPath") && !json.isNull("audioPath")) json.optString("audioPath") else null,
                        audioDurationSeconds = json.optInt("audioDurationSeconds", 0),
                        isVoiceMessage = json.optBoolean("isVoiceMessage", false),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                    _events.tryEmit(event)
                }
                "ADMIN_BROADCAST" -> {
                    val event = WebSocketRealtimeEvent.AdminNotification(
                        broadcastId = json.optString("broadcastId"),
                        title = json.optString("title"),
                        message = json.optString("message"),
                        audience = json.optString("audience", "All Users"),
                        topic = json.optString("topic", "all_users"),
                        priority = json.optString("priority", "High"),
                        category = json.optString("category", "announcement"),
                        actionRoute = json.optString("actionRoute", "notifications"),
                        senderName = json.optString("senderName", "MB Admin"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                    _events.tryEmit(event)
                }
                "EMOJI_REACTION" -> {
                    val event = WebSocketRealtimeEvent.EmojiReaction(
                        messageId = json.optLong("messageId"),
                        emoji = json.optString("emoji"),
                        userName = json.optString("userName"),
                        userRole = json.optString("userRole"),
                        channelId = json.optString("channelId"),
                        isAdded = json.optBoolean("isAdded", true),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                    _events.tryEmit(event)
                }
                "DYNAMIC_FEED_UPDATE" -> {
                    val event = WebSocketRealtimeEvent.DynamicFeedUpdate(
                        feedId = json.optLong("feedId"),
                        authorName = json.optString("authorName"),
                        authorRole = json.optString("authorRole"),
                        title = json.optString("title"),
                        content = json.optString("content"),
                        category = json.optString("category"),
                        timestampText = json.optString("timestampText", "Just now")
                    )
                    _events.tryEmit(event)
                }
                "DYNAMIC_FEED_LIKE" -> {
                    val event = WebSocketRealtimeEvent.DynamicFeedLike(
                        feedId = json.optLong("feedId"),
                        userName = json.optString("userName"),
                        newLikesCount = json.optInt("newLikesCount")
                    )
                    _events.tryEmit(event)
                }
                else -> {
                    _events.tryEmit(WebSocketRealtimeEvent.GenericBroadcast(type, payload))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming WebSocket message: ${e.message}")
        }
    }

    fun broadcastChatMessage(
        messageId: Long,
        channelId: String,
        senderName: String,
        senderRole: String,
        messageText: String,
        timestampText: String,
        attachmentFileName: String? = null,
        attachmentFileSize: String? = null,
        audioPath: String? = null,
        audioDurationSeconds: Int = 0,
        isVoiceMessage: Boolean = false
    ) {
        val json = JSONObject().apply {
            put("type", "CHAT_MESSAGE")
            put("messageId", messageId)
            put("channelId", channelId)
            put("senderName", senderName)
            put("senderRole", senderRole)
            put("messageText", messageText)
            put("timestampText", timestampText)
            if (attachmentFileName != null) put("attachmentFileName", attachmentFileName)
            if (attachmentFileSize != null) put("attachmentFileSize", attachmentFileSize)
            if (audioPath != null) put("audioPath", audioPath)
            put("audioDurationSeconds", audioDurationSeconds)
            put("isVoiceMessage", isVoiceMessage)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(json.toString())
    }

    fun broadcastAdminNotification(
        broadcastId: String,
        title: String,
        message: String,
        audience: String,
        topic: String,
        priority: String,
        category: String,
        actionRoute: String,
        senderName: String
    ) {
        val json = JSONObject().apply {
            put("type", "ADMIN_BROADCAST")
            put("broadcastId", broadcastId)
            put("title", title)
            put("message", message)
            put("audience", audience)
            put("topic", topic)
            put("priority", priority)
            put("category", category)
            put("actionRoute", actionRoute)
            put("senderName", senderName)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(json.toString())
    }

    fun broadcastEmojiReaction(
        messageId: Long,
        emoji: String,
        userName: String,
        userRole: String,
        channelId: String,
        isAdded: Boolean
    ) {
        val json = JSONObject().apply {
            put("type", "EMOJI_REACTION")
            put("messageId", messageId)
            put("emoji", emoji)
            put("userName", userName)
            put("userRole", userRole)
            put("channelId", channelId)
            put("isAdded", isAdded)
            put("timestamp", System.currentTimeMillis())
        }
        val sent = webSocket?.send(json.toString()) ?: false
        Log.d(TAG, "Sent WebSocket emoji reaction: $sent ($emoji by $userName)")

        // Local loopback dispatch to ensure immediate UI responsiveness
        _events.tryEmit(
            WebSocketRealtimeEvent.EmojiReaction(
                messageId = messageId,
                emoji = emoji,
                userName = userName,
                userRole = userRole,
                channelId = channelId,
                isAdded = isAdded,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun broadcastFeedPost(
        feedId: Long,
        authorName: String,
        authorRole: String,
        title: String,
        content: String,
        category: String,
        timestampText: String
    ) {
        val json = JSONObject().apply {
            put("type", "DYNAMIC_FEED_UPDATE")
            put("feedId", feedId)
            put("authorName", authorName)
            put("authorRole", authorRole)
            put("title", title)
            put("content", content)
            put("category", category)
            put("timestampText", timestampText)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(json.toString())

        _events.tryEmit(
            WebSocketRealtimeEvent.DynamicFeedUpdate(
                feedId = feedId,
                authorName = authorName,
                authorRole = authorRole,
                title = title,
                content = content,
                category = category,
                timestampText = timestampText
            )
        )
    }

    fun broadcastFeedLike(feedId: Long, userName: String, newLikesCount: Int) {
        val json = JSONObject().apply {
            put("type", "DYNAMIC_FEED_LIKE")
            put("feedId", feedId)
            put("userName", userName)
            put("newLikesCount", newLikesCount)
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(json.toString())

        _events.tryEmit(
            WebSocketRealtimeEvent.DynamicFeedLike(
                feedId = feedId,
                userName = userName,
                newLikesCount = newLikesCount
            )
        )
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "App closed")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }
}
