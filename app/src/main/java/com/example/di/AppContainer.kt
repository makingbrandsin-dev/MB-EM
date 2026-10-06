package com.example.di

import android.content.Context
import com.example.data.local.*
import com.example.data.repository.*

/**
 * Dependency Injection container providing singleton instances of Room Database,
 * DAOs, and Repositories throughout the application.
 */
class AppContainer(private val context: Context) {

    // Database Singleton
    val database: AppDatabase by lazy {
        DatabaseModule.provideAppDatabase(context)
    }

    // DAOs
    val employeeDao: EmployeeDao by lazy { DatabaseModule.provideEmployeeDao(database) }
    val projectDao: ProjectDao by lazy { DatabaseModule.provideProjectDao(database) }
    val taskDao: TaskDao by lazy { DatabaseModule.provideTaskDao(database) }
    val attendanceDao: AttendanceDao by lazy { DatabaseModule.provideAttendanceDao(database) }
    val leadDao: LeadDao by lazy { DatabaseModule.provideLeadDao(database) }
    val followUpDao: FollowUpDao by lazy { DatabaseModule.provideFollowUpDao(database) }
    val callLogDao: CallLogDao by lazy { DatabaseModule.provideCallLogDao(database) }
    val chatDao: ChatDao by lazy { DatabaseModule.provideChatDao(database) }
    val notificationDao: NotificationDao by lazy { DatabaseModule.provideNotificationDao(database) }
    val userProfileDao: UserProfileDao by lazy { DatabaseModule.provideUserProfileDao(database) }
    val leaveDao: LeaveDao by lazy { DatabaseModule.provideLeaveDao(database) }
    val leadSourceDao: LeadSourceDao by lazy { DatabaseModule.provideLeadSourceDao(database) }
    val callRecordingDao: CallRecordingDao by lazy { DatabaseModule.provideCallRecordingDao(database) }
    val invoiceDao: InvoiceDao by lazy { DatabaseModule.provideInvoiceDao(database) }
    val quotationDao: QuotationDao by lazy { DatabaseModule.provideQuotationDao(database) }
    val autoBrochureDao: AutoBrochureDao by lazy { DatabaseModule.provideAutoBrochureDao(database) }
    val socialReviewDao: SocialReviewDao by lazy { DatabaseModule.provideSocialReviewDao(database) }
    val clientMeetingDao: ClientMeetingDao by lazy { DatabaseModule.provideClientMeetingDao(database) }
    val expenseClaimDao: ExpenseClaimDao by lazy { DatabaseModule.provideExpenseClaimDao(database) }
    val projectMilestoneDao: ProjectMilestoneDao by lazy { DatabaseModule.provideProjectMilestoneDao(database) }
    val vaultDocumentDao: VaultDocumentDao by lazy { DatabaseModule.provideVaultDocumentDao(database) }
    val attendanceRegularizationDao: AttendanceRegularizationDao by lazy {
        DatabaseModule.provideAttendanceRegularizationDao(database)
    }
    val activityFeedDao: ActivityFeedDao by lazy { database.activityFeedDao() }
    val auditLogDao: AuditLogDao by lazy { database.auditLogDao() }
    val shiftDao: ShiftDao by lazy { database.shiftDao() }

    // Repositories
    val employeeRepository: IEmployeeRepository by lazy {
        RepositoryModule.provideEmployeeRepository(employeeDao)
    }

    val projectRepository: IProjectRepository by lazy {
        RepositoryModule.provideProjectRepository(projectDao)
    }

    val taskRepository: ITaskRepository by lazy {
        RepositoryModule.provideTaskRepository(taskDao)
    }

    val leadRepository: com.example.domain.repository.LeadRepository by lazy {
        RepositoryModule.provideLeadRepository(leadDao)
    }

    val domainTaskRepository: com.example.domain.repository.TaskRepository by lazy {
        RepositoryModule.provideDomainTaskRepository(taskDao)
    }
}
