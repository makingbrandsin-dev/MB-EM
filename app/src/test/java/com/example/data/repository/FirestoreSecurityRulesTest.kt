package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FirestoreSecurityRulesTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUser_cannotReadTasks(): Unit = runBlocking {
        auth.signOut()
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("tasks").get().await()
            }
            fail("Expected PERMISSION_DENIED exception for unauthenticated read")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun authenticatedEmployee_canCreateAndReadTask(): Unit = runBlocking {
        val uid = signInTestUser(EMPLOYEE_EMAIL)
        val taskData = hashMapOf(
            "title" to "Review Security Policy",
            "status" to "In Progress",
            "assignee" to EMPLOYEE_EMAIL,
            "createdAt" to System.currentTimeMillis()
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("tasks").document("task_emp_1").set(taskData).await()
            val doc = firestore.collection("tasks").document("task_emp_1").get().await()
            assertTrue(doc.exists())
            assertEquals("Review Security Policy", doc.getString("title"))
        }
    }

    @Test
    fun unauthorizedEmployee_cannotDeleteInvoice(): Unit = runBlocking {
        signInTestUser(ADMIN_EMAIL)
        firestore.collection("invoices").document("inv_test_1").set(
            mapOf("invoiceNumber" to "INV-999", "clientName" to "Client X", "amount" to 1000)
        ).await()

        signInTestUser(EMPLOYEE_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("invoices").document("inv_test_1").delete().await()
            }
            fail("Expected PERMISSION_DENIED when employee attempts invoice deletion")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun unauthorizedEmployee_cannotGrantSelfAdminRole(): Unit = runBlocking {
        val uid = signInTestUser(EMPLOYEE_EMAIL)
        val profileData = hashMapOf(
            "email" to EMPLOYEE_EMAIL,
            "name" to "John Employee",
            "role" to "admin",
            "status" to "ACTIVE"
        )

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(uid).set(profileData).await()
            }
            fail("Expected PERMISSION_DENIED when employee grants self admin role")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    private companion object {
        const val EMPLOYEE_EMAIL = "employee_test@makingbrands.com"
        const val ADMIN_EMAIL = "admin@makingbrands.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
