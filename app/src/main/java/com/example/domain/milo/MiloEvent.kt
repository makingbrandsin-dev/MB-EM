package com.example.domain.milo

/**
 * Event-driven architecture triggers for Milo.
 * Every user action or system update in MB Tracker emits a typed MiloEvent.
 */
sealed class MiloEvent {
    data object AppOpened : MiloEvent()
    data object EmployeeLoggedIn : MiloEvent()
    data object EmployeeLoggedOut : MiloEvent()
    data class LeadImported(val count: Int = 12) : MiloEvent()
    data class LeadCreated(val leadName: String = "New Lead") : MiloEvent()
    data class FollowUpDue(val count: Int = 4, val clientName: String? = null) : MiloEvent()
    data class TaskCompleted(val taskTitle: String = "Task") : MiloEvent()
    data class LeadConverted(val leadName: String = "Client", val amount: Double = 50000.0) : MiloEvent()
    data class Warning(val message: String = "Attention required") : MiloEvent()
    data class Error(val message: String = "System issue detected") : MiloEvent()
    data class Thinking(val query: String = "Analyzing workspace...") : MiloEvent()
    data class Celebration(val milestone: String = "Daily Target Reached!") : MiloEvent()
    data object ResetToIdle : MiloEvent()
}
