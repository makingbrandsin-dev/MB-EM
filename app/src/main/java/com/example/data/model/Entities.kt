package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

enum class Department {
    ENGINEERING,
    DESIGN,
    MARKETING,
    SALES,
    OPERATIONS,
    MANAGEMENT,
    HR
}

enum class EmployeeRole {
    ADMIN,
    MANAGER,
    TEAM_LEAD,
    DEVELOPER,
    DESIGNER,
    QA_ENGINEER,
    HR_SPECIALIST,
    SALES_EXECUTIVE
}

enum class EmployeeStatus {
    ACTIVE,
    ON_LEAVE,
    PROBATION,
    INACTIVE
}

enum class PresenceStatus {
    ONLINE,
    IN_MEETING,
    OFFLINE
}

enum class ProjectPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    OVERDUE
}

@Entity(
    tableName = "employees",
    indices = [
        Index(value = ["email"]),
        Index(value = ["department"]),
        Index(value = ["status"])
    ]
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val designation: String,
    val department: Department = Department.ENGINEERING,
    val role: EmployeeRole = EmployeeRole.DEVELOPER,
    val status: EmployeeStatus = EmployeeStatus.ACTIVE,
    val presenceStatus: PresenceStatus = PresenceStatus.ONLINE,
    val joiningDate: Date = Date(),
    val skills: List<String> = emptyList(),
    val assignedProjectIds: List<Long> = emptyList(),
    val avatarUrl: String? = null,
    val salary: Double? = null,
    val emergencyContact: String? = null,
    val password: String = "password123"
)

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD or DD MMM YYYY
    val checkInTime: String, // HH:mm:ss or HH:mm
    val checkOutTime: String? = null,
    val durationMinutes: Long = 0,
    val isWorking: Boolean = false,
    val status: String = "Present", // Present, Late, Half Day, On Leave
    val overtimeMinutes: Long = 0,
    val breakMinutes: Long = 0,
    val breakType: String = "None", // Tea Break, Lunch Break, Short Break, Custom
    val isOnBreak: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val employeeName: String = "Rahul Sharma",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = "Office HQ, Connaught Place, New Delhi",
    val isGeofenceVerified: Boolean = true,
    val selfieUri: String? = null,
    val isSynced: Boolean = true
)

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["status"]),
        Index(value = ["priority"])
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val clientName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val progressPercent: Int,
    val status: String, // Active, Completed, Overdue
    val priority: String = "High", // High, Medium, Low
    val startDate: String = "01 Sep 2025",
    val deadline: String = "28 Sep 2025",
    val managerName: String = "Rahul Sharma",
    val teamSize: Int = 5,
    val description: String = "Enterprise mobile & web portal revamp with seamless user experience.",
    val tags: List<String> = emptyList(),
    val assignedEmployeeIds: List<Long> = emptyList(),
    val budget: Double? = 250000.0,
    val createdAt: Date = Date()
)

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["status"]),
        Index(value = ["priority"]),
        Index(value = ["isCompleted"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 1,
    val projectName: String = "Website Revamp",
    val title: String,
    val dueDate: String,
    val priority: String, // High, Medium, Low
    val status: String, // Backlog, In Progress, Completed
    val isCompleted: Boolean = false,
    val assignee: String = "Rahul Sharma",
    val category: String = "Work", // Work, Personal, Urgent, Meeting, Review
    val estimatedTimeNeeded: String = "4 Hours", // e.g. "2 Hours", "1 Day", "3 Days", "1 Week"
    val dependsOnTaskId: Long? = null,
    val dependsOnTaskTitle: String? = null
)

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phoneNumber: String,
    val callMedium: String = "SIM Call", // "SIM Call", "WhatsApp Call"
    val callType: String = "Outgoing", // "Incoming", "Outgoing"
    val durationText: String = "02:45",
    val durationSeconds: Long = 165,
    val recordedAt: String = "Today, 10:45 AM",
    val filePath: String = "/recordings/rec_001.m4a",
    val audioWaveform: String = "25,45,70,85,60,95,75,50,80,65,40,90,60,35",
    val fileSizeText: String = "1.8 MB",
    val transcriptionSnippet: String = "Client discussed requirements for website revamp & custom CRM integration.",
    val isAutoSaved: Boolean = true,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String, // e.g. "INV-2026-0081"
    val clientName: String,
    val clientCompany: String,
    val clientEmail: String,
    val clientPhone: String,
    val issueDate: String,
    val dueDate: String,
    val currency: String = "₹", // "₹" or "$"
    val subtotal: Double,
    val taxPercent: Double = 18.0, // 18% GST
    val totalAmount: Double,
    val status: String = "Pending", // "Paid", "Pending", "Overdue", "Draft"
    val itemsSummary: String = "Website UI/UX Redesign & Android Application Development",
    val notes: String = "Payment terms: Net 15 days. Thank you for your business.",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationNumber: String, // e.g. "QT-2026-0042"
    val clientName: String,
    val clientCompany: String,
    val clientEmail: String,
    val clientPhone: String,
    val issueDate: String,
    val validUntil: String,
    val currency: String = "₹", // "₹" or "$"
    val subtotal: Double,
    val discountPercent: Double = 5.0,
    val taxPercent: Double = 18.0,
    val totalAmount: Double,
    val status: String = "Sent", // "Draft", "Sent", "Accepted", "Converted to Invoice", "Declined"
    val scopeOfWork: String = "Custom Mobile & Web Application Suite with Cloud Database & Lead Automation APIs",
    val termsAndConditions: String = "50% Advance on project kickoff, 50% on final UAT milestone.",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "auto_brochure_configs")
