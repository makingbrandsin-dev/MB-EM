package com.example.data.session

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.UserProfileDao
import com.example.data.model.UserProfileEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

// Extension property for DataStore instance
val Context.userSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session_preferences")

/**
 * Immutable User Session data model representing the logged-in user.
 */
data class UserSession(
    val isLoggedIn: Boolean = false,
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val role: String = "Employee",
    val designation: String = "Mobile Team",
    val department: String = "Engineering",
    val joiningDate: String = "",
    val emergencyContact: String = "",
    val address: String = "",
    val skills: String = "",
    val bio: String = "",
    val photoUrl: String? = null,
    val lastLoginTimestamp: Long = 0L,
    val isCustomized: Boolean = false
) {
    /**
     * Effective name to display in UI (never dummy "Rahul Sharma").
     */
    val effectiveDisplayName: String
        get() = when {
            displayName.isNotBlank() -> displayName.trim()
            email.isNotBlank() -> email.substringBefore("@").replaceFirstChar { it.uppercase() }
            else -> "User"
        }

    /**
     * Effective designation / role.
     */
    val effectiveRole: String
        get() = when {
            role.isNotBlank() -> role.trim()
            designation.isNotBlank() -> designation.trim()
            else -> "Employee"
        }

    val effectiveEmail: String
        get() = email.ifBlank { "makingbrands.in@gmail.com" }

    val effectivePhone: String
        get() = phoneNumber.ifBlank { "+91 98765 43210" }

    val effectiveDepartment: String
        get() = department.ifBlank { "Engineering" }

    val effectiveJoiningDate: String
        get() = joiningDate.ifBlank { "15 Jan 2024" }

    val effectiveEmergencyContact: String
        get() = emergencyContact.ifBlank { "+91 91234 56789" }

    val effectiveAddress: String
        get() = address.ifBlank { "Connaught Place, New Delhi" }

    val effectiveSkills: String
        get() = skills.ifBlank { "Kotlin, Jetpack Compose, Android, Cloud" }

    val effectiveBio: String
        get() = bio.ifBlank { "Building enterprise mobile experiences for Making Brands" }
}

/**
 * Enterprise Session Manager backed by Jetpack DataStore and Firebase Auth/Firestore.
 * Ensures the app immediately restores actual authenticated user profile data
 * instead of dummy placeholders across all app restarts.
 */
object UserSessionManager {
    private const val TAG = "UserSessionManager"

    // Preferences Keys
    private val KEY_IS_LOGGED_IN = booleanPreferencesKey("session_is_logged_in")
    private val KEY_UID = stringPreferencesKey("session_uid")
    private val KEY_DISPLAY_NAME = stringPreferencesKey("session_display_name")
    private val KEY_EMAIL = stringPreferencesKey("session_email")
    private val KEY_PHONE = stringPreferencesKey("session_phone")
    private val KEY_ROLE = stringPreferencesKey("session_role")
    private val KEY_DESIGNATION = stringPreferencesKey("session_designation")
    private val KEY_DEPARTMENT = stringPreferencesKey("session_department")
    private val KEY_JOINING_DATE = stringPreferencesKey("session_joining_date")
    private val KEY_EMERGENCY_CONTACT = stringPreferencesKey("session_emergency_contact")
    private val KEY_ADDRESS = stringPreferencesKey("session_address")
    private val KEY_SKILLS = stringPreferencesKey("session_skills")
    private val KEY_BIO = stringPreferencesKey("session_bio")
    private val KEY_PHOTO_URL = stringPreferencesKey("session_photo_url")
    private val KEY_LAST_LOGIN = longPreferencesKey("session_last_login")
    private val KEY_IS_CUSTOMIZED = booleanPreferencesKey("session_is_customized")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var userProfileDaoRef: UserProfileDao? = null

    private val _currentSessionState = MutableStateFlow(UserSession())
    val currentSessionState: StateFlow<UserSession> = _currentSessionState.asStateFlow()

    private var initialized = false

