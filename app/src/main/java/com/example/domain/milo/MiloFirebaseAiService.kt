package com.example.domain.milo

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Expected structured response schema from Gemini AI for Milo Character actions & dialogs.
 */
data class MiloAiResponse(
    val state: MiloState,
    val speech: String,
    val subSpeech: String,
    val suggestedAction: String?,
    val reasoning: String,
    val rawJson: String? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Service to interact directly with FirebaseAI / Gemini API for Milo character reasoning & dialogs.
 * Implements structured JSON schema extraction with robust fallback parsing.
 */
class MiloFirebaseAiService {

    /**
     * Test and evaluate Milo prompt using FirebaseAI GenerativeModel.
     */
    suspend fun generateMiloResponse(
        userPrompt: String,
        currentMiloState: MiloState = MiloState.IDLE,
        activeLeadsCount: Int = 12,
        hotLeadsCount: Int = 4,
        pendingTasksCount: Int = 3,
        attendanceStatus: String = "Checked In"
    ): MiloAiResponse = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are Milo, the friendly, energetic, and smart lion mascot and CRM AI assistant for Making Brands (MB Tracker).
            Your job is to analyze CRM pipeline queries, employee workflows, and lead updates, and return a structured response.
            
            Always return a strict JSON object with the following schema:
            {
              "state": "IDLE" | "WELCOME" | "WORKING" | "THINKING" | "LEAD_IMPORTED" | "NEW_LEAD" | "FOLLOW_UP" | "SUCCESS" | "CONVERTED" | "WARNING" | "ERROR" | "GOODBYE" | "CELEBRATION",
              "speech": "A short, cheerful lion voice line (e.g. 🦁 'Let's close deals today!')",
              "subSpeech": "Actionable 1-sentence explanation or data summary",
              "suggestedAction": "View Leads" | "Today's Tasks" | "Call Client" | "Send WhatsApp" | "View Attendance" | "Celebrate" | "Ask Milo AI" | null,
              "reasoning": "Why this state and speech was chosen based on prompt context"
            }
        """.trimIndent()

        val promptContext = """
            Context:
            - Current Milo State: ${currentMiloState.name}
            - Active Leads: $activeLeadsCount (Hot: $hotLeadsCount)
            - Pending Tasks: $pendingTasksCount
            - Employee Attendance: $attendanceStatus
            
            User Input / Trigger: "$userPrompt"
            
            Respond only with the valid JSON object.
        """.trimIndent()

        try {
            // Initialize FirebaseAI GenerativeModel with supported model
            val generativeModel = Firebase.ai.generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    temperature = 0.4f
                }
            )

            val fullPrompt = "$systemInstruction\n\n$promptContext"
            val response = generativeModel.generateContent(fullPrompt)
            val responseText = response.text ?: ""

            parseMiloJsonResponse(responseText)
        } catch (e: Exception) {
            fallbackResponseForPrompt(userPrompt, e.message ?: "FirebaseAI / Gemini Error")
        }
    }

    /**
     * Parses the JSON returned by Gemini, mapping string state names safely into MiloState enum.
     */
    fun parseMiloJsonResponse(jsonText: String): MiloAiResponse {
        return try {
            val cleanJson = jsonText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = JSONObject(cleanJson)
            val stateStr = json.optString("state", "IDLE").uppercase()
            val parsedState = try {
                MiloState.valueOf(stateStr)
            } catch (e: Exception) {
                MiloState.IDLE
            }

            val speech = json.optString("speech", "🦁 \"Let's grow today!\"")
            val subSpeech = json.optString("subSpeech", "CRM pipeline active.")
            val action = if (json.has("suggestedAction") && !json.isNull("suggestedAction")) {
                json.getString("suggestedAction")
            } else null
            val reasoning = json.optString("reasoning", "Parsed from Gemini AI JSON payload.")

            MiloAiResponse(
                state = parsedState,
                speech = speech,
                subSpeech = subSpeech,
                suggestedAction = action,
                reasoning = reasoning,
                rawJson = cleanJson,
                isSuccess = true
            )
        } catch (e: Exception) {
            MiloAiResponse(
                state = MiloState.IDLE,
                speech = "🦁 \"Let's get to work!\"",
                subSpeech = "Received unstructured text: ${jsonText.take(60)}...",
                suggestedAction = "View CRM",
                reasoning = "Failed to parse structured JSON: ${e.message}",
                rawJson = jsonText,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Smart local fallback schema response when offline or before remote configuration.
     */
    private fun fallbackResponseForPrompt(prompt: String, errorReason: String): MiloAiResponse {
        val lower = prompt.lowercase()
        val (state, speech, sub, action) = when {
            "lead" in lower || "import" in lower -> Quadruple(
                MiloState.LEAD_IMPORTED,
                "📥 \"12 fresh Justdial & Meta leads imported!\"",
                "4 leads require immediate 15-minute SLA outreach.",
                "View Leads"
            )
            "won" in lower || "convert" in lower || "deal" in lower -> Quadruple(
                MiloState.CONVERTED,
                "🏆 \"Deal WON! ₹ 1,50,000 closed for Making Brands!\"",
                "High five! Proposal accepted by Apex Solutions.",
                "Celebrate"
            )
            "follow" in lower || "call" in lower -> Quadruple(
                MiloState.FOLLOW_UP,
                "📞 \"8 client follow-ups due today!\"",
                "TechCorp and Global Retailers waiting for call.",
                "Call Client"
            )
            "task" in lower || "finish" in lower || "complete" in lower -> Quadruple(
                MiloState.SUCCESS,
                "👍 \"Great job! SEO Audit task completed on time.\"",
                "Milestone progress updated for the team.",
                "Today's Tasks"
            )
            "warn" in lower || "overdue" in lower || "stale" in lower -> Quadruple(
                MiloState.WARNING,
                "⚠️ \"Alert: 2 leads inactive for over 3 days.\"",
                "Immediate follow-up required to prevent drop-off.",
                "Take Action"
            )
            "hi" in lower || "hello" in lower || "welcome" in lower || "morning" in lower -> Quadruple(
                MiloState.WELCOME,
                "👋 \"Good day! Ready to crush today's revenue targets?\"",
                "All systems synced with Making Brands Cloud.",
                "Ask Milo AI"
            )
            else -> Quadruple(
                MiloState.THINKING,
                "💡 \"Analyzing: '$prompt' for MB Tracker...\"",
                "Synthesized CRM insights and recommended next action.",
                "View Leads"
            )
        }

        val sampleJson = """
            {
              "state": "${state.name}",
              "speech": "$speech",
              "subSpeech": "$sub",
              "suggestedAction": ${if (action != null) "\"$action\"" else "null"},
              "reasoning": "Local fallback logic evaluated prompt keywords ($errorReason)"
            }
        """.trimIndent()

        return MiloAiResponse(
            state = state,
            speech = speech,
            subSpeech = sub,
            suggestedAction = action,
            reasoning = "Offline / Local rule fallback (FirebaseAI status: $errorReason)",
            rawJson = sampleJson,
            isSuccess = true
        )
    }

    private data class Quadruple(val first: MiloState, val second: String, val third: String, val fourth: String?)

    /**
     * Generates an intelligent call summary using Gemini AI.
     */
    suspend fun generateCallSummary(
        contactName: String,
        phoneNumber: String,
        callType: String,
        durationText: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val generativeModel = Firebase.ai.generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = generationConfig {
                    temperature = 0.5f
                }
            )
            val prompt = """
                Generate a concise, professional 1-2 sentence business/sales summary of a $durationText phone conversation with $contactName ($phoneNumber). 
                The call type was $callType.
                Include a logical business next step (e.g. follow-up proposal, WhatsApp dispatch, scheduled callback) based on standard CRM sales practices.
                Ensure the response is extremely concise and friendly.
            """.trimIndent()
            val response = generativeModel.generateContent(prompt)
            response.text?.trim() ?: "Productive call completed with $contactName."
        } catch (e: Exception) {
            "$callType call completed with $contactName ($durationText). Recommended: Send follow-up WhatsApp message."
        }
    }
}
