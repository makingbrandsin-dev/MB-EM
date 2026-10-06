package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.R

/**
 * NotificationSoundManager: Custom Notification Sound Manager utilizing the Android
 * NotificationChannel API to register specific audio assets (WAV/OGG) for different
 * notification types (task, chat, lead, attendance).
 *
 * Guarantees native background playback handled directly by Android OS system notification services.
 */
object NotificationSoundManager {

    private const val TAG = "NotificationSoundMgr"

    // Channel IDs
    const val CHANNEL_TEAM_CHAT = "team_chat_channel"
    const val CHANNEL_TASK_UPDATES = "task_updates_channel"
    const val CHANNEL_LEAD_ALERTS = "lead_alerts_channel"
    const val CHANNEL_ATTENDANCE_ALERTS = "attendance_alerts_channel"
    const val CHANNEL_AUTH_ALERTS = "auth_security_channel"
    const val CHANNEL_BROADCAST = "admin_broadcast_channel"
    const val CHANNEL_CALL_LOGS = "call_logs_channel"

    /**
     * Resolves the android.resource:// URI for a raw audio asset.
     */
    fun getRawResourceUri(context: Context, resId: Int): Uri {
        return Uri.parse("android.resource://${context.packageName}/$resId")
    }

    fun getTaskSoundUri(context: Context): Uri = getRawResourceUri(context, R.raw.sound_task)
    fun getChatSoundUri(context: Context): Uri = getRawResourceUri(context, R.raw.sound_chat)
    fun getLeadSoundUri(context: Context): Uri = getRawResourceUri(context, R.raw.sound_lead)
    fun getAttendanceSoundUri(context: Context): Uri = getRawResourceUri(context, R.raw.sound_attendance)

    /**
     * Build standard AudioAttributes for notification sonification.
     */
    fun getNotificationAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
    }

    /**
     * Registers specific Notification Channels with distinct custom audio assets for background playback.
     */
    fun registerNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val audioAttributes = getNotificationAudioAttributes()
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val taskSoundUri = getTaskSoundUri(context)
            val chatSoundUri = getChatSoundUri(context)
            val leadSoundUri = getLeadSoundUri(context)
            val attendanceSoundUri = getAttendanceSoundUri(context)

            // 1. Task & Sprint Updates Channel (custom audio asset: sound_task.wav)
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_UPDATES,
                "Task & Sprint Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task assignments, status changes, and sprint deadline alerts with custom chime"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(taskSoundUri, audioAttributes)
            }

            // 2. Team Chat & Mentions Channel (custom audio asset: sound_chat.wav)
            val chatChannel = NotificationChannel(
                CHANNEL_TEAM_CHAT,
                "Team Chat & Mentions",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming team messages, mentions, and project discussions with crisp ping"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(chatSoundUri, audioAttributes)
            }

            // 3. Lead & CRM Alerts Channel (custom audio asset: sound_lead.wav)
            val leadChannel = NotificationChannel(
                CHANNEL_LEAD_ALERTS,
                "Lead & CRM Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "New lead assignments, brochures, and follow-ups with triumph ding"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(leadSoundUri, audioAttributes)
            }

            // 4. Attendance & Work Schedule Channel (custom audio asset: sound_attendance.wav)
            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE_ALERTS,
                "Attendance & Work Schedule",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Employee check-ins, check-outs, leave requests, and roster updates"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(attendanceSoundUri, audioAttributes)
            }

            // 5. Admin Broadcast & Push Announcements Channel
            val broadcastChannel = NotificationChannel(
                CHANNEL_BROADCAST,
                "Admin Push Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Company-wide announcements, admin broadcast alerts, and urgent notices"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 6. Security & OTP Channel
            val authChannel = NotificationChannel(
                CHANNEL_AUTH_ALERTS,
                "Security & WhatsApp OTP",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One-time passcodes and login security verification alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 7. Call Logging Channel
            val callChannel = NotificationChannel(
                CHANNEL_CALL_LOGS,
                "Call Logs & Recording Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Auto-logged incoming & outgoing phone calls and CRM client sync"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(broadcastChannel)
            notificationManager.createNotificationChannel(chatChannel)
            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(leadChannel)
            notificationManager.createNotificationChannel(attendanceChannel)
            notificationManager.createNotificationChannel(authChannel)
            notificationManager.createNotificationChannel(callChannel)

            Log.d(TAG, "Notification Channels registered with custom audio assets successfully!")
        }
    }

    /**
     * Plays a live audio preview of a specific notification raw audio asset (e.g. for settings testing).
     */
    fun previewSoundAsset(context: Context, rawResId: Int) {
        try {
            val uri = getRawResourceUri(context, rawResId)
            val mediaPlayer = MediaPlayer.create(context.applicationContext, uri)
            mediaPlayer?.apply {
                setOnCompletionListener { mp -> mp.release() }
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play sound asset preview: ${e.message}", e)
        }
    }
}
