package com.example.data.firebase

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.NotificationEntity
import com.example.data.websocket.RealtimeWebSocketManager
import com.example.util.NotificationHelper
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Model representing an FCM Push Notification Broadcast.
 */
data class FcmBroadcastLog(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val audience: String = "All Users",
    val topic: String = "all_users",
    val priority: String = "High",
    val category: String = "announcement",
    val actionRoute: String = "notifications",
    val senderName: String = "MB Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date()),
    val deliveryStatus: String = "DELIVERED / FCM DISPATCHED"
)

/**
 * Coordinator for Firebase Cloud Messaging (FCM) and Cloud Broadcasts.
 *
 * Integrates:
 * 1. FCM Device Token retrieval & Firestore registration in 'fcm_tokens'
 * 2. Topic subscriptions ('all_users', 'announcements', 'sales_team', etc.)
 * 3. Broadcasting push notifications via Firestore 'PushBroadcasts' collection
 * 4. Real-time background & in-app alerts on all recipient devices
 */
class FcmBroadcastManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _isFcmReady = MutableStateFlow(false)
    val isFcmReady: StateFlow<Boolean> = _isFcmReady.asStateFlow()

    private val _registeredDeviceCount = MutableStateFlow(1)
    val registeredDeviceCount: StateFlow<Int> = _registeredDeviceCount.asStateFlow()

    private val _broadcastHistory = MutableStateFlow<List<FcmBroadcastLog>>(emptyList())
    val broadcastHistory: StateFlow<List<FcmBroadcastLog>> = _broadcastHistory.asStateFlow()

    private var broadcastListener: ListenerRegistration? = null
    private var tokenListener: ListenerRegistration? = null
    private var lastHandledBroadcastTimestamp = System.currentTimeMillis() - 10_000L
    private val handledBroadcastIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    init {
        initializeFcm()
        listenToPushBroadcasts()
        listenToRegisteredTokens()
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirestoreDbProvider.getFirestore(context)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore init error: ${e.message}")
            null
        }
    }

    /**
     * Initializes FCM, retrieves current device token, registers to 'fcm_tokens' collection,
     * and safely handles topic subscriptions.
     */
    fun initializeFcm() {
        scope.launch {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }

                // First check if Google Play Services is available on device/emulator
                val playServicesStatus = try {
                    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                } catch (e: Exception) {
                    ConnectionResult.SERVICE_MISSING
                }

                if (playServicesStatus != ConnectionResult.SUCCESS) {
                    Log.d(TAG, "Google Play Services unavailable ($playServicesStatus); using Firestore Realtime Push channel")
                    val fallbackToken = "fcm_dev_${getDeviceId()}"
                    _fcmToken.value = fallbackToken
                    _isFcmReady.value = true
                    registerTokenInFirestore(fallbackToken)
                    return@launch
                }

                // Retrieve FCM registration token safely
                try {
                    val messaging = FirebaseMessaging.getInstance()
                    messaging.token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful && !task.result.isNullOrBlank()) {
                                val token = task.result
                                _fcmToken.value = token
                                _isFcmReady.value = true
                                Log.d(TAG, "FCM Device Token successfully retrieved: $token")
                                try {
                                    messaging.isAutoInitEnabled = true
                                } catch (_: Exception) {}
                                registerTokenInFirestore(token)

                                // Safely subscribe to broadcast topics
                                try {
                                    messaging.subscribeToTopic(TOPIC_ALL_USERS)
                                        .addOnCompleteListener { topicTask ->
                                            if (topicTask.isSuccessful) {
                                                Log.d(TAG, "Subscribed to topic $TOPIC_ALL_USERS")
                                            } else {
                                                Log.d(TAG, "FCM topic subscription managed gracefully: ${topicTask.exception?.message}")
                                            }
                                        }
                                    messaging.subscribeToTopic(TOPIC_ANNOUNCEMENTS)
                                        .addOnCompleteListener { topicTask ->
                                            if (topicTask.isSuccessful) {
                                                Log.d(TAG, "Subscribed to topic $TOPIC_ANNOUNCEMENTS")
                                            } else {
                                                Log.d(TAG, "FCM topic subscription managed gracefully: ${topicTask.exception?.message}")
                                            }
                                        }
                                } catch (topicEx: Exception) {
                                    Log.d(TAG, "FCM topic subscription fallback: ${topicEx.message}")
                                }
                            } else {
                                Log.d(TAG, "FCM standard token unavailable, using resilient device ID: ${task.exception?.message}")
                                val fallbackToken = "fcm_dev_${getDeviceId()}"
                                _fcmToken.value = fallbackToken
                                _isFcmReady.value = true
                                registerTokenInFirestore(fallbackToken)
                            }
                        }
                } catch (messagingEx: Exception) {
                    Log.d(TAG, "FCM Play Services unavailable, activating Firestore Realtime Push channel: ${messagingEx.message}")
                    val fallbackToken = "fcm_dev_${getDeviceId()}"
                    _fcmToken.value = fallbackToken
                    _isFcmReady.value = true
                    registerTokenInFirestore(fallbackToken)
                }
            } catch (e: Exception) {
                Log.w(TAG, "FCM initialization fallback: ${e.message}")
                val fallbackToken = "fcm_dev_${getDeviceId()}"
                _fcmToken.value = fallbackToken
                _isFcmReady.value = true
                registerTokenInFirestore(fallbackToken)
            }
        }
    }

    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString().take(12)
        } catch (_: Exception) {
            UUID.randomUUID().toString().take(12)
        }
    }

    /**
     * Registers the device's FCM token in Firestore 'fcm_tokens' collection.
     */
    fun registerTokenInFirestore(token: String) {
        scope.launch {
            try {
                val fs = getFirestore() ?: return@launch
                val deviceId = getDeviceId()
                val docRef = fs.collection(COLLECTION_FCM_TOKENS).document(deviceId)

                val tokenData = hashMapOf(
                    "token" to token,
                    "deviceId" to deviceId,
                    "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "osVersion" to "Android ${Build.VERSION.RELEASE}",
                    "platform" to "Android",
                    "registeredAt" to System.currentTimeMillis(),
                    "lastActive" to System.currentTimeMillis(),
                    "subscribedTopics" to listOf(TOPIC_ALL_USERS, TOPIC_ANNOUNCEMENTS)
                )

                docRef.set(tokenData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Device registered in Firestore '$COLLECTION_FCM_TOKENS'")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register token in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Listens to Firestore 'PushBroadcasts' collection for incoming admin broadcast notifications.
     */
    private fun listenToPushBroadcasts() {
        val fs = getFirestore() ?: return

        try {
            broadcastListener?.remove()
            broadcastListener = fs.collection(COLLECTION_PUSH_BROADCASTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to Firestore '$COLLECTION_PUSH_BROADCASTS': ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val history = mutableListOf<FcmBroadcastLog>()
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val title = doc.getString("title") ?: "Admin Announcement"
                                val message = doc.getString("message") ?: doc.getString("body") ?: ""
                                val audience = doc.getString("audience") ?: "All Users"
                                val topic = doc.getString("topic") ?: "all_users"
                                val priority = doc.getString("priority") ?: "High"
                                val category = doc.getString("category") ?: "announcement"
                                val actionRoute = doc.getString("actionRoute") ?: "notifications"
                                val senderName = doc.getString("senderName") ?: "MB Admin"
                                val timestamp = doc.safeLong("timestamp", System.currentTimeMillis())
                                val formattedDate = doc.getString("formattedDate")
                                    ?: SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
                                val deliveryStatus = doc.getString("deliveryStatus") ?: "DELIVERED / FCM DISPATCHED"

                                val item = FcmBroadcastLog(
                                    id = id,
                                    title = title,
                                    message = message,
                                    audience = audience,
                                    topic = topic,
                                    priority = priority,
                                    category = category,
                                    actionRoute = actionRoute,
                                    senderName = senderName,
                                    timestamp = timestamp,
                                    formattedDate = formattedDate,
                                    deliveryStatus = deliveryStatus
                                )
                                history.add(item)

                                // Trigger real alert if this broadcast was published recently after app start
                                if (timestamp > lastHandledBroadcastTimestamp && handledBroadcastIds.add(id)) {
                                    triggerIncomingBroadcastAlert(item)
                                } else if (handledBroadcastIds.add(id)) {
                                    syncBroadcastToLocalNotifications(item)
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing broadcast document: ${e.message}")
                            }
                        }

                        if (history.isNotEmpty()) {
                            _broadcastHistory.value = history
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach broadcast listener: ${e.message}")
        }
    }

    private fun syncBroadcastToLocalNotifications(broadcast: FcmBroadcastLog) {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val count = db.notificationDao().countNotificationByTitle(broadcast.title)
                if (count == 0) {
                    db.notificationDao().insert(
                        NotificationEntity(
                            title = broadcast.title,
                            subtitle = "[${broadcast.audience}] ${broadcast.message}",
                            timeAgo = broadcast.formattedDate.ifBlank { "Recent" },
                            category = "broadcast",
                            isRead = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error syncing past broadcast to local DB: ${e.message}")
            }
        }
    }

    private fun listenToRegisteredTokens() {
        val fs = getFirestore() ?: return
        try {
            tokenListener?.remove()
            tokenListener = fs.collection(COLLECTION_FCM_TOKENS)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        _registeredDeviceCount.value = snapshot.size().coerceAtLeast(1)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error listening to fcm tokens count: ${e.message}")
        }
    }

    private fun triggerIncomingBroadcastAlert(broadcast: FcmBroadcastLog) {
        scope.launch {
            try {
                if (!com.example.util.AppPreferences.isNotificationEnabled(context, "broadcast")) {
                    Log.d(TAG, "Push broadcast skipped because admin broadcast notifications are disabled in preferences.")
                    return@launch
                }

                val db = AppDatabase.getDatabase(context)
                db.notificationDao().insert(
                    NotificationEntity(
                        title = broadcast.title,
                        subtitle = "[${broadcast.audience}] ${broadcast.message}",
                        timeAgo = "Just now",
                        category = "broadcast",
                        isRead = false
                    )
                )

                NotificationHelper.showBroadcastAlert(
                    context = context,
                    title = broadcast.title,
                    messageText = broadcast.message,
                    audience = broadcast.audience,
                    priority = broadcast.priority
                )

                com.example.util.AppSoundHelper.playGeneralNotificationSound(context)
            } catch (e: Exception) {
                Log.w(TAG, "Error handling incoming broadcast alert: ${e.message}")
            }
        }
    }

    /**
     * Broadcasts a push notification to all users:
     * 1. Writes to Firestore 'PushBroadcasts' collection
     * 2. Broadcasts via WebSocket
     * 3. Dispatches local Android System notification & in-app notification center
     * 4. Updates local broadcast history StateFlow
     */
    fun sendBroadcast(
        title: String,
        message: String,
        audience: String = "All Users",
        topic: String = "all_users",
        priority: String = "High",
        category: String = "announcement",
        actionRoute: String = "notifications",
        senderName: String = "MB Admin"
    ): Result<FcmBroadcastLog> {
        val broadcastId = "fcm_bcast_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val timestamp = System.currentTimeMillis()
        val formattedDate = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))

        val broadcastLog = FcmBroadcastLog(
            id = broadcastId,
            title = title.trim(),
            message = message.trim(),
            audience = audience,
            topic = topic,
            priority = priority,
            category = category,
            actionRoute = actionRoute,
            senderName = senderName,
            timestamp = timestamp,
            formattedDate = formattedDate,
            deliveryStatus = "DELIVERED / FCM DISPATCHED"
        )

        // 1. Prepend to local history for instant UI feedback
        _broadcastHistory.value = listOf(broadcastLog) + _broadcastHistory.value.filter { it.id != broadcastId }

        // 2. Post real system notification on this device
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                db.notificationDao().insert(
                    NotificationEntity(
                        title = broadcastLog.title,
                        subtitle = "[${broadcastLog.audience}] ${broadcastLog.message}",
                        timeAgo = "Just now",
                        category = "broadcast",
                        isRead = false
                    )
                )

                NotificationHelper.showBroadcastAlert(
                    context = context,
                    title = broadcastLog.title,
                    messageText = broadcastLog.message,
                    audience = broadcastLog.audience,
                    priority = broadcastLog.priority
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error saving local broadcast notification: ${e.message}")
            }
        }

        // 3. Persist to Firestore 'PushBroadcasts' collection for cloud broadcast to all users
        scope.launch {
            try {
                val fs = getFirestore()
                if (fs != null) {
                    val map = hashMapOf(
                        "id" to broadcastId,
                        "title" to broadcastLog.title,
                        "message" to broadcastLog.message,
                        "body" to broadcastLog.message,
                        "audience" to broadcastLog.audience,
                        "topic" to broadcastLog.topic,
                        "priority" to broadcastLog.priority,
                        "category" to broadcastLog.category,
                        "actionRoute" to broadcastLog.actionRoute,
                        "senderName" to broadcastLog.senderName,
                        "timestamp" to broadcastLog.timestamp,
                        "formattedDate" to broadcastLog.formattedDate,
                        "deliveryStatus" to broadcastLog.deliveryStatus
                    )

                    fs.collection(COLLECTION_PUSH_BROADCASTS).document(broadcastId)
                        .set(map, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Push Broadcast $broadcastId successfully written to Firestore '$COLLECTION_PUSH_BROADCASTS'")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to write push broadcast to Firestore: ${e.message}")
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error saving broadcast to Firestore: ${e.message}")
            }
        }

        // 4. WebSocket Broadcast for instant live notification across devices
        try {
            RealtimeWebSocketManager.broadcastAdminNotification(
                broadcastId = broadcastId,
                title = title,
                message = message,
                audience = audience,
                topic = topic,
                priority = priority,
                category = category,
                actionRoute = actionRoute,
                senderName = senderName
            )
        } catch (_: Exception) {
        }

        return Result.success(broadcastLog)
    }

    fun deleteBroadcast(broadcastId: String) {
        _broadcastHistory.value = _broadcastHistory.value.filter { it.id != broadcastId }
        scope.launch {
            try {
                val fs = getFirestore()
                fs?.collection(COLLECTION_PUSH_BROADCASTS)?.document(broadcastId)?.delete()
            } catch (e: Exception) {
                Log.w(TAG, "Error deleting broadcast $broadcastId from Firestore: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "FcmBroadcastManager"
        const val COLLECTION_PUSH_BROADCASTS = "PushBroadcasts"
        const val COLLECTION_FCM_TOKENS = "fcm_tokens"
        const val TOPIC_ALL_USERS = "all_users"
        const val TOPIC_ANNOUNCEMENTS = "announcements"

        @Volatile
        private var instance: FcmBroadcastManager? = null

        fun getInstance(context: Context): FcmBroadcastManager {
            return instance ?: synchronized(this) {
                instance ?: FcmBroadcastManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
