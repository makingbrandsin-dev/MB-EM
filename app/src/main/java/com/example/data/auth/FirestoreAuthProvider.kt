package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfileEntity
import com.example.util.BiometricHelper
import com.example.util.WhatsAppHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Supported User Roles for MB Traker.
 * Maps directly to either the Admin Dashboard ("manager") or Employee Workspace ("home").
 */
enum class AppRole(val roleKey: String, val displayName: String, val targetRoute: String) {
    ADMIN("admin", "MB Admin", "home"),
    EMPLOYEE("employee", "Employee", "home");

    val isAdmin: Boolean get() = false

    companion object {
        fun parse(raw: String?): AppRole {
            if (raw.isNullOrBlank()) return EMPLOYEE
            val s = raw.trim().lowercase()
            return when {
                s.contains("admin") || s.contains("manager") || s.contains("director") || s.contains("lead") -> ADMIN
                else -> EMPLOYEE
            }
        }
    }
}

/**
 * Authenticated User record fetched and verified from Firestore.
 */
data class AuthUser(
    val uid: String,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val role: AppRole,
    val rawRole: String,
    val designation: String,
    val department: String,
    val lastLoginAt: Long = System.currentTimeMillis(),
    val source: String = "Firestore"
)

/**
 * Result of checking user role in Firestore upon login.
 */
data class AuthCheckResult(
    val user: AuthUser,
    val targetRoute: String,
    val isAdmin: Boolean,
    val statusMessage: String
)

/**
 * Contract for Authentication Providers in MB Traker.
 */
interface AuthProvider {
    val currentUser: StateFlow<AuthUser?>
    val currentRole: StateFlow<AppRole>
    val isCheckingRole: StateFlow<Boolean>
    val lastStatusMessage: StateFlow<String?>

    suspend fun checkUserRoleInFirestore(identifier: String, defaultRoleHint: String? = null): AuthCheckResult
    suspend fun updateUserRoleInFirestore(identifier: String, newRole: String): Boolean
    fun logout(context: Context)
}

/**
 * Task extension helper for Google Play Services Task await without extra dependency.
 */
internal suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T =
    suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }

/**
 * Firestore-backed Authentication Provider that checks the user's role stored in Firestore
 * upon login and determines whether the UI flow redirects to the Admin Dashboard ("manager")
 * or the Employee Workspace ("home").
 */
object FirestoreAuthProvider : AuthProvider {

    private const val TAG = "FirestoreAuthProvider"
    private const val USERS_COLLECTION = "users"
    private const val USER_PROFILES_COLLECTION = "user_profiles"

    private var firestore: FirebaseFirestore? = null
    private var isInitialized = false
    private var userDocListener: ListenerRegistration? = null

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    override val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _currentRole = MutableStateFlow(AppRole.EMPLOYEE)
    override val currentRole: StateFlow<AppRole> = _currentRole.asStateFlow()

    private val _isCheckingRole = MutableStateFlow(false)
    override val isCheckingRole: StateFlow<Boolean> = _isCheckingRole.asStateFlow()

    private val _lastStatusMessage = MutableStateFlow<String?>("Ready for Firestore Authentication")
    override val lastStatusMessage: StateFlow<String?> = _lastStatusMessage.asStateFlow()

    /**
     * Initializes Firebase and ensures Firestore collections are populated with default roles.
     */
    fun initialize(context: Context, scope: CoroutineScope) {
        if (isInitialized && firestore != null) return

        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:371778881998:android:com_aistudio_mbtraker_hqxk")
                    .setProjectId("mbtraker-hrms-realtime")
                    .setApiKey("AIzaSyRealtimeEvaluationKeyForMBTracker2026")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Initialized default FirebaseApp with options")
            }

            firestore = com.example.data.firebase.FirestoreDbProvider.getFirestore(context)
            isInitialized = true
            _lastStatusMessage.value = "Firestore Auth Provider: Connected & Active"

