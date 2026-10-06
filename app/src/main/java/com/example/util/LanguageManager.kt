package com.example.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    TELUGU("te", "Telugu", "తెలుగు"),
    TAMIL("ta", "Tamil", "தமிழ்")
}

object LanguageManager {
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
    }

    // Localized Strings Map for Milo & Key App Elements
    fun getMiloGreeting(language: AppLanguage = _currentLanguage.value): String {
        return when (language) {
            AppLanguage.ENGLISH -> "Good Morning, Team 👋 Let's grow today!"
            AppLanguage.TELUGU -> "శుభోదయం, టీమ్ 👋 ఈరోజు మనం అభివృద్ధి చెందుదాం!"
            AppLanguage.TAMIL -> "காலை வணக்கம், குழு 👋 இன்று நாம் வளர்ச்சி அடைவோம்!"
        }
    }

    fun getMiloLeadImported(count: Int, language: AppLanguage = _currentLanguage.value): String {
        return when (language) {
            AppLanguage.ENGLISH -> "$count new leads imported. Need immediate follow-up."
            AppLanguage.TELUGU -> "$count కొత్త లీడ్లు దిగుమతి అయ్యాయి. వెంటనే ఫాలోఅప్ చేయండి."
            AppLanguage.TAMIL -> "$count புதிய லீட்கள் இறக்குமதி செய்யப்பட்டன. உடனடி பின்தொடர்தல் தேவை."
        }
    }

    fun getMiloFollowUpPrompt(language: AppLanguage = _currentLanguage.value): String {
        return when (language) {
            AppLanguage.ENGLISH -> "You have follow-ups due today."
            AppLanguage.TELUGU -> "ఈరోజు మీ ఫాలోఅప్‌లు పెండింగ్‌లో ఉన్నాయి."
            AppLanguage.TAMIL -> "இன்று உங்களுக்கு பின்தொடர்தல்கள் உள்ளன."
        }
    }
}
