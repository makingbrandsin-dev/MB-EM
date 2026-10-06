package com.example.data.repository

import com.example.data.local.EmployeeDao
import com.example.data.local.ProjectDao
import com.example.data.local.TaskDao
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeStatus
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Interface defining data operations for Employees.
 */
interface IEmployeeRepository {
    val allEmployees: Flow<List<EmployeeEntity>>
    val employeeCount: Flow<Int>
    fun getEmployeeById(id: Long): Flow<EmployeeEntity?>
    fun getEmployeesByDepartment(department: Department): Flow<List<EmployeeEntity>>
    fun getEmployeesByStatus(status: EmployeeStatus): Flow<List<EmployeeEntity>>
    fun getEmployeesByIds(ids: List<Long>): Flow<List<EmployeeEntity>>
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity?
    fun getEmployeeByEmailFlow(email: String): Flow<EmployeeEntity?>
    suspend fun getEmployeeCountDirect(): Int
    suspend fun insert(employee: EmployeeEntity): Long
    suspend fun insertAll(employees: List<EmployeeEntity>)
    suspend fun update(employee: EmployeeEntity)
    suspend fun delete(employee: EmployeeEntity)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}

/**
 * Repository providing clean, abstracted data access for Employee entities.
 */
class EmployeeRepository(
    private val employeeDao: EmployeeDao
) : IEmployeeRepository {
    override val allEmployees: Flow<List<EmployeeEntity>> = employeeDao.getAllEmployees()
    override val employeeCount: Flow<Int> = employeeDao.getEmployeeCount()

    override fun getEmployeeById(id: Long): Flow<EmployeeEntity?> = employeeDao.getEmployeeById(id)

    override fun getEmployeesByDepartment(department: Department): Flow<List<EmployeeEntity>> =
        employeeDao.getEmployeesByDepartment(department)

    override fun getEmployeesByStatus(status: EmployeeStatus): Flow<List<EmployeeEntity>> =
        employeeDao.getEmployeesByStatus(status)

    override fun getEmployeesByIds(ids: List<Long>): Flow<List<EmployeeEntity>> =
        employeeDao.getEmployeesByIds(ids)

    override suspend fun getEmployeeByEmail(email: String): EmployeeEntity? =
        employeeDao.getEmployeeByEmail(email)

    override fun getEmployeeByEmailFlow(email: String): Flow<EmployeeEntity?> =
        employeeDao.getEmployeeByEmailFlow(email)

    override suspend fun getEmployeeCountDirect(): Int =
        employeeDao.getEmployeeCountDirect()

    override suspend fun insert(employee: EmployeeEntity): Long = employeeDao.insert(employee)

    override suspend fun insertAll(employees: List<EmployeeEntity>) = employeeDao.insertAll(employees)

    override suspend fun update(employee: EmployeeEntity) = employeeDao.update(employee)

    override suspend fun delete(employee: EmployeeEntity) = employeeDao.delete(employee)

    override suspend fun deleteById(id: Long) = employeeDao.deleteById(id)

    override suspend fun clearAll() = employeeDao.clearAll()
}

/**
 * Interface defining data operations for Projects.
 */
interface IProjectRepository {
    val allProjects: Flow<List<ProjectEntity>>
    val projectCount: Flow<Int>
    fun getProjectById(id: Long): Flow<ProjectEntity?>
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>
    suspend fun insert(project: ProjectEntity): Long
    suspend fun insertAll(projects: List<ProjectEntity>)
    suspend fun update(project: ProjectEntity)
    suspend fun updateProjectProgress(projectId: Long, progressPercent: Int, completedTasks: Int, totalTasks: Int)
    suspend fun delete(project: ProjectEntity)
    suspend fun deleteById(id: Long)
    suspend fun clearAll()
}

/**
 * Repository providing clean, abstracted data access for Project entities.
 */
class ProjectRepository(
    private val projectDao: ProjectDao
) : IProjectRepository {
    override val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    override val projectCount: Flow<Int> = projectDao.getProjectCount()

    override fun getProjectById(id: Long): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    override fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>> =
        projectDao.getProjectsByStatus(status)

    override suspend fun insert(project: ProjectEntity): Long = projectDao.insert(project)

    override suspend fun insertAll(projects: List<ProjectEntity>) = projectDao.insertAll(projects)

    override suspend fun update(project: ProjectEntity) = projectDao.update(project)

    override suspend fun updateProjectProgress(
        projectId: Long,
        progressPercent: Int,
        completedTasks: Int,
        totalTasks: Int
    ) {
        val existingProject = projectDao.getProjectById(projectId).firstOrNull() ?: return
        val updatedProject = existingProject.copy(
            progressPercent = progressPercent.coerceIn(0, 100),
            completedTasks = completedTasks,
            totalTasks = totalTasks,
            status = if (progressPercent >= 100) "Completed" else existingProject.status
        )
        projectDao.update(updatedProject)
    }

    override suspend fun delete(project: ProjectEntity) = projectDao.delete(project)

    override suspend fun deleteById(id: Long) = projectDao.deleteById(id)

    override suspend fun clearAll() = projectDao.clearAll()
}

/**
 * Interface defining data operations for Tasks.
 */
interface ITaskRepository {
    val allTasks: Flow<List<TaskEntity>>
    val pendingTaskCount: Flow<Int>
    val completedTaskCount: Flow<Int>
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>
    suspend fun getTaskCount(): Int
    suspend fun toggleTaskCompletion(task: TaskEntity): TaskEntity
    suspend fun insert(task: TaskEntity): Long
    suspend fun insertAll(tasks: List<TaskEntity>)
    suspend fun update(task: TaskEntity)
    suspend fun delete(task: TaskEntity)
    suspend fun clearAll()
}

/**
 * Repository providing clean, abstracted data access for Task entities.
 */
class TaskRepository(
    private val taskDao: TaskDao
) : ITaskRepository {
    override val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    override val pendingTaskCount: Flow<Int> = taskDao.getPendingCount()
    override val completedTaskCount: Flow<Int> = taskDao.getCompletedCount()

    override fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>> =
        taskDao.getTasksByProject(projectId)

    override suspend fun getTaskCount(): Int = taskDao.getTaskCount()

    override suspend fun toggleTaskCompletion(task: TaskEntity): TaskEntity {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted,
            status = if (!task.isCompleted) "Completed" else "In Progress"
        )
        taskDao.update(updatedTask)
        return updatedTask
    }

    override suspend fun insert(task: TaskEntity): Long = taskDao.insert(task)

    override suspend fun insertAll(tasks: List<TaskEntity>) = taskDao.insertAll(tasks)

    override suspend fun update(task: TaskEntity) = taskDao.update(task)

    override suspend fun delete(task: TaskEntity) = taskDao.delete(task)

    override suspend fun clearAll() = taskDao.clearAll()
}
