package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Centralized provider for Firestore instances that automatically resolves the provisioned
 * database ID from [R.string.firestore_database_id].
 * Falls back gracefully to default instance if custom database resolution fails.
 */
object FirestoreDbProvider {
    private const val TAG = "FirestoreDbProvider"
    private var cachedFirestore: FirebaseFirestore? = null

    fun getFirestore(context: Context? = null): FirebaseFirestore {
        cachedFirestore?.let { return it }

        val appContext = context?.applicationContext
        if (appContext != null && FirebaseApp.getApps(appContext).isEmpty()) {
            try {
                FirebaseApp.initializeApp(appContext)
                Log.d(TAG, "Default FirebaseApp initialized in FirestoreDbProvider")
            } catch (e: Exception) {
                Log.w(TAG, "Default FirebaseApp initialization warning: ${e.message}")
            }
        }

        val databaseId = try {
            appContext?.getString(R.string.firestore_database_id)?.trim()
        } catch (_: Exception) {
            null
        }

        val instance = try {
            if (!databaseId.isNullOrBlank() && databaseId != "(default)") {
                Log.d(TAG, "Connecting to provisioned Firestore database: $databaseId")
                try {
                    val app = if (FirebaseApp.getApps(appContext ?: FirebaseApp.getInstance().applicationContext).isNotEmpty()) {
                        FirebaseApp.getInstance()
                    } else null

                    if (app != null) {
                        FirebaseFirestore.getInstance(app, databaseId)
                    } else {
                        FirebaseFirestore.getInstance(databaseId)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Falling back to database ID direct instance: ${e.message}")
                    FirebaseFirestore.getInstance(databaseId)
                }
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error acquiring Firestore with custom database ID ($databaseId), falling back: ${e.message}")
            FirebaseFirestore.getInstance()
        }

        cachedFirestore = instance
        return instance
    }
}
