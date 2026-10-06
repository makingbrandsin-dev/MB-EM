package com.example.milo

/**
 * Feature 5: AI Subtask Breakdown & Smart Estimator (MiloTaskDecomposer.kt)
 * Decomposes complex task titles into actionable subtasks with time estimates.
 */
data class SubtaskItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean = false
)

data class TaskDecompositionResult(
    val originalTaskTitle: String,
    val subtasks: List<SubtaskItem>,
    val totalEstimatedMinutes: Int
)

object MiloTaskDecomposer {

    fun decomposeTask(taskTitle: String): TaskDecompositionResult {
        val lower = taskTitle.lowercase().trim()

        val subtasks = when {
            lower.contains("proposal") || lower.contains("quotation") -> listOf(
                SubtaskItem(title = "Review client scope & line items", estimatedMinutes = 15),
                SubtaskItem(title = "Calculate pricing & bulk discounts", estimatedMinutes = 20),
                SubtaskItem(title = "Draft formal PDF proposal", estimatedMinutes = 25),
                SubtaskItem(title = "Send via WhatsApp & email to client", estimatedMinutes = 10)
            )

            lower.contains("client visit") || lower.contains("site visit") || lower.contains("meeting") -> listOf(
                SubtaskItem(title = "Confirm meeting time & location via call", estimatedMinutes = 5),
                SubtaskItem(title = "Prepare product samples & presentation", estimatedMinutes = 20),
                SubtaskItem(title = "Conduct site inspection & note requirements", estimatedMinutes = 45),
                SubtaskItem(title = "Log meeting summary in MB Tracker CRM", estimatedMinutes = 10)
            )

            lower.contains("audit") || lower.contains("review") -> listOf(
                SubtaskItem(title = "Gather current performance metrics", estimatedMinutes = 15),
                SubtaskItem(title = "Identify bottleneck areas & high-value gaps", estimatedMinutes = 30),
                SubtaskItem(title = "Compile action items list for team", estimatedMinutes = 20)
            )

            lower.contains("onboard") || lower.contains("new client") -> listOf(
                SubtaskItem(title = "Send welcome kit & agreement document", estimatedMinutes = 15),
                SubtaskItem(title = "Create client account in CRM portal", estimatedMinutes = 10),
                SubtaskItem(title = "Schedule kickoff alignment call", estimatedMinutes = 15),
                SubtaskItem(title = "Assign dedicated account manager", estimatedMinutes = 5)
            )

            lower.contains("payment") || lower.contains("invoice") || lower.contains("follow up") -> listOf(
                SubtaskItem(title = "Verify pending invoice balance", estimatedMinutes = 5),
                SubtaskItem(title = "Send payment link / UPI QR code via WhatsApp", estimatedMinutes = 5),
                SubtaskItem(title = "Call client accounts representative", estimatedMinutes = 10),
                SubtaskItem(title = "Record receipt & issue payment voucher", estimatedMinutes = 10)
            )

            else -> listOf(
                SubtaskItem(title = "Analyze requirements & plan approach", estimatedMinutes = 15),
                SubtaskItem(title = "Execute core task steps", estimatedMinutes = 30),
                SubtaskItem(title = "Perform quality check & verify result", estimatedMinutes = 15),
                SubtaskItem(title = "Update CRM task status & inform manager", estimatedMinutes = 5)
            )
        }

        val totalMinutes = subtasks.sumOf { it.estimatedMinutes }

        return TaskDecompositionResult(
            originalTaskTitle = taskTitle,
            subtasks = subtasks,
            totalEstimatedMinutes = totalMinutes
        )
    }
}
