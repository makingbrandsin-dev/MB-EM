package com.example.domain.model

/**
 * Domain status enum for tracking Employee Task execution.
 */
enum class TaskStatus(val displayName: String) {
    BACKLOG("Backlog"),
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    COMPLETED("Completed"),
    BLOCKED("Blocked");

    companion object {
        fun fromString(value: String): TaskStatus =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: if (value.equals("Done", ignoreCase = true)) COMPLETED else IN_PROGRESS
    }
}

/**
 * Domain priority enum for Employee Tasks.
 */
enum class TaskPriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromString(value: String): TaskPriority =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: MEDIUM
    }
}

/**
 * Domain category enum for grouping Tasks.
 */
enum class TaskCategory(val displayName: String) {
    WORK("Work"),
    PERSONAL("Personal"),
    URGENT("Urgent"),
    MEETING("Meeting"),
    REVIEW("Review");

    companion object {
        fun fromString(value: String): TaskCategory =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: WORK
    }
}

/**
 * Domain representation of a Project / Employee Task.
 */
data class Task(
    val id: Long = 0,
    val projectId: Long = 1,
    val projectName: String = "Making Brands Core Revamp",
    val title: String,
    val description: String = "",
    val dueDate: String = "Today",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val progressPercent: Int = if (status == TaskStatus.COMPLETED) 100 else 40,
    val isCompleted: Boolean = status == TaskStatus.COMPLETED,
    val assignee: String = "Rahul Sharma",
    val assigneeId: Long = 1,
    val category: TaskCategory = TaskCategory.WORK,
    val estimatedTimeNeeded: String = "4 Hours",
    val spentTimeMinutes: Long = 0,
    val dependsOnTaskId: Long? = null,
    val dependsOnTaskTitle: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
