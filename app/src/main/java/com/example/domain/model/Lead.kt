package com.example.domain.model

/**
 * Domain status enum for a CRM Lead.
 */
enum class LeadStatus(val displayName: String) {
    NEW("New"),
    CONTACTED("Contacted"),
    INTERESTED("Interested"),
    FOLLOW_UP("Follow-up"),
    PROPOSAL("Proposal"),
    NEGOTIATION("Negotiation"),
    WON("Won"),
    LOST("Lost");

    companion object {
        fun fromString(value: String): LeadStatus =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: NEW
    }
}

/**
 * Domain priority enum for a CRM Lead.
 */
enum class LeadPriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent");

    companion object {
        fun fromString(value: String): LeadPriority =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: MEDIUM
    }
}

/**
 * Domain acquisition source enum for a CRM Lead.
 */
enum class LeadSource(val displayName: String) {
    WEBSITE("Website"),
    JUSTDIAL("Justdial"),
    FACEBOOK("Facebook"),
    GOOGLE_ADS("Google Ads"),
    WHATSAPP("WhatsApp"),
    LINKEDIN("LinkedIn"),
    OLX("OLX"),
    REFERRAL("Referral"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): LeadSource =
            entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: WEBSITE
    }
}

/**
 * Domain representation of a Sales / CRM Lead.
 */
data class Lead(
    val id: Long = 0,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val status: LeadStatus = LeadStatus.NEW,
    val priority: LeadPriority = LeadPriority.MEDIUM,
    val source: LeadSource = LeadSource.WEBSITE,
    val leadScore: Int = 80,
    val requirement: String = "Enterprise Digital Solutions",
    val potentialValue: Double = 50000.0,
    val potentialValueFormatted: String = "₹ 50,000",
    val assignedTo: String = "Rahul Sharma",
    val nextFollowUp: String = "Today, 4:30 PM",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