data class AutoBrochureConfigEntity(
    @PrimaryKey val id: Long = 1L,
    val isAutoSendEnabled: Boolean = true,
    val sendViaWhatsApp: Boolean = true,
    val sendViaEmail: Boolean = true,
    val brochureFileName: String = "MakingBrands_Company_Profile_2026.pdf",
    val brochureFileSize: String = "3.4 MB",
    val emailSubject: String = "Welcome to Making Brands — Company Profile & Solutions Brochure",
    val customMessage: String = "Hello {NAME}, thank you for contacting Making Brands! We have attached our official Company Profile & Solutions Portfolio PDF.",
    val totalSentCount: Int = 142,
    val lastSentTimestamp: String = "Today, 11:20 AM"
)

@Entity(tableName = "social_review_configs")
data class SocialReviewConfigEntity(
    @PrimaryKey val platformId: String, // "google", "trustpilot", "facebook", "instagram", "linkedin", "whatsapp", "custom"
    val platformName: String,
    val reviewUrl: String,
    val qrCodePayload: String,
    val ratingText: String = "4.9 ★",
    val totalReviewsCount: Int = 184,
    val isPrimary: Boolean = false,
    val customInviteText: String = "We’d love to hear your feedback! Tap the link or scan our QR code to review Making Brands."
)

@Entity(tableName = "leads")
data class LeadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val leadScore: Int, // e.g. 85
    val requirement: String, // e.g. Website + App Development
    val potentialValue: String, // e.g. ₹ 2,50,000 or $ 3,500
    val stage: String, // New, Contacted, Interested, Follow-up, Proposal, Negotiation, Won
    val assignedTo: String = "Arjun Mehta",
    val nextFollowUp: String = "Today, 4:30 PM",
    val notes: String = "Client requested detailed quote with milestone breakdowns.",
    val source: String = "Website", // Justdial, OLX, Facebook, Google Ads, Website, WhatsApp, LinkedIn, Other
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lead_source_configs")
data class LeadSourceConfigEntity(
    @PrimaryKey val sourceId: String, // justdial, olx, facebook, google_ads, website, whatsapp, instagram, linkedin, custom
    val displayName: String,
    val isEnabled: Boolean = true,
    val apiKey: String = "",
    val apiSecret: String = "",
    val webhookUrl: String = "",
    val defaultAssignee: String = "Rahul Sharma",
    val autoSyncIntervalMinutes: Int = 15,
    val lastSyncTime: String = "Just now",
    val totalLeadsIngested: Int = 0
)

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leadId: Long,
    val clientName: String,
    val taskDescription: String,
    val scheduledTime: String, // e.g. 10:30 AM
    val scheduledDateCategory: String, // Today, Upcoming, Completed
    val actionType: String = "Call", // Call, Open, Send proposal
    val isCompleted: Boolean = false
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val callType: String, // Incoming, Outgoing, Missed
    val status: String, // Connected, No Answer, Busy
    val timestampText: String, // Today, 09:32 AM
    val durationText: String, // 4m 12s
    val phoneNumber: String = "+91 98765 43210",
    val audioPath: String? = null,
    val summary: String? = null,
    val transcription: String? = null,
    val aiInsights: String? = null,
    val leadUpdated: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String, // general, dev_team, sales_team, marketing
    val senderName: String,
    val senderRole: String = "Team Member",
    val messageText: String,
    val timestampText: String,
    val isMe: Boolean = false,
    val attachmentFileName: String? = null,
    val attachmentFileSize: String? = null,
    val audioPath: String? = null,
    val audioDurationSeconds: Int = 0,
    val isVoiceMessage: Boolean = false,
    val isRead: Boolean = false,
    val readBy: String = "",
    val readAt: Long? = null,
    val reactionsJson: String = "", // JSON map: emoji -> list of user names
    val isSynced: Boolean = true
)

@Entity(
    tableName = "activity_feed_items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["createdAt"])
    ]
)
data class ActivityFeedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val authorName: String,
    val authorRole: String = "Team Member",
    val title: String,
    val content: String,
    val category: String = "Update", // Announcement, Achievement, Update, Milestone, Shoutout
    val likesCount: Int = 0,
    val likedByUsers: String = "", // Comma-separated names
    val reactionsJson: String = "", // JSON map: emoji -> list of user names
    val commentsCount: Int = 0,
    val isPinned: Boolean = false,
    val timestampText: String = "Just now",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subtitle: String,
    val timeAgo: String,
    val category: String, // followup, task, message, attendance, leave, project
    val isRead: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Long = 1L,
    val name: String = "",
    val role: String = "",
    val isOnboarded: Boolean = true,
    val email: String = "makingbrands.in@gmail.com",
    val phone: String = "+91 98765 43210",
    val department: String = "Engineering",
    val joiningDate: String = "15 Jan 2024",
    val emergencyContact: String = "+91 91234 56789",
    val address: String = "Connaught Place, New Delhi",
    val skills: String = "Kotlin, Jetpack Compose, Android, Cloud, UI/UX",
    val bio: String = "Building enterprise mobile experiences for Making Brands",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leave_applications")
