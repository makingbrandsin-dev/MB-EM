package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@Database(
    entities = [
        EmployeeEntity::class,
        AttendanceRecord::class,
        ProjectEntity::class,
        TaskEntity::class,
        LeadEntity::class,
        FollowUpEntity::class,
        CallLogEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        UserProfileEntity::class,
        LeaveApplicationEntity::class,
        LeadSourceConfigEntity::class,
        CallRecordingEntity::class,
        InvoiceEntity::class,
        QuotationEntity::class,
        AutoBrochureConfigEntity::class,
        SocialReviewConfigEntity::class,
        ClientMeetingEntity::class,
        ExpenseClaimEntity::class,
        ProjectMilestoneEntity::class,
        VaultDocumentEntity::class,
        AttendanceRegularizationEntity::class,
        ActivityFeedItemEntity::class,
        AuditLogEntity::class,
        ShiftScheduleEntity::class
    ],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun projectDao(): ProjectDao
    abstract fun taskDao(): TaskDao
    abstract fun leadDao(): LeadDao
    abstract fun followUpDao(): FollowUpDao
    abstract fun callLogDao(): CallLogDao
    abstract fun chatDao(): ChatDao
    abstract fun activityFeedDao(): ActivityFeedDao
    abstract fun notificationDao(): NotificationDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun leaveDao(): LeaveDao
    abstract fun leadSourceDao(): LeadSourceDao
    abstract fun callRecordingDao(): CallRecordingDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun quotationDao(): QuotationDao
    abstract fun autoBrochureDao(): AutoBrochureDao
    abstract fun socialReviewDao(): SocialReviewDao
    abstract fun clientMeetingDao(): ClientMeetingDao
    abstract fun expenseClaimDao(): ExpenseClaimDao
    abstract fun projectMilestoneDao(): ProjectMilestoneDao
    abstract fun vaultDocumentDao(): VaultDocumentDao
    abstract fun attendanceRegularizationDao(): AttendanceRegularizationDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun shiftDao(): ShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 11 to 12: Adds support for client meetings and expense claims tables.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `client_meetings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `clientName` TEXT NOT NULL,
                        `company` TEXT NOT NULL,
                        `meetingTitle` TEXT NOT NULL,
                        `agenda` TEXT NOT NULL,
                        `scheduledDateTime` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `location` TEXT NOT NULL,
                        `meetingType` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `outcomeNotes` TEXT,
                        `actionItems` TEXT NOT NULL,
                        `attendeeEmails` TEXT NOT NULL,
                        `assignedEmployee` TEXT NOT NULL,
                        `leadId` INTEGER,
                        `latitude` REAL,
                        `longitude` REAL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 12 to 13: Adds support for project milestones, vault documents, and attendance regularizations.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `project_milestones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `projectId` INTEGER NOT NULL,
                        `milestoneName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `targetDate` TEXT NOT NULL,
                        `completionDate` TEXT,
                        `status` TEXT NOT NULL,
                        `amountAllocated` REAL,
                        `isPaymentReleased` INTEGER NOT NULL,
                        `deliverables` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 13 to 14: Configures performance indices across core entities.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Indices for employees table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employees_email` ON `employees` (`email`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employees_department` ON `employees` (`department`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_employees_status` ON `employees` (`status`)")

                // Indices for projects table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_priority` ON `projects` (`priority`)")

                // Indices for tasks table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_priority` ON `tasks` (`priority`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
            }
        }

        /**
         * Migration from version 14 to 15: Adds read status fields to chat_messages.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `isRead` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `readBy` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `readAt` INTEGER")
            }
        }

        /**
         * Migration from version 15 to 16: Adds reactionsJson to chat_messages and activity_feed_items table.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `reactionsJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `activity_feed_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `authorRole` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `likesCount` INTEGER NOT NULL,
                        `likedByUsers` TEXT NOT NULL,
                        `reactionsJson` TEXT NOT NULL,
                        `commentsCount` INTEGER NOT NULL,
                        `isPinned` INTEGER NOT NULL,
                        `timestampText` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_feed_items_category` ON `activity_feed_items` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_feed_items_createdAt` ON `activity_feed_items` (`createdAt`)")
            }
        }

        /**
         * Migration from version 16 to 17: Adds isSynced column to attendance_records and chat_messages for offline Room caching.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `isSynced` INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `call_logs` ADD COLUMN `transcription` TEXT")
                db.execSQL("ALTER TABLE `call_logs` ADD COLUMN `aiInsights` TEXT")
                db.execSQL("ALTER TABLE `call_logs` ADD COLUMN `leadUpdated` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mb_traker_database"
                )
                    .addMigrations(MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_19_20)
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val appContext: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                if (!com.example.util.AppPreferences.isDummyDataCleared(appContext)) {
                    INSTANCE?.let { database ->
                        CoroutineScope(Dispatchers.IO).launch {
                            populateInitialData(database)
                        }
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Execute SQLite performance optimizations on database open
                db.execSQL("PRAGMA foreign_keys = ON")
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                if (!com.example.util.AppPreferences.isDummyDataCleared(appContext)) {
                    INSTANCE?.let { database ->
                        CoroutineScope(Dispatchers.IO).launch {
                            populateInitialData(database)
                        }
                    }
                }
            }
        }

        suspend fun ensurePopulated(db: AppDatabase, context: Context? = null) {
            try {
                if (context != null && com.example.util.AppPreferences.isDummyDataCleared(context)) {
                    android.util.Log.d("AppDatabase", "User cleared dummy data - permanently skipping auto-populate")
                    return
                }
                val employeeCount = db.employeeDao().getEmployeeCountDirect()
                val taskCount = db.taskDao().getTaskCount()
                val chatCount = db.chatDao().getMessageCount()
                val leadCount = db.leadDao().getLeadCount()
                // Only populate initial template records if the database is totally empty and un-cleared
                if (employeeCount == 0 && taskCount == 0 && chatCount == 0 && leadCount == 0) {
                    populateInitialData(db)
                }
            } catch (_: Exception) {
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Employees
            db.employeeDao().insertAll(
                listOf(
                    EmployeeEntity(
                        name = "Rahul Sharma",
                        email = "rahul.sharma@makingbrands.in",
                        phone = "+91 98765 43210",
                        designation = "Senior Android Developer",
                        department = Department.ENGINEERING,
                        role = EmployeeRole.DEVELOPER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.ONLINE,
                        joiningDate = Date(1672531199000L), // Jan 2023
                        skills = listOf("Kotlin", "Jetpack Compose", "Room", "Coroutines", "MVVM"),
                        assignedProjectIds = listOf(1L, 2L),
                        salary = 95000.0,
                        emergencyContact = "+91 98765 00001"
                    ),
                    EmployeeEntity(
                        name = "Priya Singh",
                        email = "priya.singh@makingbrands.in",
                        phone = "+91 98111 22334",
                        designation = "Lead QA Engineer",
                        department = Department.ENGINEERING,
                        role = EmployeeRole.QA_ENGINEER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.IN_MEETING,
                        joiningDate = Date(1685577600000L), // June 2023
                        skills = listOf("Manual Testing", "Espresso", "Robolectric", "API Testing"),
                        assignedProjectIds = listOf(1L, 2L),
                        salary = 85000.0,
                        emergencyContact = "+91 98111 00002"
                    ),
                    EmployeeEntity(
                        name = "Arjun Mehta",
                        email = "arjun.mehta@makingbrands.in",
                        phone = "+91 99222 33445",
                        designation = "Engineering Manager",
                        department = Department.MANAGEMENT,
                        role = EmployeeRole.MANAGER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.ONLINE,
                        joiningDate = Date(1640995200000L), // Jan 2022
                        skills = listOf("Team Leadership", "Agile/Scrum", "System Design", "Sprint Planning"),
                        assignedProjectIds = listOf(1L, 2L, 3L),
                        salary = 145000.0,
                        emergencyContact = "+91 99222 00003"
                    ),
                    EmployeeEntity(
                        name = "Neha Gupta",
                        email = "neha.gupta@makingbrands.in",
                        phone = "+91 99333 44556",
                        designation = "Senior Product Designer",
                        department = Department.DESIGN,
                        role = EmployeeRole.DESIGNER,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.OFFLINE,
                        joiningDate = Date(1693526400000L), // Sep 2023
                        skills = listOf("Figma", "Design Systems", "Prototyping", "User Research"),
                        assignedProjectIds = listOf(1L),
                        salary = 90000.0,
                        emergencyContact = "+91 99333 00004"
                    ),
                    EmployeeEntity(
                        name = "Suresh Patel",
                        email = "suresh.patel@makingbrands.in",
                        phone = "+91 99444 55667",
                        designation = "Business Development Lead",
                        department = Department.SALES,
                        role = EmployeeRole.SALES_EXECUTIVE,
                        status = EmployeeStatus.ACTIVE,
                        presenceStatus = PresenceStatus.IN_MEETING,
                        joiningDate = Date(1677628800000L), // Mar 2023
                        skills = listOf("CRM", "Client Relations", "B2B Negotiations", "Lead Generation"),
                        assignedProjectIds = listOf(3L),
                        salary = 80000.0,
                        emergencyContact = "+91 99444 00005"
                    )
                )
            )

            // Projects
            db.projectDao().insertAll(
                listOf(
                    ProjectEntity(
                        name = "Website Revamp",
                        clientName = "ABC Ltd",
                        totalTasks = 12,
                        completedTasks = 10,
                        progressPercent = 82,
                        status = "Active",
                        priority = "High",
                        startDate = "01 Sep 2025",
                        deadline = "28 Sep 2025",
                        managerName = "Rahul Sharma",
                        teamSize = 5,
                        tags = listOf("Web", "Frontend", "Next.js", "Redesign"),
                        assignedEmployeeIds = listOf(1L, 2L, 3L, 4L),
                        budget = 350000.0
                    ),
                    ProjectEntity(
                        name = "Mobile App Development",
                        clientName = "XYZ Pvt Ltd",
                        totalTasks = 8,
                        completedTasks = 4,
                        progressPercent = 45,
                        status = "Active",
                        priority = "High",
                        startDate = "15 Aug 2025",
                        deadline = "15 Oct 2025",
                        managerName = "Priya Singh",
                        teamSize = 6,
                        tags = listOf("Mobile", "Android", "Kotlin", "Compose"),
                        assignedEmployeeIds = listOf(1L, 2L, 3L),
                        budget = 500000.0
                    ),
                    ProjectEntity(
                        name = "SEO Optimization",
                        clientName = "Digital Media",
                        totalTasks = 6,
                        completedTasks = 6,
                        progressPercent = 100,
                        status = "Completed",
                        priority = "Medium",
                        startDate = "01 Aug 2025",
                        deadline = "10 Sep 2025",
                        managerName = "Rahul Sharma",
                        teamSize = 3,
                        tags = listOf("Marketing", "SEO", "Analytics"),
                        assignedEmployeeIds = listOf(3L, 5L),
                        budget = 120000.0
                    )
                )
            )

            // Tasks
            db.taskDao().insertAll(
                listOf(
                    TaskEntity(
                        title = "Homepage Design",
                        projectName = "Website Revamp",
                        dueDate = "20 Sep 2025",
                        priority = "High",
                        status = "In Progress",
                        isCompleted = false,
                        category = "Work"
                    ),
                    TaskEntity(
                        title = "API Integration",
                        projectName = "Mobile App Dev",
                        dueDate = "22 Sep 2025",
                        priority = "High",
                        status = "In Progress",
                        isCompleted = false,
                        category = "Urgent"
                    ),
                    TaskEntity(
                        title = "Quarterly Sync Meeting",
                        projectName = "Operations",
                        dueDate = "Today, 3:00 PM",
                        priority = "Medium",
                        status = "In Progress",
                        isCompleted = false,
                        category = "Meeting"
                    ),
                    TaskEntity(
                        title = "Gym Workout & Health",
                        projectName = "Personal Care",
                        dueDate = "Today, 7:00 PM",
                        priority = "Medium",
                        status = "Backlog",
                        isCompleted = false,
                        category = "Personal"
                    ),
                    TaskEntity(
                        title = "Content Writing",
                        projectName = "SEO Optimization",
                        dueDate = "26 Sep 2025",
                        priority = "Low",
                        status = "Completed",
                        isCompleted = true,
                        category = "Work"
                    ),
                    TaskEntity(
                        title = "Testing & QA",
                        projectName = "Website Revamp",
                        dueDate = "28 Sep 2025",
                        priority = "High",
                        status = "Backlog",
                        isCompleted = false,
                        category = "Review"
                    ),
                    TaskEntity(
                        title = "Database Migration",
                        projectName = "Website Revamp",
                        dueDate = "18 Sep 2025",
                        priority = "High",
                        status = "Completed",
                        isCompleted = true,
                        category = "Work"
                    )
                )
            )

            // Leads
            db.leadDao().insertAll(
                listOf(
                    LeadEntity(
                        name = "Ramesh Kumar",
                        company = "ABC Industries",
                        phone = "+91 98765 43210",
                        email = "ramesh@abc.com",
                        leadScore = 85,
                        requirement = "Website + App Development",
                        potentialValue = "₹ 2,50,000",
                        stage = "Interested",
                        assignedTo = "Arjun Mehta",
                        nextFollowUp = "Today, 4:30 PM",
                        source = "Justdial"
                    ),
                    LeadEntity(
                        name = "Suresh Patel",
                        company = "XYZ Traders",
                        phone = "+91 98111 22334",
                        email = "suresh@xyz.com",
                        leadScore = 72,
                        requirement = "ERP Customization",
                        potentialValue = "₹ 1,80,000",
                        stage = "Contacted",
                        assignedTo = "Rahul Sharma",
                        nextFollowUp = "Tomorrow, 11:00 AM",
                        source = "Facebook"
                    ),
                    LeadEntity(
                        name = "Neha Gupta",
                        company = "Digital Solutions",
                        phone = "+91 99222 33445",
                        email = "neha@digitalsol.com",
                        leadScore = 92,
                        requirement = "Cloud Migration & SEO",
                        potentialValue = "₹ 4,20,000",
                        stage = "Proposal",
                        assignedTo = "Arjun Mehta",
                        nextFollowUp = "22 Sep, 2:00 PM",
                        source = "Google Ads"
                    ),
                    LeadEntity(
                        name = "Vikas Malhotra",
                        company = "Malhotra Logistics",
                        phone = "+91 98333 44556",
                        email = "vikas@malhotra.in",
                        leadScore = 88,
                        requirement = "Fleet GPS Tracking & Web Dashboard",
                        potentialValue = "₹ 3,75,000",
                        stage = "New",
                        assignedTo = "Rahul Sharma",
                        nextFollowUp = "Today, 5:00 PM",
                        source = "WhatsApp"
                    ),
                    LeadEntity(
                        name = "Ananya Roy",
                        company = "Artisan Cafe Chain",
                        phone = "+91 97444 55667",
                        email = "ananya@artisancafe.com",
                        leadScore = 79,
                        requirement = "POS Billing & Mobile App",
                        potentialValue = "$ 4,500",
                        stage = "Negotiation",
                        assignedTo = "Arjun Mehta",
                        nextFollowUp = "Tomorrow, 3:30 PM",
                        source = "Website"
                    ),
                    LeadEntity(
                        name = "Deepak Verma",
                        company = "Verma Electronics",
                        phone = "+91 96555 66778",
                        email = "deepak@vermaelec.com",
                        leadScore = 68,
                        requirement = "Inventory Management Software",
                        potentialValue = "₹ 1,20,000",
                        stage = "Contacted",
                        assignedTo = "Rahul Sharma",
                        nextFollowUp = "25 Sep, 11:30 AM",
                        source = "OLX"
                    )
                )
            )

            // Follow-ups
            db.followUpDao().insertAll(
                listOf(
                    FollowUpEntity(
                        leadId = 1,
                        clientName = "ABC Industries",
                        taskDescription = "Call customer regarding final proposal scope",
                        scheduledTime = "10:30 AM",
                        scheduledDateCategory = "Today",
                        actionType = "Call",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 2,
                        clientName = "XYZ Traders",
                        taskDescription = "Send quotation & tech stack breakdown",
                        scheduledTime = "11:45 AM",
                        scheduledDateCategory = "Today",
                        actionType = "Open",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 1,
                        clientName = "Client Meeting",
                        taskDescription = "Follow-up video call with technical leads",
                        scheduledTime = "03:00 PM",
                        scheduledDateCategory = "Today",
                        actionType = "Call",
                        isCompleted = false
                    ),
                    FollowUpEntity(
                        leadId = 3,
                        clientName = "Digital Solutions",
                        taskDescription = "Send formal commercial proposal contract",
                        scheduledTime = "04:30 PM",
                        scheduledDateCategory = "Today",
                        actionType = "Open",
                        isCompleted = false
                    )
                )
            )

            // Call Logs
            db.callLogDao().insertAll(
                listOf(
                    CallLogEntity(
                        contactName = "Ramesh Kumar",
                        callType = "Incoming",
                        status = "Connected",
                        timestampText = "Today, 09:32 AM",
                        durationText = "4m 12s",
                        phoneNumber = "+91 98765 43210"
                    ),
                    CallLogEntity(
                        contactName = "XYZ Traders",
                        callType = "Outgoing",
                        status = "Connected",
                        timestampText = "Today, 11:15 AM",
                        durationText = "2m 41s",
                        phoneNumber = "+91 98111 22334"
                    ),
                    CallLogEntity(
                        contactName = "ABC Industries",
                        callType = "Outgoing",
                        status = "No Answer",
                        timestampText = "Today, 01:22 PM",
                        durationText = "5m 03s",
                        phoneNumber = "+91 98765 43210"
                    ),
                    CallLogEntity(
                        contactName = "Digital Solutions",
                        callType = "Incoming",
                        status = "Connected",
                        timestampText = "Yesterday, 04:22 PM",
                        durationText = "3m 03s",
                        phoneNumber = "+91 99222 33445"
                    )
                )
            )

            // Chat Messages for all channels and direct team chats
            db.chatDao().insertAll(
                listOf(
                    // Company Chat
                    ChatMessageEntity(
                        channelId = "company_chat",
                        senderName = "Arjun Mehta",
                        senderRole = "Engineering Manager",
                        messageText = "Good morning everyone! Remember our all-hands sync at 3:00 PM today.",
                        timestampText = "09:00 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "company_chat",
                        senderName = "Rahul Sharma",
                        senderRole = "Senior Developer",
                        messageText = "Good morning Arjun! The sprint milestones are tracking ahead of schedule.",
                        timestampText = "09:15 AM",
                        isMe = true
                    ),
                    ChatMessageEntity(
                        channelId = "company_chat",
                        senderName = "Neha Gupta",
                        senderRole = "Product Designer",
                        messageText = "Shared the updated design assets in Figma. Please review when you get a chance! 🎨",
                        timestampText = "09:45 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "company_chat",
                        senderName = "Suresh Patel",
                        senderRole = "Sales Lead",
                        messageText = "Great work team! Closed the XYZ Traders contract this morning! 🎉",
                        timestampText = "10:10 AM",
                        isMe = false
                    ),

                    // Dev Team
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Rahul Sharma",
                        senderRole = "Senior Developer",
                        messageText = "The new build is ready for testing with offline Room cache & biometric unlock.",
                        timestampText = "10:04 AM",
                        isMe = true
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Priya Singh",
                        senderRole = "QA Engineer",
                        messageText = "Great! I'll check and update the release checklist.",
                        timestampText = "10:32 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Priya Singh",
                        senderRole = "QA Engineer",
                        messageText = "Attached the verified release APK package for review.",
                        timestampText = "10:33 AM",
                        isMe = false,
                        attachmentFileName = "app-release-v1.2.0.apk",
                        attachmentFileSize = "2.4 MB"
                    ),
                    ChatMessageEntity(
                        channelId = "dev_team",
                        senderName = "Arjun Mehta",
                        senderRole = "Team Lead",
                        messageText = "Looks good. Deploying today after client approval.",
                        timestampText = "11:05 AM",
                        isMe = false
                    ),

                    // Sales Team
                    ChatMessageEntity(
                        channelId = "sales_team",
                        senderName = "Suresh Patel",
                        senderRole = "Sales Lead",
                        messageText = "Rohit: New high-value lead received for Fleet GPS & Web ERP from Malhotra Logistics! 🔥",
                        timestampText = "09:30 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "sales_team",
                        senderName = "Rahul Sharma",
                        senderRole = "Technical Lead",
                        messageText = "Auto-brochure PDF has been dispatched via WhatsApp to the client.",
                        timestampText = "09:35 AM",
                        isMe = true
                    ),
                    ChatMessageEntity(
                        channelId = "sales_team",
                        senderName = "Suresh Patel",
                        senderRole = "Sales Lead",
                        messageText = "Client responded warmly, scheduled follow-up call for 4:30 PM today.",
                        timestampText = "10:00 AM",
                        isMe = false
                    ),

                    // Marketing
                    ChatMessageEntity(
                        channelId = "marketing",
                        senderName = "Priya Singh",
                        senderRole = "Campaign Coordinator",
                        messageText = "Google Ads & Facebook campaign metrics updated. Inbound lead count is up 34% this week! 📈",
                        timestampText = "08:45 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "marketing",
                        senderName = "Neha Gupta",
                        senderRole = "Creative Lead",
                        messageText = "Company profile brochure v2026 is published with new case studies.",
                        timestampText = "09:20 AM",
                        isMe = false
                    ),

                    // Project Alpha
                    ChatMessageEntity(
                        channelId = "project_alpha",
                        senderName = "Suresh Patel",
                        senderRole = "Account Manager",
                        messageText = "Client meeting scheduled with ABC Ltd technical directors tomorrow at 11 AM.",
                        timestampText = "10:15 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "project_alpha",
                        senderName = "Rahul Sharma",
                        senderRole = "Lead Architect",
                        messageText = "Architecture deck and API specifications ready.",
                        timestampText = "10:45 AM",
                        isMe = true
                    ),

                    // Direct Messages with Priya Singh (dm_2)
                    ChatMessageEntity(
                        channelId = "dm_2",
                        senderName = "Priya Singh",
                        senderRole = "QA Engineer",
                        messageText = "Hey Rahul, did you push the commit for biometric authentication?",
                        timestampText = "10:15 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "dm_2",
                        senderName = "Rahul Sharma",
                        senderRole = "Senior Developer",
                        messageText = "Yes Priya, biometrics and WhatsApp OTP are fully integrated and verified!",
                        timestampText = "10:18 AM",
                        isMe = true
                    ),

                    // Direct Messages with Arjun Mehta (dm_3)
                    ChatMessageEntity(
                        channelId = "dm_3",
                        senderName = "Arjun Mehta",
                        senderRole = "Engineering Manager",
                        messageText = "Rahul, outstanding speed on the CRM leads refactor. Client was very impressed.",
                        timestampText = "11:20 AM",
                        isMe = false
                    ),
                    ChatMessageEntity(
                        channelId = "dm_3",
                        senderName = "Rahul Sharma",
                        senderRole = "Senior Developer",
                        messageText = "Thank you Arjun! Happy to keep pushing quality forward.",
                        timestampText = "11:22 AM",
                        isMe = true
                    )
                )
            )

            // Notifications - clean initial state (no dummy notifications)
            // Real notifications will be inserted dynamically based on real user actions and live broadcasts

            val leaveDao = db.leaveDao()
            leaveDao.insert(
                LeaveApplicationEntity(
                    username = "Rahul Sharma",
                    leaveType = "Casual Leave",
                    startDate = "22 Sep 2025",
                    endDate = "23 Sep 2025",
                    totalDays = 2,
                    reason = "Family function in hometown",
                    status = "Approved",
                    appliedDate = "14 Sep 2025"
                )
            )
            leaveDao.insert(
                LeaveApplicationEntity(
                    username = "Rahul Sharma",
                    leaveType = "Sick Leave",
                    startDate = "05 Sep 2025",
                    endDate = "05 Sep 2025",
                    totalDays = 1,
                    reason = "Viral fever and doctor consultation",
                    status = "Approved",
                    appliedDate = "05 Sep 2025"
                )
            )

            // Automated Shift Schedules & Work Rotations Initial Seed
            val shiftDao = db.shiftDao()
            shiftDao.insertAll(
                listOf(
                    ShiftScheduleEntity(
                        employeeName = "Arjun Mehta",
                        employeeRole = "Engineering Manager",
                        shiftType = "Morning Shift",
                        shiftCode = "MOR-01",
                        startTime = "08:00 AM",
                        endTime = "05:00 PM",
                        date = "2026-10-06",
                        dayOfWeek = "Tuesday",
                        location = "Main HQ - Floor 3",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "In Progress",
                        notes = "Primary engineering lead rotation"
                    ),
                    ShiftScheduleEntity(
                        employeeName = "Rahul Sharma",
                        employeeRole = "Operations Specialist",
                        shiftType = "General Day Shift",
                        shiftCode = "DAY-01",
                        startTime = "09:30 AM",
                        endTime = "06:30 PM",
                        date = "2026-10-06",
                        dayOfWeek = "Tuesday",
                        location = "Main HQ - Floor 3",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "In Progress",
                        notes = "Standard operations & client dispatch"
                    ),
                    ShiftScheduleEntity(
                        employeeName = "Rahul Sharma",
                        employeeRole = "Operations Specialist",
                        shiftType = "Morning Shift",
                        shiftCode = "MOR-01",
                        startTime = "08:00 AM",
                        endTime = "05:00 PM",
                        date = "2026-10-07",
                        dayOfWeek = "Wednesday",
                        location = "Main HQ - Floor 3",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "Scheduled",
                        notes = "Early client support handover"
                    ),
                    ShiftScheduleEntity(
                        employeeName = "Rahul Sharma",
                        employeeRole = "Operations Specialist",
                        shiftType = "Afternoon Shift",
                        shiftCode = "AFT-02",
                        startTime = "01:00 PM",
                        endTime = "10:00 PM",
                        date = "2026-10-08",
                        dayOfWeek = "Thursday",
                        location = "Remote / WFH",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "Scheduled",
                        notes = "Late afternoon coverage & WFH"
                    ),
                    ShiftScheduleEntity(
                        employeeName = "Rahul Sharma",
                        employeeRole = "Operations Specialist",
                        shiftType = "Night Shift",
                        shiftCode = "NIG-03",
                        startTime = "10:00 PM",
                        endTime = "06:00 AM",
                        date = "2026-10-09",
                        dayOfWeek = "Friday",
                        location = "Main HQ - Floor 3",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "Scheduled",
                        notes = "Night standby & server deployment support"
                    ),
                    ShiftScheduleEntity(
                        employeeName = "Priya Nair",
                        employeeRole = "Senior Designer",
                        shiftType = "General Day Shift",
                        shiftCode = "DAY-01",
                        startTime = "10:00 AM",
                        endTime = "07:00 PM",
                        date = "2026-10-07",
                        dayOfWeek = "Wednesday",
                        location = "Remote / WFH",
                        rotationCycleName = "Q4 Rotation - Week 41",
                        status = "Scheduled",
                        notes = "UX design sprint rotation"
                    )
                )
            )

            // Lead Source Platform API Configs
            db.leadSourceDao().insertAll(
                listOf(
                    LeadSourceConfigEntity(
                        sourceId = "justdial",
                        displayName = "Justdial Leads API",
                        isEnabled = true,
                        apiKey = "JD_API_SEC_8829104",
                        apiSecret = "jd_oauth_key_77a9b",
                        webhookUrl = "https://api.mbtraker.in/webhooks/justdial",
                        defaultAssignee = "Arjun Mehta",
                        lastSyncTime = "10 mins ago",
                        totalLeadsIngested = 42
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "olx",
                        displayName = "OLX Business Ingestion",
                        isEnabled = true,
                        apiKey = "OLX_BIZ_KEY_449102",
                        apiSecret = "olx_partner_sec_991b",
                        webhookUrl = "https://api.mbtraker.in/webhooks/olx",
                        defaultAssignee = "Rahul Sharma",
                        lastSyncTime = "25 mins ago",
                        totalLeadsIngested = 18
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "facebook",
                        displayName = "Facebook Lead Ads",
                        isEnabled = true,
                        apiKey = "EAABw...MetaToken",
                        apiSecret = "fb_app_secret_892bca",
                        webhookUrl = "https://api.mbtraker.in/webhooks/meta-lead-gen",
                        defaultAssignee = "Rahul Sharma",
                        lastSyncTime = "5 mins ago",
                        totalLeadsIngested = 64
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "google_ads",
                        displayName = "Google Ads Webhook",
                        isEnabled = true,
                        apiKey = "AIzaSy...GAdsApiKey",
                        apiSecret = "gads_webhook_token_9934",
                        webhookUrl = "https://api.mbtraker.in/webhooks/google-ads",
                        defaultAssignee = "Arjun Mehta",
                        lastSyncTime = "1 hour ago",
                        totalLeadsIngested = 35
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "website",
                        displayName = "Website Contact Form API",
                        isEnabled = true,
                        apiKey = "WEB_FORM_API_KEY_0019",
                        apiSecret = "web_endpoint_secret_554a",
                        webhookUrl = "https://api.mbtraker.in/webhooks/website-form",
                        defaultAssignee = "Rahul Sharma",
                        lastSyncTime = "Just now",
                        totalLeadsIngested = 89
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "whatsapp",
                        displayName = "WhatsApp Cloud API",
                        isEnabled = true,
                        apiKey = "WHATSAPP_TOKEN_BEARER_881",
                        apiSecret = "wa_verify_token_mb39",
                        webhookUrl = "https://api.mbtraker.in/webhooks/whatsapp",
                        defaultAssignee = "Rahul Sharma",
                        lastSyncTime = "Just now",
                        totalLeadsIngested = 57
                    ),
                    LeadSourceConfigEntity(
                        sourceId = "linkedin",
                        displayName = "LinkedIn Lead Gen",
                        isEnabled = false,
                        apiKey = "",
                        apiSecret = "",
                        webhookUrl = "https://api.mbtraker.in/webhooks/linkedin",
                        defaultAssignee = "Arjun Mehta",
                        lastSyncTime = "Never",
                        totalLeadsIngested = 0
                    )
                )
            )

            // Initial Call Recordings (SIM & WhatsApp calls saved to local DB)
            db.callRecordingDao().insertAll(
                listOf(
                    CallRecordingEntity(
                        contactName = "Vikram Aditya (Global Tech)",
                        phoneNumber = "+91 98450 11223",
                        callMedium = "SIM Call",
                        callType = "Outgoing",
                        durationText = "03:42",
                        durationSeconds = 222,
                        recordedAt = "Today, 11:15 AM",
                        filePath = "/recordings/sim_rec_vikram.m4a",
                        audioWaveform = "30,55,80,60,95,70,40,85,65,45,90,75,35,80,95,60,40",
                        fileSizeText = "2.4 MB",
                        transcriptionSnippet = "Confirmed scope for custom CRM, invoice automation, and Android app build.",
                        isAutoSaved = true
                    ),
                    CallRecordingEntity(
                        contactName = "Ananya Roy (Apex Retail)",
                        phoneNumber = "+91 99123 45678",
                        callMedium = "WhatsApp Call",
                        callType = "Incoming",
                        durationText = "05:18",
                        durationSeconds = 318,
                        recordedAt = "Today, 09:40 AM",
                        filePath = "/recordings/wa_rec_ananya.m4a",
                        audioWaveform = "20,40,60,85,90,75,60,80,95,70,50,65,85,70,45",
                        fileSizeText = "3.6 MB",
                        transcriptionSnippet = "Customer inquired about company profile PDF and quotation for digital marketing.",
                        isAutoSaved = true
                    ),
                    CallRecordingEntity(
                        contactName = "Rohan Verma (Solar Dynamics)",
                        phoneNumber = "+91 98765 43210",
                        callMedium = "SIM Call",
                        callType = "Outgoing",
                        durationText = "02:10",
                        durationSeconds = 130,
                        recordedAt = "Yesterday, 04:20 PM",
                        filePath = "/recordings/sim_rec_rohan.m4a",
                        audioWaveform = "35,60,75,50,65,90,80,45,70,85,55,40,75",
                        fileSizeText = "1.5 MB",
                        transcriptionSnippet = "Discussed milestone delivery dates and quotation terms.",
                        isAutoSaved = true
                    )
                )
            )

            // Initial Invoices
            db.invoiceDao().insertAll(
                listOf(
                    InvoiceEntity(
                        invoiceNumber = "INV-2026-0042",
                        clientName = "Vikram Aditya",
                        clientCompany = "Global Tech Enterprises",
                        clientEmail = "vikram@globaltech.com",
                        clientPhone = "+91 98450 11223",
                        issueDate = "15 Sep 2026",
                        dueDate = "30 Sep 2026",
                        currency = "₹",
                        subtotal = 180000.0,
                        taxPercent = 18.0,
                        totalAmount = 212400.0,
                        status = "Pending",
                        itemsSummary = "Phase 1: Enterprise Android App & CRM Module Implementation",
                        notes = "Payment terms: Net 15 days via NEFT / RTGS / UPI."
                    ),
                    InvoiceEntity(
                        invoiceNumber = "INV-2026-0041",
                        clientName = "Ananya Roy",
                        clientCompany = "Apex Retail Brands",
                        clientEmail = "ananya@apexretail.in",
                        clientPhone = "+91 99123 45678",
                        issueDate = "10 Sep 2026",
                        dueDate = "25 Sep 2026",
                        currency = "₹",
                        subtotal = 95000.0,
                        taxPercent = 18.0,
                        totalAmount = 112100.0,
                        status = "Paid",
                        itemsSummary = "E-Commerce Ingestion Webhooks & Catalog Setup",
                        notes = "Payment received with thanks."
                    ),
                    InvoiceEntity(
                        invoiceNumber = "INV-2026-0040",
                        clientName = "Michael Scott",
                        clientCompany = "Dunder Cloud USA",
                        clientEmail = "m.scott@dundercloud.com",
                        clientPhone = "+1 555 019 2834",
                        issueDate = "01 Sep 2026",
                        dueDate = "16 Sep 2026",
                        currency = "$",
                        subtotal = 3200.0,
                        taxPercent = 0.0,
                        totalAmount = 3200.0,
                        status = "Paid",
                        itemsSummary = "Cloud API Integration & UI Dashboard Development",
                        notes = "International Wire Transfer Completed."
                    )
                )
            )

            // Initial Quotations
            db.quotationDao().insertAll(
                listOf(
                    QuotationEntity(
                        quotationNumber = "QT-2026-0018",
                        clientName = "Kunal Shah",
                        clientCompany = "Credence Logistics",
                        clientEmail = "kunal@credencelog.com",
                        clientPhone = "+91 97110 33445",
                        issueDate = "18 Sep 2026",
                        validUntil = "05 Oct 2026",
                        currency = "₹",
                        subtotal = 250000.0,
                        discountPercent = 5.0,
                        taxPercent = 18.0,
                        totalAmount = 280250.0,
                        status = "Sent",
                        scopeOfWork = "Fleet Management Mobile App, Real-Time Driver Attendance & Route Optimizer",
                        termsAndConditions = "50% Advance upon work order, 30% after alpha build, 20% on final store launch."
                    ),
                    QuotationEntity(
                        quotationNumber = "QT-2026-0017",
                        clientName = "Priya Menon",
                        clientCompany = "DesignCraft Studio",
                        clientEmail = "priya@designcraft.in",
                        clientPhone = "+91 98200 66778",
                        issueDate = "12 Sep 2026",
                        validUntil = "28 Sep 2026",
                        currency = "₹",
                        subtotal = 120000.0,
                        discountPercent = 0.0,
                        taxPercent = 18.0,
                        totalAmount = 141600.0,
                        status = "Accepted",
                        scopeOfWork = "Branding, Company Profile PDF Brochure Design & Interactive Client Review Portal",
                        termsAndConditions = "100% advance for creative assets milestone."
                    )
                )
            )

            // Initial Auto Company Profile PDF Brochure Config
            db.autoBrochureDao().insertOrUpdate(
                AutoBrochureConfigEntity(
                    id = 1L,
                    isAutoSendEnabled = true,
                    sendViaWhatsApp = true,
                    sendViaEmail = true,
                    brochureFileName = "MakingBrands_Company_Profile_2026.pdf",
                    brochureFileSize = "3.4 MB",
                    emailSubject = "Welcome to Making Brands — Company Profile & Case Studies",
                    customMessage = "Hello {NAME}, thank you for contacting Making Brands! We have attached our official Company Profile & Portfolio PDF to help you explore our services.",
                    totalSentCount = 142,
                    lastSentTimestamp = "Today, 11:20 AM"
                )
            )

            // Initial Social Media Review & QR Code Configs
            db.socialReviewDao().insertAll(
                listOf(
                    SocialReviewConfigEntity(
                        platformId = "google",
                        platformName = "Google Business Profile",
                        reviewUrl = "https://g.page/r/makingbrands/review",
                        qrCodePayload = "https://g.page/r/makingbrands/review",
                        ratingText = "4.9 ★",
                        totalReviewsCount = 184,
                        isPrimary = true,
                        customInviteText = "Thank you for choosing Making Brands! We'd love your 5-star Google review."
                    ),
                    SocialReviewConfigEntity(
                        platformId = "trustpilot",
                        platformName = "Trustpilot",
                        reviewUrl = "https://www.trustpilot.com/evaluate/makingbrands.in",
                        qrCodePayload = "https://www.trustpilot.com/evaluate/makingbrands.in",
                        ratingText = "4.8 ★",
                        totalReviewsCount = 92,
                        isPrimary = false,
                        customInviteText = "Share your verified experience on Trustpilot."
                    ),
                    SocialReviewConfigEntity(
                        platformId = "whatsapp",
                        platformName = "WhatsApp Business Feedback",
                        reviewUrl = "https://wa.me/919876543210?text=I%20would%20like%20to%20rate%20Making%20Brands%20services",
                        qrCodePayload = "https://wa.me/919876543210?text=I%20would%20like%20to%20rate%20Making%20Brands%20services",
                        ratingText = "5.0 ★",
                        totalReviewsCount = 310,
                        isPrimary = false,
                        customInviteText = "Send direct client feedback on our VIP WhatsApp support channel."
                    ),
                    SocialReviewConfigEntity(
                        platformId = "instagram",
                        platformName = "Instagram (@makingbrands)",
                        reviewUrl = "https://instagram.com/makingbrands.in",
                        qrCodePayload = "https://instagram.com/makingbrands.in",
                        ratingText = "12.4k Followers",
                        totalReviewsCount = 420,
                        isPrimary = false,
                        customInviteText = "Tag us in your project launch story on Instagram!"
                    ),
                    SocialReviewConfigEntity(
                        platformId = "facebook",
                        platformName = "Facebook Page",
                        reviewUrl = "https://facebook.com/makingbrands.in/reviews",
                        qrCodePayload = "https://facebook.com/makingbrands.in/reviews",
                        ratingText = "4.9 ★",
                        totalReviewsCount = 68,
                        isPrimary = false,
                        customInviteText = "Leave a recommendation on our Facebook page."
                    ),
                    SocialReviewConfigEntity(
                        platformId = "linkedin",
                        platformName = "LinkedIn Recommendation",
                        reviewUrl = "https://linkedin.com/company/makingbrands",
                        qrCodePayload = "https://linkedin.com/company/makingbrands",
                        ratingText = "5.0 ★",
                        totalReviewsCount = 45,
                        isPrimary = false,
                        customInviteText = "Recommend Making Brands on LinkedIn."
                    )
                )
            )

            // Seed Client Meetings (Field Sales Check-ins)
            db.clientMeetingDao().insert(
                ClientMeetingEntity(
                    clientName = "Vikram Singhania",
                    company = "Apex Global Corp",
                    meetingPurpose = "Enterprise Mobile App & Lead Automation Architecture Review",
                    latitude = 28.6304,
                    longitude = 77.2177,
                    locationName = "Apex Tower, 4th Floor, Barakhamba Road, Connaught Place",
                    checkInTime = "11:00 AM",
                    checkOutTime = "12:15 PM",
                    meetingNotes = "Client was impressed by real-time Firestore sync & biometric sign-in. Requested custom milestone deliverables by end of week.",
                    outcome = "Proposal Requested"
                )
            )
            db.clientMeetingDao().insert(
                ClientMeetingEntity(
                    clientName = "Ananya Sharma",
                    company = "Zenith Retail Solutions",
                    meetingPurpose = "E-Commerce Catalog & Payment Gateway Integration Pitch",
                    latitude = 28.5355,
                    longitude = 77.2410,
                    locationName = "DLF Cyber City / South Court Saket",
                    checkInTime = "02:30 PM",
                    checkOutTime = "03:45 PM",
                    meetingNotes = "Signed off on initial Phase 1 scoping document. Follow up on Tuesday with formal quotation.",
                    outcome = "Deal Won"
                )
            )

            // Seed Expense & Reimbursement Claims
            db.expenseClaimDao().insertAll(
                listOf(
                    ExpenseClaimEntity(
                        employeeName = "Rahul Sharma",
                        category = "Travel & Fuel",
                        amount = 650.0,
                        date = "18 Sep 2026",
                        merchant = "Uber Premier / Delhi NCR",
                        description = "Travel to Apex Global Corp client office for executive demo.",
                        status = "Approved",
                        reviewedBy = "Finance Admin"
                    ),
                    ExpenseClaimEntity(
                        employeeName = "Arjun Mehta",
                        category = "Client Dinner",
                        amount = 2450.0,
                        date = "17 Sep 2026",
                        merchant = "The Imperial Hotel, Janpath",
                        description = "Working lunch with key stakeholders from Zenith Retail Solutions.",
                        status = "Pending",
                        reviewedBy = null
                    ),
                    ExpenseClaimEntity(
                        employeeName = "Priya Patel",
                        category = "Software & Tools",
                        amount = 1800.0,
                        date = "15 Sep 2026",
                        merchant = "Figma Enterprise License",
                        description = "Monthly UI/UX prototyping seat renewal for Client App project.",
                        status = "Reimbursed",
                        reviewedBy = "Operations Lead"
                    )
                )
            )

            // Seed Project Milestones
            db.projectMilestoneDao().insertAll(
                listOf(
                    ProjectMilestoneEntity(
                        projectId = 1L,
                        projectName = "Enterprise Mobile Portal",
                        title = "Sprint 1: Architecture & Auth Provider Setup",
                        description = "Role-based authentication, WhatsApp OTP, biometric encryption.",
                        targetDate = "10 Sep 2026",
                        completionPercent = 100,
                        isCompleted = true,
                        approvedByClient = true
                    ),
                    ProjectMilestoneEntity(
                        projectId = 1L,
                        projectName = "Enterprise Mobile Portal",
                        title = "Sprint 2: Geofence Attendance & Field Sales CRM",
                        description = "Location verification, client meeting check-ins, lead tracking.",
                        targetDate = "20 Sep 2026",
                        completionPercent = 85,
                        isCompleted = false,
                        approvedByClient = false
                    ),
                    ProjectMilestoneEntity(
                        projectId = 1L,
                        projectName = "Enterprise Mobile Portal",
                        title = "Sprint 3: Offline Vault & Automated Standup Reports",
                        description = "Document asset vault, exportable timesheets, team digests.",
                        targetDate = "30 Sep 2026",
                        completionPercent = 40,
                        isCompleted = false,
                        approvedByClient = false
                    )
                )
            )

            // Seed Document & Asset Vault
            db.vaultDocumentDao().insertAll(
                listOf(
                    VaultDocumentEntity(
                        title = "Making Brands Company Profile 2026",
                        category = "Brochures",
                        fileType = "PDF",
                        fileSize = "3.4 MB",
                        downloadUrlOrPath = "https://makingbrands.in/brochure/MakingBrands_Company_Profile.pdf",
                        description = "Comprehensive enterprise solutions portfolio, case studies, client testimonials.",
                        isOfflineAvailable = true,
                        uploadedAt = "Sep 2026"
                    ),
                    VaultDocumentEntity(
                        title = "Standard Mobile & Web Project Proposal Template",
                        category = "Proposal Templates",
                        fileType = "DOCX",
                        fileSize = "1.2 MB",
                        downloadUrlOrPath = "https://makingbrands.in/templates/MB_Proposal_Template_v2.docx",
                        description = "Standard RFP response template with SLA clauses and milestone scopes.",
                        isOfflineAvailable = true,
                        uploadedAt = "Aug 2026"
                    ),
                    VaultDocumentEntity(
                        title = "Official Brand Identity & Vector Assets Kit",
                        category = "Branding & Logos",
                        fileType = "ZIP",
                        fileSize = "12.8 MB",
                        downloadUrlOrPath = "https://makingbrands.in/assets/MakingBrands_Brand_Kit.zip",
                        description = "High-res PNGs, SVG vector logos, typography guides, presentation color palettes.",
                        isOfflineAvailable = true,
                        uploadedAt = "Sep 2026"
                    ),
                    VaultDocumentEntity(
                        title = "Employee Code of Conduct & Remote Work Policy",
                        category = "HR Policies",
                        fileType = "PDF",
                        fileSize = "850 KB",
                        downloadUrlOrPath = "https://makingbrands.in/hr/MB_Employee_Handbook_2026.pdf",
                        description = "Attendance guidelines, geofenced punch-in rules, leave regularization procedures.",
                        isOfflineAvailable = true,
                        uploadedAt = "Jul 2026"
                    )
                )
            )
        }
    }
}
