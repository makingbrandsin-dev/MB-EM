package com.example.milo

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.util.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Feature 1: Proactive Pipeline Pulse (MiloPipelinePulseWorker.kt)
 * A background WorkManager job that scans the database daily for stale high-value leads
 * (> ₹10,000 uncontacted for 2+ hours) and dispatches a proactive notification with Milo's WARNING pose.
 */
class MiloPipelinePulseWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(appContext)
            val leadDao = db.leadDao()

            val currentTime = System.currentTimeMillis()
            val twoHoursAgo = currentTime - (2 * 60 * 60 * 1000)

            val allLeads = leadDao.getAllLeadsDirectly()

            // Scan leads that are pending/new with budget >= 10,000 updated more than 2 hours ago
            val staleLeads = allLeads.filter { lead ->
                val parsedValue = lead.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                val isNewOrContacted = lead.stage.equals("NEW", ignoreCase = true) ||
                        lead.stage.equals("CONTACTED", ignoreCase = true) ||
                        lead.stage.equals("PROPOSAL", ignoreCase = true)
                parsedValue >= 10000.0 && isNewOrContacted && lead.createdAt <= twoHoursAgo
            }

            if (staleLeads.isNotEmpty()) {
                val totalValue = staleLeads.sumOf {
                    it.potentialValue.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                }
                val leadNames = staleLeads.take(3).joinToString(", ") { it.name }
                val title = "⚠️ Milo Pipeline Alert: ${staleLeads.size} Stale High-Value Leads"
                val body = "₹${String.format("%,.0f", totalValue)} pending contact (>2 hrs)! Leads: $leadNames"

                NotificationHelper.showNotification(
                    context = appContext,
                    title = title,
                    message = body,
                    notificationId = 2001
                )
                Log.d("MiloPipelinePulse", "Notification sent for ${staleLeads.size} stale leads.")
            } else {
                Log.d("MiloPipelinePulse", "No stale high-value leads found today.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("MiloPipelinePulseWorker", "Error scanning pipeline pulse", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "milo_pipeline_pulse_worker"

        fun schedulePeriodicPulse(context: Context) {
            val pulseWorkRequest = PeriodicWorkRequestBuilder<MiloPipelinePulseWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                pulseWorkRequest
            )
        }
    }
}
