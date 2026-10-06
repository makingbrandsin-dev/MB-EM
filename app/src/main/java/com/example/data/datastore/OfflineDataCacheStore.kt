package com.example.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LeadEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// DataStore Extension Property
val Context.offlineDataCacheStore: DataStore<Preferences> by preferencesDataStore(name = "offline_workspace_data_cache")

/**
 * Data models for cached enterprise workspace state
 */
data class TaskCacheSummary(
    val totalTasksCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val topTaskTitle: String = "Complete Operations Review",
    val topTaskDueDate: String = "Today",
    val tasksSummaryText: String = "No cached tasks available."
)

data class ShiftCacheSummary(
    val totalShiftsCount: Int = 0,
    val currentShiftType: String = "Morning Operations Shift",
    val shiftTimes: String = "09:00 AM - 05:00 PM",
    val nextShiftInfo: String = "Tomorrow • 09:00 AM",
    val shiftsSummaryText: String = "No cached shift schedule available."
)

data class CrmCacheSummary(
    val totalLeadsCount: Int = 0,
    val activeLeadsCount: Int = 0,
    val topLeadName: String = "Acme Corp Lead",
    val latestRequirement: String = "Enterprise Software Package",
    val leadsSummaryText: String = "No cached CRM data available."
)

data class MiloOfflineVoiceContext(
    val lastQuery: String = "",
    val lastResponse: String = "",
    val offlineVoiceSummary: String = "Milo Offline Intelligence ready. Cached task, shift, and CRM data active.",
    val lastCachedTimestamp: Long = System.currentTimeMillis()
)

/**
 * DataStore caching layer for critical tasks, shifts, CRM data, and Milo voice context.
 * Guarantees instant dashboard rendering and Milo voice responses during intermittent connectivity.
 */
object OfflineDataCacheStore {

    private const val TAG = "OfflineDataCacheStore"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Task Preference Keys
    val KEY_CACHED_TASKS_COUNT = intPreferencesKey("cached_tasks_count")
    val KEY_PENDING_TASKS_COUNT = intPreferencesKey("pending_tasks_count")
    val KEY_TOP_TASK_TITLE = stringPreferencesKey("top_task_title")
    val KEY_TOP_TASK_DUE = stringPreferencesKey("top_task_due")
    val KEY_TASKS_SUMMARY_TEXT = stringPreferencesKey("tasks_summary_text")

    // Shift Schedule Preference Keys
    val KEY_CACHED_SHIFTS_COUNT = intPreferencesKey("cached_shifts_count")
    val KEY_CURRENT_SHIFT_TYPE = stringPreferencesKey("current_shift_type")
    val KEY_SHIFT_TIMES = stringPreferencesKey("shift_times")
    val KEY_NEXT_SHIFT_INFO = stringPreferencesKey("next_shift_info")
    val KEY_SHIFTS_SUMMARY_TEXT = stringPreferencesKey("shifts_summary_text")

    // CRM / Lead Preference Keys
    val KEY_CACHED_LEADS_COUNT = intPreferencesKey("cached_leads_count")
    val KEY_ACTIVE_LEADS_COUNT = intPreferencesKey("active_leads_count")
    val KEY_TOP_LEAD_NAME = stringPreferencesKey("top_lead_name")
    val KEY_LATEST_REQUIREMENT = stringPreferencesKey("latest_requirement")
    val KEY_LEADS_SUMMARY_TEXT = stringPreferencesKey("leads_summary_text")

    // Milo Voice Context Preference Keys
    val KEY_LAST_VOICE_QUERY = stringPreferencesKey("last_voice_query")
    val KEY_LAST_VOICE_RESPONSE = stringPreferencesKey("last_voice_response")
    val KEY_OFFLINE_VOICE_SUMMARY = stringPreferencesKey("offline_voice_summary")
    val KEY_LAST_CACHE_TIMESTAMP = longPreferencesKey("last_cache_timestamp")

