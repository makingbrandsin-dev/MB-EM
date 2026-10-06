package com.example.domain.milo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MiloAiInsight(
    val title: String,
    val summary: String,
    val actionLabel: String? = null,
    val actionRoute: String? = null,
    val draftMessage: String? = null
)

/**
 * Domain ViewModel for managing Milo's emotional & functional State Machine.
 * Manages MiloState via StateFlow and processes domain MiloEvent triggers.
 */
class MiloViewModel : ViewModel() {

    private val _state = MutableStateFlow(MiloState.WELCOME)
    val state: StateFlow<MiloState> = _state.asStateFlow()

    private val _speechText = MutableStateFlow("🦁 \"Let's grow today!\"")
    val speechText: StateFlow<String> = _speechText.asStateFlow()

    private val _subSpeechText = MutableStateFlow("12 new leads arrived. 4 need follow-up today.")
    val subSpeechText: StateFlow<String> = _subSpeechText.asStateFlow()

    private val _suggestedAction = MutableStateFlow<String?>("View Leads")
    val suggestedAction: StateFlow<String?> = _suggestedAction.asStateFlow()

    private val _isAiPanelOpen = MutableStateFlow(false)
    val isAiPanelOpen: StateFlow<Boolean> = _isAiPanelOpen.asStateFlow()

    private val _aiInsights = MutableStateFlow<List<MiloAiInsight>>(emptyList())
    val aiInsights: StateFlow<List<MiloAiInsight>> = _aiInsights.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val aiService = MiloFirebaseAiService()

    private var autoResetJob: Job? = null

    init {
        refreshAiInsights()
    }

    /**
     * Process an application event and update Milo's state and speech.
     */
    fun handleEvent(event: MiloEvent) {
        autoResetJob?.cancel()

        when (event) {
            is MiloEvent.AppOpened -> {
                _state.value = MiloState.WELCOME
                _speechText.value = "🦁 \"Welcome to MB Tracker! Ready to crush targets?\""
                _subSpeechText.value = "Synced with Making Brands Cloud. 128 Active Leads."
                _suggestedAction.value = "View Dashboard"
                scheduleAutoReset(8000)
            }
            is MiloEvent.EmployeeLoggedIn -> {
                _state.value = MiloState.WELCOME
                _speechText.value = "👋 \"Good Morning! Check-in recorded.\""
                _subSpeechText.value = "Your attendance timer is running. Have a great shift!"
                _suggestedAction.value = "Today's Tasks"
                scheduleAutoReset(6000)
            }
            is MiloEvent.EmployeeLoggedOut -> {
                _state.value = MiloState.GOODBYE
                _speechText.value = "🙋 \"Shift ended! Great work today.\""
                _subSpeechText.value = "Total work duration logged to timesheet."
                _suggestedAction.value = "Sign In"
            }
            is MiloEvent.LeadImported -> {
                _state.value = MiloState.LEAD_IMPORTED
                _speechText.value = "📥 \"${event.count} new Justdial & Meta leads imported!\""
                _subSpeechText.value = "Auto-assigned to sales representatives."
                _suggestedAction.value = "View Leads"
                scheduleAutoReset(7000)
            }
            is MiloEvent.LeadCreated -> {
                _state.value = MiloState.NEW_LEAD
                _speechText.value = "✨ \"New lead created: ${event.leadName}!\""
                _subSpeechText.value = "Pipeline potential updated in CRM."
                _suggestedAction.value = "Open Lead"
                scheduleAutoReset(6000)
            }
            is MiloEvent.FollowUpDue -> {
                _state.value = MiloState.FOLLOW_UP
                _speechText.value = "📞 \"${event.count} client follow-ups due today!\""
                _subSpeechText.value = event.clientName?.let { "Next: Call $it" } ?: "Keep response times under 15 minutes."
                _suggestedAction.value = "Call Now"
                scheduleAutoReset(7000)
            }
            is MiloEvent.TaskCompleted -> {
                _state.value = MiloState.SUCCESS
                _speechText.value = "👍 \"Task completed: ${event.taskTitle}!\""
                _subSpeechText.value = "Milestone progress updated for the team."
                _suggestedAction.value = "Next Task"
                scheduleAutoReset(5000)
            }
            is MiloEvent.LeadConverted -> {
                _state.value = MiloState.CONVERTED
                _speechText.value = "🏆 \"WON! Lead converted: ${event.leadName}!\""
                _subSpeechText.value = "₹ ${String.format("%,.0f", event.amount)} revenue added to Making Brands."
                _suggestedAction.value = "Celebrate"
                scheduleAutoReset(8000)
            }
            is MiloEvent.Warning -> {
                _state.value = MiloState.WARNING
                _speechText.value = "⚠️ \"Alert: ${event.message}\""
                _subSpeechText.value = "Lead inactive for 4 days or missed milestone."
                _suggestedAction.value = "Take Action"
                scheduleAutoReset(6000)
            }
            is MiloEvent.Error -> {
                _state.value = MiloState.ERROR
                _speechText.value = "❌ \"System note: ${event.message}\""
                _subSpeechText.value = "Check connectivity or retry operation."
                _suggestedAction.value = "Retry"
                scheduleAutoReset(5000)
            }
            is MiloEvent.Thinking -> {
                _state.value = MiloState.THINKING
                _speechText.value = "💡 \"Milo AI is analyzing ${event.query}...\""
                _subSpeechText.value = "Synthesizing CRM data, tasks & employee logs."
                _suggestedAction.value = null
            }
            is MiloEvent.Celebration -> {
                _state.value = MiloState.CELEBRATION
                _speechText.value = "🎉 \"Milestone: ${event.milestone}!\""
                _subSpeechText.value = "Top performance across Making Brands this week!"
                _suggestedAction.value = "View Report"
                scheduleAutoReset(7000)
            }
            is MiloEvent.ResetToIdle -> {
                _state.value = MiloState.IDLE
                _speechText.value = "🦁 \"Let's grow today!\""
                _subSpeechText.value = "12 New Leads • 8 Follow-ups due"
                _suggestedAction.value = "Ask Milo AI"
            }
        }
    }

