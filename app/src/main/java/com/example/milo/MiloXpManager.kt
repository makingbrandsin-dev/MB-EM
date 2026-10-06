package com.example.milo

import android.content.Context
import android.util.Log
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Milo Accessory & Theme Unlocks
 */
enum class MiloThemeAccessory(
    val id: String,
    val title: String,
    val requiredLevel: Int,
    val requiredXp: Int,
    val iconEmoji: String,
    val description: String,
    val primaryColorHex: Long
) {
    CLASSIC(
        id = "classic",
        title = "Classic Milo",
        requiredLevel = 1,
        requiredXp = 0,
        iconEmoji = "🦁",
        description = "Standard Milo Field Lion Companion",
        primaryColorHex = 0xFF298CD8
    ),
    CROWN(
        id = "crown",
        title = "Crown of Achievement",
        requiredLevel = 2,
        requiredXp = 100,
        iconEmoji = "👑",
        description = "Royal Golden Crown unlocked at Level 2",
        primaryColorHex = 0xFFFFD700
    ),
    CYBER_SHADES(
        id = "cyber_shades",
        title = "Golden Cyber Shades",
        requiredLevel = 3,
        requiredXp = 250,
        iconEmoji = "🕶️",
        description = "Futuristic Cyber Matrix Shades unlocked at Level 3",
        primaryColorHex = 0xFF00E5FF
    ),
    ROYAL_CAPE(
        id = "royal_cape",
        title = "Royal Triumph Cape",
        requiredLevel = 4,
        requiredXp = 500,
        iconEmoji = "🧥",
        description = "Regal Crimson Cape worn by elite deal closers at Level 4",
        primaryColorHex = 0xFFE53935
    ),
    LEGENDARY_GOLD(
        id = "legendary_gold",
        title = "Legendary Field Master",
        requiredLevel = 5,
        requiredXp = 1000,
        iconEmoji = "🌟",
        description = "Full Holographic Golden Lion Aura unlocked at Level 5",
        primaryColorHex = 0xFFFFB300
    );

    companion object {
        fun fromId(id: String): MiloThemeAccessory {
            return entries.find { it.id == id } ?: CLASSIC
        }
    }
}

/**
 * Log entry for earned XP
 */
data class MiloXpLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val reason: String,
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Current State of Milo XP System
 */
data class MiloXpState(
    val totalXp: Int = 0,
    val currentLevel: Int = 1,
    val levelTitle: String = "Cub Agent 🦁",
    val xpForCurrentLevel: Int = 0,
    val xpForNextLevel: Int = 100,
    val progressInLevel: Float = 0.0f,
    val equippedTheme: MiloThemeAccessory = MiloThemeAccessory.CLASSIC,
    val unlockedThemes: List<MiloThemeAccessory> = listOf(MiloThemeAccessory.CLASSIC),
    val logs: List<MiloXpLog> = emptyList(),
    val recentLevelUp: Int? = null // Set when a level-up triggers for popup dialogs
)

/**
 * Milo XP & Gamification Manager
 */
object MiloXpManager {

    private const val PREFS_NAME = "milo_xp_preferences"
    private const val KEY_TOTAL_XP = "key_total_xp"
    private const val KEY_EQUIPPED_THEME = "key_equipped_theme"
    private const val KEY_LOGS_JSON = "key_logs_json"

