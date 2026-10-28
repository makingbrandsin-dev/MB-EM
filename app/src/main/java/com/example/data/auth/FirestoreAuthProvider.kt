package com.example.data.auth

import android.content.Context
import android.util.Log
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Canonical Application Roles for MB EM and MB Admin.
 * Strictly verified from server records (Firestore /admins or /users).
 */
enum class AppRole(val roleKey: String, val displayName: String, val targetRoute: String) {
    ADMIN("admin", "Admin", "home"),
    EMPLOYEE("employee", "Employee", "home");

    val isAdmin: Boolean get() = this == ADMIN

    companion object {
        fun parse(raw: String?): AppRole {
            if (raw.isNullOrBlank()) return EMPLOYEE
            val s = raw.trim().lowercase()
            return if (s == "admin") ADMIN else EMPLOYEE
        }
    }
}

/**
 * Authenticated User record fetched and verified strictly from Firestore.
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
    val status: String = "ACTIVE",
    val orgId: String = "org_default",
    val lastLoginAt: Long = System.currentTimeMillis(),
    val source: String = "Firestore"
) {
    val isAdmin: Boolean get() = role == AppRole.ADMIN
    val isPending: Boolean get() = status.equals("PENDING", ignoreCase = true)
    val isDisabled: Boolean get() = status.equals("DISABLED", ignoreCase = true) || status.equals("INACTIVE", ignoreCase = true)
}

/**
 * Result of checking user role in Firestore upon login.
 */
data class AuthCheckResult(
    val user: AuthUser,
    val targetRoute: String,
    val isAdmin: Boolean,
    val isPending: Boolean,
    val statusMessage: String
)

/**
 * Contract for Authentication Providers in MB EM.
 */
interface AuthProvider {
    val currentUser: StateFlow<AuthUser?>
    val currentRole: StateFlow<AppRole>
    val isCheckingRole: StateFlow<Boolean>
    val lastStatusMessage: StateFlow<String?>

    suspend fun checkUserRoleInFirestore(
        identifier: String,
        defaultRoleHint: String? = null,
        authenticatedUid: String? = null
    ): AuthCheckResult
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
 * Authoritative Firestore-backed Role and Identity Verification Provider.
 * Enforces zero-trust:
 * - Admin rights are only granted if document exists in `/admins/{uid}` or role=="admin" in `/users/{uid}`.
 * - No local APK account seeding.
 * - No substring matching on emails or phone numbers.
 * - Unknown or new users default strictly to `EMPLOYEE` with status `PENDING`.
 * - Fails closed if account is deactivated.
 */
object FirestoreAuthProvider : AuthProvider {