    /**
     * Manually set state (useful for interactive showcase and UI testing).
     */
    fun setState(newState: MiloState) {
        autoResetJob?.cancel()
        _state.value = newState
        _speechText.value = "🦁 \"${newState.defaultSpeech}\""
        _subSpeechText.value = newState.description
        _suggestedAction.value = when (newState) {
            MiloState.LEAD_IMPORTED, MiloState.NEW_LEAD -> "View Leads"
            MiloState.FOLLOW_UP -> "Call Client"
            MiloState.CONVERTED, MiloState.CELEBRATION -> "View Revenue"
            MiloState.WORKING, MiloState.SUCCESS -> "View Tasks"
            MiloState.THINKING -> "AI Insights"
            MiloState.WARNING -> "Check Alert"
            else -> "Ask Milo AI"
        }
    }

    private fun scheduleAutoReset(delayMs: Long) {
        autoResetJob = viewModelScope.launch {
            delay(delayMs)
            if (_state.value != MiloState.IDLE) {
                _state.value = MiloState.IDLE
                _speechText.value = "🦁 \"Let's grow today!\""
                _subSpeechText.value = "12 New Leads • 8 Follow-ups due"
                _suggestedAction.value = "Ask Milo AI"
            }
        }
    }

    fun openAiAssistant() {
        _isAiPanelOpen.value = true
        handleEvent(MiloEvent.Thinking("today's focus & performance"))
    }

    fun closeAiAssistant() {
        _isAiPanelOpen.value = false
        if (_state.value == MiloState.THINKING) {
            handleEvent(MiloEvent.ResetToIdle)
        }
    }

    fun askMiloAiQuery(query: String) {
        viewModelScope.launch {
            _isAiThinking.value = true
            handleEvent(MiloEvent.Thinking(query))

            try {
                val aiResponse = aiService.generateMiloResponse(
                    userPrompt = query,
                    currentMiloState = _state.value
                )
                _state.value = aiResponse.state
                _speechText.value = aiResponse.speech
                _subSpeechText.value = aiResponse.subSpeech
                _suggestedAction.value = aiResponse.suggestedAction
            } catch (e: Exception) {
                when {
                    query.contains("focus", ignoreCase = true) || query.contains("today", ignoreCase = true) -> {
                        _state.value = MiloState.WELCOME
                        _speechText.value = "🎯 \"8 leads need follow-up today and 3 tasks are overdue.\""
                        _subSpeechText.value = "Prioritize high-value Digital Marketing leads first."
                        _suggestedAction.value = "Open Follow-ups"
                    }
                    query.contains("lead", ignoreCase = true) || query.contains("import", ignoreCase = true) -> {
                        _state.value = MiloState.LEAD_IMPORTED
                        _speechText.value = "📥 \"12 new Justdial leads arrived this morning.\""
                        _subSpeechText.value = "4 require immediate WhatsApp outreach."
                        _suggestedAction.value = "View Leads"
                    }
                    query.contains("attendance", ignoreCase = true) || query.contains("team", ignoreCase = true) -> {
                        _state.value = MiloState.WORKING
                        _speechText.value = "👥 \"18/24 team members checked in on time.\""
                        _subSpeechText.value = "6 members currently in the field."
                        _suggestedAction.value = "View Attendance"
                    }
                    query.contains("draft", ignoreCase = true) || query.contains("message", ignoreCase = true) -> {
                        _state.value = MiloState.SUCCESS
                        _speechText.value = "💬 \"Drafted WhatsApp follow-up for Apex Solutions!\""
                        _subSpeechText.value = "One-tap to send customized proposal message."
                        _suggestedAction.value = "Send WhatsApp"
                    }
                    else -> {
                        _state.value = MiloState.SUCCESS
                        _speechText.value = "✨ \"Analysis complete! Everything looks on track.\""
                        _subSpeechText.value = "Let me know if you need specific client drafts."
                        _suggestedAction.value = "View CRM"
                    }
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    private fun refreshAiInsights() {
        _aiInsights.value = listOf(
            MiloAiInsight(
                title = "Immediate Client Follow-ups",
                summary = "8 high-priority leads haven't been contacted in 24 hours. Contact Apex Solutions and TechCorp today.",
                actionLabel = "Review Follow-ups",
                actionRoute = "followups",
                draftMessage = "Hello! Following up on your website & digital marketing inquiry with Making Brands. Can we connect for 5 mins today?"
            ),
            MiloAiInsight(
                title = "New Lead Batch Imported",
                summary = "12 fresh leads received from Justdial & Meta Ads campaigns. 5 assigned to your queue.",
                actionLabel = "View 12 New Leads",
                actionRoute = "leads"
            ),
            MiloAiInsight(
                title = "Milestone & Target Forecast",
                summary = "Your team is at 78% of monthly conversion target. 3 proposals in negotiation phase.",
                actionLabel = "View Quotations",
                actionRoute = "invoices"
            )
        )
    }

    fun reset() {
        handleEvent(MiloEvent.ResetToIdle)
    }
}