    private val _xpState = MutableStateFlow(MiloXpState())
    val xpState: StateFlow<MiloXpState> = _xpState.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        loadState(context)
    }

    private fun loadState(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val totalXp = prefs.getInt(KEY_TOTAL_XP, 0)
            val equippedThemeId = prefs.getString(KEY_EQUIPPED_THEME, MiloThemeAccessory.CLASSIC.id) ?: MiloThemeAccessory.CLASSIC.id
            val logsJsonStr = prefs.getString(KEY_LOGS_JSON, "[]") ?: "[]"

            val equippedTheme = MiloThemeAccessory.fromId(equippedThemeId)
            val logs = parseLogsJson(logsJsonStr)

            updateCalculatedState(totalXp, equippedTheme, logs, levelUpNotice = null)
        } catch (e: Exception) {
            Log.e("MiloXpManager", "Error loading Milo XP state", e)
        }
    }

    private fun saveState(context: Context) {
        try {
            val currentState = _xpState.value
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonArray = JSONArray()
            currentState.logs.take(30).forEach { log ->
                val obj = JSONObject()
                obj.put("id", log.id)
                obj.put("reason", log.reason)
                obj.put("xpEarned", log.xpEarned)
                obj.put("timestamp", log.timestamp)
                jsonArray.put(obj)
            }

            prefs.edit()
                .putInt(KEY_TOTAL_XP, currentState.totalXp)
                .putString(KEY_EQUIPPED_THEME, currentState.equippedTheme.id)
                .putString(KEY_LOGS_JSON, jsonArray.toString())
                .apply()
        } catch (e: Exception) {
            Log.e("MiloXpManager", "Error saving Milo XP state", e)
        }
    }

    fun addXp(context: Context, amount: Int, reason: String) {
        init(context)
        val current = _xpState.value
        val newTotalXp = current.totalXp + amount
        val newLog = MiloXpLog(reason = reason, xpEarned = amount)
        val updatedLogs = listOf(newLog) + current.logs

        val oldLevel = current.currentLevel
        val newLevel = calculateLevel(newTotalXp)
        val levelUpNotice = if (newLevel > oldLevel) newLevel else null

        updateCalculatedState(
            totalXp = newTotalXp,
            equippedTheme = current.equippedTheme,
            logs = updatedLogs,
            levelUpNotice = levelUpNotice
        )

        saveState(context)

        if (levelUpNotice != null) {
            val levelInfo = getLevelInfo(newLevel)
            NotificationHelper.showNotification(
                context = context,
                title = "🎉 LEVEL UP! Level $newLevel ${levelInfo.first}",
                message = "Congratulations! You earned $amount XP for '$reason' and reached Level $newLevel!",
                notificationId = 4001
            )
        }
    }

    fun equipTheme(context: Context, theme: MiloThemeAccessory) {
        init(context)
        val current = _xpState.value
        if (current.unlockedThemes.contains(theme)) {
            _xpState.value = current.copy(equippedTheme = theme)
            saveState(context)
        }
    }

    fun dismissLevelUpNotice() {
        _xpState.value = _xpState.value.copy(recentLevelUp = null)
    }

    private fun updateCalculatedState(
        totalXp: Int,
        equippedTheme: MiloThemeAccessory,
        logs: List<MiloXpLog>,
        levelUpNotice: Int?
    ) {
        val level = calculateLevel(totalXp)
        val (levelTitle, currentLevelXp, nextLevelXp) = getLevelInfo(level)

        val unlocked = MiloThemeAccessory.entries.filter { totalXp >= it.requiredXp }

        val progress = if (nextLevelXp > currentLevelXp) {
            ((totalXp - currentLevelXp).toFloat() / (nextLevelXp - currentLevelXp).toFloat()).coerceIn(0f, 1f)
        } else 1.0f

        val safeEquipped = if (unlocked.contains(equippedTheme)) equippedTheme else MiloThemeAccessory.CLASSIC

        _xpState.value = MiloXpState(
            totalXp = totalXp,
            currentLevel = level,
            levelTitle = levelTitle,
            xpForCurrentLevel = currentLevelXp,
            xpForNextLevel = nextLevelXp,
            progressInLevel = progress,
            equippedTheme = safeEquipped,
            unlockedThemes = unlocked,
            logs = logs,
            recentLevelUp = levelUpNotice
        )
    }

    private fun calculateLevel(totalXp: Int): Int {
        return when {
            totalXp >= 1000 -> 5
            totalXp >= 500 -> 4
            totalXp >= 250 -> 3
            totalXp >= 100 -> 2
            else -> 1
        }
    }

    private fun getLevelInfo(level: Int): Triple<String, Int, Int> {
        return when (level) {
            1 -> Triple("Cub Agent 🦁", 0, 100)
            2 -> Triple("Prowler Specialist 👑", 100, 250)
            3 -> Triple("Roaring Executive 🕶️", 250, 500)
            4 -> Triple("Apex Director 🧥", 500, 1000)
            5 -> Triple("Legendary Field Master 🌟", 1000, 2000)
            else -> Triple("Legendary Master 🌟", 1000, 2000)
        }
    }

    private fun parseLogsJson(jsonStr: String): List<MiloXpLog> {
        val list = mutableListOf<MiloXpLog>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    MiloXpLog(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        reason = obj.optString("reason", "Action Completed"),
                        xpEarned = obj.optInt("xpEarned", 10),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MiloXpManager", "Error parsing logs JSON", e)
        }
        return list
    }

    fun onTaskCompleted(context: Context, taskTitle: String) {
        addXp(context, 50, "Completed Task: $taskTitle")
    }

    fun onLeadCreated(context: Context, leadName: String) {
        addXp(context, 25, "New Lead Created: $leadName")
    }

    fun onLeadConverted(context: Context, leadName: String, amount: Double) {
        val formattedAmt = String.format("%,.0f", amount)
        addXp(context, 150, "🏆 Deal Won: $leadName (₹$formattedAmt)")
    }

    fun onFollowUpDone(context: Context, clientName: String) {
        addXp(context, 30, "Follow-up Call Completed: $clientName")
    }

    fun onMacroExecuted(context: Context, macroName: String) {
        addXp(context, 40, "One-Shot Macro Executed: $macroName")
    }

    fun onDailyCheckIn(context: Context) {
        addXp(context, 20, "Daily Attendance Punch-In")
    }
}
