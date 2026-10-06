package com.example.util

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "mb_traker_app_prefs"
    private const val KEY_DUMMY_DATA_CLEARED = "dummy_data_cleared"
    private const val KEY_APP_INSTALL_DATE = "app_install_date"
    private const val KEY_ONBOARDING_COMPLETED = "milo_onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }

    fun getAppInstallDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var date = prefs.getString(KEY_APP_INSTALL_DATE, null)
        if (date.isNullOrBlank()) {
            date = getCurrentDateString()
            prefs.edit().putString(KEY_APP_INSTALL_DATE, date).apply()
        }
        return date
    }

    fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun isDummyDataCleared(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DUMMY_DATA_CLEARED, false)
    }

    fun setDummyDataCleared(context: Context, cleared: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DUMMY_DATA_CLEARED, cleared)
            .apply()
    }

    private const val KEY_AUTO_CALL_RECORDING = "auto_call_recording_enabled"

    fun isAutoCallRecordingEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_CALL_RECORDING, true)
    }

    fun setAutoCallRecordingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CALL_RECORDING, enabled)
            .apply()
    }

    private const val KEY_BATTERY_SAVER_MODE = "battery_saver_mode"
    private const val KEY_BATTERY_SAVER_THRESHOLD = "battery_saver_threshold"
    private const val KEY_CUSTOM_APP_LOGO_URI = "custom_app_logo_uri"
    private const val KEY_APP_LOGO_PRESET = "app_logo_preset"
    private const val KEY_APP_ICON_BG_COLOR = "app_icon_bg_color"

    fun getCustomAppLogoUri(context: Context): String? {
        val uri = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_APP_LOGO_URI, null)
        return if (uri.isNullOrBlank()) null else uri
    }

    fun saveCustomAppLogoUri(context: Context, uriString: String?) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_APP_LOGO_URI, uriString?.trim())
            .apply()
    }

    fun getAppLogoPreset(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LOGO_PRESET, "MILO_LION") ?: "MILO_LION"
    }

    fun saveAppLogoPreset(context: Context, presetId: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LOGO_PRESET, presetId)
            .apply()
    }

    fun getAppIconBgColor(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_ICON_BG_COLOR, "#0F172A") ?: "#0F172A"
    }

    fun saveAppIconBgColor(context: Context, hexColor: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_ICON_BG_COLOR, hexColor)
            .apply()
    }

    private const val KEY_USER_NAME = "persisted_user_name"
    private const val KEY_USER_ROLE = "persisted_user_role"
    private const val KEY_USER_EMAIL = "persisted_user_email"
    private const val KEY_USER_PHONE = "persisted_user_phone"
    private const val KEY_USER_PROFILE_SET = "is_user_profile_set"

    fun getUserName(context: Context): String? {
        val name = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
        return if (name.isNullOrBlank()) null else name.trim()
    }

    fun saveUserName(context: Context, name: String) {
        if (name.isNotBlank()) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_NAME, name.trim())
                .putBoolean(KEY_USER_PROFILE_SET, true)
                .apply()
        }
    }

    fun getUserRole(context: Context): String? {
        val role = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ROLE, null)
        return if (role.isNullOrBlank()) null else role.trim()
    }

    fun saveUserRole(context: Context, role: String) {
        if (role.isNotBlank()) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ROLE, role.trim())
                .apply()
        }
    }

    fun getUserEmail(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_EMAIL, null)
    }

    fun saveUserEmail(context: Context, email: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_EMAIL, email.trim())
            .apply()
    }

    fun isUserProfileSet(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USER_PROFILE_SET, false)
    }

    fun clearPersistedUserProfile(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PHONE)
            .putBoolean(KEY_USER_PROFILE_SET, false)
            .apply()
    }

    fun getBatterySaverMode(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BATTERY_SAVER_MODE, "AUTO") ?: "AUTO"
    }

    fun setBatterySaverMode(context: Context, mode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BATTERY_SAVER_MODE, mode)
            .apply()
    }

    fun getBatterySaverThreshold(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BATTERY_SAVER_THRESHOLD, 20)
    }

    fun setBatterySaverThreshold(context: Context, threshold: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BATTERY_SAVER_THRESHOLD, threshold)
            .apply()
    }

    private const val KEY_DIALED_NUMBERS = "dialed_numbers_set"

    fun addAppDialedNumber(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_DIALED_NUMBERS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.add(number.trim())
        val clean = number.filter { it.isDigit() }.takeLast(10)
        if (clean.isNotBlank()) {
            currentSet.add(clean)
        }
        prefs.edit().putStringSet(KEY_DIALED_NUMBERS, currentSet).apply()
    }

    fun isNumberDialedFromApp(context: Context, number: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_DIALED_NUMBERS, emptySet()) ?: emptySet()
        val trimmed = number.trim()
        val clean = number.filter { it.isDigit() }.takeLast(10)
        return currentSet.contains(trimmed) || (clean.isNotBlank() && currentSet.contains(clean))
    }

    // --- Dynamic Notification Category Toggles (Options in Admin Panel) ---
    fun isNotificationEnabled(context: Context, category: String): Boolean {
        val key = "notif_enabled_${category.lowercase()}"
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(key, true) // Enabled by default
    }

    fun setNotificationEnabled(context: Context, category: String, enabled: Boolean) {
        val key = "notif_enabled_${category.lowercase()}"
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, enabled)
            .apply()
    }

    fun isNotificationSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean("notif_sound_enabled", true) // Enabled by default
    }

    fun setNotificationSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notif_sound_enabled", enabled)
            .apply()
    }
}
