package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * MiloHaptics manages tactile feedback patterns for the Milo AI Companion.
 * Uses hardware Vibrator / VibrationEffect when available, with Compose HapticFeedback fallback.
 */
object MiloHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Tactile feedback when tapping a button or action
     */
    fun performButtonClick(context: Context, hapticFeedback: HapticFeedback? = null) {
        performButtonTap(context, hapticFeedback)
    }

    /**
     * Tactile feedback when tapping the Floating Ask Milo Button or Milo mascot
     */
    fun performButtonTap(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(25)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile feedback when waking up Milo from sleep (2-stage ascending pulse)
     */
    fun performWakeUp(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 40, 50)
                val amplitudes = intArrayOf(0, 100, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 20, 40, 50), -1)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Soft, subtle tactile feedback when Milo falls asleep
     */
    fun performSleep(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(18, 70))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(18)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Crisp tactile feedback when typing / entering listening state
     */
    fun performListening(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Micro pulse when AI starts crunching / thinking
     */
    fun performThinking(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(20, 120))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Energetic burst / roar vibration when Milo finishes thinking and delivers his response
     */
    fun performSuccess(context: Context, hapticFeedback: HapticFeedback? = null) {
        performRoarResponse(context, hapticFeedback)
    }

    fun performRoarResponse(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 35, 30, 75)
                val amplitudes = intArrayOf(0, 180, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 35, 30, 75), -1)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Double click confirmation for synced leads, tasks, or actions
     */
    fun performActionSuccess(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 50, 30)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 50, 30), -1)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile feedback specifically when tapping the Milo mascot image on the dashboard
     * (playful, distinctive double micro-pulse feeling like a lion's friendly paw tap)
     */
    fun performMiloMascotTap(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 25, 45, 35)
                val amplitudes = intArrayOf(0, 160, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 25, 45, 35), -1)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile feedback when sending a chat message (snappy confirmation click)
     */
    fun performMessageSent(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 20, 25, 30)
                val amplitudes = intArrayOf(0, 160, 0, 240)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Light tactile tick for emoji reaction, like, or filter chip tap
     */
    fun performReactionTick(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(15, 120))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Deep, satisfying tactile confirmation for attendance check-in / check-out
     */
    fun performClockInFeedback(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                return
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(45)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Tactile warning / deletion feedback (distinct triple pulse)
     */
    fun performActionWarning(context: Context, hapticFeedback: HapticFeedback? = null) {
        val vibrator = getVibrator(context)
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 30)
                val amplitudes = intArrayOf(0, 220, 0, 180)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                return
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 40, 30), -1)
                return
            }
        }
        hapticFeedback?.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

typealias AppHaptics = MiloHaptics

