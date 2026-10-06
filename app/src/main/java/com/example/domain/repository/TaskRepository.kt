package com.example.domain.repository

import com.example.domain.model.Task
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository interface for managing and tracking employee task progress and status.
 */
interface TaskRepository {
    /**
     * Observe all tasks across all projects as a reactive Flow.
     */
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Observe a specific task by its unique ID.
     */
    fun getTaskById(id: Long): Flow<Task?>

    /**
     * Filter tasks belonging to a specific project.
     */
    fun getTasksByProject(projectId: Long): Flow<List<Task>>

    /**
     * Filter tasks assigned to a specific employee name.
     */
    fun getTasksByAssignee(assignee: String): Flow<List<Task>>

    /**
     * Filter tasks by their current execution status.
     */
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    /**
     * Observe pending / in-progress tasks.
     */
    fun getPendingTasks(): Flow<List<Task>>

    /**
     * Observe completed tasks.
     */
    fun getCompletedTasks(): Flow<List<Task>>

    /**
     * Observe count of pending tasks.
     */
    fun getPendingTaskCount(): Flow<Int>

    /**
     * Observe count of completed tasks.
     */
    fun getCompletedTaskCount(): Flow<Int>

    /**
     * Retrieve immediate count of all tasks.
     */
    suspend fun getTaskCount(): Int

    /**
     * Insert a new task into persistence.
     */
    suspend fun insertTask(task: Task): Long

    /**
     * Insert a batch of tasks.
     */
    suspend fun insertTasks(tasks: List<Task>)

    /**
     * Update task details.
     */
    suspend fun updateTask(task: Task)

    /**
     * Update task progress percent and execution status.
     */
    suspend fun updateTaskProgress(taskId: Long, progressPercent: Int, status: TaskStatus)

    /**
     * Toggle the completion status of a task.
     */
    suspend fun toggleTaskCompletion(taskId: Long): Task?

    /**
     * Delete a task.
     */
    suspend fun deleteTask(task: Task)

    /**
     * Delete a task by ID.
     */
    suspend fun deleteTaskById(id: Long)

    /**
     * Clear all tasks.
     */
    suspend fun clearAllTasks()
}
