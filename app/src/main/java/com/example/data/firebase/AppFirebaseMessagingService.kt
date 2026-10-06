package com.example.data.firebase

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.ChatMessageEntity
import com.example.data.model.NotificationEntity
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SERVICE", "Refreshed FCM Device Token: $token")
        FirebaseRealtimeManager.updateFcmToken(token)
        try {
            FcmBroadcastManager.getInstance(applicationContext).registerTokenInFirestore(token)
        } catch (_: Exception) {
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_SERVICE", "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notif = remoteMessage.notification

        val type = data["type"] ?: data["category"] ?: "general"
        val title = data["title"] ?: notif?.title ?: "Team Alert"
        val body = data["body"] ?: data["message"] ?: notif?.body ?: "You have a new update"
        val senderName = data["sender_name"] ?: "Team Member"
        val senderRole = data["sender_role"] ?: "Colleague"
        val channelId = data["channel_id"] ?: "company_chat"
        val channelTitle = data["channel_title"] ?: "Team Chat"
        val taskId = data["task_id"]?.toLongOrNull() ?: 0L
        val audience = data["audience"] ?: "All Users"
        val priority = data["priority"] ?: "High"

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)

                // 1. Insert into in-app notifications
                db.notificationDao().insert(
                    NotificationEntity(
                        title = title,
                        subtitle = if (type == "broadcast" || type == "announcement") "[$audience] $body" else body,
                        timeAgo = "Just now",
                        category = when (type) {
                            "chat" -> "chat"
                            "broadcast", "announcement" -> "broadcast"
                            else -> "task"
                        },
                        isRead = false
                    )
                )

                // 2. Post appropriate background alert
                when (type) {
                    "chat" -> {
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        db.chatDao().insert(
                            ChatMessageEntity(
                                channelId = channelId,
                                senderName = senderName,
                                senderRole = senderRole,
                                messageText = body,
                                timestampText = timeFormat.format(Date()),
                                isMe = false
                            )
                        )

                        NotificationHelper.showChatAlert(
                            context = applicationContext,
                            senderName = senderName,
                            messageText = body,
                            channelTitle = channelTitle,
                            channelId = channelId
                        )
                    }
                    "lead", "crm", "followup" -> {
                        NotificationHelper.showLeadAlert(
                            context = applicationContext,
                            leadName = title,
                            company = senderName,
                            requirement = body
                        )
                    }
                    "attendance", "leave", "roster" -> {
                        NotificationHelper.showAttendanceAlert(
                            context = applicationContext,
                            title = title,
                            messageText = body
                        )
                    }
                    "broadcast", "announcement" -> {
                        NotificationHelper.showBroadcastAlert(
                            context = applicationContext,
                            title = title,
                            messageText = body,
                            audience = audience,
                            priority = priority
                        )
                    }
                    else -> {
                        NotificationHelper.showTaskAlert(
                            context = applicationContext,
                            title = title,
                            messageText = body,
                            taskId = taskId
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FCM_SERVICE", "Error processing FCM notification payload", e)
            }
        }
    }
}
