package com.example.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.ActivityFeedDao
import com.example.data.local.AttendanceDao
import com.example.data.local.ChatDao
import com.example.data.local.EmployeeDao
import com.example.data.local.LeadDao
import com.example.data.local.NotificationDao
import com.example.data.local.TaskDao
import com.example.data.local.UserProfileDao
import com.example.data.local.LeaveDao
import com.example.data.local.InvoiceDao
import com.example.data.local.QuotationDao
import com.example.data.local.ShiftDao
import com.example.data.model.ActivityFeedItemEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.ChatMessageEntity
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeStatus
import com.example.data.model.InvoiceEntity
import com.example.data.model.QuotationEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.LeadEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.LeaveApplicationEntity
import com.example.data.model.HolidayItem
import com.example.data.websocket.RealtimeWebSocketManager
import com.example.data.websocket.WebSocketRealtimeEvent
import com.example.util.NotificationHelper
import com.example.util.ReactionUtils
import com.example.util.AppSoundHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

enum class NetworkSyncStatus {
    SYNCED,
    SYNCING,
    OFFLINE,
    ERROR
}

data class SyncQueueSummary(
    val pendingLeads: Int = 0,
    val pendingTasks: Int = 0,
    val pendingAttendance: Int = 0,
    val pendingChatMessages: Int = 0
) {
    val totalPending: Int get() = pendingLeads + pendingTasks + pendingAttendance + pendingChatMessages
}

data class SyncState(
    val status: NetworkSyncStatus = NetworkSyncStatus.SYNCED,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
    val isSimulatedOffline: Boolean = false,
    val pendingSummary: SyncQueueSummary = SyncQueueSummary(),
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val statusMessage: String = "Firebase Realtime: Live & Synced"
) {
    val formattedLastSyncTime: String
        get() {
            val now = System.currentTimeMillis()
            val diffSec = (now - lastSyncTimestamp) / 1000
            return when {
                diffSec < 15 -> "Just now"
                diffSec < 60 -> "${diffSec}s ago"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(lastSyncTimestamp))
            }
        }
}

object FirebaseRealtimeManager {
    private const val TAG = "FirebaseRealtimeManager"
    private var firestore: FirebaseFirestore? = null
    private var attendanceListener: ListenerRegistration? = null
    private var taskListener: ListenerRegistration? = null
    private var projectListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null
    private var chatListener: ListenerRegistration? = null
    private var feedListener: ListenerRegistration? = null
    private val messageReactionListeners = ConcurrentHashMap<Long, ListenerRegistration>()
    private var currentEmployeeName: String = "Rahul Sharma"
    private var isInitialized = false
    private var appScope: CoroutineScope? = null
    private var appContext: Context? = null

    // Pending Sync Queues for offline mutations
    private val pendingLeadsQueue = ConcurrentLinkedQueue<LeadEntity>()
    private val pendingTasksQueue = ConcurrentLinkedQueue<TaskEntity>()
    private val pendingAttendanceQueue = ConcurrentLinkedQueue<AttendanceRecord>()
    private val pendingChatMessagesQueue = ConcurrentLinkedQueue<ChatMessageEntity>()

    private var attendanceDaoRef: AttendanceDao? = null
    private var chatDaoRef: ChatDao? = null
    private var projectDaoRef: com.example.data.local.ProjectDao? = null
    private var notificationDaoRef: NotificationDao? = null
    private var leaveDaoRef: LeaveDao? = null
    private var shiftDaoRef: ShiftDao? = null
    private var notificationListener: ListenerRegistration? = null
    private val handledNotificationDocIds = ConcurrentHashMap.newKeySet<String>()

    private var isRealNetworkConnected = true
    private var isSimulatedOffline = false

