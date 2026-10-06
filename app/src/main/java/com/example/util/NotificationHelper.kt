package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {

    const val CHANNEL_TEAM_CHAT = "team_chat_channel"
    const val CHANNEL_TASK_UPDATES = "task_updates_channel"
    const val CHANNEL_LEAD_ALERTS = "lead_alerts_channel"
    const val CHANNEL_ATTENDANCE_ALERTS = "attendance_alerts_channel"
    const val CHANNEL_AUTH_ALERTS = "auth_security_channel"
    const val CHANNEL_BROADCAST = "admin_broadcast_channel"
    const val CHANNEL_CALL_LOGS = "call_logs_channel"

    fun createNotificationChannels(context: Context) {
        NotificationSoundManager.registerNotificationChannels(context)
    }

    private fun getMiloLargeIcon(context: Context) = try {
        BitmapFactory.decodeResource(context.resources, R.drawable.milo_lion)
    } catch (_: Exception) {
        null
    }

    fun showBroadcastAlert(
        context: Context,
        title: String,
        messageText: String,
        audience: String = "All Users",
        priority: String = "High"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "notifications")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_BROADCAST)
            .setSmallIcon(R.drawable.ic_stat_milo_general)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("📢 $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("[$audience] $messageText"))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(if (priority.equals("urgent", ignoreCase = true) || priority.equals("critical", ignoreCase = true)) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 5000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playGeneralNotificationSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showChatAlert(
        context: Context,
        senderName: String,
        messageText: String,
        channelTitle: String = "Team Chat",
        channelId: String = "company_chat"
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            putExtra("channelId", channelId)
            putExtra("channelTitle", channelTitle)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chatSoundUri = NotificationSoundManager.getChatSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_TEAM_CHAT)
            .setSmallIcon(R.drawable.ic_stat_milo_chat)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("💬 $senderName ($channelTitle)")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(chatSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 1000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playChatNotificationSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showTaskAlert(
        context: Context,
        title: String,
        messageText: String,
        taskId: Long = 0L
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "tasks")
            putExtra("taskId", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val taskSoundUri = NotificationSoundManager.getTaskSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_TASK_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_milo_task)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("⚡ $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(taskSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 2000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playTaskCompletedSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showLeadAlert(
        context: Context,
        leadName: String,
        company: String,
        requirement: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "crm")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val leadSoundUri = NotificationSoundManager.getLeadSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_LEAD_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_milo_lead)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("🎯 New Lead: $leadName ($company)")
            .setContentText("Requirement: $requirement")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Requirement: $requirement\nAuto-brochure ready for dispatch."))
            .setAutoCancel(true)
            .setSound(leadSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 3000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playLeadAddedSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showAttendanceAlert(
        context: Context,
        title: String,
        messageText: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "attendance")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val attendanceSoundUri = NotificationSoundManager.getAttendanceSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ATTENDANCE_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_milo_attendance)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("⏰ $title")
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setAutoCancel(true)
            .setSound(attendanceSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 6000
            notificationManager.notify(notificationId, notificationBuilder.build())
            AppSoundHelper.playGeneralNotificationSound(context)
        } catch (_: SecurityException) {
        }
    }

    fun showOtpAlert(
        context: Context,
        otpCode: String,
        phone: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "otp")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_AUTH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_milo_general)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle("🔐 MB Traker OTP: $otpCode")
            .setContentText("Your WhatsApp login code is $otpCode for $phone.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("One-Time Passcode for MB Traker sign-in is $otpCode. Valid for 10 minutes."))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = 9999
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showCallAlert(
        context: Context,
        contactName: String,
        callType: String,
        durationText: String,
        phoneNumber: String
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "call_tracker")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = when (callType.lowercase()) {
            "missed" -> "🔴 Missed Call: $contactName"
            "incoming" -> "📲 Incoming Call Logged: $contactName"
            else -> "📞 Outgoing Call Logged: $contactName"
        }

        val subtitle = "$callType Call • $durationText • $phoneNumber"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_CALL_LOGS)
            .setSmallIcon(R.drawable.ic_stat_milo_general)
            .setLargeIcon(getMiloLargeIcon(context))
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Contact: $contactName\nNumber: $phoneNumber\nType: $callType\nDuration: $durationText\nSaved to CRM Call Tracker."))
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 4000
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 1001
    ) {
        showBroadcastAlert(
            context = context,
            title = title,
            messageText = message,
            audience = "Milo Assistant",
            priority = "High"
        )
    }

    fun clearCategoryNotifications(context: Context, category: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Cancel specific ID ranges corresponding to categories
            when (category.lowercase()) {
                "task", "tasks" -> {
                    for (id in 2000..12000) {
                        notificationManager.cancel(id)
                    }
                }
                "chat", "message", "team_chat" -> {
                    for (id in 1000..11000) {
                        notificationManager.cancel(id)
                    }
                }
                "crm", "lead", "followup" -> {
                    for (id in 3000..13000) {
                        notificationManager.cancel(id)
                    }
                }
                "attendance", "leave" -> {
                    for (id in 6000..16000) {
                        notificationManager.cancel(id)
                    }
                }
                "broadcast", "announcement" -> {
                    for (id in 5000..15000) {
                        notificationManager.cancel(id)
                    }
                }
                else -> {
                    notificationManager.cancelAll()
                }
            }

            // 2. On API 23+, clear active notifications by channel or ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val targetChannel = when (category.lowercase()) {
                    "task", "tasks" -> CHANNEL_TASK_UPDATES
                    "chat", "message", "team_chat" -> CHANNEL_TEAM_CHAT
                    "crm", "lead", "followup" -> CHANNEL_LEAD_ALERTS
                    "attendance", "leave" -> CHANNEL_ATTENDANCE_ALERTS
                    "broadcast", "announcement" -> CHANNEL_BROADCAST
                    else -> null
                }
                val activeNotifs = notificationManager.activeNotifications
                activeNotifs.forEach { statusBarNotif ->
                    if (targetChannel != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (statusBarNotif.notification.channelId == targetChannel) {
                                notificationManager.cancel(statusBarNotif.tag, statusBarNotif.id)
                                notificationManager.cancel(statusBarNotif.id)
                            }
                        } else {
                            notificationManager.cancel(statusBarNotif.tag, statusBarNotif.id)
                            notificationManager.cancel(statusBarNotif.id)
                        }
                    } else if (category.lowercase() == "all") {
                        notificationManager.cancel(statusBarNotif.tag, statusBarNotif.id)
                        notificationManager.cancel(statusBarNotif.id)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
