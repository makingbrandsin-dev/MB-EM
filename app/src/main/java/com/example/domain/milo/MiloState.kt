package com.example.domain.milo

import androidx.compose.ui.graphics.Color

/**
 * The 13 core animation and emotional states for Milo,
 * the official lion assistant of MB Tracker.
 */
enum class MiloState(
    val title: String,
    val description: String,
    val emoji: String,
    val defaultSpeech: String,
    val primaryColor: Color = Color(0xFF705C30)
) {
    IDLE(
        title = "Idle",
        description = "Breathing, blinking, small tail movement",
        emoji = "🦁",
        defaultSpeech = "Ready when you are! Let's make today productive.",
        primaryColor = Color(0xFF705C30)
    ),
    WELCOME(
        title = "Welcome",
        description = "Waves and smiles when you open the app",
        emoji = "👋",
        defaultSpeech = "Good morning! Let's grow today!",
        primaryColor = Color(0xFF705C30)
    ),
    WORKING(
        title = "Working",
        description = "Typing and focused on tasks with MB laptop",
        emoji = "💻",
        defaultSpeech = "Syncing records and optimizing workflows...",
        primaryColor = Color(0xFF4A7C59)
    ),
    THINKING(
        title = "Thinking",
        description = "Hand on chin with floating idea animation",
        emoji = "💡",
        defaultSpeech = "Analyzing your pipeline and team activity...",
        primaryColor = Color(0xFF705C30)
    ),
    LEAD_IMPORTED(
        title = "Lead Imported",
        description = "Excited expression, points to new leads",
        emoji = "📥",
        defaultSpeech = "New leads just arrived! Check your inbox.",
        primaryColor = Color(0xFF4A7C59)
    ),
    NEW_LEAD(
        title = "New Lead",
        description = "Celebrates new lead creation",
        emoji = "✨",
        defaultSpeech = "Awesome! New lead recorded in the CRM.",
        primaryColor = Color(0xFF705C30)
    ),
    FOLLOW_UP(
        title = "Follow Up",
        description = "Holds phone and shows reminder calendar",
        emoji = "📞",
        defaultSpeech = "Follow-ups due today! Give your clients a call.",
        primaryColor = Color(0xFF059669)
    ),
    SUCCESS(
        title = "Success",
        description = "Thumbs up for completed task",
        emoji = "👍",
        defaultSpeech = "Great job! Task completed on time.",
        primaryColor = Color(0xFF10B981)
    ),
    CONVERTED(
        title = "Converted",
        description = "Big celebration with confetti & victory",
        emoji = "🏆",
        defaultSpeech = "Deal WON! Another milestone for Making Brands!",
        primaryColor = Color(0xFFEAB308)
    ),
    WARNING(
        title = "Warning",
        description = "Alert gesture with serious focused face",
        emoji = "⚠️",
        defaultSpeech = "Attention needed: Follow-up is overdue!",
        primaryColor = Color(0xFFEA580C)
    ),
    ERROR(
        title = "Error",
        description = "Concerned expression for system issues",
        emoji = "❌",
        defaultSpeech = "Oops, something went wrong. Let's fix it.",
        primaryColor = Color(0xFFDC2626)
    ),
    GOODBYE(
        title = "Goodbye",
        description = "Waves when you logout",
        emoji = "🙋",
        defaultSpeech = "Great work today! See you tomorrow.",
        primaryColor = Color(0xFF475569)
    ),
    CELEBRATION(
        title = "Celebration",
        description = "Jump and confetti for major milestones",
        emoji = "🎉",
        defaultSpeech = "Incredible milestone achieved! High five!",
        primaryColor = Color(0xFF8B5CF6)
    )
}
