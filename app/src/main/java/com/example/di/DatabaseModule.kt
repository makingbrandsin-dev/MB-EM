package com.example.di

import android.content.Context
import com.example.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for provisioning Room database and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideEmployeeDao(database: AppDatabase): EmployeeDao {
        return database.employeeDao()
    }

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao {
        return database.projectDao()
    }

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    fun provideLeadDao(database: AppDatabase): LeadDao {
        return database.leadDao()
    }

    @Provides
    fun provideFollowUpDao(database: AppDatabase): FollowUpDao {
        return database.followUpDao()
    }

    @Provides
    fun provideCallLogDao(database: AppDatabase): CallLogDao {
        return database.callLogDao()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    fun provideLeaveDao(database: AppDatabase): LeaveDao {
        return database.leaveDao()
    }

    @Provides
    fun provideLeadSourceDao(database: AppDatabase): LeadSourceDao {
        return database.leadSourceDao()
    }

    @Provides
    fun provideCallRecordingDao(database: AppDatabase): CallRecordingDao {
        return database.callRecordingDao()
    }

    @Provides
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao {
        return database.invoiceDao()
    }

    @Provides
    fun provideQuotationDao(database: AppDatabase): QuotationDao {
        return database.quotationDao()
    }

    @Provides
    fun provideAutoBrochureDao(database: AppDatabase): AutoBrochureDao {
        return database.autoBrochureDao()
    }

    @Provides
    fun provideSocialReviewDao(database: AppDatabase): SocialReviewDao {
        return database.socialReviewDao()
    }

    @Provides
    fun provideClientMeetingDao(database: AppDatabase): ClientMeetingDao {
        return database.clientMeetingDao()
    }

    @Provides
    fun provideExpenseClaimDao(database: AppDatabase): ExpenseClaimDao {
        return database.expenseClaimDao()
    }

    @Provides
    fun provideProjectMilestoneDao(database: AppDatabase): ProjectMilestoneDao {
        return database.projectMilestoneDao()
    }

    @Provides
    fun provideVaultDocumentDao(database: AppDatabase): VaultDocumentDao {
        return database.vaultDocumentDao()
    }

    @Provides
    fun provideAttendanceRegularizationDao(database: AppDatabase): AttendanceRegularizationDao {
        return database.attendanceRegularizationDao()
    }
}
