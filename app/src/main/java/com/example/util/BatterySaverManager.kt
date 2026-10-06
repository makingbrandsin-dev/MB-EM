package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BatterySaverMode {
    AUTO,
    ALWAYS_ON,
    OFF
}

data class BatteryStateInfo(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false,
    val mode: BatterySaverMode = BatterySaverMode.AUTO,
    val lowThresholdPercent: Int = 20,
    val isSaverActive: Boolean = false,
    val syncIntervalSeconds: Int = 5,
    val statusDescription: String = "Normal Power Mode"
)

/**
 * Manager for device battery monitoring and power optimization.
 * Automatically throttles Firestore listeners, WebSocket updates, and background sync when battery is low.
 */
class BatterySaverManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentMode = try {
        BatterySaverMode.valueOf(AppPreferences.getBatterySaverMode(context))
    } catch (_: Exception) {
        BatterySaverMode.AUTO
    }

    private var currentThreshold = AppPreferences.getBatterySaverThreshold(context)

    private val _batteryState = MutableStateFlow(
        BatteryStateInfo(
            mode = currentMode,
            lowThresholdPercent = currentThreshold
        )
    )
    val batteryState: StateFlow<BatteryStateInfo> = _batteryState.asStateFlow()

    private val _isBatterySaverActive = MutableStateFlow(false)
    val isBatterySaverActive: StateFlow<Boolean> = _isBatterySaverActive.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(cntx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateBatteryStatus(intent)
            }
        }
    }

    init {
        registerBatteryReceiver()
    }

    private fun registerBatteryReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = context.registerReceiver(batteryReceiver, filter)
            if (initialIntent != null) {
                updateBatteryStatus(initialIntent)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register battery receiver: ${e.message}")
        }
    }

    private fun updateBatteryStatus(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        reevaluateSaverState(pct, isCharging)
    }

    private fun reevaluateSaverState(levelPct: Int, isCharging: Boolean) {
        val isLow = levelPct <= currentThreshold
        val isSaverActive = when (currentMode) {
            BatterySaverMode.ALWAYS_ON -> true
            BatterySaverMode.OFF -> false
            BatterySaverMode.AUTO -> isLow && !isCharging
        }

        val syncSecs = if (isSaverActive) 60 else 5
        val statusDesc = when {
            isSaverActive && currentMode == BatterySaverMode.ALWAYS_ON -> "⚡ Battery Saver Force Enabled (Listeners throttled to 60s)"
            isSaverActive -> "⚡ Battery Low ($levelPct%) — Real-time sync throttled to save power"
            isCharging -> "🔌 Charging ($levelPct%) — Normal sync speed"
            else -> "🔋 Normal Battery ($levelPct%)"
        }

        val updated = BatteryStateInfo(
            levelPercent = levelPct,
            isCharging = isCharging,
            mode = currentMode,
            lowThresholdPercent = currentThreshold,
            isSaverActive = isSaverActive,
            syncIntervalSeconds = syncSecs,
            statusDescription = statusDesc
        )

        _batteryState.value = updated
        _isBatterySaverActive.value = isSaverActive
    }

    fun setMode(mode: BatterySaverMode) {
        currentMode = mode
        AppPreferences.setBatterySaverMode(context, mode.name)
        val current = _batteryState.value
        reevaluateSaverState(current.levelPercent, current.isCharging)
    }

    fun setThreshold(percent: Int) {
        val clamped = percent.coerceIn(10, 50)
        currentThreshold = clamped
        AppPreferences.setBatterySaverThreshold(context, clamped)
        val current = _batteryState.value
        reevaluateSaverState(current.levelPercent, current.isCharging)
    }

    fun getSyncIntervalMs(): Long {
        return if (_isBatterySaverActive.value) 60_000L else 5_000L
    }

    fun getBannerAutoScrollDelayMs(): Long {
        return if (_isBatterySaverActive.value) 15_000L else 5_000L
    }

    companion object {
        private const val TAG = "BatterySaverManager"

        @Volatile
        private var instance: BatterySaverManager? = null

        fun getInstance(context: Context): BatterySaverManager {
            return instance ?: synchronized(this) {
                instance ?: BatterySaverManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
