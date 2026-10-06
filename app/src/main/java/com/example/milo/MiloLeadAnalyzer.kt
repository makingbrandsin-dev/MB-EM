package com.example.milo

import com.example.domain.milo.MiloState

/**
 * Feature 4: Inbound Lead Sentiment & Urgency AI Scanner (MiloLeadAnalyzer.kt)
 * Scans incoming requirement text for urgency keywords ("asap", "urgent", "immediately", "quote today"),
 * auto-escalates priority, and adjusts lead score.
 */
enum class LeadSentiment {
    POSITIVE,
    NEUTRAL,
    URGENT,
    HIGH_RISK
}

data class LeadAnalysisResult(
    val requirementText: String,
    val sentiment: LeadSentiment,
    val urgencyScore: Int, // 0 to 100
    val suggestedPriority: String, // "URGENT", "HIGH", "MEDIUM", "LOW"
    val detectedKeywords: List<String>,
    val actionRecommendation: String,
    val recommendedMiloState: MiloState
)

object MiloLeadAnalyzer {

    private val URGENT_KEYWORDS = listOf(
        "asap", "urgent", "immediately", "right now", "emergency", "today",
        "quick", "fast", "deadline", "top priority", "instant"
    )

    private val POSITIVE_KEYWORDS = listOf(
        "ready to buy", "interested", "looking for", "budget approved",
        "send invoice", "proceed", "agreed", "finalized", "great"
    )

    private val HIGH_RISK_KEYWORDS = listOf(
        "competitor", "too expensive", "dissatisfied", "cancel", "delay",
        "issue", "complaint", "doubtful", "problem"
    )

    fun analyzeLead(requirementText: String, estimatedValue: Double = 0.0): LeadAnalysisResult {
        val lowerText = requirementText.lowercase()
        val detectedKeywords = mutableListOf<String>()

        var score = 40 // Base score

        // High value boost
        if (estimatedValue >= 50000) {
            score += 25
            detectedKeywords.add("High Value (₹${String.format("%,.0f", estimatedValue)})")
        } else if (estimatedValue >= 15000) {
            score += 15
        }

        // Urgent keywords scan
        URGENT_KEYWORDS.forEach { kw ->
            if (lowerText.contains(kw)) {
                score += 20
                detectedKeywords.add(kw)
            }
        }

        // Positive keywords scan
        POSITIVE_KEYWORDS.forEach { kw ->
            if (lowerText.contains(kw)) {
                score += 15
                detectedKeywords.add(kw)
            }
        }

        // High risk keywords scan
        var isRisk = false
        HIGH_RISK_KEYWORDS.forEach { kw ->
            if (lowerText.contains(kw)) {
                isRisk = true
                detectedKeywords.add(kw)
            }
        }

        val finalScore = score.coerceIn(0, 100)

        val sentiment = when {
            isRisk -> LeadSentiment.HIGH_RISK
            finalScore >= 75 -> LeadSentiment.URGENT
            finalScore >= 55 -> LeadSentiment.POSITIVE
            else -> LeadSentiment.NEUTRAL
        }

        val priority = when {
            finalScore >= 80 -> "URGENT"
            finalScore >= 60 -> "HIGH"
            finalScore >= 40 -> "MEDIUM"
            else -> "LOW"
        }

        val actionRecommendation = when (sentiment) {
            LeadSentiment.URGENT -> "⚡ Immediate Call Required! Client expressed high urgency."
            LeadSentiment.HIGH_RISK -> "⚠️ Risk Alert: Address concerns & send custom discounted quotation."
            LeadSentiment.POSITIVE -> "💬 High Conversion Probability! Send WhatsApp product brochure."
            LeadSentiment.NEUTRAL -> "📅 Schedule standard follow-up call within 24 hours."
        }

        val recommendedMiloState = when (sentiment) {
            LeadSentiment.URGENT -> MiloState.WARNING
            LeadSentiment.HIGH_RISK -> MiloState.WARNING
            LeadSentiment.POSITIVE -> MiloState.CONVERTED
            LeadSentiment.NEUTRAL -> MiloState.NEW_LEAD
        }

        return LeadAnalysisResult(
            requirementText = requirementText,
            sentiment = sentiment,
            urgencyScore = finalScore,
            suggestedPriority = priority,
            detectedKeywords = detectedKeywords.distinct(),
            actionRecommendation = actionRecommendation,
            recommendedMiloState = recommendedMiloState
        )
    }
}