    private val _syncState = MutableStateFlow(
        SyncState(
            status = NetworkSyncStatus.SYNCED,
            isOnline = true,
            isSyncing = false,
            lastSyncTimestamp = System.currentTimeMillis(),
            statusMessage = "Firebase Realtime: Live & Synced"
        )
    )
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Backward compatibility flows
    val isRealtimeConnected: StateFlow<Boolean>
        get() = MutableStateFlow(_syncState.value.isOnline && _syncState.value.status != NetworkSyncStatus.OFFLINE)
    val lastSyncTimestamp: StateFlow<Long>
        get() = MutableStateFlow(_syncState.value.lastSyncTimestamp)
    val syncStatus: StateFlow<String>
        get() = MutableStateFlow(_syncState.value.statusMessage)

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    fun initialize(
        context: Context,
        attendanceDao: AttendanceDao,
        userProfileDao: UserProfileDao,
        taskDao: TaskDao? = null,
        leadDao: LeadDao? = null,
        chatDao: ChatDao? = null,
        activityFeedDao: ActivityFeedDao? = null,
        scope: CoroutineScope,
        notificationDao: NotificationDao? = null,
        projectDao: com.example.data.local.ProjectDao? = null,
        leaveDao: LeaveDao? = null,
        invoiceDao: com.example.data.local.InvoiceDao? = null,
        quotationDao: com.example.data.local.QuotationDao? = null,
        shiftDao: ShiftDao? = null
    ) {
        attendanceDaoRef = attendanceDao
        chatDaoRef = chatDao
        notificationDaoRef = notificationDao
        projectDaoRef = projectDao
        leaveDaoRef = leaveDao
        shiftDaoRef = shiftDao
        appContext = context.applicationContext
        appScope = scope
        registerNetworkCallback(context)
        NotificationHelper.createNotificationChannels(context)

        // Initialize Realtime WebSocket Connection
        try {
            RealtimeWebSocketManager.connect()
            scope.launch {
                RealtimeWebSocketManager.events.collect { wsEvent ->
                    when (wsEvent) {
                        is WebSocketRealtimeEvent.ChatMessage -> {
                            if (chatDao != null) {
                                val isSenderMe = wsEvent.senderName.trim().equals(currentEmployeeName.trim(), ignoreCase = true)
                                val existing = chatDao.getMessageById(wsEvent.messageId)
                                if (existing == null) {
                                    val newMsg = ChatMessageEntity(
                                        id = wsEvent.messageId,
                                        channelId = wsEvent.channelId,
                                        senderName = wsEvent.senderName,
                                        senderRole = wsEvent.senderRole,
                                        messageText = wsEvent.messageText,
                                        timestampText = wsEvent.timestampText,
                                        isMe = isSenderMe,
                                        attachmentFileName = wsEvent.attachmentFileName,
                                        attachmentFileSize = wsEvent.attachmentFileSize,
                                        audioPath = wsEvent.audioPath,
                                        audioDurationSeconds = wsEvent.audioDurationSeconds,
                                        isVoiceMessage = wsEvent.isVoiceMessage,
                                        isRead = isSenderMe,
                                        isSynced = true
                                    )
                                    chatDao.insert(newMsg)

                                    if (!isSenderMe) {
                                        appContext?.let { ctx ->
                                            NotificationHelper.showChatAlert(
                                                context = ctx,
                                                senderName = wsEvent.senderName,
                                                messageText = wsEvent.messageText,
                                                channelTitle = "Team Chat (#${wsEvent.channelId})",
                                                channelId = wsEvent.channelId
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        is WebSocketRealtimeEvent.AdminNotification -> {
                            appContext?.let { ctx ->
                                try {
                                    val db = com.example.data.local.AppDatabase.getDatabase(ctx)
                                    db.notificationDao().insert(
                                        com.example.data.model.NotificationEntity(
                                            title = wsEvent.title,
                                            subtitle = "[${wsEvent.audience}] ${wsEvent.message}",
                                            timeAgo = "Just now",
                                            category = "broadcast",
                                            isRead = false
                                        )
                                    )
                                    NotificationHelper.showBroadcastAlert(
                                        context = ctx,
                                        title = wsEvent.title,
                                        messageText = wsEvent.message,
                                        audience = wsEvent.audience,
                                        priority = wsEvent.priority
                                    )
                                } catch (ne: Exception) {
                                    Log.w(TAG, "Error processing incoming admin notification: ${ne.message}")
                                }
                            }
                        }
                        is WebSocketRealtimeEvent.EmojiReaction -> {
                            if (chatDao != null) {
                                val existing = chatDao.getMessageById(wsEvent.messageId)
                                if (existing != null) {
                                    val (newJson, _) = ReactionUtils.toggleUserReaction(
                                        existing.reactionsJson,
                                        wsEvent.emoji,
                                        wsEvent.userName
                                    )
                                    chatDao.updateReactions(wsEvent.messageId, newJson)
                                }
                            }
                        }
                        is WebSocketRealtimeEvent.DynamicFeedUpdate -> {
                            if (activityFeedDao != null) {
                                activityFeedDao.insert(
                                    ActivityFeedItemEntity(
                                        id = wsEvent.feedId,
                                        authorName = wsEvent.authorName,
                                        authorRole = wsEvent.authorRole,
                                        title = wsEvent.title,
                                        content = wsEvent.content,
                                        category = wsEvent.category,
                                        timestampText = wsEvent.timestampText,
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        is WebSocketRealtimeEvent.DynamicFeedLike -> {
                            activityFeedDao?.updateLikes(
                                wsEvent.feedId,
                                wsEvent.newLikesCount,
                                wsEvent.userName
                            )
                        }
                        else -> Unit
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "WebSocket initialization warning: ${e.message}")
        }

        if (isInitialized && firestore != null) {
            fetchAndRegisterFcmToken(context)
            if (chatDao != null && globalChatListener == null) {
                startGlobalChatListener(chatDao, scope)
            }
            if (chatDao != null && chatListener == null) {
                startRealtimeChatListener("dev_team", chatDao, scope)
            }
            if (notificationDao != null && notificationListener == null) {
                startRealtimeNotificationListener(notificationDao, scope)
            }
            if (activityFeedDao != null && feedListener == null) {
                startRealtimeFeedListener(activityFeedDao, scope)
            }
            if (taskDao != null && taskListener == null) {
                startRealtimeTaskListener(taskDao, scope)
            }
            if (projectDao != null && projectListener == null) {
                startRealtimeProjectListener(projectDao, scope)
            }
            if (leadDao != null && leadListener == null) {
                startRealtimeLeadListener(leadDao, scope)
            }
            if (leaveDao != null && leaveListener == null) {
                startRealtimeLeaveListener(leaveDao, scope)
            }
            if (shiftDao != null && shiftListener == null) {
                startRealtimeShiftScheduleListener(shiftDao, scope)
            }
            if (holidayListener == null) {
                startRealtimeHolidayListener(scope)
            }
            startBrandingConfigListener(context)
            return
        }

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Initialized default FirebaseApp")
            }

            firestore = FirestoreDbProvider.getFirestore(context)
            startBrandingConfigListener(context)
            isInitialized = true
            updateSyncState(
                status = NetworkSyncStatus.SYNCED,
                isOnline = isEffectiveOnline(),
                statusMessage = "Firebase Realtime: Connected (Firestore Database Live)"
            )

            // Monitor Battery Saver Mode state
            scope.launch {
                com.example.util.BatterySaverManager.getInstance(context).isBatterySaverActive.collect { isSaverActive ->
                    val statusMsg = if (isSaverActive) {
                        "⚡ Battery Saver Active — Real-time listeners throttled (60s interval)"
                    } else {
                        "Firebase Realtime: Connected (WebSockets + Firestore)"
                    }
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = isEffectiveOnline(),
                        statusMessage = statusMsg
                    )
                }
            }

            // Register and fetch FCM token gracefully (if available)
            fetchAndRegisterFcmToken(context)

            // Attach real-time snapshot listeners
            startRealtimeAttendanceListener(attendanceDao, scope)
            startRealtimeProfileListener(userProfileDao, scope)
            if (taskDao != null) {
                startRealtimeTaskListener(taskDao, scope)
            }
            if (projectDao != null) {
                startRealtimeProjectListener(projectDao, scope)
            }
            if (chatDao != null) {
                startGlobalChatListener(chatDao, scope)
                startRealtimeChatListener("dev_team", chatDao, scope)
            }
            if (activityFeedDao != null) {
                startRealtimeFeedListener(activityFeedDao, scope)
            }
            if (notificationDao != null) {
                startRealtimeNotificationListener(notificationDao, scope)
            }
            if (leadDao != null) {
                startRealtimeLeadListener(leadDao, scope)
            }
            if (leaveDao != null) {
                startRealtimeLeaveListener(leaveDao, scope)
            }
            if (shiftDao != null) {
                startRealtimeShiftScheduleListener(shiftDao, scope)
            }
            if (invoiceDao != null) {
                startRealtimeInvoiceListener(invoiceDao, scope)
            }
            if (quotationDao != null) {
                startRealtimeQuotationListener(quotationDao, scope)
            }
            startRealtimeHolidayListener(scope)
            scope.launch(Dispatchers.IO) {
                seedInitialFirestoreDatabase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}", e)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Firebase Standby: Offline Mode"
            )
        }
    }

    private fun fetchAndRegisterFcmToken(context: Context) {
        // FCM auto-init is disabled in manifest to avoid hard failure exceptions in dev/emulator environments
        Log.d(TAG, "FCM background auto-sync disabled; using real-time Firestore listeners for notifications and sync.")
    }

    fun updateFcmToken(token: String?) {
        if (token.isNullOrBlank()) return
        _fcmToken.value = token
        appScope?.launch(Dispatchers.IO) {
            try {
                firestore?.collection("fcm_devices")?.document("device_default")?.set(
                    mapOf(
                        "token" to token,
                        "updatedAt" to Date(),
                        "platform" to "Android",
                        "appVersion" to "1.0.0"
                    ),
                    SetOptions.merge()
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to upload FCM token to Firestore: ${e.message}")
            }
        }
    }

    fun triggerLocalChatAlert(
        context: Context,
        senderName: String,
        messageText: String,
        channelTitle: String = "Company Chat",
        channelId: String = "company_chat"
    ) {
        NotificationHelper.showChatAlert(context, senderName, messageText, channelTitle, channelId)
    }

    fun triggerLocalTaskAlert(
        context: Context,
        title: String,
        messageText: String,
        taskId: Long = 0L
    ) {
        NotificationHelper.showTaskAlert(context, title, messageText, taskId)
    }

    private fun registerNetworkCallback(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                isRealNetworkConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

                val builder = NetworkRequest.Builder()
                connectivityManager.registerNetworkCallback(builder.build(), object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isRealNetworkConnected = true
                        handleConnectivityChanged()
                    }

                    override fun onLost(network: Network) {
                        isRealNetworkConnected = false
                        handleConnectivityChanged()
                    }
                })
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network callback setup skipped: ${e.message}")
        }
    }

    fun isEffectiveOnline(): Boolean {
        return isRealNetworkConnected && !isSimulatedOffline
    }

    private fun handleConnectivityChanged() {
        val online = isEffectiveOnline()
        if (online) {
            appScope?.launch(Dispatchers.IO) {
                val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size + pendingChatMessagesQueue.size
                val unsyncedAttCount = attendanceDaoRef?.getUnsyncedAttendance()?.size ?: 0
                val unsyncedChatCount = chatDaoRef?.getUnsyncedMessages()?.size ?: 0
                if (totalPending > 0 || unsyncedAttCount > 0 || unsyncedChatCount > 0) {
                    processPendingSyncQueue()
                } else {
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        statusMessage = "Firebase Realtime: Live & Synced"
                    )
                }
            }
        } else {
            val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size + pendingChatMessagesQueue.size
            val msg = if (totalPending > 0) "Offline: $totalPending updates cached in Room" else "Offline Mode · Room Cache Active"
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = msg
            )
        }
    }

    fun toggleSimulatedOffline(forceOffline: Boolean) {
        isSimulatedOffline = forceOffline
        handleConnectivityChanged()
    }

    private fun updateSyncState(
        status: NetworkSyncStatus? = null,
        isOnline: Boolean? = null,
        isSyncing: Boolean? = null,
        statusMessage: String? = null,
        newTimestamp: Long? = null
    ) {
        val current = _syncState.value
        val summary = SyncQueueSummary(
            pendingLeads = pendingLeadsQueue.size,
            pendingTasks = pendingTasksQueue.size,
            pendingAttendance = pendingAttendanceQueue.size,
            pendingChatMessages = pendingChatMessagesQueue.size
        )
        _syncState.value = current.copy(
            status = status ?: current.status,
            isOnline = isOnline ?: current.isOnline,
            isSyncing = isSyncing ?: current.isSyncing,
            isSimulatedOffline = isSimulatedOffline,
            pendingSummary = summary,
            lastSyncTimestamp = newTimestamp ?: current.lastSyncTimestamp,
            statusMessage = statusMessage ?: current.statusMessage
        )
    }

    // ==========================================
    // SYNC OPERATIONS (Leads, Tasks, Attendance)
    // ==========================================

    fun syncLeadToFirebase(lead: LeadEntity) {
        if (!isEffectiveOnline()) {
            pendingLeadsQueue.removeIf { it.id == lead.id }
            pendingLeadsQueue.add(lead)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: ${pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size} pending"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing lead '${lead.name}'..."
        )

        try {
            val docId = if (lead.id > 0L) "lead_${lead.id}" else "lead_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to lead.id,
                "customerName" to lead.name,
                "company" to lead.company,
                "phone" to lead.phone,
                "email" to lead.email,
                "leadScore" to lead.leadScore,
                "requirement" to lead.requirement,
                "value" to (lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0),
                "status" to lead.stage,
                "assignedTo" to lead.assignedTo,
                "nextFollowUp" to lead.nextFollowUp,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("leads")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingLeadsQueue.remove(lead)
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced lead $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase lead sync error: ${e.message}")
                    pendingLeadsQueue.add(lead)
                    updateSyncState(
                        status = NetworkSyncStatus.ERROR,
                        isSyncing = false,
                        statusMessage = "Sync failed: Retrying soon"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncLeadToFirebase: ${e.message}")
            pendingLeadsQueue.add(lead)
            updateSyncState(
                status = NetworkSyncStatus.ERROR,
                isSyncing = false,
                statusMessage = "Sync failed"
            )
        }
    }

    fun syncTaskToFirebase(task: TaskEntity) {
        if (!isEffectiveOnline()) {
            pendingTasksQueue.removeIf { it.id == task.id }
            pendingTasksQueue.add(task)
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: ${pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size} pending"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing task '${task.title}'..."
        )

        try {
            val docId = if (task.id > 0L) "task_${task.id}" else "task_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "projectName" to task.projectName,
                "priority" to task.priority,
                "dueDate" to task.dueDate,
                "status" to task.status,
                "isCompleted" to task.isCompleted,
                "category" to task.category,
                "estimatedTimeNeeded" to task.estimatedTimeNeeded,
                "assignee" to task.assignee,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("tasks")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingTasksQueue.remove(task)
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced task $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase task sync error: ${e.message}")
                    pendingTasksQueue.add(task)
                    updateSyncState(
                        status = NetworkSyncStatus.ERROR,
                        isSyncing = false,
                        statusMessage = "Sync failed: Retrying soon"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncTaskToFirebase: ${e.message}")
            pendingTasksQueue.add(task)
            updateSyncState(
                status = NetworkSyncStatus.ERROR,
                isSyncing = false,
                statusMessage = "Sync failed"
            )
        }
    }

    fun deleteTaskFromFirebase(taskId: Long) {
        if (!isEffectiveOnline()) return
        try {
            val docId = "task_$taskId"
            firestore?.collection("tasks")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task from Firebase: ${e.message}")
        }
    }

    fun syncProjectToFirebase(project: ProjectEntity) {
        if (!isEffectiveOnline()) {
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: Project cached in Room"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing project '${project.name}'..."
        )

        try {
            val docId = if (project.id > 0L) "project_${project.id}" else "project_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to project.id,
                "name" to project.name,
                "clientName" to project.clientName,
                "totalTasks" to project.totalTasks,
                "completedTasks" to project.completedTasks,
                "progressPercent" to project.progressPercent,
                "status" to project.status,
                "priority" to project.priority,
                "startDate" to project.startDate,
                "deadline" to project.deadline,
                "managerName" to project.managerName,
                "teamSize" to project.teamSize,
                "description" to project.description,
                "budget" to project.budget,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("projects")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced project $docId to Firebase")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase project sync error: ${e.message}")
                    updateSyncState(
                        status = NetworkSyncStatus.ERROR,
                        isSyncing = false,
                        statusMessage = "Sync failed: Retrying soon"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncProjectToFirebase: ${e.message}")
            updateSyncState(
                status = NetworkSyncStatus.ERROR,
                isSyncing = false,
                statusMessage = "Sync failed"
            )
        }
    }

    fun deleteProjectFromFirebase(projectId: Long) {
        if (!isEffectiveOnline()) return
        try {
            val docId = "project_$projectId"
            firestore?.collection("projects")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting project from Firebase: ${e.message}")
        }
    }

    fun syncAttendanceToFirebase(record: AttendanceRecord) {
        if (!isEffectiveOnline()) {
            pendingAttendanceQueue.removeIf { it.id == record.id }
            pendingAttendanceQueue.add(record)
            val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size + pendingChatMessagesQueue.size
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: Attendance cached in Room ($totalPending pending)"
            )
            return
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = "Syncing attendance..."
        )

        try {
            val docId = if (record.id > 0L) "att_${record.id}" else "att_${record.timestamp}"
            val data = hashMapOf(
                "id" to record.id,
                "date" to record.date,
                "checkInTime" to record.checkInTime,
                "checkOutTime" to record.checkOutTime,
                "durationMinutes" to record.durationMinutes,
                "isWorking" to record.isWorking,
                "status" to record.status,
                "overtimeMinutes" to record.overtimeMinutes,
                "breakMinutes" to record.breakMinutes,
                "timestamp" to record.timestamp,
                "employeeName" to record.employeeName,
                "latitude" to (record.latitude ?: com.example.util.LocationHelper.OFFICE_LAT),
                "longitude" to (record.longitude ?: com.example.util.LocationHelper.OFFICE_LNG),
                "locationAddress" to (record.locationAddress ?: com.example.util.LocationHelper.OFFICE_NAME),
                "isGeofenceVerified" to record.isGeofenceVerified,
                "selfieUri" to record.selfieUri,
                "action" to if (record.isWorking) "CLOCK_IN" else "CLOCK_OUT",
                "clockInTimestamp" to record.timestamp,
                "clockOutTimestamp" to if (!record.isWorking) System.currentTimeMillis() else null,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("attendance_records")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingAttendanceQueue.remove(record)
                    appScope?.launch(Dispatchers.IO) {
                        attendanceDaoRef?.markAttendanceAsSynced(record.id)
                    }
                    val now = System.currentTimeMillis()
                    updateSyncState(
                        status = NetworkSyncStatus.SYNCED,
                        isOnline = true,
                        isSyncing = false,
                        statusMessage = "Firebase Realtime: Live & Synced",
                        newTimestamp = now
                    )
                    Log.d(TAG, "Successfully synced attendance $docId to Firebase and marked in Room")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Firebase attendance sync error: ${e.message}")
                    pendingAttendanceQueue.add(record)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncAttendanceToFirebase: ${e.message}")
            pendingAttendanceQueue.add(record)
        }
    }

    fun syncProfileToFirebase(profile: UserProfileEntity) {
        try {
            val docId = "user_${profile.id}"
            val data = hashMapOf(
                "id" to profile.id,
                "name" to profile.name,
                "role" to profile.role,
                "isOnboarded" to profile.isOnboarded,
                "updatedAt" to profile.updatedAt
            )
            firestore?.collection("user_profiles")?.document(docId)
                ?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Successfully synced profile $docId to Firebase")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncProfileToFirebase: ${e.message}")
        }
    }

    fun syncNow(scope: CoroutineScope? = appScope) {
        val targetScope = scope ?: CoroutineScope(Dispatchers.IO)
        targetScope.launch(Dispatchers.IO) {
            if (!isEffectiveOnline()) {
                updateSyncState(
                    status = NetworkSyncStatus.OFFLINE,
                    isOnline = false,
                    statusMessage = "Cannot sync while Offline"
                )
                return@launch
            }
            processPendingSyncQueue()
        }
    }

    private suspend fun processPendingSyncQueue() {
        val roomUnsyncedAtt = attendanceDaoRef?.getUnsyncedAttendance().orEmpty()
        val roomUnsyncedMsgs = chatDaoRef?.getUnsyncedMessages().orEmpty()
        val totalCount = pendingLeadsQueue.size + pendingTasksQueue.size + 
                (pendingAttendanceQueue.size + roomUnsyncedAtt.size) + 
                (pendingChatMessagesQueue.size + roomUnsyncedMsgs.size)
        
        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isSyncing = true,
            statusMessage = if (totalCount > 0) "Syncing $totalCount Room cached updates..." else "Syncing with Firestore..."
        )

        // 1. Process leads
        val leadsToSync = pendingLeadsQueue.toList()
        for (lead in leadsToSync) {
            try {
                val docId = if (lead.id > 0L) "lead_${lead.id}" else "lead_${System.currentTimeMillis()}"
                val data = hashMapOf(
                    "id" to lead.id,
                    "customerName" to lead.name,
                    "company" to lead.company,
                    "phone" to lead.phone,
                    "email" to lead.email,
                    "leadScore" to lead.leadScore,
                    "requirement" to lead.requirement,
                    "value" to (lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0),
                    "status" to lead.stage,
                    "assignedTo" to lead.assignedTo,
                    "nextFollowUp" to lead.nextFollowUp,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("leads")?.document(docId)?.set(data, SetOptions.merge())
                pendingLeadsQueue.remove(lead)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued lead: ${e.message}")
            }
        }

        // 2. Process tasks
        val tasksToSync = pendingTasksQueue.toList()
        for (task in tasksToSync) {
            try {
                val docId = if (task.id > 0L) "task_${task.id}" else "task_${System.currentTimeMillis()}"
                val data = hashMapOf(
                    "id" to task.id,
                    "title" to task.title,
                    "projectName" to task.projectName,
                    "priority" to task.priority,
                    "dueDate" to task.dueDate,
                    "status" to task.status,
                    "isCompleted" to task.isCompleted,
                    "category" to task.category,
                    "estimatedTimeNeeded" to task.estimatedTimeNeeded,
                    "assignee" to task.assignee,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("tasks")?.document(docId)?.set(data, SetOptions.merge())
                pendingTasksQueue.remove(task)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued task: ${e.message}")
            }
        }

        // 3. Process attendance (combine pending queue and Room unsynced records)
        val allAttToSync = (pendingAttendanceQueue.toList() + roomUnsyncedAtt).distinctBy { it.id }
        for (att in allAttToSync) {
            try {
                val docId = if (att.id > 0L) "att_${att.id}" else "att_${att.timestamp}"
                val data = hashMapOf(
                    "id" to att.id,
                    "date" to att.date,
                    "checkInTime" to att.checkInTime,
                    "checkOutTime" to att.checkOutTime,
                    "durationMinutes" to att.durationMinutes,
                    "isWorking" to att.isWorking,
                    "status" to att.status,
                    "overtimeMinutes" to att.overtimeMinutes,
                    "breakMinutes" to att.breakMinutes,
                    "timestamp" to att.timestamp,
                    "employeeName" to att.employeeName,
                    "latitude" to (att.latitude ?: com.example.util.LocationHelper.OFFICE_LAT),
                    "longitude" to (att.longitude ?: com.example.util.LocationHelper.OFFICE_LNG),
                    "locationAddress" to (att.locationAddress ?: com.example.util.LocationHelper.OFFICE_NAME),
                    "isGeofenceVerified" to att.isGeofenceVerified,
                    "selfieUri" to att.selfieUri,
                    "action" to if (att.isWorking) "CLOCK_IN" else "CLOCK_OUT",
                    "clockInTimestamp" to att.timestamp,
                    "clockOutTimestamp" to if (!att.isWorking) System.currentTimeMillis() else null,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore?.collection("attendance_records")?.document(docId)?.set(data, SetOptions.merge())
                pendingAttendanceQueue.remove(att)
                attendanceDaoRef?.markAttendanceAsSynced(att.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued attendance: ${e.message}")
            }
        }

        // 4. Process chat messages (combine pending queue and Room unsynced records)
        val allMsgsToSync = (pendingChatMessagesQueue.toList() + roomUnsyncedMsgs).distinctBy { it.id }
        for (msg in allMsgsToSync) {
            try {
                var finalAudioPath = msg.audioPath
                // If it's a voice message with a local file on device, upload to Firebase Storage first
                if (msg.isVoiceMessage && !finalAudioPath.isNullOrBlank() && !finalAudioPath.startsWith("http")) {
                    val localFile = java.io.File(finalAudioPath)
                    if (localFile.exists() && localFile.length() > 0) {
                        try {
                            val uploadResult = FirebaseStorageManager.uploadVoiceNote(msg.channelId, localFile)
                            if (uploadResult.isSuccess) {
                                val remoteUrl = uploadResult.getOrNull()
                                if (!remoteUrl.isNullOrBlank()) {
                                    finalAudioPath = remoteUrl
                                    chatDaoRef?.updateAudioPath(msg.id, remoteUrl)
                                }
                            }
                        } catch (ue: Exception) {
                            Log.w(TAG, "Voice note upload in sync queue warning: ${ue.message}")
                        }
                    }
                }

                val docId = "msg_${msg.id}"
                val data = hashMapOf<String, Any?>(
                    "id" to msg.id,
                    "channelId" to msg.channelId,
                    "senderName" to msg.senderName,
                    "senderRole" to msg.senderRole,
                    "messageText" to msg.messageText,
                    "timestampText" to msg.timestampText,
                    "isMe" to false, // Evaluated on recipient
                    "attachmentFileName" to msg.attachmentFileName,
                    "attachmentFileSize" to msg.attachmentFileSize,
                    "audioPath" to finalAudioPath,
                    "audioDurationSeconds" to msg.audioDurationSeconds,
                    "isVoiceMessage" to msg.isVoiceMessage,
                    "createdAt" to msg.id,
                    "isRead" to msg.isRead,
                    "readBy" to if (msg.readBy.isNotBlank()) listOf(msg.readBy) else emptyList<String>(),
                    "readAt" to msg.readAt
                )
                firestore?.collection("chat_messages")?.document(docId)?.set(data, SetOptions.merge())
                pendingChatMessagesQueue.remove(msg)
                chatDaoRef?.markMessageAsSynced(msg.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed syncing queued chat message: ${e.message}")
            }
        }

        delay(600) // Brief graceful visual confirmation
        val now = System.currentTimeMillis()
        updateSyncState(
            status = NetworkSyncStatus.SYNCED,
            isOnline = true,
            isSyncing = false,
            statusMessage = "Firebase Realtime: Live & Synced",
            newTimestamp = now
        )
    }

    // ==========================================
    // REALTIME SNAPSHOT LISTENERS
    // ==========================================

    private fun startRealtimeAttendanceListener(attendanceDao: AttendanceDao, scope: CoroutineScope) {
        try {
            attendanceListener?.remove()
            attendanceListener = firestore?.collection("attendance_records")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime attendance listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                try {
                                    val id = doc.safeLongOrNull("id")
                                        ?: doc.id.replace("att_", "").toLongOrNull()
                                        ?: 0L
                                    val date = doc.safeString("date", "")
                                    val checkInTime = doc.safeString("checkInTime", "")
                                    val checkOutTime = doc.getString("checkOutTime")
                                    val durationMinutes = doc.safeLong("durationMinutes", 0L)
                                    val isWorking = doc.safeBoolean("isWorking", false)
                                    val status = doc.safeString("status", "Present")
                                    val overtimeMinutes = doc.safeLong("overtimeMinutes", 0L)
                                    val timestamp = doc.safeLong("timestamp", System.currentTimeMillis())
                                    val employeeName = doc.safeString("employeeName", "Rahul Sharma")

                                if (id > 0L) {
                                    val record = AttendanceRecord(
                                        id = id,
                                        date = date,
                                        checkInTime = checkInTime,
                                        checkOutTime = checkOutTime,
                                        durationMinutes = durationMinutes,
                                        isWorking = isWorking,
                                        status = status,
                                        overtimeMinutes = overtimeMinutes,
                                        timestamp = timestamp,
                                        employeeName = employeeName
                                    )
                                    val existing = attendanceDao.getAttendanceRecordByIdDirect(id)
                                    attendanceDao.insert(record)

                                    val isMyUpdate = employeeName.trim().equals(currentEmployeeName.trim(), ignoreCase = true)
                                    if (!isMyUpdate && existing != record) {
                                        val isNew = existing == null
                                        val statusChanged = existing != null && (existing.status != status || existing.isWorking != isWorking)
                                        if (isNew) {
                                            triggerLiveAlertAndNotification(
                                                title = "⏰ Attendance Check-In",
                                                subtitle = "$employeeName checked in on $date at $checkInTime ($status)",
                                                category = "attendance"
                                            )
                                        } else if (statusChanged) {
                                            triggerLiveAlertAndNotification(
                                                title = "🔄 Attendance Roster Updated",
                                                subtitle = "$employeeName status updated to '$status' (Working: $isWorking)",
                                                category = "attendance"
                                            )
                                        }
                                    }
                                }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Skipping malformed attendance doc ${doc.id}: ${e.message}")
                                }
                            }
                            updateSyncState(
                                status = NetworkSyncStatus.SYNCED,
                                isOnline = true,
                                newTimestamp = System.currentTimeMillis(),
                                statusMessage = "Firebase Realtime: Synced (${snapshot.size()} live entries)"
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start attendance listener: ${e.message}")
        }
    }

    private fun startRealtimeTaskListener(taskDao: TaskDao, scope: CoroutineScope) {
        try {
            taskListener?.remove()
            taskListener = firestore?.collection("tasks")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime task listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                try {
                                    val id = doc.safeLongOrNull("id")
                                        ?: doc.id.replace("task_", "").toLongOrNull()
                                        ?: Math.abs(doc.id.hashCode().toLong())
                                    val title = doc.safeString("title", "")
                                    val projectName = doc.safeString("projectName", "")
                                    val priority = doc.safeString("priority", "Medium")
                                    val dueDate = doc.safeString("dueDate", "")
                                    val status = doc.safeString("status", "In Progress")
                                    val isCompleted = doc.safeBoolean("isCompleted", false)
                                    val category = doc.safeString("category", "Work")
                                    val estimatedTimeNeeded = doc.safeString("estimatedTimeNeeded", "4 Hours")
                                    val assignee = doc.safeString("assignee", "Rahul Sharma")

                                    if (id > 0L && title.isNotBlank()) {
                                        val task = TaskEntity(
                                            id = id,
                                            title = title,
                                            projectName = projectName,
                                            priority = priority,
                                            dueDate = dueDate,
                                            status = status,
                                            isCompleted = isCompleted,
                                            category = category,
                                            estimatedTimeNeeded = estimatedTimeNeeded,
                                            assignee = assignee
                                        )
                                        val existing = taskDao.getTaskByIdDirect(id)
                                        taskDao.insert(task)

                                        if (existing != task) {
                                            val isNew = existing == null
                                            val isStatusChanged = existing != null && (existing.status != status || existing.isCompleted != isCompleted)
                                            if (isNew) {
                                                triggerLiveAlertAndNotification(
                                                    title = "📋 New Task Assigned",
                                                    subtitle = "\"$title\" assigned to $assignee for project $projectName",
                                                    category = "task"
                                                )
                                            } else if (isStatusChanged) {
                                                triggerLiveAlertAndNotification(
                                                    title = "🔄 Task Status Changed",
                                                    subtitle = "\"$title\" updated to '$status'",
                                                    category = "task"
                                                )
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Skipping malformed task doc ${doc.id}: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start task listener: ${e.message}")
        }
    }

    private fun startRealtimeProjectListener(projectDao: com.example.data.local.ProjectDao, scope: CoroutineScope) {
        try {
            projectListener?.remove()
            projectListener = firestore?.collection("projects")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime project listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                try {
                                    val id = doc.safeLongOrNull("id")
                                        ?: doc.id.replace("project_", "").toLongOrNull()
                                        ?: 0L
                                    val name = doc.safeString("name", "")
                                    val clientName = doc.safeString("clientName", "")
                                    val totalTasks = doc.safeInt("totalTasks", 10)
                                    val completedTasks = doc.safeInt("completedTasks", 0)
                                    val progressPercent = doc.safeInt("progressPercent", 0)
                                    val status = doc.safeString("status", "Active")
                                    val priority = doc.safeString("priority", "High")
                                    val startDate = doc.safeString("startDate", "01 Sep 2025")
                                    val deadline = doc.safeString("deadline", "28 Sep 2025")
                                    val managerName = doc.safeString("managerName", "Rahul Sharma")
                                    val teamSize = doc.safeInt("teamSize", 5)
                                    val description = doc.safeString("description", "")
                                    val budget = doc.safeLongOrNull("budget")?.toDouble()

                                    if (id > 0L && name.isNotBlank()) {
                                        val project = ProjectEntity(
                                            id = id,
                                            name = name,
                                            clientName = clientName,
                                            totalTasks = totalTasks,
                                            completedTasks = completedTasks,
                                            progressPercent = progressPercent,
                                            status = status,
                                            priority = priority,
                                            startDate = startDate,
                                            deadline = deadline,
                                            managerName = managerName,
                                            teamSize = teamSize,
                                            description = description,
                                            budget = budget
                                        )
                                        projectDao.insert(project)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Skipping malformed project doc ${doc.id}: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start project listener: ${e.message}")
        }
    }

    private var brandingListener: ListenerRegistration? = null

    fun startBrandingConfigListener(context: Context) {
        try {
            brandingListener?.remove()
            brandingListener = firestore?.collection("app_config")?.document("branding")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val logoUrl = snapshot.getString("appLogoUrl") ?: snapshot.getString("appLogoUri")
                    val bgHex = snapshot.getString("bgHexColor") ?: "#0F172A"
                    if (!logoUrl.isNullOrBlank()) {
                        com.example.util.AppPreferences.saveCustomAppLogoUri(context, logoUrl)
                    }
                    if (bgHex.isNotBlank()) {
                        com.example.util.AppPreferences.saveAppIconBgColor(context, bgHex)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start branding config listener: ${e.message}")
        }
    }

    private fun startRealtimeProfileListener(userProfileDao: UserProfileDao, scope: CoroutineScope) {
        try {
            profileListener?.remove()
            profileListener = firestore?.collection("user_profiles")?.document("user_1")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        return@addSnapshotListener
                    }
                    scope.launch(Dispatchers.IO) {
                        try {
                            val name = snapshot.safeString("name", "")
                            val role = snapshot.safeString("role", "Employee")
                            if (name.isBlank()) return@launch
                            val isOnboarded = snapshot.safeBoolean("isOnboarded", true)
                            val updatedAt = snapshot.safeLong("updatedAt", System.currentTimeMillis())

                            val profile = UserProfileEntity(
                                id = 1L,
                                name = name,
                                role = role,
                                isOnboarded = isOnboarded,
                                updatedAt = updatedAt
                            )
                            userProfileDao.insertOrUpdateProfile(profile)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error updating profile from snapshot: ${e.message}")
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start profile listener: ${e.message}")
        }
    }

    fun setCurrentEmployeeName(name: String) {
        if (name.isNotBlank()) {
            currentEmployeeName = name
        }
    }

    // ==========================================
    // NOTIFICATION MODULE FIRESTORE SNAPSHOTS
    // ==========================================

    fun startRealtimeNotificationListener(notificationDao: NotificationDao, scope: CoroutineScope) {
        notificationDaoRef = notificationDao
        if (!isEffectiveOnline()) return
        try {
            notificationListener?.remove()
            notificationListener = attachNotificationSnapshotListener(notificationDao, scope)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start realtime notification listener: ${e.message}")
        }
    }

    fun attachNotificationSnapshotListener(
        notificationDao: NotificationDao,
        scope: CoroutineScope
    ): ListenerRegistration? {
        if (!isEffectiveOnline() || firestore == null) return null
        return try {
            firestore?.collection("notifications")
                ?.orderBy("createdAt", Query.Direction.DESCENDING)
                ?.limit(60)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime notification listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        processNotificationSnapshot(snapshot, notificationDao, scope)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach notification snapshot listener: ${e.message}")
            null
        }
    }

    private fun processNotificationSnapshot(
        snapshot: QuerySnapshot,
        notificationDao: NotificationDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                try {
                    val docId = doc.id
                    val id = doc.safeLongOrNull("id") ?: Math.abs(docId.hashCode().toLong())
                    val title = doc.safeString("title", "")
                    val subtitle = doc.safeString("subtitle", doc.safeString("message", ""))
                    val timeAgo = doc.safeString("timeAgo", "Just now")
                    val category = doc.safeString("category", "broadcast")
                    val isRead = doc.safeBoolean("isRead", false)
                    val targetAudience = doc.safeString("audience", "All Users")
                    val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())

                    if (title.isNotBlank()) {
                        appContext?.let { ctx ->
                            if (com.example.util.AppPreferences.isNotificationEnabled(ctx, category)) {
                                val entity = NotificationEntity(
                                    id = id,
                                    title = title,
                                    subtitle = subtitle,
                                    timeAgo = timeAgo,
                                    category = category,
                                    isRead = isRead
                                )
                                notificationDao.insert(entity)

                                // Alert user if this is a newly arrived notification published recently
                                if (handledNotificationDocIds.add(docId)) {
                                    val isRecent = System.currentTimeMillis() - createdAt < 90_000L
                                    if (isRecent) {
                                        NotificationHelper.showBroadcastAlert(
                                            context = ctx,
                                            title = title,
                                            messageText = subtitle,
                                            audience = targetAudience,
                                            priority = doc.safeString("priority", "High")
                                        )
                                        AppSoundHelper.playGeneralNotificationSound(ctx)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed notification doc ${doc.id}: ${e.message}")
                }
            }
        }
    }

    fun triggerLiveAlertAndNotification(
        title: String,
        subtitle: String,
        category: String,
        targetAudience: String = "All Users"
    ) {
        val ctx = appContext ?: return
        val scope = appScope ?: return
        
        // Check if the specific notification category is enabled in Settings
        if (!com.example.util.AppPreferences.isNotificationEnabled(ctx, category)) {
            Log.d(TAG, "Notification category '$category' is muted in Admin configuration settings.")
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                // 1. Save locally in Room DB
                val entity = NotificationEntity(
                    title = title,
                    subtitle = subtitle,
                    timeAgo = "Just now",
                    category = category,
                    isRead = false
                )
                val insertedId = notificationDaoRef?.insert(entity) ?: 0L
                
                // 2. Sync notification to Firebase so all devices retrieve it
                if (insertedId > 0L) {
                    syncNotificationToFirebase(entity.copy(id = insertedId))
                }

                // 3. Play sound feedback
                AppSoundHelper.playGeneralNotificationSound(ctx)

                // 4. Trigger localized Heads-up Android notifications depending on type
                when (category) {
                    "attendance" -> NotificationHelper.showAttendanceAlert(ctx, title, subtitle)
                    "task" -> NotificationHelper.showTaskAlert(ctx, title, subtitle)
                    "followup" -> NotificationHelper.showLeadAlert(ctx, title, "Lead Update", subtitle)
                    "leave" -> NotificationHelper.showAttendanceAlert(ctx, title, subtitle)
                    "message" -> NotificationHelper.showChatAlert(ctx, "Team", subtitle)
                    else -> NotificationHelper.showBroadcastAlert(ctx, title, subtitle, targetAudience)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error in triggerLiveAlertAndNotification: ${e.message}")
            }
        }
    }

    fun syncNotificationToFirebase(notification: NotificationEntity) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = if (notification.id > 0L) "notif_${notification.id}" else "notif_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to (if (notification.id > 0L) notification.id else System.currentTimeMillis()),
                "title" to notification.title,
                "subtitle" to notification.subtitle,
                "timeAgo" to notification.timeAgo,
                "category" to notification.category,
                "isRead" to notification.isRead,
                "createdAt" to System.currentTimeMillis()
            )
            firestore?.collection("notifications")?.document(docId)?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Notification synced to Firestore '$docId'")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed syncing notification to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing notification: ${e.message}")
        }
    }

    private var globalChatListener: ListenerRegistration? = null

    fun startGlobalChatListener(chatDao: ChatDao, scope: CoroutineScope) {
        if (!isEffectiveOnline()) return
        try {
            globalChatListener?.remove()
            globalChatListener = firestore?.collection("chat_messages")
                ?.orderBy("createdAt", Query.Direction.DESCENDING)
                ?.limit(150)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Global chat listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        processChatSnapshot(snapshot, chatDao, scope)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start global chat listener: ${e.message}")
        }
    }

    fun attachChatSnapshotListener(
        channelId: String,
        chatDao: ChatDao,
        scope: CoroutineScope
    ): ListenerRegistration? {
        if (!isEffectiveOnline()) return null
        return try {
            val channelVariants = listOf(
                channelId,
                channelId.replace("-", "_"),
                channelId.replace("_", "-")
            ).distinct()

            firestore?.collection("chat_messages")
                ?.whereIn("channelId", channelVariants)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Channel snapshot listener error for $channelId: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        processChatSnapshot(snapshot, chatDao, scope, activeChannelId = channelId)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach channel chat snapshot listener: ${e.message}")
            null
        }
    }

    fun startRealtimeChatListener(channelId: String, chatDao: ChatDao, scope: CoroutineScope) {
        if (!isEffectiveOnline()) return
        try {
            chatListener?.remove()
            chatListener = attachChatSnapshotListener(channelId, chatDao, scope)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start chat listener: ${e.message}")
        }
    }

    private fun processChatSnapshot(
        snapshot: QuerySnapshot,
        chatDao: ChatDao,
        scope: CoroutineScope,
        activeChannelId: String? = null
    ) {
        scope.launch(Dispatchers.IO) {
            for (doc in snapshot.documents) {
                try {
                    val msgId = doc.safeLongOrNull("id") ?: doc.id.replace("msg_", "").toLongOrNull() ?: Math.abs(doc.id.hashCode().toLong())
                    val cId = doc.safeString("channelId", activeChannelId ?: "company_chat")
                    val senderName = doc.safeString("senderName", "Team")
                    val senderRole = doc.safeString("senderRole", "Member")
                    val messageText = doc.safeString("messageText", "")
                    val timestampText = doc.safeString("timestampText", "")
                    val isSenderMe = senderName.trim().equals(currentEmployeeName.trim(), ignoreCase = true)
                    val isMe = isSenderMe
                    val attachmentFileName = doc.getString("attachmentFileName")
                    val attachmentFileSize = doc.getString("attachmentFileSize")
                    val audioPath = doc.getString("audioPath")
                    val audioDurationSeconds = doc.safeInt("audioDurationSeconds", 0)
                    val isVoiceMessage = doc.safeBoolean("isVoiceMessage", !audioPath.isNullOrBlank())

                val readByRaw = doc.get("readBy")
                val readByList = when (readByRaw) {
                    is List<*> -> readByRaw.filterIsInstance<String>()
                    is String -> if (readByRaw.isNotBlank()) listOf(readByRaw) else emptyList()
                    else -> emptyList()
                }
                val readByStr = readByList.joinToString(", ")
                val isDocRead = doc.safeBoolean("isRead", readByList.isNotEmpty())
                val readAt = doc.safeLongOrNull("readAt")
                val reactionsJsonFromDoc = doc.safeString("reactionsJson", "")

                // Auto-mark as read in Firestore if recipient has this channel open in real-time
                val isChannelActiveForRecipient = activeChannelId != null && 
                    activeChannelId.replace("-", "_").equals(cId.replace("-", "_"), ignoreCase = true)
                val recipientAlreadyRead = readByList.any { it.equals(currentEmployeeName.trim(), ignoreCase = true) }

                if (isChannelActiveForRecipient && !isSenderMe && (!isDocRead || !recipientAlreadyRead)) {
                    val now = System.currentTimeMillis()
                    doc.reference.update(
                        mapOf(
                            "isRead" to true,
                            "readBy" to com.google.firebase.firestore.FieldValue.arrayUnion(currentEmployeeName.trim()),
                            "readAt" to now
                        )
                    )
                }

                val existingMsg = chatDao.getMessageById(msgId)
                val finalReactionsJson = if (reactionsJsonFromDoc.isNotBlank()) {
                    reactionsJsonFromDoc
                } else {
                    existingMsg?.reactionsJson ?: ""
                }

                chatDao.insert(
                    ChatMessageEntity(
                        id = msgId,
                        channelId = cId,
                        senderName = senderName,
                        senderRole = senderRole,
                        messageText = messageText,
                        timestampText = timestampText,
                        isMe = isMe,
                        attachmentFileName = attachmentFileName,
                        attachmentFileSize = attachmentFileSize,
                        audioPath = audioPath ?: existingMsg?.audioPath,
                        audioDurationSeconds = if (audioDurationSeconds > 0) audioDurationSeconds else (existingMsg?.audioDurationSeconds ?: 0),
                        isVoiceMessage = isVoiceMessage || (existingMsg?.isVoiceMessage == true),
                        isRead = isDocRead || (isChannelActiveForRecipient && !isSenderMe),
                        readBy = if (readByStr.isBlank() && isChannelActiveForRecipient && !isSenderMe) currentEmployeeName.trim() else readByStr,
                        readAt = readAt ?: (if (isChannelActiveForRecipient && !isSenderMe) System.currentTimeMillis() else null),
                        reactionsJson = finalReactionsJson
                    )
                )

                // Real-time subcollection listener for emoji reactions under chat_messages/{docId}/reactions
                if (!messageReactionListeners.containsKey(msgId)) {
                    try {
                        val subListener = doc.reference.collection("reactions")
                            .addSnapshotListener { rSnapshot, rErr ->
                                if (rErr == null && rSnapshot != null) {
                                    val grouped = mutableMapOf<String, MutableList<String>>()
                                    for (rDoc in rSnapshot.documents) {
                                        val emoji = rDoc.getString("emoji") ?: continue
                                        val uName = rDoc.getString("userName") ?: continue
                                        val list = grouped.getOrPut(emoji) { mutableListOf() }
                                        if (!list.any { it.equals(uName.trim(), ignoreCase = true) }) {
                                            list.add(uName.trim())
                                        }
                                    }
                                    val formatted = ReactionUtils.formatReactions(grouped)
                                    scope.launch(Dispatchers.IO) {
                                        chatDao.updateReactions(msgId, formatted)
                                    }
                                }
                            }
                        if (subListener != null) {
                            messageReactionListeners[msgId] = subListener
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Subcollection listener setup error: ${e.message}")
                    }
                }

                // When another employee or admin sends a message, notify this recipient only (not the sender)
                if (!isSenderMe) {
                    val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())
                    if (System.currentTimeMillis() - createdAt < 120000L) {
                        appContext?.let { ctx ->
                            NotificationHelper.showChatAlert(
                                context = ctx,
                                senderName = senderName,
                                messageText = messageText,
                                channelTitle = "Team Chat (#$cId)",
                                channelId = cId
                            )
                        }
                    }
                }
                } catch (e: Exception) {
                    Log.w(TAG, "Skipping malformed chat doc ${doc.id}: ${e.message}")
                }
            }
        }
    }

    fun syncChatMessageToFirebase(
        channelId: String,
        senderName: String,
        senderRole: String,
        messageText: String,
        timestampText: String,
        attachmentFileName: String? = null,
        attachmentFileSize: String? = null,
        messageId: Long? = null,
        audioPath: String? = null,
        audioDurationSeconds: Int = 0,
        isVoiceMessage: Boolean = false
    ) {
        val finalId = messageId ?: System.currentTimeMillis()
        val chatEntity = ChatMessageEntity(
            id = finalId,
            channelId = channelId,
            senderName = senderName,
            senderRole = senderRole,
            messageText = messageText,
            timestampText = timestampText,
            isMe = true,
            attachmentFileName = attachmentFileName,
            attachmentFileSize = attachmentFileSize,
            audioPath = audioPath,
            audioDurationSeconds = audioDurationSeconds,
            isVoiceMessage = isVoiceMessage,
            isSynced = false
        )

        // 1. Instant WebSocket broadcast across active clients
        try {
            RealtimeWebSocketManager.broadcastChatMessage(
                messageId = finalId,
                channelId = channelId,
                senderName = senderName,
                senderRole = senderRole,
                messageText = messageText,
                timestampText = timestampText,
                attachmentFileName = attachmentFileName,
                attachmentFileSize = attachmentFileSize,
                audioPath = audioPath,
                audioDurationSeconds = audioDurationSeconds,
                isVoiceMessage = isVoiceMessage
            )
        } catch (we: Exception) {
            Log.w(TAG, "WebSocket chat broadcast warning: ${we.message}")
        }

        if (!isEffectiveOnline()) {
            pendingChatMessagesQueue.removeIf { it.id == finalId }
            pendingChatMessagesQueue.add(chatEntity)
            val totalPending = pendingLeadsQueue.size + pendingTasksQueue.size + pendingAttendanceQueue.size + pendingChatMessagesQueue.size
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                statusMessage = "Offline: Message cached in Room ($totalPending pending)"
            )
            return
        }

        try {
            val docId = "msg_$finalId"
            val data = hashMapOf<String, Any?>(
                "id" to finalId,
                "channelId" to channelId,
                "senderName" to senderName,
                "senderRole" to senderRole,
                "messageText" to messageText,
                "timestampText" to timestampText,
                "isMe" to false, // Evaluated on receiver
                "attachmentFileName" to attachmentFileName,
                "attachmentFileSize" to attachmentFileSize,
                "audioPath" to audioPath,
                "audioDurationSeconds" to audioDurationSeconds,
                "isVoiceMessage" to isVoiceMessage,
                "createdAt" to System.currentTimeMillis(),
                "isRead" to false,
                "readBy" to emptyList<String>(),
                "readAt" to null
            )
            firestore?.collection("chat_messages")?.document(docId)?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    pendingChatMessagesQueue.removeIf { it.id == finalId }
                    appScope?.launch(Dispatchers.IO) {
                        chatDaoRef?.markMessageAsSynced(finalId)
                    }
                    Log.d(TAG, "Successfully synced chat message $docId to Firestore and marked in Room")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed syncing chat message to Firestore: ${e.message}")
                    pendingChatMessagesQueue.removeIf { it.id == finalId }
                    pendingChatMessagesQueue.add(chatEntity)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing chat message: ${e.message}")
            pendingChatMessagesQueue.removeIf { it.id == finalId }
            pendingChatMessagesQueue.add(chatEntity)
        }
    }

    fun updateChatMessageAudioPathInFirebase(messageId: Long, audioPath: String) {
        if (!isEffectiveOnline()) return
        try {
            val docId = "msg_$messageId"
            firestore?.collection("chat_messages")?.document(docId)?.update(
                mapOf(
                    "audioPath" to audioPath,
                    "isVoiceMessage" to true
                )
            )?.addOnSuccessListener {
                Log.d(TAG, "Successfully updated audioPath in Firestore for $docId")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update audioPath in Firestore: ${e.message}")
        }
    }

    fun markChannelMessagesAsReadInFirebase(
        channelId: String,
        recipientName: String,
        chatDao: ChatDao?,
        scope: CoroutineScope
    ) {
        if (!isEffectiveOnline() || recipientName.isBlank()) return
        try {
            val channelVariants = listOf(
                channelId,
                channelId.replace("-", "_"),
                channelId.replace("_", "-")
            ).distinct()

            firestore?.collection("chat_messages")
                ?.whereIn("channelId", channelVariants)
                ?.get()
                ?.addOnSuccessListener { snapshot ->
                    if (snapshot == null || snapshot.isEmpty) return@addOnSuccessListener
                    val now = System.currentTimeMillis()
                    val batch = firestore?.batch()
                    var updatedCount = 0

                    for (doc in snapshot.documents) {
                        val senderName = doc.getString("senderName") ?: ""
                        // Only mark as read if it was sent by someone else
                        if (!senderName.trim().equals(recipientName.trim(), ignoreCase = true)) {
                            val readByList = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                            val isAlreadyReadByMe = readByList.any { it.equals(recipientName.trim(), ignoreCase = true) }
                            val isDocRead = doc.safeBoolean("isRead", false)

                            if (!isDocRead || !isAlreadyReadByMe) {
                                batch?.update(
                                    doc.reference,
                                    mapOf(
                                        "isRead" to true,
                                        "readBy" to com.google.firebase.firestore.FieldValue.arrayUnion(recipientName.trim()),
                                        "readAt" to now
                                    )
                                )
                                updatedCount++
                            }
                        }
                    }

                    if (updatedCount > 0 && batch != null) {
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Marked $updatedCount messages as read in Firestore for $channelId by $recipientName")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Failed committing batch read status: ${e.message}")
                            }
                    }

                    if (chatDao != null) {
                        scope.launch(Dispatchers.IO) {
                            chatDao.markChannelMessagesAsRead(channelId, recipientName.trim(), now)
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to query channel messages to mark as read: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed marking channel messages as read: ${e.message}")
        }
    }

    fun markMessageAsReadInFirebase(
        messageId: Long,
        recipientName: String,
        chatDao: ChatDao?,
        scope: CoroutineScope
    ) {
        if (!isEffectiveOnline() || recipientName.isBlank()) return
        try {
            val docId = "msg_$messageId"
            val now = System.currentTimeMillis()
            firestore?.collection("chat_messages")?.document(docId)
                ?.update(
                    mapOf(
                        "isRead" to true,
                        "readBy" to com.google.firebase.firestore.FieldValue.arrayUnion(recipientName.trim()),
                        "readAt" to now
                    )
                )
                ?.addOnSuccessListener {
                    Log.d(TAG, "Message $messageId marked as read in Firestore")
                }
                ?.addOnFailureListener {
                    // Fallback query if docId was randomized
                    firestore?.collection("chat_messages")
                        ?.whereEqualTo("id", messageId)
                        ?.limit(1)
                        ?.get()
                        ?.addOnSuccessListener { snap ->
                            val doc = snap?.documents?.firstOrNull()
                            doc?.reference?.update(
                                mapOf(
                                    "isRead" to true,
                                    "readBy" to com.google.firebase.firestore.FieldValue.arrayUnion(recipientName.trim()),
                                    "readAt" to now
                                )
                            )
                        }
                }

            if (chatDao != null) {
                scope.launch(Dispatchers.IO) {
                    chatDao.markMessageAsRead(messageId, recipientName.trim(), now)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark message as read: ${e.message}")
        }
    }

    fun setTypingStatus(
        channelId: String,
        userName: String,
        isTyping: Boolean
    ) {
        if (!isEffectiveOnline() || userName.isBlank()) return
        try {
            val userKey = userName.trim().lowercase().replace(" ", "_")
            val cId = channelId.trim().lowercase().replace("-", "_")
            val docId = "${cId}_$userKey"
            val data = hashMapOf(
                "channelId" to channelId,
                "normalizedChannelId" to cId,
                "userName" to userName.trim(),
                "isTyping" to isTyping,
                "lastTypedAt" to System.currentTimeMillis()
            )
            firestore?.collection("typing_status")?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Error updating typing status: ${e.message}")
        }
    }

    fun attachTypingStatusListener(
        channelId: String,
        currentUserName: String,
        onTypingUsersChanged: (List<String>) -> Unit
    ): ListenerRegistration? {
        if (!isEffectiveOnline()) return null
        return try {
            val cId = channelId.trim().lowercase().replace("-", "_")
            firestore?.collection("typing_status")
                ?.whereEqualTo("normalizedChannelId", cId)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Typing status listen error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val now = System.currentTimeMillis()
                        val typingList = snapshot.documents.mapNotNull { doc ->
                            val isTyping = doc.safeBoolean("isTyping", false)
                            val uName = doc.safeString("userName", "")
                            val lastTyped = doc.safeLong("lastTypedAt", 0L)
                            val isSelf = uName.trim().equals(currentUserName.trim(), ignoreCase = true)

                            // Active if typing within last 6 seconds and not self
                            if (isTyping && !isSelf && uName.isNotBlank() && (now - lastTyped < 6000L)) {
                                uName
                            } else {
                                null
                            }
                        }.distinct()
                        onTypingUsersChanged(typingList)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach typing listener: ${e.message}")
            null
        }
    }

    /**
     * Toggles an emoji reaction on a message:
     * 1. Updates local Room DB immediately
     * 2. Broadcasts instantaneous WebSocket event
     * 3. Persists in Firestore subcollection under chat_messages/{docId}/reactions/{reactionDocId}
     *    and updates parent reactionsJson summary
     */
    fun toggleEmojiReactionInFirebase(
        messageId: Long,
        emoji: String,
        userName: String,
        userRole: String,
        channelId: String,
        chatDao: ChatDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            val existing = chatDao.getMessageById(messageId)
            val currentJson = existing?.reactionsJson ?: ""
            val (updatedJson, isAdded) = ReactionUtils.toggleUserReaction(currentJson, emoji, userName)

            // 1. Immediate local DB update
            chatDao.updateReactions(messageId, updatedJson)

            // 2. Immediate WebSocket broadcast to all active users
            try {
                RealtimeWebSocketManager.broadcastEmojiReaction(
                    messageId = messageId,
                    emoji = emoji,
                    userName = userName,
                    userRole = userRole,
                    channelId = channelId,
                    isAdded = isAdded
                )
            } catch (e: Exception) {
                Log.w(TAG, "WebSocket emoji broadcast warning: ${e.message}")
            }

            // 3. Persist in Firestore subcollection under each message: chat_messages/msg_$messageId/reactions/{reactionId}
            if (isEffectiveOnline() && firestore != null) {
                try {
                    val docId = "msg_$messageId"
                    val cleanUser = userName.trim().replace(Regex("[^a-zA-Z0-9_]"), "_").lowercase()
                    val reactionDocId = "${cleanUser}_${emoji.hashCode()}"
                    val reactionRef = firestore?.collection("chat_messages")
                        ?.document(docId)
                        ?.collection("reactions")
                        ?.document(reactionDocId)

                    if (isAdded) {
                        val reactionData = hashMapOf(
                            "emoji" to emoji,
                            "userName" to userName.trim(),
                            "userRole" to userRole,
                            "messageId" to messageId,
                            "channelId" to channelId,
                            "timestamp" to System.currentTimeMillis()
                        )
                        reactionRef?.set(reactionData, SetOptions.merge())
                    } else {
                        reactionRef?.delete()
                    }

                    // Update parent message reactionsJson summary
                    firestore?.collection("chat_messages")
                        ?.document(docId)
                        ?.update("reactionsJson", updatedJson)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing reaction to Firestore subcollection: ${e.message}")
                }
            }
        }
    }

    /**
     * Attaches a real-time Firestore listener for dynamic user-generated feeds and announcements.
     */
    fun startRealtimeFeedListener(activityFeedDao: ActivityFeedDao, scope: CoroutineScope) {
        if (!isEffectiveOnline()) return
        try {
            feedListener?.remove()
            feedListener = firestore?.collection("activity_feed_items")
                ?.orderBy("createdAt", Query.Direction.DESCENDING)
                ?.limit(60)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.safeLongOrNull("id")
                                    ?: doc.id.replace("feed_", "").toLongOrNull()
                                    ?: Math.abs(doc.id.hashCode().toLong())
                                val item = ActivityFeedItemEntity(
                                    id = if (id != 0L) id else Math.abs(doc.id.hashCode().toLong()),
                                    authorName = doc.safeString("authorName", "Team Member"),
                                    authorRole = doc.safeString("authorRole", "Colleague"),
                                    title = doc.safeString("title", ""),
                                    content = doc.safeString("content", ""),
                                    category = doc.safeString("category", "Update"),
                                    likesCount = doc.safeInt("likesCount", 0),
                                    likedByUsers = doc.safeString("likedByUsers", ""),
                                    reactionsJson = doc.safeString("reactionsJson", ""),
                                    commentsCount = doc.safeInt("commentsCount", 0),
                                    isPinned = doc.safeBoolean("isPinned", false),
                                    timestampText = doc.safeString("timestampText", "Just now"),
                                    createdAt = doc.safeLong("createdAt", System.currentTimeMillis())
                                )
                                activityFeedDao.insert(item)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error parsing feed doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start realtime feed listener: ${e.message}")
        }
    }

    /**
     * Publishes a dynamic user-generated feed post:
     * 1. Persists locally in Room DB
     * 2. Broadcasts via WebSocket
     * 3. Syncs to Firestore collection
     */
    fun syncFeedItemToFirebase(
        item: ActivityFeedItemEntity,
        activityFeedDao: ActivityFeedDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            activityFeedDao.insert(item)

            // Broadcast via WebSocket
            try {
                RealtimeWebSocketManager.broadcastFeedPost(
                    feedId = item.id,
                    authorName = item.authorName,
                    authorRole = item.authorRole,
                    title = item.title,
                    content = item.content,
                    category = item.category,
                    timestampText = item.timestampText
                )
            } catch (e: Exception) {
                Log.w(TAG, "WebSocket feed broadcast warning: ${e.message}")
            }

            if (isEffectiveOnline() && firestore != null) {
                try {
                    val docId = "feed_${item.id}"
                    val data = hashMapOf(
                        "id" to item.id,
                        "authorName" to item.authorName,
                        "authorRole" to item.authorRole,
                        "title" to item.title,
                        "content" to item.content,
                        "category" to item.category,
                        "likesCount" to item.likesCount,
                        "likedByUsers" to item.likedByUsers,
                        "reactionsJson" to item.reactionsJson,
                        "commentsCount" to item.commentsCount,
                        "isPinned" to item.isPinned,
                        "timestampText" to item.timestampText,
                        "createdAt" to item.createdAt
                    )
                    firestore?.collection("activity_feed_items")?.document(docId)?.set(data, SetOptions.merge())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing feed item to Firestore: ${e.message}")
                }
            }
        }
    }

    /**
     * Toggles a like on a dynamic feed item with real-time WebSocket + Firestore sync.
     */
    fun toggleFeedLikeInFirebase(
        feedId: Long,
        userName: String,
        currentLikesCount: Int,
        currentLikedBy: String,
        activityFeedDao: ActivityFeedDao,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            val list = if (currentLikedBy.isBlank()) mutableListOf() else currentLikedBy.split(", ").filter { it.isNotBlank() }.toMutableList()
            val alreadyLiked = list.any { it.equals(userName.trim(), ignoreCase = true) }
            if (alreadyLiked) {
                list.removeAll { it.equals(userName.trim(), ignoreCase = true) }
            } else {
                list.add(userName.trim())
            }
            val newLikesCount = list.size
            val newLikedBy = list.joinToString(", ")

            // 1. Local DB update
            activityFeedDao.updateLikes(feedId, newLikesCount, newLikedBy)

            // 2. WebSocket broadcast
            try {
                RealtimeWebSocketManager.broadcastFeedLike(feedId, userName, newLikesCount)
            } catch (e: Exception) {
                Log.w(TAG, "WebSocket feed like broadcast warning: ${e.message}")
            }

            // 3. Firestore sync
            if (isEffectiveOnline() && firestore != null) {
                try {
                    val docId = "feed_$feedId"
                    firestore?.collection("activity_feed_items")?.document(docId)?.update(
                        mapOf(
                            "likesCount" to newLikesCount,
                            "likedByUsers" to newLikedBy
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update feed like in Firestore: ${e.message}")
                }
            }
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T? =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result, null)
            }
            addOnFailureListener { exception ->
                Log.w(TAG, "Task failed: ${exception.message}")
                if (continuation.isActive) continuation.resume(null, null)
            }
        }

    suspend fun refreshAllFromFirestore(
        taskDao: TaskDao,
        attendanceDao: AttendanceDao,
        leadDao: LeadDao,
        employeeDao: EmployeeDao,
        projectDao: com.example.data.local.ProjectDao? = null
    ) = withContext(Dispatchers.IO) {
        if (!isEffectiveOnline()) {
            updateSyncState(
                status = NetworkSyncStatus.OFFLINE,
                isOnline = false,
                isSyncing = false,
                statusMessage = "Cannot refresh while Offline"
            )
            return@withContext
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCING,
            isOnline = true,
            isSyncing = true,
            statusMessage = "Refreshing latest updates from Firestore..."
        )

        // 1. Flush any pending outgoing sync queue first
        try {
            processPendingSyncQueue()
        } catch (e: Exception) {
            Log.w(TAG, "Error flushing queue during refresh: ${e.message}")
        }

        // 2. Fetch latest tasks from Firestore collection
        try {
            val tasksSnapshot = firestore?.collection("tasks")?.get(Source.DEFAULT)?.awaitTask()
            if (tasksSnapshot != null && !tasksSnapshot.isEmpty) {
                for (doc in tasksSnapshot.documents) {
                    try {
                        val id = doc.safeLongOrNull("id") ?: doc.id.replace("task_", "").toLongOrNull() ?: 0L
                        val title = doc.safeString("title", "")
                        val projectName = doc.safeString("projectName", "")
                        val priority = doc.safeString("priority", "Medium")
                        val dueDate = doc.safeString("dueDate", "")
                        val status = doc.safeString("status", "In Progress")
                        val isCompleted = doc.safeBoolean("isCompleted", false)
                        val category = doc.safeString("category", "Work")
                        val estimatedTimeNeeded = doc.safeString("estimatedTimeNeeded", "4 Hours")
                        val assignee = doc.safeString("assignee", "Rahul Sharma")

                        if (id > 0L && title.isNotBlank()) {
                            val task = TaskEntity(
                                id = id,
                                title = title,
                                projectName = projectName,
                                priority = priority,
                                dueDate = dueDate,
                                status = status,
                                isCompleted = isCompleted,
                                category = category,
                                estimatedTimeNeeded = estimatedTimeNeeded,
                                assignee = assignee
                            )
                            taskDao.insert(task)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed task doc ${doc.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Refresh tasks failed: ${e.message}")
        }

        // 3. Fetch latest attendance records from Firestore
        try {
            val attSnapshot = firestore?.collection("attendance_records")?.get(Source.DEFAULT)?.awaitTask()
            if (attSnapshot != null && !attSnapshot.isEmpty) {
                for (doc in attSnapshot.documents) {
                    try {
                        val id = doc.safeLongOrNull("id") ?: doc.id.replace("att_", "").toLongOrNull() ?: 0L
                        val date = doc.safeString("date", "")
                        val checkInTime = doc.safeString("checkInTime", "")
                        val checkOutTime = doc.getString("checkOutTime")
                        val durationMinutes = doc.safeLong("durationMinutes", 0L)
                        val isWorking = doc.safeBoolean("isWorking", false)
                        val status = doc.safeString("status", "Present")
                        val overtimeMinutes = doc.safeLong("overtimeMinutes", 0L)
                        val timestamp = doc.safeLong("timestamp", System.currentTimeMillis())
                        val employeeName = doc.safeString("employeeName", "Rahul Sharma")

                        if (id > 0L) {
                            val record = AttendanceRecord(
                                id = id,
                                date = date,
                                checkInTime = checkInTime,
                                checkOutTime = checkOutTime,
                                durationMinutes = durationMinutes,
                                isWorking = isWorking,
                                status = status,
                                overtimeMinutes = overtimeMinutes,
                                timestamp = timestamp,
                                employeeName = employeeName
                            )
                            attendanceDao.insert(record)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed att doc ${doc.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Refresh attendance failed: ${e.message}")
        }

        // 4. Fetch latest leads from Firestore
        try {
            val leadsSnapshot = firestore?.collection("leads")?.get(Source.DEFAULT)?.awaitTask()
            if (leadsSnapshot != null && !leadsSnapshot.isEmpty) {
                for (doc in leadsSnapshot.documents) {
                    try {
                        val id = doc.safeLongOrNull("id") ?: doc.id.replace("lead_", "").toLongOrNull() ?: 0L
                        val name = doc.safeString("customerName", "")
                        val company = doc.safeString("company", "")
                        val phone = doc.safeString("phone", "")
                        val email = doc.safeString("email", "")
                        val leadScore = doc.safeInt("leadScore", 70)
                        val requirement = doc.safeString("requirement", "")
                        val value = doc.safeLongOrNull("value")?.toDouble() ?: 0.0
                        val status = doc.safeString("status", "New")
                        val assignedTo = doc.safeString("assignedTo", "Rahul Sharma")
                        val nextFollowUp = doc.safeString("nextFollowUp", "")

                        if (id > 0L && name.isNotBlank()) {
                            val lead = LeadEntity(
                                id = id,
                                name = name,
                                company = company,
                                phone = phone,
                                email = email,
                                leadScore = leadScore,
                                requirement = requirement,
                                potentialValue = "₹" + String.format(java.util.Locale.US, "%,.0f", value),
                                stage = status,
                                assignedTo = assignedTo,
                                nextFollowUp = nextFollowUp
                            )
                            leadDao.insert(lead)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping malformed lead doc ${doc.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Refresh leads failed: ${e.message}")
        }

        // 5. Fetch latest projects from Firestore
        if (projectDao != null) {
            try {
                val projSnapshot = firestore?.collection("projects")?.get(Source.DEFAULT)?.awaitTask()
                if (projSnapshot != null && !projSnapshot.isEmpty) {
                    for (doc in projSnapshot.documents) {
                        try {
                            val id = doc.safeLongOrNull("id") ?: doc.id.replace("project_", "").toLongOrNull() ?: 0L
                            val name = doc.safeString("name", "")
                            val clientName = doc.safeString("clientName", "")
                            val totalTasks = doc.safeInt("totalTasks", 10)
                            val completedTasks = doc.safeInt("completedTasks", 0)
                            val progressPercent = doc.safeInt("progressPercent", 0)
                            val status = doc.safeString("status", "Active")
                            val priority = doc.safeString("priority", "High")
                            val startDate = doc.safeString("startDate", "01 Sep 2025")
                            val deadline = doc.safeString("deadline", "28 Sep 2025")
                            val managerName = doc.safeString("managerName", "Rahul Sharma")
                            val teamSize = doc.safeInt("teamSize", 5)
                            val description = doc.safeString("description", "")
                            val budget = doc.safeLongOrNull("budget")?.toDouble()

                            if (id > 0L && name.isNotBlank()) {
                                val project = ProjectEntity(
                                    id = id,
                                    name = name,
                                    clientName = clientName,
                                    totalTasks = totalTasks,
                                    completedTasks = completedTasks,
                                    progressPercent = progressPercent,
                                    status = status,
                                    priority = priority,
                                    startDate = startDate,
                                    deadline = deadline,
                                    managerName = managerName,
                                    teamSize = teamSize,
                                    description = description,
                                    budget = budget
                                )
                                projectDao.insert(project)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping malformed project doc ${doc.id}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Refresh projects failed: ${e.message}")
            }
        }

        updateSyncState(
            status = NetworkSyncStatus.SYNCED,
            isOnline = true,
            isSyncing = false,
            statusMessage = "Firestore: Live & Synced Just Now",
            newTimestamp = System.currentTimeMillis()
        )
    }

    // --- LEADS REAL-TIME LISTENER ---
    private var leadListener: ListenerRegistration? = null

    fun startRealtimeLeadListener(leadDao: LeadDao, scope: CoroutineScope) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            leadListener?.remove()
            leadListener = firestore?.collection("leads")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.safeLongOrNull("id")
                                    ?: doc.id.replace("lead_", "").toLongOrNull()
                                    ?: Math.abs(doc.id.hashCode().toLong())
                                val name = doc.safeString("customerName", "")
                                val company = doc.safeString("company", "")
                                val phone = doc.safeString("phone", "")
                                val email = doc.safeString("email", "")
                                val leadScore = doc.safeInt("leadScore", 70)
                                val requirement = doc.safeString("requirement", "")
                                val stage = doc.safeString("status", "New")
                                val assignedTo = doc.safeString("assignedTo", "")
                                val nextFollowUp = doc.safeString("nextFollowUp", "")
                                
                                val isNew = leadDao.getLeadByIdDirect(id) == null
                                val potVal = doc.safeDouble("value", 0.0).toLong()
                                
                                val lead = LeadEntity(
                                    id = id,
                                    name = name,
                                    company = company,
                                    phone = phone,
                                    email = email,
                                    leadScore = leadScore,
                                    requirement = requirement,
                                    potentialValue = "₹ $potVal",
                                    stage = stage,
                                    assignedTo = assignedTo,
                                    nextFollowUp = nextFollowUp
                                )
                                leadDao.insert(lead)
                                
                                if (isNew && name.isNotBlank()) {
                                    triggerLiveAlertAndNotification(
                                        title = "🎯 New Lead: $name ($company)",
                                        subtitle = "Requirement: $requirement. Assigned to: $assignedTo",
                                        category = "followup"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing lead doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start lead listener: ${e.message}")
        }
    }

    // --- LEAVES REAL-TIME LISTENER ---
    private var leaveListener: ListenerRegistration? = null

    fun startRealtimeLeaveListener(leaveDao: LeaveDao, scope: CoroutineScope) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            leaveListener?.remove()
            leaveListener = firestore?.collection("leave_applications")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.safeLongOrNull("id")
                                    ?: doc.id.replace("leave_", "").toLongOrNull()
                                    ?: Math.abs(doc.id.hashCode().toLong())
                                val username = doc.safeString("username", "")
                                val leaveType = doc.safeString("leaveType", "Casual Leave")
                                val startDate = doc.safeString("startDate", "")
                                val endDate = doc.safeString("endDate", "")
                                val totalDays = doc.safeInt("totalDays", 1)
                                val reason = doc.safeString("reason", "")
                                val status = doc.safeString("status", "Pending")
                                val appliedDate = doc.safeString("appliedDate", "")
                                val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())
                                
                                val existing = leaveDao.getLeaveByIdDirect(id)
                                val isNew = existing == null
                                val isStatusChanged = existing != null && existing.status != status
                                
                                val leave = LeaveApplicationEntity(
                                    id = id,
                                    username = username,
                                    leaveType = leaveType,
                                    startDate = startDate,
                                    endDate = endDate,
                                    totalDays = totalDays,
                                    reason = reason,
                                    status = status,
                                    appliedDate = appliedDate,
                                    createdAt = createdAt
                                )
                                leaveDao.insert(leave)
                                
                                if (isNew && username.isNotBlank()) {
                                    triggerLiveAlertAndNotification(
                                        title = "📅 New Leave Request",
                                        subtitle = "$leaveType submitted by $username for $startDate to $endDate",
                                        category = "leave",
                                        targetAudience = "Management"
                                    )
                                } else if (isStatusChanged) {
                                    triggerLiveAlertAndNotification(
                                        title = "🔄 Leave Status Updated",
                                        subtitle = "Leave for $username has been $status",
                                        category = "leave",
                                        targetAudience = "Employee"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing leave doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start leave listener: ${e.message}")
        }
    }

    // --- HOLIDAYS REAL-TIME LISTENER ---
    private var holidayListener: ListenerRegistration? = null
    var onHolidaysUpdatedCallback: ((List<HolidayItem>) -> Unit)? = null

    fun startRealtimeHolidayListener(scope: CoroutineScope) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            holidayListener?.remove()
            holidayListener = firestore?.collection("holidays")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = mutableListOf<HolidayItem>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.safeLongOrNull("id")
                                ?: doc.id.replace("holiday_", "").toLongOrNull()
                                ?: Math.abs(doc.id.hashCode().toLong())
                            val title = doc.safeString("title", "")
                            val date = doc.safeString("date", "")
                            val day = doc.safeString("day", "")
                            val type = doc.safeString("type", "Public Holiday")
                            list.add(HolidayItem(id = id, title = title, date = date, day = day, type = type))
                        } catch (e: Exception) {
                            Log.w(TAG, "Error processing holiday doc ${doc.id}: ${e.message}")
                        }
                    }
                    if (list.isNotEmpty()) {
                        onHolidaysUpdatedCallback?.invoke(list)
                        appContext?.let { ctx ->
                            AppSoundHelper.playGeneralNotificationSound(ctx)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start holiday listener: ${e.message}")
        }
    }

    // --- HOLIDAYS & LEAVES SYNC AND DELETE HELPERS ---
    fun syncLeaveToFirebase(leave: LeaveApplicationEntity) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = if (leave.id > 0L) "leave_${leave.id}" else "leave_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to (if (leave.id > 0L) leave.id else System.currentTimeMillis()),
                "username" to leave.username,
                "leaveType" to leave.leaveType,
                "startDate" to leave.startDate,
                "endDate" to leave.endDate,
                "totalDays" to leave.totalDays,
                "reason" to leave.reason,
                "status" to leave.status,
                "appliedDate" to leave.appliedDate,
                "createdAt" to leave.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("leave_applications")?.document(docId)?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Leave Application synced to Firestore '$docId'")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed syncing leave application: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error in syncLeaveToFirebase: ${e.message}")
        }
    }

    fun deleteLeaveFromFirebase(leaveId: Long) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "leave_$leaveId"
            firestore?.collection("leave_applications")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting leave from Firebase: ${e.message}")
        }
    }

    fun syncHolidayToFirebase(holiday: HolidayItem) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "holiday_${holiday.id}"
            val data = hashMapOf(
                "id" to holiday.id,
                "title" to holiday.title,
                "date" to holiday.date,
                "day" to holiday.day,
                "type" to holiday.type,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("holidays")?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing holiday to Firebase: ${e.message}")
        }
    }

    fun deleteHolidayFromFirebase(holidayId: Long) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "holiday_$holidayId"
            firestore?.collection("holidays")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting holiday from Firebase: ${e.message}")
        }
    }

    // --- AUTOMATED SHIFT SCHEDULES & WORK ROTATIONS REAL-TIME SYNC ---
    private var shiftListener: ListenerRegistration? = null

    fun startRealtimeShiftScheduleListener(shiftDao: ShiftDao, scope: CoroutineScope) {
        if (shiftListener != null || firestore == null) return
        try {
            shiftListener = firestore?.collection("shift_schedules")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Shift schedules listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        scope.launch(Dispatchers.IO) {
                            for (doc in snapshot.documents) {
                                try {
                                    val id = doc.safeLong("id", System.currentTimeMillis())
                                    val employeeName = doc.safeString("employeeName", "Arjun Mehta")
                                    val employeeRole = doc.safeString("employeeRole", "Operations Specialist")
                                    val shiftType = doc.safeString("shiftType", "Morning Shift")
                                    val shiftCode = doc.safeString("shiftCode", "MOR-01")
                                    val startTime = doc.safeString("startTime", "08:00 AM")
                                    val endTime = doc.safeString("endTime", "06:00 PM")
                                    val date = doc.safeString("date", "2026-10-07")
                                    val dayOfWeek = doc.safeString("dayOfWeek", "Wednesday")
                                    val location = doc.safeString("location", "Main HQ - Floor 3")
                                    val rotationCycleName = doc.safeString("rotationCycleName", "Q4 Rotation Cycle - Week 41")
                                    val status = doc.safeString("status", "Scheduled")
                                    val notes = doc.safeString("notes", "Standard shift assignment")
                                    val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())

                                    val shift = ShiftScheduleEntity(
                                        id = id,
                                        employeeName = employeeName,
                                        employeeRole = employeeRole,
                                        shiftType = shiftType,
                                        shiftCode = shiftCode,
                                        startTime = startTime,
                                        endTime = endTime,
                                        date = date,
                                        dayOfWeek = dayOfWeek,
                                        location = location,
                                        rotationCycleName = rotationCycleName,
                                        status = status,
                                        notes = notes,
                                        createdAt = createdAt
                                    )
                                    shiftDao.insert(shift)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error processing shift doc ${doc.id}: ${e.message}")
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start shift schedule listener: ${e.message}")
        }
    }

    fun syncShiftScheduleToFirebase(shift: ShiftScheduleEntity) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = if (shift.id > 0L) "shift_${shift.id}" else "shift_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to (if (shift.id > 0L) shift.id else System.currentTimeMillis()),
                "employeeName" to shift.employeeName,
                "employeeRole" to shift.employeeRole,
                "shiftType" to shift.shiftType,
                "shiftCode" to shift.shiftCode,
                "startTime" to shift.startTime,
                "endTime" to shift.endTime,
                "date" to shift.date,
                "dayOfWeek" to shift.dayOfWeek,
                "location" to shift.location,
                "rotationCycleName" to shift.rotationCycleName,
                "status" to shift.status,
                "notes" to shift.notes,
                "createdAt" to shift.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("shift_schedules")?.document(docId)?.set(data, SetOptions.merge())
                ?.addOnSuccessListener {
                    Log.d(TAG, "Shift schedule synced to Firestore '$docId'")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing shift schedule to Firebase: ${e.message}")
        }
    }

    fun deleteShiftScheduleFromFirebase(shiftId: Long) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "shift_$shiftId"
            firestore?.collection("shift_schedules")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting shift schedule from Firebase: ${e.message}")
        }
    }

    // --- INVOICES REAL-TIME LISTENER & SYNC ---
    private var invoiceListener: ListenerRegistration? = null

    fun startRealtimeInvoiceListener(invoiceDao: com.example.data.local.InvoiceDao, scope: CoroutineScope) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            invoiceListener?.remove()
            invoiceListener = firestore?.collection("invoices")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.safeLongOrNull("id")
                                    ?: doc.id.replace("inv_", "").toLongOrNull()
                                    ?: Math.abs(doc.id.hashCode().toLong())
                                val invoiceNumber = doc.safeString("invoiceNumber", "INV-${doc.id}")
                                val clientName = doc.safeString("clientName", "")
                                val clientCompany = doc.safeString("clientCompany", "")
                                val clientEmail = doc.safeString("clientEmail", "")
                                val clientPhone = doc.safeString("clientPhone", "")
                                val issueDate = doc.safeString("issueDate", "")
                                val dueDate = doc.safeString("dueDate", "")
                                val currency = doc.safeString("currency", "₹")
                                val subtotal = doc.safeDouble("subtotal", 0.0)
                                val taxPercent = doc.safeDouble("taxPercent", 18.0)
                                val totalAmount = doc.safeDouble("totalAmount", subtotal * 1.18)
                                val status = doc.safeString("status", "Pending")
                                val itemsSummary = doc.safeString("itemsSummary", "Services Deliverable")
                                val notes = doc.safeString("notes", "Payment terms: Net 15 days.")
                                val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())

                                val invoice = InvoiceEntity(
                                    id = id,
                                    invoiceNumber = invoiceNumber,
                                    clientName = clientName,
                                    clientCompany = clientCompany,
                                    clientEmail = clientEmail,
                                    clientPhone = clientPhone,
                                    issueDate = issueDate,
                                    dueDate = dueDate,
                                    currency = currency,
                                    subtotal = subtotal,
                                    taxPercent = taxPercent,
                                    totalAmount = totalAmount,
                                    status = status,
                                    itemsSummary = itemsSummary,
                                    notes = notes,
                                    createdAt = createdAt
                                )
                                invoiceDao.insert(invoice)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing invoice doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start invoice listener: ${e.message}")
        }
    }

    fun syncInvoiceToFirebase(invoice: InvoiceEntity) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = if (invoice.id > 0L) "inv_${invoice.id}" else "inv_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to (if (invoice.id > 0L) invoice.id else System.currentTimeMillis()),
                "invoiceNumber" to invoice.invoiceNumber,
                "clientName" to invoice.clientName,
                "clientCompany" to invoice.clientCompany,
                "clientEmail" to invoice.clientEmail,
                "clientPhone" to invoice.clientPhone,
                "issueDate" to invoice.issueDate,
                "dueDate" to invoice.dueDate,
                "currency" to invoice.currency,
                "subtotal" to invoice.subtotal,
                "taxPercent" to invoice.taxPercent,
                "totalAmount" to invoice.totalAmount,
                "status" to invoice.status,
                "itemsSummary" to invoice.itemsSummary,
                "notes" to invoice.notes,
                "createdAt" to invoice.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("invoices")?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing invoice to Firebase: ${e.message}")
        }
    }

    fun deleteInvoiceFromFirebase(invoiceId: Long) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "inv_$invoiceId"
            firestore?.collection("invoices")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting invoice from Firebase: ${e.message}")
        }
    }

    // --- QUOTATIONS REAL-TIME LISTENER & SYNC ---
    private var quotationListener: ListenerRegistration? = null

    fun startRealtimeQuotationListener(quotationDao: com.example.data.local.QuotationDao, scope: CoroutineScope) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            quotationListener?.remove()
            quotationListener = firestore?.collection("quotations")
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    scope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            try {
                                val id = doc.safeLongOrNull("id")
                                    ?: doc.id.replace("qt_", "").toLongOrNull()
                                    ?: Math.abs(doc.id.hashCode().toLong())
                                val quotationNumber = doc.safeString("quotationNumber", "QT-${doc.id}")
                                val clientName = doc.safeString("clientName", "")
                                val clientCompany = doc.safeString("clientCompany", "")
                                val clientEmail = doc.safeString("clientEmail", "")
                                val clientPhone = doc.safeString("clientPhone", "")
                                val issueDate = doc.safeString("issueDate", "")
                                val validUntil = doc.safeString("validUntil", "")
                                val currency = doc.safeString("currency", "₹")
                                val subtotal = doc.safeDouble("subtotal", 0.0)
                                val discountPercent = doc.safeDouble("discountPercent", 0.0)
                                val taxPercent = doc.safeDouble("taxPercent", 18.0)
                                val totalAmount = doc.safeDouble("totalAmount", subtotal * 1.18)
                                val status = doc.safeString("status", "Sent")
                                val scopeOfWork = doc.safeString("scopeOfWork", "Custom Application Suite")
                                val termsAndConditions = doc.safeString("termsAndConditions", "50% Advance on kickoff, 50% on milestone.")
                                val createdAt = doc.safeLong("createdAt", System.currentTimeMillis())

                                val quotation = QuotationEntity(
                                    id = id,
                                    quotationNumber = quotationNumber,
                                    clientName = clientName,
                                    clientCompany = clientCompany,
                                    clientEmail = clientEmail,
                                    clientPhone = clientPhone,
                                    issueDate = issueDate,
                                    validUntil = validUntil,
                                    currency = currency,
                                    subtotal = subtotal,
                                    discountPercent = discountPercent,
                                    taxPercent = taxPercent,
                                    totalAmount = totalAmount,
                                    status = status,
                                    scopeOfWork = scopeOfWork,
                                    termsAndConditions = termsAndConditions,
                                    createdAt = createdAt
                                )
                                quotationDao.insert(quotation)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error processing quotation doc ${doc.id}: ${e.message}")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start quotation listener: ${e.message}")
        }
    }

    fun syncQuotationToFirebase(quotation: QuotationEntity) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = if (quotation.id > 0L) "qt_${quotation.id}" else "qt_${System.currentTimeMillis()}"
            val data = hashMapOf(
                "id" to (if (quotation.id > 0L) quotation.id else System.currentTimeMillis()),
                "quotationNumber" to quotation.quotationNumber,
                "clientName" to quotation.clientName,
                "clientCompany" to quotation.clientCompany,
                "clientEmail" to quotation.clientEmail,
                "clientPhone" to quotation.clientPhone,
                "issueDate" to quotation.issueDate,
                "validUntil" to quotation.validUntil,
                "currency" to quotation.currency,
                "subtotal" to quotation.subtotal,
                "discountPercent" to quotation.discountPercent,
                "taxPercent" to quotation.taxPercent,
                "totalAmount" to quotation.totalAmount,
                "status" to quotation.status,
                "scopeOfWork" to quotation.scopeOfWork,
                "termsAndConditions" to quotation.termsAndConditions,
                "createdAt" to quotation.createdAt,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("quotations")?.document(docId)?.set(data, SetOptions.merge())
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing quotation to Firebase: ${e.message}")
        }
    }

    fun deleteQuotationFromFirebase(quotationId: Long) {
        if (!isEffectiveOnline() || firestore == null) return
        try {
            val docId = "qt_$quotationId"
            firestore?.collection("quotations")?.document(docId)?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting quotation from Firebase: ${e.message}")
        }
    }

    private suspend fun seedInitialFirestoreDatabase() {
        val fs = firestore ?: return
        try {
            // 1. App Configuration & Telemetry Metadata
            val configDoc = fs.collection("app_config").document("general")
            val configSnapshot = configDoc.get().awaitTask()
            if (configSnapshot == null || !configSnapshot.exists()) {
                val appConfig = hashMapOf(
                    "appName" to "MB Taker",
                    "appSubTitle" to "Enterprise Suite & Smart Operations",
                    "version" to "1.0.0",
                    "databaseReady" to true,
                    "realtimeEnabled" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                configDoc.set(appConfig, SetOptions.merge()).awaitTask()
                Log.d(TAG, "Seeded initial app_config to Firestore")
            }

            // 2. Initial Welcome Activity Feed
            val feedCol = fs.collection("activity_feed_items")
            val feedSnapshot = feedCol.limit(1).get().awaitTask()
            if (feedSnapshot == null || feedSnapshot.isEmpty) {
                val welcomeFeed = hashMapOf(
                    "id" to 1L,
                    "authorName" to "MB Operations",
                    "authorRole" to "Admin",
                    "title" to "🎉 Welcome to MB Taker Enterprise",
                    "content" to "All enterprise tasks, real-time attendance, leads, and company updates are now seamlessly synchronized with Firestore.",
                    "category" to "announcement",
                    "timestampText" to "Just now",
                    "likesCount" to 5,
                    "createdAt" to System.currentTimeMillis()
                )
                feedCol.document("feed_welcome_1").set(welcomeFeed, SetOptions.merge()).awaitTask()
                Log.d(TAG, "Seeded welcome activity feed item to Firestore")
            }

            // 3. Initial Welcome Push Broadcast Announcement
            val broadcastCol = fs.collection("push_broadcasts")
            val broadcastSnapshot = broadcastCol.limit(1).get().awaitTask()
            if (broadcastSnapshot == null || broadcastSnapshot.isEmpty) {
                val welcomeBroadcast = hashMapOf(
                    "id" to "broadcast_init_1",
                    "title" to "🚀 Enterprise Database Connected",
                    "message" to "Cloud Firestore database connection is active. All employee workflows and admin telemetry are live.",
                    "audience" to "All Users",
                    "priority" to "High",
                    "category" to "announcement",
                    "timestamp" to System.currentTimeMillis(),
                    "senderName" to "System"
                )
                broadcastCol.document("broadcast_init_1").set(welcomeBroadcast, SetOptions.merge()).awaitTask()
                Log.d(TAG, "Seeded welcome push broadcast to Firestore")
            }

            // 4. Initial Holidays Schedule (if empty)
            val holidaysCol = fs.collection("holidays")
            val holidaysSnapshot = holidaysCol.limit(1).get().awaitTask()
            if (holidaysSnapshot == null || holidaysSnapshot.isEmpty) {
                val initialHolidays = listOf(
                    mapOf("id" to 1L, "title" to "Republic Day", "date" to "26 Jan 2026", "day" to "Monday", "type" to "National Holiday"),
                    mapOf("id" to 2L, "title" to "Holi", "date" to "14 Mar 2026", "day" to "Saturday", "type" to "Festival"),
                    mapOf("id" to 3L, "title" to "Independence Day", "date" to "15 Aug 2026", "day" to "Saturday", "type" to "National Holiday"),
                    mapOf("id" to 4L, "title" to "Gandhi Jayanti", "date" to "02 Oct 2026", "day" to "Friday", "type" to "National Holiday"),
                    mapOf("id" to 5L, "title" to "Diwali", "date" to "08 Nov 2026", "day" to "Sunday", "type" to "Festival"),
                    mapOf("id" to 6L, "title" to "Christmas", "date" to "25 Dec 2026", "day" to "Friday", "type" to "Festival")
                )
                for (h in initialHolidays) {
                    holidaysCol.document("holiday_${h["id"]}").set(h, SetOptions.merge()).awaitTask()
                }
                Log.d(TAG, "Seeded corporate holidays to Firestore")
            }
        } catch (e: Exception) {
            Log.w(TAG, "seedInitialFirestoreDatabase notice: ${e.message}")
        }
    }
}
