package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.firebase.FirebaseRealtimeManager
import android.util.Log

class LeadIntentWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val callLogDao = db.callLogDao()
            val leadDao = db.leadDao()

            val unprocessedLogs = callLogDao.getUnprocessedCallLogs()
            val highIntentKeywords = listOf("buy", "interested", "meeting", "deal", "pricing", "quote", "demo")

            for (log in unprocessedLogs) {
                val insights = log.aiInsights ?: ""
                val transcript = log.transcription ?: ""
                val combinedText = "$insights $transcript"

                if (highIntentKeywords.any { combinedText.contains(it, ignoreCase = true) }) {
                    val lead = leadDao.getLeadByPhone(log.phoneNumber)
                    if (lead != null && lead.stage != "Hot") {
                        val updatedLead = lead.copy(stage = "Hot")
                        leadDao.update(updatedLead)
                        try {
                            FirebaseRealtimeManager.syncLeadToFirebase(updatedLead)
                        } catch (e: Exception) {
                            Log.e("LeadIntentWorker", "Error syncing lead to Firebase", e)
                        }
                        Log.d("LeadIntentWorker", "Lead ${lead.name} updated to Hot based on call intent")
                    }
                }
                callLogDao.markAsProcessed(log.id)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("LeadIntentWorker", "Error processing call logs in background worker", e)
            Result.retry()
        }
    }
}