    private const val TAG = "FirestoreAuthProvider"
    private const val USERS_COLLECTION = "users"
    private const val ADMINS_COLLECTION = "admins"

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
            _lastStatusMessage.value = "Firestore Auth Provider: Active"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firestore Auth Provider: ${e.message}", e)
            _lastStatusMessage.value = "Firestore Auth Provider Error (${e.message})"
        }
    }

    /**
     * Queries Firestore for authoritative role and organization status.
     * Checks server-controlled /admins/{uid} collection first, then /users/{uid}.
     * Fails closed if user is disabled.
     */
    override suspend fun checkUserRoleInFirestore(
        identifier: String,
        defaultRoleHint: String?,
        authenticatedUid: String?
    ): AuthCheckResult = withContext(Dispatchers.IO) {
        _isCheckingRole.value = true
        val cleanPhone = WhatsAppHelper.sanitizePhoneNumber(identifier)
        val fs = firestore ?: com.example.data.firebase.FirestoreDbProvider.getFirestore()

        _lastStatusMessage.value = "Verifying role in Firestore..."

        val effectiveUid = authenticatedUid?.ifBlank { null }
            ?: (if (identifier.contains("@")) identifier.trim().lowercase().replace("@", "_").replace(".", "_")
                else cleanPhone.ifBlank { "anonymous" })

        var isServerAdmin = false
        var fetchedRole: String? = null
        var fetchedName: String? = null
        var fetchedDesignation: String? = null
        var fetchedDepartment: String? = null
        var fetchedEmail: String? = null
        var fetchedPhone: String? = null
        var fetchedStatus = "ACTIVE"
        var fetchedOrgId = "org_default"
        var userDocExists = false

        if (fs != null) {
            try {
                // 1. Authoritative check: verify server-controlled /admins collection
                val adminDoc = fs.collection(ADMINS_COLLECTION).document(effectiveUid).get().awaitTask()
                if (adminDoc.exists()) {
                    isServerAdmin = true
                    fetchedRole = "admin"
                    fetchedName = adminDoc.getString("name") ?: "Admin"
                    fetchedDesignation = adminDoc.getString("designation") ?: "Administrator"
                    fetchedDepartment = adminDoc.getString("department") ?: "Management"
                    fetchedEmail = adminDoc.getString("email")
                    fetchedPhone = adminDoc.getString("phone")
                    fetchedStatus = adminDoc.getString("status") ?: "ACTIVE"
                    fetchedOrgId = adminDoc.getString("orgId") ?: "org_default"
                    userDocExists = true
                } else if (cleanPhone.isNotBlank()) {
                    val adminPhoneDoc = fs.collection(ADMINS_COLLECTION).document(cleanPhone).get().awaitTask()
                    if (adminPhoneDoc.exists()) {
                        isServerAdmin = true
                        fetchedRole = "admin"
                        userDocExists = true
                    }
                }

                // 2. Query /users collection by document ID or fields
                if (!isServerAdmin) {
                    val userDoc = fs.collection(USERS_COLLECTION).document(effectiveUid).get().awaitTask()
                    if (userDoc.exists()) {
                        userDocExists = true
                        val docRole = userDoc.getString("role")
                        if (docRole.equals("admin", ignoreCase = true)) {
                            isServerAdmin = true
                            fetchedRole = "admin"
                        } else {
                            fetchedRole = "employee"
                        }
                        fetchedName = userDoc.getString("name") ?: userDoc.getString("displayName")
                        fetchedDesignation = userDoc.getString("designation")
                        fetchedDepartment = userDoc.getString("department")
                        fetchedEmail = userDoc.getString("email")
                        fetchedPhone = userDoc.getString("phoneNumber") ?: userDoc.getString("phone")
                        fetchedStatus = userDoc.getString("status") ?: "ACTIVE"
                        fetchedOrgId = userDoc.getString("orgId") ?: "org_default"
                    } else if (identifier.contains("@")) {
                        val emailQuery = fs.collection(USERS_COLLECTION)
                            .whereEqualTo("email", identifier.trim().lowercase())
                            .limit(1)
                            .get()
                            .awaitTask()
                        if (!emailQuery.isEmpty) {
                            userDocExists = true
                            val first = emailQuery.documents[0]
                            val docRole = first.getString("role")
                            if (docRole.equals("admin", ignoreCase = true)) {
                                isServerAdmin = true
                                fetchedRole = "admin"
                            } else {
                                fetchedRole = "employee"
                            }
                            fetchedName = first.getString("name") ?: first.getString("displayName")
                            fetchedDesignation = first.getString("designation")
                            fetchedDepartment = first.getString("department")
                            fetchedEmail = first.getString("email")
                            fetchedPhone = first.getString("phoneNumber") ?: first.getString("phone")
                            fetchedStatus = first.getString("status") ?: "ACTIVE"
                            fetchedOrgId = first.getString("orgId") ?: "org_default"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Role check error in Firestore: ${e.message}")
            }
        }

        // 3. New user registration: default strictly to employee with PENDING approval
        if (!userDocExists) {
            fetchedRole = "employee"
            fetchedStatus = "PENDING"
            fetchedOrgId = "org_default"
            fetchedName = if (identifier.contains("@")) {
                identifier.substringBefore("@").replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            } else {
                "Employee"
            }
            fetchedDesignation = "Operations Specialist"
            fetchedDepartment = "Operations"
            fetchedEmail = if (identifier.contains("@")) identifier.trim().lowercase() else ""
            fetchedPhone = if (!identifier.contains("@")) identifier.trim() else ""

            if (fs != null && effectiveUid.isNotBlank()) {
                try {
                    val initialDoc = mapOf(
                        "uid" to effectiveUid,
                        "name" to fetchedName,
                        "email" to fetchedEmail,
                        "phoneNumber" to fetchedPhone,
                        "role" to "employee",
                        "status" to "PENDING",
                        "orgId" to "org_default",
                        "designation" to fetchedDesignation,
                        "department" to fetchedDepartment,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis()
                    )
                    fs.collection(USERS_COLLECTION).document(effectiveUid)
                        .set(initialDoc, SetOptions.merge())
                        .awaitTask()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not write initial pending employee record: ${e.message}")
                }
            }
        }

        val appRole = if (isServerAdmin || fetchedRole == "admin") AppRole.ADMIN else AppRole.EMPLOYEE
        val finalStatus = fetchedStatus
        val finalOrgId = fetchedOrgId
        val isPending = finalStatus.equals("PENDING", ignoreCase = true)
        val isDisabled = finalStatus.equals("DISABLED", ignoreCase = true) || finalStatus.equals("INACTIVE", ignoreCase = true)

        if (isDisabled) {
            _isCheckingRole.value = false
            _lastStatusMessage.value = "Account deactivated"
            throw IllegalStateException("Your account has been deactivated. Please contact your organization administrator.")
        }

        val authUser = AuthUser(
            uid = effectiveUid,
            name = fetchedName ?: if (appRole.isAdmin) "MB Admin" else "Employee",
            phoneNumber = fetchedPhone ?: identifier.trim(),
            email = fetchedEmail ?: if (identifier.contains("@")) identifier.trim() else "",
            role = appRole,
            rawRole = if (appRole.isAdmin) "admin" else "employee",
            designation = fetchedDesignation ?: (if (appRole.isAdmin) "Administrator" else "Operations Specialist"),
            department = fetchedDepartment ?: (if (appRole.isAdmin) "Management" else "Operations"),
            status = finalStatus,
            orgId = finalOrgId,
            lastLoginAt = System.currentTimeMillis(),
            source = "Firestore"
        )

        _currentUser.value = authUser
        _currentRole.value = appRole
        _isCheckingRole.value = false

        val statusMsg = when {
            isPending -> "Account pending administrator approval"
            appRole.isAdmin -> "Verified Administrator (Server Record)"
            else -> "Verified Employee"
        }
        _lastStatusMessage.value = statusMsg

        if (fs != null && effectiveUid.isNotBlank()) {
            try {
                fs.collection(USERS_COLLECTION).document(effectiveUid)
                    .set(mapOf("lastLoginAt" to System.currentTimeMillis()), SetOptions.merge())
            } catch (_: Exception) {}
        }

        attachRealtimeRoleListener(effectiveUid)

        AuthCheckResult(
            user = authUser,
            targetRoute = appRole.targetRoute,
            isAdmin = appRole.isAdmin,
            isPending = isPending,
            statusMessage = statusMsg
        )
    }

    private fun attachRealtimeRoleListener(docId: String) {
        val fs = firestore ?: return
        try {
            userDocListener?.remove()
            userDocListener = fs.collection(USERS_COLLECTION).document(docId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    val liveRoleStr = snapshot.getString("role") ?: return@addSnapshotListener
                    val liveStatus = snapshot.getString("status") ?: "ACTIVE"
                    val liveOrgId = snapshot.getString("orgId") ?: "org_default"
                    val updatedRole = AppRole.parse(liveRoleStr)
                    val current = _currentUser.value ?: return@addSnapshotListener
                    _currentRole.value = updatedRole
                    _currentUser.value = current.copy(
                        role = updatedRole,
                        rawRole = liveRoleStr,
                        status = liveStatus,
                        orgId = liveOrgId
                    )
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