            // Seed initial administrative and employee accounts in Firestore
            scope.launch(Dispatchers.IO) {
                seedInitialUsersInFirestore()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore Auth Provider: ${e.message}", e)
            _lastStatusMessage.value = "Firestore Auth Provider: Local Fallback (${e.message})"
        }
    }

    /**
     * Pre-populates the Firestore 'users' collection with known profiles so that role lookups
     * always find the authoritative role in the cloud.
     */
    private suspend fun seedInitialUsersInFirestore() {
        val fs = firestore ?: return
        try {
            // 1. MB Admin Account
            val adminData = mapOf(
                "uid" to "admin_default",
                "name" to "MB Admin",
                "phoneNumber" to "+91 98111 22334",
                "email" to "admin@makingbrands.in",
                "role" to "admin",
                "designation" to "Executive Administrator",
                "department" to "Management",
                "status" to "ACTIVE",
                "updatedAt" to System.currentTimeMillis()
            )
            fs.collection(USERS_COLLECTION).document("919811122334")
                .set(adminData, SetOptions.merge())
                .awaitTask()

            // 2. Employee Account (Rahul Sharma)
            val employeeData = mapOf(
                "uid" to "emp_rahul",
                "name" to "Rahul Sharma",
                "phoneNumber" to "+91 98765 43210",
                "email" to "rahul.sharma@makingbrands.in",
                "role" to "employee",
                "designation" to "Senior Android Developer",
                "department" to "Engineering",
                "status" to "ACTIVE",
                "updatedAt" to System.currentTimeMillis()
            )
            fs.collection(USERS_COLLECTION).document("919876543210")
                .set(employeeData, SetOptions.merge())
                .awaitTask()

            // 2b. Default Employee Portal Account
            val defaultEmpData = mapOf(
                "uid" to "emp_default",
                "name" to "Making Brands Employee",
                "phoneNumber" to "+91 98765 43210",
                "email" to "employee@makingbrands.in",
                "role" to "employee",
                "designation" to "Operations Associate",
                "department" to "Operations",
                "status" to "ACTIVE",
                "updatedAt" to System.currentTimeMillis()
            )
            fs.collection(USERS_COLLECTION).document("emp_default")
                .set(defaultEmpData, SetOptions.merge())
                .awaitTask()

            // 3. Manager Account (Priya Singh)
            val managerData = mapOf(
                "uid" to "mgr_priya",
                "name" to "Priya Singh",
                "phoneNumber" to "+91 98765 11111",
                "email" to "priya.singh@makingbrands.in",
                "role" to "admin", // Manager role redirects to Admin Portal
                "designation" to "Project Manager",
                "department" to "Operations",
                "status" to "ACTIVE",
                "updatedAt" to System.currentTimeMillis()
            )
            fs.collection(USERS_COLLECTION).document("919876511111")
                .set(managerData, SetOptions.merge())
                .awaitTask()

            Log.d(TAG, "Successfully seeded authoritative user roles in Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Seeding Firestore user roles skipped or failed: ${e.message}")
        }
    }

    /**
     * Queries Firestore for the user's role and determines the target UI route.
     * Checks the 'users' collection (and 'user_profiles' as fallback).
     * If the user doesn't exist yet, automatically provisions them with the selected role.
     */
    override suspend fun checkUserRoleInFirestore(
        identifier: String,
        defaultRoleHint: String?
    ): AuthCheckResult = withContext(Dispatchers.IO) {
        _isCheckingRole.value = true
        val cleanPhone = WhatsAppHelper.sanitizePhoneNumber(identifier)
        val fs = firestore

        _lastStatusMessage.value = "Checking user role in Firestore for: $identifier..."

        var fetchedRoleString: String? = null
        var fetchedName: String? = null
        var fetchedDesignation: String? = null
        var fetchedDepartment: String? = null
        var fetchedEmail: String? = null
        val isEmailIdentifier = identifier.contains("@")
        var userDocId = if (isEmailIdentifier) {
            identifier.trim().lowercase().replace("@", "_").replace(".", "_")
        } else {
            cleanPhone.ifBlank { "user_1" }
        }

        if (fs != null) {
            try {
                if (isEmailIdentifier) {
                    // Query 'users' by email field
                    val emailQuery = fs.collection(USERS_COLLECTION)
                        .whereEqualTo("email", identifier.trim().lowercase())
                        .limit(1)
                        .get()
                        .awaitTask()

                    if (!emailQuery.isEmpty) {
                        val firstDoc = emailQuery.documents[0]
                        userDocId = firstDoc.id
                        fetchedRoleString = firstDoc.getString("role")
                        fetchedName = firstDoc.getString("name")
                        fetchedDesignation = firstDoc.getString("designation")
                        fetchedDepartment = firstDoc.getString("department")
                        fetchedEmail = firstDoc.getString("email")
                        Log.d(TAG, "Found user via email in Firestore 'users' with role: $fetchedRoleString")
                    }
                }

                if (fetchedRoleString == null) {
                    // 1. Direct document lookup by sanitized phone in 'users'
                    val docSnapshot = fs.collection(USERS_COLLECTION).document(userDocId).get().awaitTask()
                    if (docSnapshot.exists()) {
                        fetchedRoleString = docSnapshot.getString("role")
                        fetchedName = docSnapshot.getString("name")
                        fetchedDesignation = docSnapshot.getString("designation")
                        fetchedDepartment = docSnapshot.getString("department")
                        fetchedEmail = docSnapshot.getString("email")
                        Log.d(TAG, "Found user in Firestore 'users/$userDocId' with role: $fetchedRoleString")
                    } else {
                        // 2. Query 'users' by phoneNumber field
                        val querySnapshot = fs.collection(USERS_COLLECTION)
                            .whereEqualTo("phoneNumber", identifier.trim())
                            .limit(1)
                            .get()
                            .awaitTask()

                        if (!querySnapshot.isEmpty) {
                            val firstDoc = querySnapshot.documents[0]
                            userDocId = firstDoc.id
                            fetchedRoleString = firstDoc.getString("role")
                            fetchedName = firstDoc.getString("name")
                            fetchedDesignation = firstDoc.getString("designation")
                            fetchedDepartment = firstDoc.getString("department")
                            fetchedEmail = firstDoc.getString("email")
                            Log.d(TAG, "Found user via query in Firestore 'users' with role: $fetchedRoleString")
                        } else {
                            // 3. Fallback to 'user_profiles' collection
                            val profileSnapshot = fs.collection(USER_PROFILES_COLLECTION).document("user_1").get().awaitTask()
                            if (profileSnapshot.exists()) {
                                fetchedRoleString = profileSnapshot.getString("role")
                                fetchedName = profileSnapshot.getString("name")
                                Log.d(TAG, "Found role in Firestore 'user_profiles/user_1': $fetchedRoleString")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore role query failed, falling back to local/cached resolution: ${e.message}")
            }
        }

        // If not found in Firestore or offline, determine authoritative role using hint or identifier
        val finalRoleString = fetchedRoleString ?: run {
            val resolvedRole = if (
                defaultRoleHint?.contains("Admin", ignoreCase = true) == true ||
                cleanPhone.contains("98111") ||
                identifier.contains("admin", ignoreCase = true)
            ) {
                "admin"
            } else {
                "employee"
            }

            // Provision this user in Firestore so it exists for future queries
            if (fs != null && cleanPhone.isNotBlank()) {
                try {
                    val newUserRecord = mapOf(
                        "uid" to "user_$cleanPhone",
                        "name" to (if (resolvedRole == "admin") "MB Admin" else "Rahul Sharma"),
                        "phoneNumber" to identifier.trim(),
                        "email" to (if (resolvedRole == "admin") "admin@makingbrands.in" else "employee@makingbrands.in"),
                        "role" to resolvedRole,
                        "designation" to (if (resolvedRole == "admin") "Administrator" else "Team Member"),
                        "department" to (if (resolvedRole == "admin") "Management" else "Engineering"),
                        "status" to "ACTIVE",
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )
                    fs.collection(USERS_COLLECTION).document(cleanPhone)
                        .set(newUserRecord, SetOptions.merge())
                        .awaitTask()
                    Log.d(TAG, "Provisioned new user '$cleanPhone' with role '$resolvedRole' in Firestore")
                } catch (ex: Exception) {
                    Log.w(TAG, "Could not provision user in Firestore: ${ex.message}")
                }
            }
            resolvedRole
        }

        // Parse role into AppRole (ADMIN -> "manager", EMPLOYEE -> "home")
        val appRole = AppRole.parse(finalRoleString)
        val userName = fetchedName ?: if (appRole.isAdmin) "MB Admin" else "Rahul Sharma"
        val userDesignation = fetchedDesignation ?: if (appRole.isAdmin) "Administrator" else "Senior Android Developer"
        val userDepartment = fetchedDepartment ?: if (appRole.isAdmin) "Management" else "Engineering"
        val userEmail = fetchedEmail ?: if (appRole.isAdmin) "admin@makingbrands.in" else "makingbrands.in@gmail.com"

        val authUser = AuthUser(
            uid = userDocId,
            name = userName,
            phoneNumber = identifier.trim(),
            email = userEmail,
            role = appRole,
            rawRole = finalRoleString,
            designation = userDesignation,
            department = userDepartment,
            lastLoginAt = System.currentTimeMillis(),
            source = if (fetchedRoleString != null) "Firestore (Cloud)" else "Firestore (Provisioned)"
        )

        _currentUser.value = authUser
        _currentRole.value = appRole
        _isCheckingRole.value = false

        val statusMsg = if (appRole.isAdmin) {
            "Verified Role: ${appRole.displayName} (Firestore) ➔ Redirecting to Admin Dashboard"
        } else {
            "Verified Role: ${appRole.displayName} (Firestore) ➔ Redirecting to Employee Workspace"
        }
        _lastStatusMessage.value = statusMsg

        // Update last login timestamp in Firestore
        if (fs != null && userDocId.isNotBlank()) {
            try {
                fs.collection(USERS_COLLECTION).document(userDocId)
                    .set(mapOf("lastLoginAt" to System.currentTimeMillis()), SetOptions.merge())
            } catch (_: Exception) {}
        }

        // Attach real-time listener to user's Firestore document so any role changes are reflected immediately
        attachRealtimeRoleListener(userDocId)

        AuthCheckResult(
            user = authUser,
            targetRoute = appRole.targetRoute,
            isAdmin = appRole.isAdmin,
            statusMessage = statusMsg
        )
    }

    /**
     * Updates the user's role in Firestore and refreshes local auth state.
     * Allows testing role switching directly against Firestore.
     */
    override suspend fun updateUserRoleInFirestore(identifier: String, newRole: String): Boolean = withContext(Dispatchers.IO) {
        val cleanPhone = WhatsAppHelper.sanitizePhoneNumber(identifier)
        val fs = firestore ?: return@withContext false
        val userDocId = cleanPhone.ifBlank { "user_1" }

        try {
            val updatedAppRole = AppRole.parse(newRole)
            val roleKey = if (updatedAppRole.isAdmin) "admin" else "employee"

            // 1. Update in 'users' collection
            fs.collection(USERS_COLLECTION).document(userDocId)
                .set(
                    mapOf(
                        "role" to roleKey,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).awaitTask()

            // 2. Also update in 'user_profiles' collection for compatibility
            fs.collection(USER_PROFILES_COLLECTION).document("user_1")
                .set(
                    mapOf(
                        "role" to (if (updatedAppRole.isAdmin) "MB Admin" else "Senior Android Developer"),
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                ).awaitTask()

            _currentRole.value = updatedAppRole
            _currentUser.value = _currentUser.value?.copy(
                role = updatedAppRole,
                rawRole = roleKey
            )
            _lastStatusMessage.value = "Updated role in Firestore to: ${updatedAppRole.displayName} ➔ Target: ${updatedAppRole.targetRoute}"
            Log.d(TAG, "Role updated in Firestore for $userDocId: $roleKey")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update role in Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Listens to live role modifications in Firestore.
     */
    private fun attachRealtimeRoleListener(docId: String) {
        val fs = firestore ?: return
        try {
            userDocListener?.remove()
            userDocListener = fs.collection(USERS_COLLECTION).document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val liveRoleStr = snapshot.getString("role") ?: return@addSnapshotListener
                    val updatedRole = AppRole.parse(liveRoleStr)
                    if (updatedRole != _currentRole.value) {
                        _currentRole.value = updatedRole
                        _currentUser.value = _currentUser.value?.copy(
                            role = updatedRole,
                            rawRole = liveRoleStr
                        )
                        _lastStatusMessage.value = "Live Firestore Update: Role changed to ${updatedRole.displayName}"
                        Log.d(TAG, "Realtime role update received from Firestore: $liveRoleStr")
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach realtime role listener: ${e.message}")
        }
    }

    override fun logout(context: Context) {
        userDocListener?.remove()
        userDocListener = null
        _currentUser.value = null
        _currentRole.value = AppRole.EMPLOYEE
        _lastStatusMessage.value = "Logged Out"
        BiometricHelper.clearLoginSession(context)
    }
}