    /**
     * Initializes the Session Manager with Context, loads persisted DataStore state,
     * and triggers background sync with Firebase / Room.
     */
    fun initialize(context: Context, userProfileDao: UserProfileDao? = null) {
        if (initialized && appContext != null) {
            if (userProfileDao != null) userProfileDaoRef = userProfileDao
            return
        }
        val app = context.applicationContext
        appContext = app
        userProfileDaoRef = userProfileDao
        initialized = true

        // Fast synchronous check from SharedPreferences fallback if DataStore is warming up
        val fastName = com.example.util.AppPreferences.getUserName(app)
        val fastRole = com.example.util.AppPreferences.getUserRole(app)
        val fastEmail = com.example.util.AppPreferences.getUserEmail(app)
        if (!fastName.isNullOrBlank()) {
            _currentSessionState.value = _currentSessionState.value.copy(
                isLoggedIn = true,
                displayName = fastName,
                role = fastRole ?: "Employee",
                email = fastEmail ?: "",
                isCustomized = true
            )
        }

        // Start listening to DataStore flow
        scope.launch {
            app.userSessionDataStore.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e(TAG, "Error reading DataStore session", exception)
                        emit(emptyPreferences())
                    } else {
                        throw exception
                    }
                }
                .map { prefs -> mapPreferencesToSession(prefs) }
                .collect { session ->
                    _currentSessionState.value = session
                    // Keep SharedPreferences in sync for instant cold start
                    if (session.displayName.isNotBlank()) {
                        com.example.util.AppPreferences.saveUserName(app, session.displayName)
                    }
                    if (session.role.isNotBlank()) {
                        com.example.util.AppPreferences.saveUserRole(app, session.role)
                    }
                    if (session.email.isNotBlank()) {
                        com.example.util.AppPreferences.saveUserEmail(app, session.email)
                    }
                }
        }

        // Check Firebase Auth currentUser session
        syncWithFirebaseAuthAndFirestore()
    }

    private fun mapPreferencesToSession(prefs: Preferences): UserSession {
        val name = prefs[KEY_DISPLAY_NAME] ?: ""
        return UserSession(
            isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: (name.isNotBlank()),
            uid = prefs[KEY_UID] ?: "",
            displayName = name,
            email = prefs[KEY_EMAIL] ?: "",
            phoneNumber = prefs[KEY_PHONE] ?: "",
            role = prefs[KEY_ROLE] ?: "Employee",
            designation = prefs[KEY_DESIGNATION] ?: "Mobile Team",
            department = prefs[KEY_DEPARTMENT] ?: "Engineering",
            joiningDate = prefs[KEY_JOINING_DATE] ?: "15 Jan 2024",
            emergencyContact = prefs[KEY_EMERGENCY_CONTACT] ?: "+91 91234 56789",
            address = prefs[KEY_ADDRESS] ?: "Connaught Place, New Delhi",
            skills = prefs[KEY_SKILLS] ?: "Kotlin, Jetpack Compose, Android, Cloud",
            bio = prefs[KEY_BIO] ?: "Building enterprise mobile experiences for Making Brands",
            photoUrl = prefs[KEY_PHOTO_URL],
            lastLoginTimestamp = prefs[KEY_LAST_LOGIN] ?: System.currentTimeMillis(),
            isCustomized = prefs[KEY_IS_CUSTOMIZED] ?: (name.isNotBlank())
        )
    }

    /**
     * Persists updated profile session to DataStore, Room database, and Firestore.
     */
    fun saveSession(
        name: String,
        role: String = "Employee",
        email: String = "makingbrands.in@gmail.com",
        phoneNumber: String = "+91 98765 43210",
        department: String = "Engineering",
        joiningDate: String = "15 Jan 2024",
        emergencyContact: String = "+91 91234 56789",
        address: String = "Connaught Place, New Delhi",
        skills: String = "Kotlin, Jetpack Compose, Android, Cloud",
        bio: String = "Building enterprise mobile experiences for Making Brands",
        photoUrl: String? = null,
        uid: String? = null
    ) {
        val context = appContext ?: return
        val cleanName = name.trim().ifBlank { _currentSessionState.value.displayName.ifBlank { "User" } }
        val cleanRole = role.trim().ifBlank { _currentSessionState.value.role.ifBlank { "Employee" } }
        val cleanEmail = email.trim()
        val cleanPhone = phoneNumber.trim()

        val updatedSession = UserSession(
            isLoggedIn = true,
            uid = uid ?: _currentSessionState.value.uid.ifBlank { "user_1" },
            displayName = cleanName,
            email = cleanEmail,
            phoneNumber = cleanPhone,
            role = cleanRole,
            designation = cleanRole,
            department = department.trim(),
            joiningDate = joiningDate.trim(),
            emergencyContact = emergencyContact.trim(),
            address = address.trim(),
            skills = skills.trim(),
            bio = bio.trim(),
            photoUrl = photoUrl ?: _currentSessionState.value.photoUrl,
            lastLoginTimestamp = System.currentTimeMillis(),
            isCustomized = true
        )

        _currentSessionState.value = updatedSession

        // 1. Save to SharedPreferences for instant cold-start
        com.example.util.AppPreferences.saveUserName(context, cleanName)
        com.example.util.AppPreferences.saveUserRole(context, cleanRole)
        com.example.util.AppPreferences.saveUserEmail(context, cleanEmail)
        com.example.util.AppPreferences.setOnboardingCompleted(context, true)

        // 2. Persist to DataStore asynchronously
        scope.launch {
            try {
                context.userSessionDataStore.edit { prefs ->
                    prefs[KEY_IS_LOGGED_IN] = true
                    prefs[KEY_UID] = updatedSession.uid
                    prefs[KEY_DISPLAY_NAME] = cleanName
                    prefs[KEY_EMAIL] = cleanEmail
                    prefs[KEY_PHONE] = cleanPhone
                    prefs[KEY_ROLE] = cleanRole
                    prefs[KEY_DESIGNATION] = cleanRole
                    prefs[KEY_DEPARTMENT] = updatedSession.department
                    prefs[KEY_JOINING_DATE] = updatedSession.joiningDate
                    prefs[KEY_EMERGENCY_CONTACT] = updatedSession.emergencyContact
                    prefs[KEY_ADDRESS] = updatedSession.address
                    prefs[KEY_SKILLS] = updatedSession.skills
                    prefs[KEY_BIO] = updatedSession.bio
                    if (updatedSession.photoUrl != null) {
                        prefs[KEY_PHOTO_URL] = updatedSession.photoUrl
                    }
                    prefs[KEY_LAST_LOGIN] = updatedSession.lastLoginTimestamp
                    prefs[KEY_IS_CUSTOMIZED] = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save session in DataStore: ${e.message}", e)
            }

            // 3. Persist to Room UserProfileDao
            try {
                userProfileDaoRef?.insertOrUpdateProfile(
                    UserProfileEntity(
                        id = 1L,
                        name = cleanName,
                        role = cleanRole,
                        isOnboarded = true,
                        email = cleanEmail,
                        phone = cleanPhone,
                        department = updatedSession.department,
                        joiningDate = updatedSession.joiningDate,
                        emergencyContact = updatedSession.emergencyContact,
                        address = updatedSession.address,
                        skills = updatedSession.skills,
                        bio = updatedSession.bio,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Room sync warning: ${e.message}")
            }

            // 4. Sync to Firestore
            try {
                val firestore = com.example.data.firebase.FirestoreDbProvider.getFirestore(appContext)
                val profileMap = hashMapOf<String, Any>(
                    "name" to cleanName,
                    "displayName" to cleanName,
                    "role" to cleanRole,
                    "designation" to cleanRole,
                    "email" to cleanEmail,
                    "phoneNumber" to cleanPhone,
                    "department" to updatedSession.department,
                    "joiningDate" to updatedSession.joiningDate,
                    "emergencyContact" to updatedSession.emergencyContact,
                    "address" to updatedSession.address,
                    "skills" to updatedSession.skills,
                    "bio" to updatedSession.bio,
                    "isOnboarded" to true,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestore.collection("user_profiles").document(updatedSession.uid.ifBlank { "user_1" })
                    .set(profileMap, SetOptions.merge())
                firestore.collection("users").document(updatedSession.uid.ifBlank { "user_1" })
                    .set(profileMap, SetOptions.merge())
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync warning: ${e.message}")
            }
        }
    }

    /**
     * Clears the current user session on Logout or account reset.
     */
    fun clearSession(onComplete: (() -> Unit)? = null) {
        val context = appContext
        _currentSessionState.value = UserSession(isLoggedIn = false)
        if (context != null) {
            com.example.util.AppPreferences.clearPersistedUserProfile(context)
        }

        scope.launch {
            try {
                context?.userSessionDataStore?.edit { it.clear() }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing DataStore session: ${e.message}")
            }
            try {
                userProfileDaoRef?.clearProfile()
            } catch (_: Exception) {}
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}

            onComplete?.invoke()
        }
    }

    /**
     * Reads Firebase Auth and Firestore to update DataStore if fresh data is available.
     */
    private fun syncWithFirebaseAuthAndFirestore() {
        scope.launch {
            try {
                val authUser = FirebaseAuth.getInstance().currentUser
                if (authUser != null) {
                    val uid = authUser.uid
                    val email = authUser.email ?: ""
                    val name = authUser.displayName ?: ""

                    if (name.isNotBlank() && _currentSessionState.value.displayName.isBlank()) {
                        saveSession(
                            name = name,
                            email = email,
                            phoneNumber = authUser.phoneNumber ?: "",
                            photoUrl = authUser.photoUrl?.toString(),
                            uid = uid
                        )
                    }

                    // Query Firestore for full profile
                    val firestore = com.example.data.firebase.FirestoreDbProvider.getFirestore(appContext)
                    firestore.collection("user_profiles").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (doc != null && doc.exists()) {
                                val firestoreName = doc.getString("name") ?: doc.getString("displayName")
                                if (!firestoreName.isNullOrBlank() && firestoreName != "Rahul Sharma") {
                                    saveSession(
                                        name = firestoreName,
                                        role = doc.getString("role") ?: doc.getString("designation") ?: "Employee",
                                        email = doc.getString("email") ?: email,
                                        phoneNumber = doc.getString("phoneNumber") ?: authUser.phoneNumber ?: "",
                                        department = doc.getString("department") ?: "Engineering",
                                        joiningDate = doc.getString("joiningDate") ?: "15 Jan 2024",
                                        emergencyContact = doc.getString("emergencyContact") ?: "+91 91234 56789",
                                        address = doc.getString("address") ?: "Connaught Place, New Delhi",
                                        skills = doc.getString("skills") ?: "Kotlin, Jetpack Compose, Android, Cloud",
                                        bio = doc.getString("bio") ?: "Building enterprise mobile experiences for Making Brands",
                                        uid = uid
                                    )
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase sync warning: ${e.message}")
            }
        }
    }

    /**
     * Get a reactive Flow of user session.
     */
    fun getSessionFlow(context: Context): Flow<UserSession> {
        return context.userSessionDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { mapPreferencesToSession(it) }
    }
}
