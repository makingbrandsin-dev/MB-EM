package com.example.di

import com.example.data.local.EmployeeDao
import com.example.data.local.ProjectDao
import com.example.data.local.TaskDao
import com.example.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for provisioning repositories with singleton lifecycles.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideEmployeeRepository(employeeDao: EmployeeDao): EmployeeRepository {
        return EmployeeRepository(employeeDao)
    }

    @Provides
    @Singleton
    fun provideIEmployeeRepository(employeeRepository: EmployeeRepository): IEmployeeRepository {
        return employeeRepository
    }

    @Provides
    @Singleton
    fun provideProjectRepository(projectDao: ProjectDao): ProjectRepository {
        return ProjectRepository(projectDao)
    }

    @Provides
    @Singleton
    fun provideIProjectRepository(projectRepository: ProjectRepository): IProjectRepository {
        return projectRepository
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepository(taskDao)
    }

    @Provides
    @Singleton
    fun provideITaskRepository(taskRepository: TaskRepository): ITaskRepository {
        return taskRepository
    }

    @Provides
    @Singleton
    fun provideLeadRepository(leadDao: com.example.data.local.LeadDao): com.example.domain.repository.LeadRepository {
        return LeadRepositoryImpl(leadDao)
    }

    @Provides
    @Singleton
    fun provideDomainTaskRepository(taskDao: TaskDao): com.example.domain.repository.TaskRepository {
        return TaskRepositoryImpl(taskDao)
    }
}