data class LeaveApplicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val leaveType: String = "Casual Leave", // Casual Leave, Sick Leave, Paid Leave, Work From Home
    val startDate: String, // e.g. "16 Sep 2025"
    val endDate: String,   // e.g. "17 Sep 2025"
    val totalDays: Int = 1,
    val reason: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val appliedDate: String = "18 Sep 2025",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "client_meetings")
data class ClientMeetingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val company: String,
    val meetingPurpose: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "Client Office",
    val checkInTime: String,
    val checkOutTime: String? = null,
    val meetingNotes: String = "",
    val outcome: String = "Follow-up Required", // "Follow-up Required", "Proposal Requested", "Deal Won", "Needs Revision"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expense_claims")
data class ExpenseClaimEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeName: String = "Rahul Sharma",
    val category: String, // "Travel & Fuel", "Client Dinner", "Office Supplies", "Lodging", "Software & Tools"
    val amount: Double,
    val currency: String = "₹",
    val date: String,
    val merchant: String,
    val description: String,
    val receiptUri: String? = null,
    val status: String = "Pending", // "Pending", "Approved", "Reimbursed", "Rejected"
    val reviewedBy: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_milestones")
data class ProjectMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long = 1L,
    val projectName: String = "Enterprise Portal",
    val title: String,
    val description: String = "",
    val targetDate: String,
    val completionPercent: Int = 0, // 0 to 100
    val isCompleted: Boolean = false,
    val approvedByClient: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "Brochures", "Proposal Templates", "Branding & Logos", "Standard Contracts", "HR Policies"
    val fileType: String = "PDF", // "PDF", "DOCX", "PNG", "ZIP"
    val fileSize: String = "2.4 MB",
    val downloadUrlOrPath: String = "https://makingbrands.in/docs/sample.pdf",
    val description: String = "",
    val isOfflineAvailable: Boolean = true,
    val uploadedAt: String = "Today",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_regularizations")
data class AttendanceRegularizationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeName: String = "Rahul Sharma",
    val date: String,
    val requestedInTime: String,
    val requestedOutTime: String,
    val reason: String,
    val remarks: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val appliedAt: Long = System.currentTimeMillis()
)

data class HolidayItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val date: String,
    val day: String,
    val type: String = "Gazetted Holiday"
)

data class MiloKnowledgeItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val category: String = "Company FAQ",
    val content: String,
    val tags: List<String> = emptyList(),
    val lastUpdated: String = "Today"
)

data class CompanyProfile(
    val companyName: String = "Making Brands Pvt Ltd",
    val tagline: String = "Track · Manage · Grow",
    val industry: String = "Enterprise IT & Digital Marketing",
    val email: String = "contact@makingbrands.in",
    val phone: String = "+91 98765 43210",
    val website: String = "https://makingbrands.in",
    val address: String = "DLF Cyber City, Phase 2, Gurugram, Haryana 122002",
    val gstNumber: String = "07AAAAA0000A1Z5",
    val brochureUrl: String = "https://makingbrands.in/brochure.pdf",
    val overview: String = "Making Brands provides end-to-end Enterprise CRM, Workforce Attendance & Tracking, Lead Automation, and AI-powered operations management."
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String, // e.g. "TASK_REASSIGN", "ATTENDANCE_CHANGE", "LEAD_STATUS_UPDATE"
    val description: String,
    val performedBy: String,
    val entityId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shift_schedules")
data class ShiftScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeName: String = "Arjun Mehta",
    val employeeRole: String = "Operations Specialist",
    val shiftType: String = "Morning Shift", // "Morning Shift", "Afternoon Shift", "Night Shift", "General Day Shift", "Weekend On-Call"
    val shiftCode: String = "MOR-01", // "MOR-01", "AFT-02", "NIG-03", "DAY-01", "ONC-04"
    val startTime: String = "09:00 AM",
    val endTime: String = "06:00 PM",
    val date: String = "2026-10-07", // e.g. "2026-10-07"
    val dayOfWeek: String = "Wednesday", // "Monday", "Tuesday", etc.
    val location: String = "Main HQ - Floor 3", // "Main HQ - Floor 3", "Remote / WFH", "Client Site"
    val rotationCycleName: String = "Q4 Rotation Cycle - Week 41",
    val status: String = "Scheduled", // "Scheduled", "In Progress", "Completed", "Swapped", "On Leave"
    val notes: String = "Standard shift rotation assignment",
    val createdAt: Long = System.currentTimeMillis()
)


