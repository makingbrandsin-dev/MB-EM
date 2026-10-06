package com.example.data.firebase

import com.google.firebase.firestore.DocumentSnapshot

/**
 * Robust extraction helpers for DocumentSnapshot fields that prevent
 * java.lang.RuntimeException: Field 'x' is not a java.lang.Number / type mismatch
 * when Firestore documents contain stringified numbers or heterogenous types.
 */
fun DocumentSnapshot.safeLong(field: String, default: Long = 0L): Long {
    return try {
        when (val v = get(field)) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: default
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.safeLongOrNull(field: String): Long? {
    return try {
        when (val v = get(field)) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull()
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun DocumentSnapshot.safeInt(field: String, default: Int = 0): Int {
    return try {
        when (val v = get(field)) {
            is Number -> v.toInt()
            is String -> v.toIntOrNull() ?: default
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.safeBoolean(field: String, default: Boolean = false): Boolean {
    return try {
        when (val v = get(field)) {
            is Boolean -> v
            is String -> v.toBoolean()
            is Number -> v.toInt() != 0
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.safeString(field: String, default: String = ""): String {
    return try {
        when (val v = get(field)) {
            null -> default
            is String -> v
            else -> v.toString()
        }
    } catch (_: Exception) {
        default
    }
}

fun DocumentSnapshot.safeDouble(field: String, default: Double = 0.0): Double {
    return try {
        when (val v = get(field)) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull() ?: default
            else -> default
        }
    } catch (_: Exception) {
        default
    }
}
