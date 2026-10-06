package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Helper utility for handling Admin App Logo & Launcher Icon updates.
 * Saves custom uploaded logo files locally and updates app icon preferences.
 */
object AppIconHelper {
    private const val TAG = "AppIconHelper"
    private const val CUSTOM_LOGO_FILENAME = "custom_app_logo.png"

    data class AppLogoPreset(
        val id: String,
        val title: String,
        val subtitle: String,
        val bgHex: String,
        val previewUri: String? = null,
        val iconRes: Int? = null
    )

    fun getBuiltInPresets(): List<AppLogoPreset> {
        return listOf(
            AppLogoPreset("MILO_LION", "🦁 Milo Mascot", "Classic Lion Gold & Slate", "#0F172A"),
            AppLogoPreset("BLUE_SHIELD", "⚡ 20X Tech Shield", "Electric Blue Enterprise", "#1E3A8A"),
            AppLogoPreset("GOLD_CROWN", "👑 Executive Crown", "Royal Gold & Dark Onyx", "#18181B"),
            AppLogoPreset("ROCKET_GROWTH", "🚀 Growth Rocket", "Modern Violet & Indigo", "#312E81"),
            AppLogoPreset("EMERALD_GLOBE", "🌿 Emerald Globe", "Sustainable Green Tech", "#064E3B")
        )
    }

    /**
     * Saves a Uri selected by Admin from Gallery/Camera into internal storage as custom_app_logo.png
     */
    fun saveCustomLogoFromUri(context: Context, imageUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val destFile = File(context.filesDir, CUSTOM_LOGO_FILENAME)

            if (inputStream != null) {
                FileOutputStream(destFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()

                val savedPath = destFile.absolutePath
                val localUri = Uri.fromFile(destFile).toString()
                AppPreferences.saveCustomAppLogoUri(context, localUri)
                Log.d(TAG, "Custom logo saved successfully to $savedPath")
                localUri
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom app logo from Uri: ${e.message}", e)
            null
        }
    }

    /**
     * Resets the logo to default Milo Mascot
     */
    fun resetToDefaultLogo(context: Context) {
        AppPreferences.saveCustomAppLogoUri(context, null)
        AppPreferences.saveAppLogoPreset(context, "MILO_LION")
        AppPreferences.saveAppIconBgColor(context, "#0F172A")
        val destFile = File(context.filesDir, CUSTOM_LOGO_FILENAME)
        if (destFile.exists()) {
            destFile.delete()
        }
    }
}
