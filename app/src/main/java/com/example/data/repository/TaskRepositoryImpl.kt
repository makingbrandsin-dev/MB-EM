package com.example.data.repository

import com.example.data.local.TaskDao
import com.example.data.model.TaskEntity
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskStatus
import com.example.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Data layer implementation of [TaskRepository] backed by Room [TaskDao].
 * Provides reactive data streams and progress tracking for employee tasks.
 */
class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTaskById(id: Long): Flow<Task?> =
        taskDao.getTaskById(id).map { it?.toDomain() }

    override fun getTasksByProject(projectId: Long): Flow<List<Task>> =
        taskDao.getTasksByProject(projectId).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTasksByAssignee(assignee: String): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.filter { it.assignee.equals(assignee, ignoreCase = true) }
                .map { it.toDomain() }
        }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.filter {
                it.status.equals(status.displayName, ignoreCase = true) ||
                    it.status.equals(status.name, ignoreCase = true)
            }.map { it.toDomain() }
        }

    override fun getPendingTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.filter { !it.isCompleted }.map { it.toDomain() }
        }

    override fun getCompletedTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { entities ->
            entities.filter { it.isCompleted }.map { it.toDomain() }
        }

    override fun getPendingTaskCount(): Flow<Int> =
        taskDao.getPendingCount()

    override fun getCompletedTaskCount(): Flow<Int> =
        taskDao.getCompletedCount()

    override suspend fun getTaskCount(): Int =
        taskDao.getTaskCount()

    override suspend fun insertTask(task: Task): Long =
        taskDao.insert(task.toEntity())

    override suspend fun insertTasks(tasks: List<Task>) =
        taskDao.insertAll(tasks.map { it.toEntity() })

    override suspend fun updateTask(task: Task) =
        taskDao.update(task.toEntity())

    override suspend fun updateTaskProgress(taskId: Long, progressPercent: Int, status: TaskStatus) {
        val existingEntity = taskDao.getTaskById(taskId).firstOrNull() ?: return
        val isCompleted = status == TaskStatus.COMPLETED || progressPercent >= 100
        val updated = existingEntity.copy(
            status = if (isCompleted) TaskStatus.COMPLETED.displayName else status.displayName,
            isCompleted = isCompleted
        )
        taskDao.update(updated)
    }

    override suspend fun toggleTaskCompletion(taskId: Long): Task? {
        val existingEntity = taskDao.getTaskById(taskId).firstOrNull() ?: return null
        val newCompleted = !existingEntity.isCompleted
        val updated = existingEntity.copy(
            isCompleted = newCompleted,
            status = if (newCompleted) TaskStatus.COMPLETED.displayName else TaskStatus.IN_PROGRESS.displayName
        )
        taskDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun deleteTask(task: Task) =
        taskDao.delete(task.toEntity())

    override suspend fun deleteTaskById(id: Long) =
        taskDao.deleteById(id)

    override suspend fun clearAllTasks() =
        taskDao.clearAll()

    // --- Data Mapper Extensions ---

    private fun TaskEntity.toDomain(): Task {
        val parsedStatus = TaskStatus.fromString(status)
        val parsedPriority = TaskPriority.fromString(priority)
        val parsedCategory = TaskCategory.fromString(category)

        return Task(
            id = id,
            projectId = projectId,
            projectName = projectName,
            title = title,
            description = "",
            dueDate = dueDate,
            priority = parsedPriority,
            status = if (isCompleted) TaskStatus.COMPLETED else parsedStatus,
            progressPercent = if (isCompleted) 100 else 50,
            isCompleted = isCompleted,
            assignee = assignee,
            assigneeId = 1L,
            category = parsedCategory,
            estimatedTimeNeeded = estimatedTimeNeeded,
            spentTimeMinutes = 0L,
            dependsOnTaskId = dependsOnTaskId,
            dependsOnTaskTitle = dependsOnTaskTitle,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun Task.toEntity(): TaskEntity =
        TaskEntity(
            id = id,
            projectId = projectId,
            projectName = projectName,
            title = title,
            dueDate = dueDate,
            priority = priority.displayName,
            status = status.displayName,
            isCompleted = isCompleted || status == TaskStatus.COMPLETED,
            assignee = assignee,
            category = category.displayName,
            estimatedTimeNeeded = estimatedTimeNeeded,
            dependsOnTaskId = dependsOnTaskId,
            dependsOnTaskTitle = dependsOnTaskTitle
        )
}
