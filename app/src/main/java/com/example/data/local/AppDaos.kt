package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY id DESC LIMIT 1")
    fun getLatestAttendance(): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance_records ORDER BY id DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecord): Long

    @Update
    suspend fun update(record: AttendanceRecord)

    @Delete
    suspend fun delete(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0 ORDER BY id ASC")
    suspend fun getUnsyncedAttendance(): List<AttendanceRecord>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isSynced = 0")
    fun getUnsyncedAttendanceCount(): Flow<Int>

    @Query("UPDATE attendance_records SET isSynced = 1 WHERE id = :id")
    suspend fun markAttendanceAsSynced(id: Long)

    @Query("UPDATE attendance_records SET isSynced = 1")
    suspend fun markAllAttendanceAsSynced()

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getAttendanceRecordByIdDirect(id: Long): AttendanceRecord?

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY id ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees ORDER BY id ASC")
    suspend fun getAllEmployeesDirectly(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun getEmployeeById(id: Long): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE department = :department ORDER BY name ASC")
    fun getEmployeesByDepartment(department: Department): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE status = :status ORDER BY name ASC")
    fun getEmployeesByStatus(status: EmployeeStatus): Flow<List<EmployeeEntity>>

    @Query("SELECT COUNT(*) FROM employees")
    fun getEmployeeCount(): Flow<Int>

    @Query("SELECT * FROM employees WHERE id IN (:ids)")
    fun getEmployeesByIds(ids: List<Long>): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getEmployeeByEmail(email: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun getEmployeeByEmailFlow(email: String): Flow<EmployeeEntity?>

    @Query("SELECT COUNT(*) FROM employees")
    suspend fun getEmployeeCountDirect(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(employee: EmployeeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM employees")
    suspend fun clearAll()
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY id ASC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdDirect(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY id ASC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<ProjectEntity>)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM projects")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId ORDER BY id DESC")
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface LeadDao {
    @Query("SELECT * FROM leads WHERE phone = :phone LIMIT 1")
    suspend fun getLeadByPhone(phone: String): LeadEntity?

    @Query("SELECT * FROM leads ORDER BY id DESC, createdAt DESC")
    fun getAllLeads(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads ORDER BY id DESC, createdAt DESC")
    suspend fun getAllLeadsDirectly(): List<LeadEntity>

    @Query("SELECT * FROM leads WHERE id = :id")
    fun getLeadById(id: Long): Flow<LeadEntity?>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getLeadByIdDirect(id: Long): LeadEntity?

    @Query("SELECT * FROM leads WHERE source = :source ORDER BY id DESC, createdAt DESC")
    fun getLeadsBySource(source: String): Flow<List<LeadEntity>>

    @Query("SELECT COUNT(*) FROM leads")
    suspend fun getLeadCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(lead: LeadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(leads: List<LeadEntity>)

    @Update
    suspend fun update(lead: LeadEntity)

    @Delete
    suspend fun delete(lead: LeadEntity)

    @Query("DELETE FROM leads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM leads")
    suspend fun clearAll()
}

@Dao
interface LeadSourceDao {
    @Query("SELECT * FROM lead_source_configs ORDER BY displayName ASC")
    fun getAllSourceConfigs(): Flow<List<LeadSourceConfigEntity>>

    @Query("SELECT * FROM lead_source_configs WHERE sourceId = :sourceId LIMIT 1")
    fun getSourceConfigById(sourceId: String): Flow<LeadSourceConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: LeadSourceConfigEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<LeadSourceConfigEntity>)

    @Update
    suspend fun update(config: LeadSourceConfigEntity)

    @Delete
    suspend fun delete(config: LeadSourceConfigEntity)
}

@Dao
interface FollowUpDao {
    @Query("SELECT * FROM follow_ups ORDER BY id DESC")
    fun getAllFollowUps(): Flow<List<FollowUpEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(followUp: FollowUpEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(followUps: List<FollowUpEntity>)

    @Update
    suspend fun update(followUp: FollowUpEntity)

    @Query("DELETE FROM follow_ups")
    suspend fun clearAll()
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs WHERE callType != 'Incoming' AND callType != 'Missed' ORDER BY id DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE leadUpdated = 0 AND transcription IS NOT NULL AND aiInsights IS NOT NULL")
    suspend fun getUnprocessedCallLogs(): List<CallLogEntity>

    @Query("UPDATE call_logs SET leadUpdated = 1 WHERE id = :id")
    suspend fun markAsProcessed(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CallLogEntity>)

    @Delete
    suspend fun delete(log: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAll()
}

@Dao
interface ChatDao {
    @Query("""
        SELECT * FROM chat_messages 
        WHERE channelId = :channelId 
           OR REPLACE(channelId, '-', '_') = REPLACE(:channelId, '-', '_')
        ORDER BY id ASC
    """)
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY id DESC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun getMessageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Delete
    suspend fun delete(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()

    @Query("""
        UPDATE chat_messages 
        SET isRead = 1, readBy = :readerName, readAt = :readAt
        WHERE (channelId = :channelId OR REPLACE(channelId, '-', '_') = REPLACE(:channelId, '-', '_'))
          AND isMe = 0
          AND isRead = 0
    """)
    suspend fun markChannelMessagesAsRead(channelId: String, readerName: String, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_messages SET isRead = 1, readBy = :readerName, readAt = :readAt WHERE id = :messageId")
    suspend fun markMessageAsRead(messageId: Long, readerName: String, readAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT COUNT(*) FROM chat_messages 
        WHERE (channelId = :channelId OR REPLACE(channelId, '-', '_') = REPLACE(:channelId, '-', '_'))
          AND isMe = 0
          AND isRead = 0
    """)
    fun getUnreadCountForChannel(channelId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isMe = 0 AND isRead = 0")
    fun getTotalUnreadChatCount(): Flow<Int>

    @Query("UPDATE chat_messages SET reactionsJson = :reactionsJson WHERE id = :messageId")
    suspend fun updateReactions(messageId: Long, reactionsJson: String)

    @Query("UPDATE chat_messages SET audioPath = :audioPath WHERE id = :messageId")
    suspend fun updateAudioPath(messageId: Long, audioPath: String)

    @Query("SELECT * FROM chat_messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Long): ChatMessageEntity?

    @Query("SELECT * FROM chat_messages WHERE isSynced = 0 ORDER BY id ASC")
    suspend fun getUnsyncedMessages(): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE isSynced = 0")
    fun getUnsyncedMessageCount(): Flow<Int>

    @Query("UPDATE chat_messages SET isSynced = 1 WHERE id = :id")
    suspend fun markMessageAsSynced(id: Long)

    @Query("UPDATE chat_messages SET isSynced = 1")
    suspend fun markAllMessagesAsSynced()
}

@Dao
interface ActivityFeedDao {
    @Query("SELECT * FROM activity_feed_items ORDER BY isPinned DESC, createdAt DESC")
    fun getAllFeedItems(): Flow<List<ActivityFeedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ActivityFeedItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ActivityFeedItemEntity>)

    @Query("UPDATE activity_feed_items SET likesCount = :count, likedByUsers = :likedBy WHERE id = :id")
    suspend fun updateLikes(id: Long, count: Int, likedBy: String)

    @Query("UPDATE activity_feed_items SET reactionsJson = :reactionsJson WHERE id = :id")
    suspend fun updateReactions(id: Long, reactionsJson: String)

    @Delete
    suspend fun delete(item: ActivityFeedItemEntity)

    @Query("DELETE FROM activity_feed_items")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY id DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun getNotificationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE isRead = 0")
    suspend fun getUnreadNotificationsDirect(): List<NotificationEntity>

    @Query("SELECT COUNT(*) FROM notifications WHERE title = :title")
    suspend fun countNotificationByTitle(title: String): Int

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE subtitle LIKE '%·%' AND (timeAgo LIKE '%m ago' OR timeAgo LIKE '%h ago')")
    suspend fun removeDummyNotifications()
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT COUNT(*) FROM user_profile WHERE id = 1 AND isOnboarded = 1")
    suspend fun isOnboarded(): Int

    @Query("UPDATE user_profile SET name = :name, role = :role, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateNameAndRole(name: String, role: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET name = '', role = '', email = '', phone = '', department = '', emergencyContact = '', address = '', skills = '', bio = '', updatedAt = :updatedAt WHERE id = 1")
    suspend fun clearProfileFields(updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_profile")
    suspend fun clearProfile()
}

@Dao
interface LeaveDao {
    @Query("SELECT * FROM leave_applications ORDER BY id DESC")
    fun getAllLeaves(): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications WHERE username = :username ORDER BY id DESC")
    fun getLeavesForUser(username: String): Flow<List<LeaveApplicationEntity>>

    @Query("SELECT * FROM leave_applications WHERE id = :id")
    suspend fun getLeaveByIdDirect(id: Long): LeaveApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(leave: LeaveApplicationEntity): Long

    @Update
    suspend fun update(leave: LeaveApplicationEntity)

    @Delete
    suspend fun delete(leave: LeaveApplicationEntity)

    @Query("DELETE FROM leave_applications")
    suspend fun clearAll()
}

@Dao
interface CallRecordingDao {
    @Query("SELECT * FROM call_recordings ORDER BY id DESC")
    fun getAllRecordings(): Flow<List<CallRecordingEntity>>

    @Query("SELECT * FROM call_recordings WHERE callMedium = :medium ORDER BY id DESC")
    fun getRecordingsByMedium(medium: String): Flow<List<CallRecordingEntity>>

    @Query("SELECT COUNT(*) FROM call_recordings")
    fun getRecordingsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: CallRecordingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recordings: List<CallRecordingEntity>)

    @Update
    suspend fun update(recording: CallRecordingEntity)

    @Delete
    suspend fun delete(recording: CallRecordingEntity)

    @Query("DELETE FROM call_recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM call_recordings")
    suspend fun clearAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY id DESC")
    fun getAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY id DESC")
    fun getInvoicesByStatus(status: String): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM invoices")
    fun getInvoiceCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Delete
    suspend fun delete(invoice: InvoiceEntity)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY id DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE status = :status ORDER BY id DESC")
    fun getQuotationsByStatus(status: String): Flow<List<QuotationEntity>>

    @Query("SELECT COUNT(*) FROM quotations")
    fun getQuotationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quotation: QuotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quotations: List<QuotationEntity>)

    @Update
    suspend fun update(quotation: QuotationEntity)

    @Delete
    suspend fun delete(quotation: QuotationEntity)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM quotations")
    suspend fun clearAll()
}

@Dao
interface AutoBrochureDao {
    @Query("SELECT * FROM auto_brochure_configs WHERE id = 1 LIMIT 1")
    fun getAutoBrochureConfig(): Flow<AutoBrochureConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: AutoBrochureConfigEntity)

    @Query("UPDATE auto_brochure_configs SET totalSentCount = totalSentCount + 1, lastSentTimestamp = :lastSent WHERE id = 1")
    suspend fun incrementSentCount(lastSent: String)
}

@Dao
interface SocialReviewDao {
    @Query("SELECT * FROM social_review_configs ORDER BY platformName ASC")
    fun getAllReviewConfigs(): Flow<List<SocialReviewConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SocialReviewConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<SocialReviewConfigEntity>)

    @Update
    suspend fun update(config: SocialReviewConfigEntity)

    @Delete
    suspend fun delete(config: SocialReviewConfigEntity)

    @Query("DELETE FROM social_review_configs")
    suspend fun clearAll()
}

@Dao
interface ClientMeetingDao {
    @Query("SELECT * FROM client_meetings ORDER BY id DESC")
    fun getAllMeetings(): Flow<List<ClientMeetingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meeting: ClientMeetingEntity): Long

    @Update
    suspend fun update(meeting: ClientMeetingEntity)

    @Query("DELETE FROM client_meetings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM client_meetings")
    suspend fun clearAll()
}

@Dao
interface ExpenseClaimDao {
    @Query("SELECT * FROM expense_claims ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseClaimEntity>>

    @Query("SELECT * FROM expense_claims WHERE status = :status ORDER BY id DESC")
    fun getExpensesByStatus(status: String): Flow<List<ExpenseClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseClaimEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseClaimEntity>)

    @Update
    suspend fun update(expense: ExpenseClaimEntity)

    @Query("UPDATE expense_claims SET status = :status, reviewedBy = :reviewedBy WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, reviewedBy: String)

    @Query("DELETE FROM expense_claims WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM expense_claims")
    suspend fun clearAll()
}

@Dao
interface ProjectMilestoneDao {
    @Query("SELECT * FROM project_milestones ORDER BY id ASC")
    fun getAllMilestones(): Flow<List<ProjectMilestoneEntity>>

    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY id ASC")
    fun getMilestonesByProject(projectId: Long): Flow<List<ProjectMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(milestone: ProjectMilestoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(milestones: List<ProjectMilestoneEntity>)

    @Update
    suspend fun update(milestone: ProjectMilestoneEntity)

    @Query("UPDATE project_milestones SET completionPercent = :percent, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: Long, percent: Int, isCompleted: Boolean)

    @Query("DELETE FROM project_milestones WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM project_milestones")
    suspend fun clearAll()
}

@Dao
interface VaultDocumentDao {
    @Query("SELECT * FROM vault_documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE category = :category ORDER BY id DESC")
    fun getDocumentsByCategory(category: String): Flow<List<VaultDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: VaultDocumentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<VaultDocumentEntity>)

    @Delete
    suspend fun delete(document: VaultDocumentEntity)

    @Query("DELETE FROM vault_documents WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_documents")
    suspend fun clearAll()
}

@Dao
interface AttendanceRegularizationDao {
    @Query("SELECT * FROM attendance_regularizations ORDER BY id DESC")
    fun getAllRegularizations(): Flow<List<AttendanceRegularizationEntity>>

    @Query("SELECT * FROM attendance_regularizations WHERE employeeName = :empName ORDER BY id DESC")
    fun getRegularizationsForEmployee(empName: String): Flow<List<AttendanceRegularizationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(regularization: AttendanceRegularizationEntity): Long

    @Update
    suspend fun update(regularization: AttendanceRegularizationEntity)

    @Query("UPDATE attendance_regularizations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM attendance_regularizations")
    suspend fun clearAll()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shift_schedules ORDER BY date ASC")
    fun getAllShifts(): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE employeeName = :employeeName ORDER BY date ASC")
    fun getShiftsForEmployee(employeeName: String): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE date >= :startDate ORDER BY date ASC")
    fun getUpcomingShifts(startDate: String): Flow<List<ShiftScheduleEntity>>

    @Query("SELECT * FROM shift_schedules WHERE id = :id")
    suspend fun getShiftByIdDirect(id: Long): ShiftScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shift: ShiftScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftScheduleEntity>)

    @Update
    suspend fun update(shift: ShiftScheduleEntity)

    @Delete
    suspend fun delete(shift: ShiftScheduleEntity)

    @Query("DELETE FROM shift_schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM shift_schedules")
    suspend fun clearAll()
}