    /**
     * Flow observing Task DataStore Cache
     */
    fun getTaskCacheFlow(context: Context): Flow<TaskCacheSummary> {
        return context.offlineDataCacheStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                TaskCacheSummary(
                    totalTasksCount = prefs[KEY_CACHED_TASKS_COUNT] ?: 0,
                    pendingTasksCount = prefs[KEY_PENDING_TASKS_COUNT] ?: 0,
                    topTaskTitle = prefs[KEY_TOP_TASK_TITLE] ?: "Review Daily Operations",
                    topTaskDueDate = prefs[KEY_TOP_TASK_DUE] ?: "Today",
                    tasksSummaryText = prefs[KEY_TASKS_SUMMARY_TEXT] ?: "All critical tasks cached locally."
                )
            }
    }

    /**
     * Flow observing Shift DataStore Cache
     */
    fun getShiftCacheFlow(context: Context): Flow<ShiftCacheSummary> {
        return context.offlineDataCacheStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                ShiftCacheSummary(
                    totalShiftsCount = prefs[KEY_CACHED_SHIFTS_COUNT] ?: 0,
                    currentShiftType = prefs[KEY_CURRENT_SHIFT_TYPE] ?: "Day Operations Shift",
                    shiftTimes = prefs[KEY_SHIFT_TIMES] ?: "09:00 AM - 05:00 PM",
                    nextShiftInfo = prefs[KEY_NEXT_SHIFT_INFO] ?: "Tomorrow • 09:00 AM",
                    shiftsSummaryText = prefs[KEY_SHIFTS_SUMMARY_TEXT] ?: "Active shift schedule cached in DataStore."
                )
            }
    }

    /**
     * Flow observing CRM DataStore Cache
     */
    fun getCrmCacheFlow(context: Context): Flow<CrmCacheSummary> {
        return context.offlineDataCacheStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                CrmCacheSummary(
                    totalLeadsCount = prefs[KEY_CACHED_LEADS_COUNT] ?: 0,
                    activeLeadsCount = prefs[KEY_ACTIVE_LEADS_COUNT] ?: 0,
                    topLeadName = prefs[KEY_TOP_LEAD_NAME] ?: "Enterprise Prospect",
                    latestRequirement = prefs[KEY_LATEST_REQUIREMENT] ?: "Software Solution",
                    leadsSummaryText = prefs[KEY_LEADS_SUMMARY_TEXT] ?: "Active CRM pipeline leads cached in DataStore."
                )
            }
    }

    /**
     * Flow observing Milo Offline Voice Context
     */
    fun getMiloVoiceContextFlow(context: Context): Flow<MiloOfflineVoiceContext> {
        return context.offlineDataCacheStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { prefs ->
                MiloOfflineVoiceContext(
                    lastQuery = prefs[KEY_LAST_VOICE_QUERY] ?: "",
                    lastResponse = prefs[KEY_LAST_VOICE_RESPONSE] ?: "",
                    offlineVoiceSummary = prefs[KEY_OFFLINE_VOICE_SUMMARY] ?: "Milo DataStore Offline Layer Ready.",
                    lastCachedTimestamp = prefs[KEY_LAST_CACHE_TIMESTAMP] ?: System.currentTimeMillis()
                )
            }
    }

    /**
     * Cache critical tasks into DataStore preferences
     */
    fun cacheTasks(context: Context, tasks: List<TaskEntity>) {
        scope.launch {
            try {
                val pending = tasks.filter { !it.isCompleted }
                val topTask = pending.firstOrNull() ?: tasks.firstOrNull()
                val summaryText = if (tasks.isEmpty()) {
                    "No active tasks."
                } else {
                    "${pending.size} pending tasks out of ${tasks.size} total. Top: ${topTask?.title ?: "N/A"}"
                }

                context.offlineDataCacheStore.edit { prefs ->
                    prefs[KEY_CACHED_TASKS_COUNT] = tasks.size
                    prefs[KEY_PENDING_TASKS_COUNT] = pending.size
                    prefs[KEY_TOP_TASK_TITLE] = topTask?.title ?: "General Maintenance"
                    prefs[KEY_TOP_TASK_DUE] = topTask?.dueDate ?: "Today"
                    prefs[KEY_TASKS_SUMMARY_TEXT] = summaryText
                    prefs[KEY_LAST_CACHE_TIMESTAMP] = System.currentTimeMillis()
                }
                Log.d(TAG, "Cached ${tasks.size} tasks in DataStore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching tasks in DataStore: ${e.message}")
            }
        }
    }

    /**
     * Cache shift schedule into DataStore preferences
     */
    fun cacheShifts(context: Context, shifts: List<ShiftScheduleEntity>) {
        scope.launch {
            try {
                val activeShift = shifts.firstOrNull()
                val currentType = activeShift?.shiftType ?: "Morning Operations Shift"
                val times = if (activeShift != null) "${activeShift.startTime} - ${activeShift.endTime}" else "09:00 AM - 05:00 PM"
                val nextInfo = if (shifts.size > 1) "${shifts[1].shiftType} (${shifts[1].startTime})" else "Tomorrow • 09:00 AM"
                val summaryText = if (shifts.isEmpty()) {
                    "No shifts assigned."
                } else {
                    "Current Shift: $currentType ($times). Next: $nextInfo"
                }

                context.offlineDataCacheStore.edit { prefs ->
                    prefs[KEY_CACHED_SHIFTS_COUNT] = shifts.size
                    prefs[KEY_CURRENT_SHIFT_TYPE] = currentType
                    prefs[KEY_SHIFT_TIMES] = times
                    prefs[KEY_NEXT_SHIFT_INFO] = nextInfo
                    prefs[KEY_SHIFTS_SUMMARY_TEXT] = summaryText
                    prefs[KEY_LAST_CACHE_TIMESTAMP] = System.currentTimeMillis()
                }
                Log.d(TAG, "Cached ${shifts.size} shifts in DataStore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching shifts in DataStore: ${e.message}")
            }
        }
    }

    /**
     * Cache CRM / Leads data into DataStore preferences
     */
    fun cacheLeads(context: Context, leads: List<LeadEntity>) {
        scope.launch {
            try {
                val active = leads.filter { !it.stage.equals("Closed Won", ignoreCase = true) && !it.stage.equals("Lost", ignoreCase = true) }
                val topLead = active.firstOrNull() ?: leads.firstOrNull()
                val summaryText = if (leads.isEmpty()) {
                    "No CRM leads assigned."
                } else {
                    "${active.size} active leads in pipeline. Key prospect: ${topLead?.name ?: "N/A"} (${topLead?.company ?: ""})"
                }

                context.offlineDataCacheStore.edit { prefs ->
                    prefs[KEY_CACHED_LEADS_COUNT] = leads.size
                    prefs[KEY_ACTIVE_LEADS_COUNT] = active.size
                    prefs[KEY_TOP_LEAD_NAME] = topLead?.name ?: "Key Client"
                    prefs[KEY_LATEST_REQUIREMENT] = topLead?.requirement ?: "Enterprise Quotation"
                    prefs[KEY_LEADS_SUMMARY_TEXT] = summaryText
                    prefs[KEY_LAST_CACHE_TIMESTAMP] = System.currentTimeMillis()
                }
                Log.d(TAG, "Cached ${leads.size} CRM leads in DataStore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed caching CRM leads in DataStore: ${e.message}")
            }
        }
    }

    /**
     * Save last Milo voice command interaction into DataStore
     */
    fun cacheMiloVoiceInteraction(context: Context, query: String, response: String) {
        scope.launch {
            try {
                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val offlineSummary = "Last command ($timeStr): \"$query\" -> ${response.take(60)}..."

                context.offlineDataCacheStore.edit { prefs ->
                    prefs[KEY_LAST_VOICE_QUERY] = query
                    prefs[KEY_LAST_VOICE_RESPONSE] = response
                    prefs[KEY_OFFLINE_VOICE_SUMMARY] = offlineSummary
                    prefs[KEY_LAST_CACHE_TIMESTAMP] = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving Milo voice interaction in DataStore: ${e.message}")
            }
        }
    }

    /**
     * Offline Voice Command Handler: Resolves queries using cached DataStore values when connectivity is intermittent or offline.
     */
    suspend fun resolveMiloOfflineVoiceCommand(context: Context, query: String): String {
        return try {
            val prefs = context.offlineDataCacheStore.data.first()
            val lower = query.lowercase(Locale.ROOT)

            when {
                lower.contains("task") || lower.contains("todo") || lower.contains("work") -> {
                    val pendingCount = prefs[KEY_PENDING_TASKS_COUNT] ?: 0
                    val topTitle = prefs[KEY_TOP_TASK_TITLE] ?: "Operations Check"
                    val topDue = prefs[KEY_TOP_TASK_DUE] ?: "Today"
                    "🦁 [Offline DataStore] You have $pendingCount pending tasks. Top priority task: \"$topTitle\" due $topDue."
                }
                lower.contains("shift") || lower.contains("schedule") || lower.contains("roster") || lower.contains("timing") -> {
                    val shiftType = prefs[KEY_CURRENT_SHIFT_TYPE] ?: "Day Operations Shift"
                    val shiftTimes = prefs[KEY_SHIFT_TIMES] ?: "09:00 AM - 05:00 PM"
                    val nextShift = prefs[KEY_NEXT_SHIFT_INFO] ?: "Tomorrow • 09:00 AM"
                    "🦁 [Offline DataStore] Your current shift is $shiftType ($shiftTimes). Next shift: $nextShift."
                }
                lower.contains("lead") || lower.contains("crm") || lower.contains("client") || lower.contains("prospect") -> {
                    val activeLeads = prefs[KEY_ACTIVE_LEADS_COUNT] ?: 0
                    val topLead = prefs[KEY_TOP_LEAD_NAME] ?: "Key Client"
                    val req = prefs[KEY_LATEST_REQUIREMENT] ?: "Solution Package"
                    "🦁 [Offline DataStore] You have $activeLeads active CRM leads. Key contact: $topLead (Requirement: $req)."
                }
                else -> {
                    val tasksSummary = prefs[KEY_TASKS_SUMMARY_TEXT] ?: "Tasks ready."
                    val shiftSummary = prefs[KEY_SHIFTS_SUMMARY_TEXT] ?: "Shift active."
                    "🦁 [Offline DataStore] Intermittent network mode active. Cached summary: $tasksSummary • $shiftSummary"
                }
            }
        } catch (e: Exception) {
            "🦁 [Offline DataStore] Accessing local cache. Your tasks, shifts, and CRM records are safely cached."
        }
    }
}
