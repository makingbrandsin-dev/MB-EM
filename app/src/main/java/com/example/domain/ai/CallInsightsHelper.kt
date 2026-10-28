package com.example.domain.ai

import com.example.data.model.CallLogEntity
import com.example.data.local.AppDatabase
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.util.AppSoundHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.remoteconfig.remoteConfig

object CallInsightsHelper {
    private const val TAG = "CallInsightsHelper"

    suspend fun processCallRecording(
        context: Context,
        callLog: CallLogEntity
    ): CallLogEntity = withContext(Dispatchers.IO) {
        // 1. Transcription (mock, or real implementation using Gemini)
        val transcription = "This is a placeholder transcription for call: ${callLog.contactName}"
        
        // 2. Intent Recognition using Gemini API
        val prompt = """
            Analyze this call transcript and identify if the customer is interested in our services. 
            Transcript: $transcription
            Return a JSON object: {"isLead": true/false, "insight": "short summary", "leadStatus": "New/Qualified/Not Interested"}
        """.trimIndent()
        
        // Call Gemini API (using a hypothetical service or Retrofit implementation)
        val aiResult = callGeminiForInsights(prompt)
        
        // 3. Update CallLogEntity
        val updatedLog = callLog.copy(
            transcription = transcription,
            aiInsights = aiResult,
            leadUpdated = true
        )
        
        // 4. Update Room
        val db = AppDatabase.getDatabase(context)
        db.callLogDao().insert(updatedLog)
        
        // 5. Update Firestore if it's a lead
        if (aiResult.contains("isLead\": true")) {
             // Logic to update Lead status in Firestore
             Log.d(TAG, "Lead identified for contact: ${callLog.contactName}")
        }
        
        updatedLog
    }

    private suspend fun callGeminiForInsights(prompt: String): String {
        return try {
            val modelName = Firebase.remoteConfig.getString("milo_gemini_model")
            val generativeModel = Firebase.ai.generativeModel(modelName = modelName)
            val response = generativeModel.generateContent(prompt)
            response.text ?: "{\"isLead\": false, \"insight\": \"No response from AI\", \"leadStatus\": \"New\"}"
        } catch (e: Exception) {
            Log.e(TAG, "AI call failed: ${e.message}")
            "{\"isLead\": false, \"insight\": \"AI call error\", \"leadStatus\": \"New\"}"
        }
    }
}
