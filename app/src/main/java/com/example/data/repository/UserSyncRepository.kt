package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.firebase.FirestoreDbProvider
import com.example.data.local.AppDatabase
import com.example.data.model.NotificationEntity
import com.example.data.model.TaskEntity
import com.example.util.AppSoundHelper
import com.example.util.NotificationHelper
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Real-time Admin Broadcast model representing live broadcasts pushed from the admin panel.
 */
data class AdminBroadcast(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val message: String = "",
    val audience: String = "All Users",
    val priority: String = "High",
    val category: String = "announcement",
    val topic: String = "all_users",
    val actionRoute: String = "notifications",
    val senderName: String = "MB Admin",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date()),
    val isRead: Boolean = false
)

/**
 * Step 1: Real-Time Listener Repository (UserSyncRepository.kt)
 * Maintains persistent open snapshot listeners (`addSnapshotListener`) with Firestore so that
 * any admin write (e.g. from 'admin_broadcasts' or shared 'tasks') triggers an immediate, live UI refresh
 * across all connected user devices without requiring app restarts or manual reloads.
 */
class UserSyncRepository(
    private val context: Context,
    private val appDatabase: AppDatabase? = null
) {
    companion object {
        private const val TAG = "UserSyncRepository"
        const val COLLECTION_ADMIN_BROADCASTS = "admin_broadcasts"
        const val COLLECTION_PUSH_BROADCASTS = "PushBroadcasts"
        const val COLLECTION_SHARED_TASKS = "tasks"

        @Volatile
        private var INSTANCE: UserSyncRepository? = null

        fun getInstance(context: Context, database: AppDatabase? = null): UserSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSyncRepository(
                    context.applicationContext,
                    database ?: AppDatabase.getDatabase(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handledBroadcastIds = HashSet<String>()
    private val appInitTime = System.currentTimeMillis() - 5000 // Grace margin for recent pushes

    private val firestore: FirebaseFirestore
        get() = FirestoreDbProvider.getFirestore(context)

    /**
     * Listens for real-time updates from the 'admin_broadcasts' (and 'PushBroadcasts') collections.
     * Uses [addSnapshotListener] wrapped inside a reactive Kotlin [callbackFlow].
     * Whenever the admin posts or modifies a broadcast, Firestore instantly triggers this callback,
     * emitting the updated list to all active ViewModel collectors.
     */
    fun getAdminBroadcastsLive(): Flow<List<AdminBroadcast>> = callbackFlow {
        Log.d(TAG, "Opening real-time snapshot listener on '$COLLECTION_ADMIN_BROADCASTS'...")

        var adminListener: ListenerRegistration? = null
        var pushListener: ListenerRegistration? = null

        val currentAdminItems = mutableMapOf<String, AdminBroadcast>()
        val currentPushItems = mutableMapOf<String, AdminBroadcast>()

        fun emitCombined() {
            val merged = (currentAdminItems.values + currentPushItems.values)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
            trySend(merged)
        }

        try {
            // 1. Primary Real-time listener for 'admin_broadcasts'
            adminListener = firestore.collection(COLLECTION_ADMIN_BROADCASTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in '$COLLECTION_ADMIN_BROADCASTS' snapshot listener: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        currentAdminItems.clear()
                        for (doc in snapshot.documents) {
                            val item = parseBroadcast(doc)
                            if (item != null) {
                                currentAdminItems[item.id] = item
                                checkAndTriggerLiveAlert(item)
                            }
                        }
                        emitCombined()
                    }
                }

            // 2. Secondary Real-time listener for 'PushBroadcasts' (cross-system compatibility)
            pushListener = firestore.collection(COLLECTION_PUSH_BROADCASTS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in '$COLLECTION_PUSH_BROADCASTS' snapshot listener: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        currentPushItems.clear()
                        for (doc in snapshot.documents) {
                            val item = parseBroadcast(doc)
                            if (item != null) {
                                currentPushItems[item.id] = item
                                checkAndTriggerLiveAlert(item)
                            }
                        }
                        emitCombined()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach admin_broadcasts snapshot listeners: ${e.message}", e)
        }

        awaitClose {
            Log.d(TAG, "Releasing real-time snapshot listeners for admin broadcasts")
            adminListener?.remove()
            pushListener?.remove()
        }
    }

    /**
     * Listens for real-time updates from the 'tasks' collection.
     * Any task created, assigned, or updated by the admin triggers this listener immediately.
     */
    fun getSharedTasksLive(): Flow<List<TaskEntity>> = callbackFlow {
        Log.d(TAG, "Opening real-time snapshot listener on '$COLLECTION_SHARED_TASKS'...")

        var taskListener: ListenerRegistration? = null

        try {
            taskListener = firestore.collection(COLLECTION_SHARED_TASKS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error in '$COLLECTION_SHARED_TASKS' snapshot listener: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val tasks = mutableListOf<TaskEntity>()
                        for (doc in snapshot.documents) {
                            val task = parseTask(doc)
                            if (task != null) {
                                tasks.add(task)
                            }
                        }
                        trySend(tasks)

                        // Synchronize to local Room database in background so offline and sub-screens reflect changes
                        syncTasksToLocal(tasks)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach shared tasks snapshot listener: ${e.message}", e)
        }

        awaitClose {
            Log.d(TAG, "Releasing real-time snapshot listener for shared tasks")
            taskListener?.remove()
        }
    }

    /**
     * Publishes a live broadcast from the admin panel to Firestore.
     * Writes simultaneously to 'admin_broadcasts' and 'PushBroadcasts'.
     * Triggers snapshot listeners on all active user devices in real time.
     */
    suspend fun sendBroadcast(broadcast: AdminBroadcast): Result<Unit> {
        return try {
            val payload = hashMapOf(
                "id" to broadcast.id,
                "title" to broadcast.title,
                "message" to broadcast.message,
                "body" to broadcast.message,
                "audience" to broadcast.audience,
                "priority" to broadcast.priority,
                "category" to broadcast.category,
                "topic" to broadcast.topic,
                "actionRoute" to broadcast.actionRoute,
                "senderName" to broadcast.senderName,
                "timestamp" to broadcast.timestamp,
                "formattedDate" to broadcast.formattedDate,
                "isRead" to broadcast.isRead,
                "createdAt" to FieldValue.serverTimestamp()
            )

            // Write to 'admin_broadcasts'
            firestore.collection(COLLECTION_ADMIN_BROADCASTS)
                .document(broadcast.id)
                .set(payload, SetOptions.merge())
                .await()

            // Also write to 'PushBroadcasts' for unified dispatching
            firestore.collection(COLLECTION_PUSH_BROADCASTS)
                .document(broadcast.id)
                .set(payload, SetOptions.merge())
                .await()

            Log.d(TAG, "Admin broadcast successfully dispatched to Firestore: ${broadcast.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch admin broadcast to Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deletes a broadcast from Firestore by ID.
     */
    suspend fun deleteBroadcast(broadcastId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_ADMIN_BROADCASTS).document(broadcastId).delete().await()
            firestore.collection(COLLECTION_PUSH_BROADCASTS).document(broadcastId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete broadcast $broadcastId: ${e.message}")
            Result.failure(e)
        }
    }

    private fun parseBroadcast(doc: DocumentSnapshot): AdminBroadcast? {
        return try {
            val id = doc.getString("id") ?: doc.id
            val title = doc.getString("title") ?: return null
            val message = doc.getString("message") ?: doc.getString("body") ?: ""
            val audience = doc.getString("audience") ?: "All Users"
            val priority = doc.getString("priority") ?: "High"
            val category = doc.getString("category") ?: "announcement"
            val topic = doc.getString("topic") ?: "all_users"
            val actionRoute = doc.getString("actionRoute") ?: "notifications"
            val senderName = doc.getString("senderName") ?: "MB Admin"
            val timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis()
            val formattedDate = doc.getString("formattedDate")
                ?: SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(timestamp))
            val isRead = doc.getBoolean("isRead") ?: false

            AdminBroadcast(
                id = id,
                title = title,
                message = message,
                audience = audience,
                priority = priority,
                category = category,
                topic = topic,
                actionRoute = actionRoute,
                senderName = senderName,
                timestamp = timestamp,
                formattedDate = formattedDate,
                isRead = isRead
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse admin broadcast doc: ${e.message}")
            null
        }
    }

    private fun parseTask(doc: DocumentSnapshot): TaskEntity? {
        return try {
            val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.hashCode().toLong().let { if (it < 0) -it else it }
            val projectId = (doc.get("projectId") as? Number)?.toLong() ?: 1L
            val projectName = doc.getString("projectName") ?: "Project Task"
            val title = doc.getString("title") ?: return null
            val dueDate = doc.getString("dueDate") ?: "Today"
            val priority = doc.getString("priority") ?: "Medium"
            val status = doc.getString("status") ?: "Backlog"
            val isCompleted = doc.getBoolean("isCompleted") ?: false
            val assignee = doc.getString("assignee") ?: "Unassigned"
            val category = doc.getString("category") ?: "Work"
            val estimatedTimeNeeded = doc.getString("estimatedTimeNeeded") ?: "2 Hours"

            TaskEntity(
                id = id,
                projectId = projectId,
                projectName = projectName,
                title = title,
                dueDate = dueDate,
                priority = priority,
                status = status,
                isCompleted = isCompleted,
                assignee = assignee,
                category = category,
                estimatedTimeNeeded = estimatedTimeNeeded
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse task doc: ${e.message}")
            null
        }
    }

    private fun checkAndTriggerLiveAlert(broadcast: AdminBroadcast) {
        if (broadcast.timestamp > appInitTime && handledBroadcastIds.add(broadcast.id)) {
            // Live broadcast pushed by admin while app is running!
            scope.launch {
                try {
                    // 1. Play alert sound
                    AppSoundHelper.playGeneralNotificationSound(context)
                    // 2. Show in-app system notification alert
                    NotificationHelper.showBroadcastAlert(
                        context = context,
                        title = broadcast.title,
                        messageText = "[${broadcast.audience}] ${broadcast.message}",
                        audience = broadcast.audience,
                        priority = broadcast.priority
                    )
                    // 3. Save to local notifications table
                    saveToLocalNotifications(broadcast)
                } catch (e: Exception) {
                    Log.w(TAG, "Live alert notification fallback: ${e.message}")
                }
            }
        } else if (handledBroadcastIds.add(broadcast.id)) {
            // Sync to local notifications silently
            scope.launch {
                saveToLocalNotifications(broadcast)
            }
        }
    }

    private suspend fun saveToLocalNotifications(broadcast: AdminBroadcast) {
        try {
            val db = appDatabase ?: AppDatabase.getDatabase(context)
            val dao = db.notificationDao()
            val existingCount = dao.countNotificationByTitle(broadcast.title)
            if (existingCount == 0) {
                dao.insert(
                    NotificationEntity(
                        title = broadcast.title,
                        subtitle = "[${broadcast.audience}] ${broadcast.message}",
                        timeAgo = broadcast.formattedDate.ifBlank { "Just now" },
                        category = "broadcast",
                        isRead = broadcast.isRead
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache notification locally: ${e.message}")
        }
    }

    private fun syncTasksToLocal(tasks: List<TaskEntity>) {
        if (tasks.isEmpty()) return
        scope.launch {
            try {
                val db = appDatabase ?: AppDatabase.getDatabase(context)
                val taskDao = db.taskDao()
                taskDao.insertAll(tasks)
                Log.d(TAG, "Synced ${tasks.size} shared tasks from Firestore to local Room")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to sync tasks into local Room: ${e.message}")
            }
        }
    }
}
