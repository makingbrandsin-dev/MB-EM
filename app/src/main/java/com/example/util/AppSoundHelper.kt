package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * AppSoundHelper: Provides distinct acoustic feedback for key business actions:
 * - Task completed (celebratory chime)
 * - Project done (triumphant completion chime)
 * - Added new lead (positive success ding)
 * - Chat notification (crisp message ping)
 * - General alerts
 */
object AppSoundHelper {

    private const val TAG = "AppSoundHelper"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun playTaskCompletedSound(context: Context) {
        playToneSequence(
            tones = listOf(
                ToneGenerator.TONE_PROP_BEEP to 80,
                ToneGenerator.TONE_PROP_ACK to 140
            ),
            fallbackType = RingtoneManager.TYPE_NOTIFICATION,
            context = context
        )
    }

    fun playProjectDoneSound(context: Context) {
        playToneSequence(
            tones = listOf(
                ToneGenerator.TONE_PROP_BEEP to 80,
                ToneGenerator.TONE_PROP_BEEP2 to 120,
                ToneGenerator.TONE_PROP_ACK to 220
            ),
            fallbackType = RingtoneManager.TYPE_NOTIFICATION,
            context = context
        )
    }

    fun playLeadAddedSound(context: Context) {
        playToneSequence(
            tones = listOf(
                ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE to 90,
                ToneGenerator.TONE_PROP_ACK to 160
            ),
            fallbackType = RingtoneManager.TYPE_NOTIFICATION,
            context = context
        )
    }

    fun playChatNotificationSound(context: Context) {
        playToneSequence(
            tones = listOf(
                ToneGenerator.TONE_PROP_BEEP to 70
            ),
            fallbackType = RingtoneManager.TYPE_NOTIFICATION,
            context = context
        )
    }

    fun playGeneralNotificationSound(context: Context) {
        if (!com.example.util.AppPreferences.isNotificationSoundEnabled(context)) return
        playSystemNotificationSound(context)
    }

    private fun playToneSequence(
        tones: List<Pair<Int, Int>>,
        fallbackType: Int,
        context: Context
    ) {
        if (!com.example.util.AppPreferences.isNotificationSoundEnabled(context)) return
        Thread {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                for ((index, tonePair) in tones.withIndex()) {
                    toneGen.startTone(tonePair.first, tonePair.second)
                    Thread.sleep(tonePair.second.toLong() + 40L)
                }
                toneGen.release()
            } catch (e: Exception) {
                Log.d(TAG, "ToneGenerator unavailable, playing default ringtone: ${e.message}")
                mainHandler.post {
                    playSystemNotificationSound(context, fallbackType)
                }
            }
        }.start()
    }

    private fun playSystemNotificationSound(context: Context, type: Int = RingtoneManager.TYPE_NOTIFICATION) {
        if (!com.example.util.AppPreferences.isNotificationSoundEnabled(context)) return
        try {
            val uri = RingtoneManager.getDefaultUri(type)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ringtone sound", e)
        }
    }
}
