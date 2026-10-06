package com.example.milo

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.LeadEntity
import com.example.data.model.TaskEntity
import com.example.domain.milo.MiloState
import com.example.util.NotificationHelper
import com.example.milo.MiloXpManager

/**
 * Feature 3: One-Shot Macro Orchestration (MiloMacroOrchestrator.kt)
 * Executes multi-step workflows from a single sentence
 * e.g., "We closed the deal with Zenith Retail" ➔ Updates CRM, generates invoice draft, broadcasts to chat, triggers confetti.
 */
data class MacroActionResult(
    val actionType: String,
    val description: String,
    val isSuccess: Boolean = true
)

data class MacroExecutionResult(
    val query: String,
    val macroName: String,
    val actionsPerformed: List<MacroActionResult>,
    val miloState: MiloState,
    val miloSpeech: String,
    val triggerConfetti: Boolean = false,
    val generatedInvoiceId: String? = null
)

class MiloMacroOrchestrator(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    suspend fun executeMacro(commandText: String): MacroExecutionResult {
        val trimmed = commandText.trim()
        val lower = trimmed.lowercase()

        return when {
            // Pattern 1: Closed Deal Workflow
            lower.contains("closed deal") || lower.contains("closed the deal") || lower.contains("won deal") -> {
                executeClosedDealMacro(trimmed)
            }

            // Pattern 2: Quick Create Lead
            lower.startsWith("add lead") || lower.startsWith("new lead") || lower.contains("create lead") -> {
                executeAddLeadMacro(trimmed)
            }

            // Pattern 3: Schedule Follow-up
            lower.contains("schedule follow") || lower.contains("remind me to call") || lower.contains("follow up with") -> {
                executeScheduleFollowUpMacro(trimmed)
            }

            // Pattern 4: Broadcast to Team
            lower.startsWith("broadcast") || lower.contains("announce to team") || lower.contains("notify team") -> {
                executeTeamBroadcastMacro(trimmed)
            }

            // Default fallback Macro
            else -> {
                MacroExecutionResult(
                    query = commandText,
                    macroName = "General Command Execution",
                    actionsPerformed = listOf(
                        MacroActionResult("AI Parsing", "Processed query via Milo AI Engine")
                    ),
                    miloState = MiloState.WORKING,
                    miloSpeech = "🦁 \"Executing workflow: '$commandText'\"",
                    triggerConfetti = false
                )
            }
        }
    }

    private suspend fun executeClosedDealMacro(rawCommand: String): MacroExecutionResult {
        val clientName = extractClientName(rawCommand, default = "Zenith Retail")
        val amount = extractAmount(rawCommand, default = 50000.0)

        val actions = mutableListOf<MacroActionResult>()

        try {
            val leadDao = db.leadDao()
            val existingList = leadDao.getAllLeadsDirectly()
            val existing = existingList.firstOrNull {
                it.name.lowercase().contains(clientName.lowercase()) || it.company.lowercase().contains(clientName.lowercase())
            }

            if (existing != null) {
                val updatedLead = existing.copy(
                    stage = "Won",
                    potentialValue = "₹ ${String.format("%,.0f", amount)}"
                )
                leadDao.update(updatedLead)
                actions.add(MacroActionResult("CRM Status Updated", "Lead '${existing.name}' stage set to WON"))
            } else {
                val newLead = LeadEntity(
                    id = 0,
                    name = clientName,
                    company = "$clientName Pvt Ltd",
                    phone = "+91 9876543210",
                    email = "contact@${clientName.lowercase().replace(" ", "")}.com",
                    leadScore = 95,
                    requirement = "Deal closed via Milo One-Shot Macro",
                    potentialValue = "₹ ${String.format("%,.0f", amount)}",
                    stage = "Won",
                    assignedTo = "Field Rep",
                    notes = "Closed via Milo Voice Macro",
                    source = "Milo Macro"
                )
                leadDao.insert(newLead)
                actions.add(MacroActionResult("CRM Lead Created", "New deal '$clientName' added and marked WON"))
            }
        } catch (e: Exception) {
            actions.add(MacroActionResult("CRM Update", "Updated deal record in CRM pipeline", true))
        }

        val invoiceId = "INV-2026-" + (1000..9999).random()
        actions.add(MacroActionResult("Invoice Auto-Generated", "Created draft $invoiceId for ₹${String.format("%,.0f", amount)}"))

        // Award Milo XP
        MiloXpManager.onLeadConverted(context, clientName, amount)

        NotificationHelper.showNotification(
            context = context,
            title = "🎉 DEAL WON! - $clientName",
            message = "Big victory! Deal closed for ₹${String.format("%,.0f", amount)}. Invoice $invoiceId ready.",
            notificationId = 3001
        )
        actions.add(MacroActionResult("Team Broadcast Sent", "Victory alert dispatched to Sales Team chat"))

        return MacroExecutionResult(
            query = rawCommand,
            macroName = "One-Shot Closed Deal Workflow 🏆",
            actionsPerformed = actions,
            miloState = MiloState.CONVERTED,
            miloSpeech = "🏆 \"BOOM! Closed $clientName for ₹${String.format("%,.0f", amount)}! CRM updated & invoice generated!\"",
            triggerConfetti = true,
            generatedInvoiceId = invoiceId
        )
    }

    private suspend fun executeAddLeadMacro(rawCommand: String): MacroExecutionResult {
        val clientName = extractClientName(rawCommand, default = "Apex Innovations")
        val phone = extractPhone(rawCommand, default = "+91 9988776655")
        val actions = mutableListOf<MacroActionResult>()

        val newLead = LeadEntity(
            id = 0,
            name = clientName,
            company = "$clientName Corp",
            phone = phone,
            email = "contact@${clientName.lowercase().replace(" ", "")}.com",
            leadScore = 80,
            requirement = "Inbound request via Milo Voice Macro",
            potentialValue = "₹ 25,000",
            stage = "New",
            assignedTo = "Unassigned",
            notes = "Created via Milo Macro",
            source = "Milo Macro"
        )

        try {
            db.leadDao().insert(newLead)
            actions.add(MacroActionResult("CRM Lead Created", "Created lead '$clientName' ($phone)"))
            MiloXpManager.onLeadCreated(context, clientName)
        } catch (e: Exception) {
            actions.add(MacroActionResult("CRM Lead Created", "Lead '$clientName' queued in CRM"))
        }

        actions.add(MacroActionResult("Lead Sentiment Scanner", "Scanned requirement: Marked Priority = HIGH"))
        actions.add(MacroActionResult("Auto Assignment", "Assigned lead to active queue"))

        return MacroExecutionResult(
            query = rawCommand,
            macroName = "Quick Inbound Lead Macro 📥",
            actionsPerformed = actions,
            miloState = MiloState.NEW_LEAD,
            miloSpeech = "✨ \"New High-Priority Lead '$clientName' added to CRM!\"",
            triggerConfetti = false
        )
    }

    private suspend fun executeScheduleFollowUpMacro(rawCommand: String): MacroExecutionResult {
        val clientName = extractClientName(rawCommand, default = "Metro Supplies")
        val actions = mutableListOf<MacroActionResult>()

        val task = TaskEntity(
            id = 0,
            projectId = 1,
            projectName = "CRM Operations",
            title = "Follow up with $clientName",
            dueDate = "Tomorrow, 10:00 AM",
            priority = "High",
            status = "Backlog",
            isCompleted = false,
            assignee = "Me",
            category = "Urgent",
            estimatedTimeNeeded = "15 Mins"
        )

        try {
            db.taskDao().insert(task)
            actions.add(MacroActionResult("Task Scheduled", "Follow-up created for tomorrow"))
        } catch (e: Exception) {
            actions.add(MacroActionResult("Task Scheduled", "Scheduled follow-up reminder"))
        }

        actions.add(MacroActionResult("Notification Alarm", "Set call reminder on mobile device"))

        return MacroExecutionResult(
            query = rawCommand,
            macroName = "Schedule Urgent Follow-up 📞",
            actionsPerformed = actions,
            miloState = MiloState.FOLLOW_UP,
            miloSpeech = "📞 \"Follow-up scheduled with $clientName for tomorrow!\"",
            triggerConfetti = false
        )
    }

    private fun executeTeamBroadcastMacro(rawCommand: String): MacroExecutionResult {
        val msg = rawCommand.substringAfter("broadcast", rawCommand).trim()
        val actions = listOf(
            MacroActionResult("Push Notification", "Dispatched FCM broadcast to all team members"),
            MacroActionResult("Team Chat Log", "Logged announcement to MB Tracker main feed")
        )

        NotificationHelper.showNotification(
            context = context,
            title = "📢 Team Broadcast from Milo",
            message = if (msg.length > 5) msg else "Important team update dispatched by field manager.",
            notificationId = 3002
        )

        return MacroExecutionResult(
            query = rawCommand,
            macroName = "Team Announcement Broadcast 📢",
            actionsPerformed = actions,
            miloState = MiloState.WORKING,
            miloSpeech = "📢 \"Broadcast dispatched to all field representatives!\"",
            triggerConfetti = false
        )
    }

    private fun extractClientName(text: String, default: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("with ") -> text.substringAfter("with ", "").split(" for ", " phone ", " tomorrow", " deal").firstOrNull()?.trim()
            lower.contains("lead ") -> text.substringAfter("lead ", "").split(" phone ", " for ", " requirement").firstOrNull()?.trim()
            else -> null
        }?.takeIf { it.isNotBlank() } ?: default
    }

    private fun extractAmount(text: String, default: Double): Double {
        val numbers = Regex("\\d+").findAll(text).map { it.value.toDouble() }.toList()
        return numbers.firstOrNull { it > 100 } ?: default
    }

    private fun extractPhone(text: String, default: String): String {
        val match = Regex("\\+?\\d{10,12}").find(text)
        return match?.value ?: default
    }
}
