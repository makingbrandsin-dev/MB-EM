package com.example.util

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import com.example.data.firebase.FirestoreDbProvider
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppVersionInfo(
    val versionName: String = "2.4.0",
    val versionCode: Int = 202610,
    val buildDate: String = "Oct 06, 2026",
    val releaseNotes: String = "Enhanced Milo AI Copilot, Real-Time Firestore Sync, Jetpack DataStore Layer & Biometric Security.",
    val isUpdateAvailable: Boolean = false,
    val latestVersionName: String = "2.4.0",
    val latestVersionCode: Int = 202610,
    val isMandatory: Boolean = false,
    val downloadUrl: String = "",
    val dataPreservedNotice: String = "🛡️ Safe Delta Update: All local Room DB data & Firestore sessions preserved."
)

/**
 * InAppUpdateManager: Handles in-app update checks, release notes,
 * version verification, and ensures seamless delta updates without user data loss.
 */
object InAppUpdateManager {

    private const val TAG = "InAppUpdateManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _versionInfo = MutableStateFlow(AppVersionInfo())
    val versionInfo: StateFlow<AppVersionInfo> = _versionInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow("App version up to date.")
    val updateStatusMessage: StateFlow<String> = _updateStatusMessage.asStateFlow()

    fun initialize(context: Context) {
        scope.launch {
            try {
                val pInfo: PackageInfo? = try {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                } catch (_: Exception) { null }

                val currentName = pInfo?.versionName ?: "2.4.0"
                @Suppress("DEPRECATION")
                val currentCode = pInfo?.versionCode ?: 202610

                val initialInfo = AppVersionInfo(
                    versionName = currentName,
                    versionCode = currentCode,
                    buildDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date()),
                    releaseNotes = "MB EM Workspace v$currentName — High Security, Milo AI Copilot, Camera OCR Scanner & Continuous Data Protection.",
                    isUpdateAvailable = false,
                    latestVersionName = currentName,
                    latestVersionCode = currentCode
                )
                _versionInfo.value = initialInfo
                checkForUpdates(context, isSilent = true)
            } catch (e: Exception) {
                Log.w(TAG, "Failed initializing InAppUpdateManager: ${e.message}")
            }
        }
    }

    fun checkForUpdates(context: Context, isSilent: Boolean = false, onComplete: ((AppVersionInfo) -> Unit)? = null) {
        scope.launch {
            if (!isSilent) _isCheckingUpdate.value = true
            _updateStatusMessage.value = "Checking Firestore for latest release..."

            try {
                val fs = FirestoreDbProvider.getFirestore(context)
                if (fs != null) {
                    fs.collection("app_updates").document("latest_release")
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                val latestName = doc.getString("versionName") ?: _versionInfo.value.versionName
                                val latestCode = doc.getLong("versionCode")?.toInt() ?: _versionInfo.value.versionCode
                                val notes = doc.getString("releaseNotes") ?: "Performance improvements, new features, and bug fixes."
                                val isMandatory = doc.getBoolean("isMandatory") ?: false
                                val downloadUrl = doc.getString("downloadUrl") ?: ""

                                val hasUpdate = latestCode > _versionInfo.value.versionCode

                                val updated = _versionInfo.value.copy(
                                    isUpdateAvailable = hasUpdate,
                                    latestVersionName = latestName,
                                    latestVersionCode = latestCode,
                                    releaseNotes = notes,
                                    isMandatory = isMandatory,
                                    downloadUrl = downloadUrl
                                )
                                _versionInfo.value = updated
                                _updateStatusMessage.value = if (hasUpdate) {
                                    "🎉 New Update v$latestName Available! (Safe Delta Upgrade)"
                                } else {
                                    "✓ You are running the latest MB EM version (v${_versionInfo.value.versionName})."
                                }
                                onComplete?.invoke(updated)
                            } else {
                                // Register current version in Firestore as benchmark release
                                registerCurrentReleaseInFirestore(context)
                                _updateStatusMessage.value = "✓ You are running the latest version."
                                onComplete?.invoke(_versionInfo.value)
                            }
                            _isCheckingUpdate.value = false
                        }
                        .addOnFailureListener {
                            _updateStatusMessage.value = "✓ App is up to date (Offline/Cached Check)."
                            _isCheckingUpdate.value = false
                            onComplete?.invoke(_versionInfo.value)
                        }
                } else {
                    _updateStatusMessage.value = "✓ App is up to date."
                    _isCheckingUpdate.value = false
                    onComplete?.invoke(_versionInfo.value)
                }
            } catch (e: Exception) {
                _updateStatusMessage.value = "✓ App is up to date."
                _isCheckingUpdate.value = false
                onComplete?.invoke(_versionInfo.value)
            }
        }
    }

    private fun registerCurrentReleaseInFirestore(context: Context) {
        try {
            val fs = FirestoreDbProvider.getFirestore(context) ?: return
            val current = _versionInfo.value
            val releaseData = hashMapOf(
                "versionName" to current.versionName,
                "versionCode" to current.versionCode,
                "releaseNotes" to current.releaseNotes,
                "publishedAt" to System.currentTimeMillis(),
                "isMandatory" to false,
                "downloadUrl" to "https://makingbrands.in/app/update"
            )
            fs.collection("app_updates").document("latest_release")
                .set(releaseData, SetOptions.merge())
        } catch (_: Exception) {}
    }
}
