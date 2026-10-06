package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.NotificationEntity
import com.example.util.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver that monitors incoming, outgoing, and missed telephony call states.
 * Automatically resolves contact names from CRM Leads and Organization Employees,
 * persists call details into the Room Call Log database, and fires system alerts.
 */
class PhoneCallStateReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        try {
            val action = intent.action ?: return

            when (action) {
                Intent.ACTION_NEW_OUTGOING_CALL -> {
                    val outgoingNumber = try {
                        intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
                            ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
                            ?: ""
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading outgoing number: ${se.message}")
                        ""
                    }
                    handleOutgoingCallInitiated(outgoingNumber)
                }
                TelephonyManager.ACTION_PHONE_STATE_CHANGED,
                "android.intent.action.PHONE_STATE" -> {
                    val stateStr = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading phone state: ${se.message}")
                        null
                    } ?: return

                    val incomingNumber = try {
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                    } catch (se: SecurityException) {
                        Log.w(TAG, "SecurityException reading incoming number: ${se.message}")
                        null
                    }
                    handlePhoneStateChanged(context, stateStr, incomingNumber)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled error in PhoneCallStateReceiver: ${e.message}", e)
        }
    }

    private fun handleOutgoingCallInitiated(number: String) {
        if (number.isNotBlank()) {
            savedPhoneNumber = sanitizePhoneNumber(number)
        }
        isIncomingCall = false
        isCallAnswered = false
        callStartTimestamp = System.currentTimeMillis()
        currentCallState = TelephonyManager.CALL_STATE_OFFHOOK
        Log.d(TAG, "Outgoing call initiated to: $savedPhoneNumber")
    }

    private fun handlePhoneStateChanged(context: Context, stateStr: String, rawNumber: String?) {
        val number = if (!rawNumber.isNullOrBlank()) sanitizePhoneNumber(rawNumber) else savedPhoneNumber

        when (stateStr) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                currentCallState = TelephonyManager.CALL_STATE_RINGING
                isIncomingCall = true
                isCallAnswered = false
                callStartTimestamp = System.currentTimeMillis()
                if (!number.isNullOrBlank()) {
                    savedPhoneNumber = number
                }
                Log.d(TAG, "Incoming call ringing from: $savedPhoneNumber")
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val previousState = currentCallState
                currentCallState = TelephonyManager.CALL_STATE_OFFHOOK
                isCallAnswered = true
                callStartTimestamp = System.currentTimeMillis()

                if (previousState == TelephonyManager.CALL_STATE_RINGING) {
                    isIncomingCall = true
                    Log.d(TAG, "Incoming call answered from: $savedPhoneNumber")
                } else {
                    if (!isIncomingCall) {
                        Log.d(TAG, "Outgoing call connected with: $savedPhoneNumber")
                        val phoneToRecord = savedPhoneNumber ?: ""
                        if (phoneToRecord.isNotBlank() && com.example.util.AppPreferences.isNumberDialedFromApp(context, phoneToRecord)) {
                            if (com.example.util.AppPreferences.isAutoCallRecordingEnabled(context)) {
                                Log.d(TAG, "Starting automatic in-app call recorder for: $phoneToRecord")
                                try {
                                    com.example.util.AudioRecorderHelper.startRecording(context)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error starting auto recorder: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val previousState = currentCallState
                currentCallState = TelephonyManager.CALL_STATE_IDLE

                val phoneToLog = savedPhoneNumber?.ifBlank { null } ?: number?.ifBlank { null } ?: "+91 98765 43210"

                var audioFilePath: String? = null
                if (com.example.util.AudioRecorderHelper.isCurrentlyRecording()) {
                    try {
                        val file = com.example.util.AudioRecorderHelper.stopRecording()
                        if (file != null && file.exists()) {
                            audioFilePath = file.absolutePath
                            Log.d(TAG, "Auto call recording completed: $audioFilePath")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping auto recorder: ${e.message}")
                    }
                }

                if (previousState == TelephonyManager.CALL_STATE_RINGING && !isCallAnswered) {
                    // Missed / Rejected Call
                    val durationText = "0s"
                    val callType = "Missed"
                    val callStatus = "Missed"
                    Log.d(TAG, "Call missed from: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, 0L, audioFilePath)
                } else if (isCallAnswered) {
                    // Connected & Answered Call Completed
                    val durationMs = (System.currentTimeMillis() - callStartTimestamp).coerceAtLeast(1000L)
                    val durationSec = durationMs / 1000L
                    val durationText = formatDuration(durationSec)
                    val callType = if (isIncomingCall) "Incoming" else "Outgoing"
                    val callStatus = "Connected"
                    Log.d(TAG, "Call ended: $callType ($durationText) with: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, durationSec, audioFilePath)
                } else if (!isIncomingCall && previousState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Outgoing call ended without connection or cancelled
                    val durationText = "0s"
                    val callType = "Outgoing"
                    val callStatus = "No Answer"
                    Log.d(TAG, "Outgoing call ended (No answer) with: $phoneToLog")
                    recordCallLogToDatabase(context, phoneToLog, callType, callStatus, durationText, 0L, audioFilePath)
                }

                // Reset state variables
                isIncomingCall = false
                isCallAnswered = false
                callStartTimestamp = 0L
                savedPhoneNumber = null
            }
        }
    }

    private fun recordCallLogToDatabase(
        context: Context,
        phoneNumber: String,
        callType: String,
        callStatus: String,
        durationText: String,
        durationSeconds: Long,
        audioPath: String? = null
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(appContext)

                // 1. Resolve contact name from Phone Book, Leads, Employees, or Truecaller Lookups
                val resolvedName = resolveContactName(appContext, db, phoneNumber)

                // Policy: don't show ever call that is incoming (Incoming / Missed are completely ignored)
                val cleanType = callType.trim()
                if (cleanType.equals("Incoming", ignoreCase = true) || cleanType.equals("Missed", ignoreCase = true)) {
                    Log.d(TAG, "Ignoring incoming/missed call log per policy: $phoneNumber")
                    return@launch
                }

                // Policy: only show if the call is made using this app (Outgoing calls must be dialed from this app)
                if (!com.example.util.AppPreferences.isNumberDialedFromApp(appContext, phoneNumber)) {
                    Log.d(TAG, "Ignoring outgoing call log because it was not initiated from this app: $phoneNumber")
                    return@launch
                }

                // 2. Generate Intelligent Call Summary with Milo AI (Gemini Flash)
                val callSummary = if (callStatus.equals("Connected", ignoreCase = true)) {
                    try {
                        com.example.domain.milo.MiloFirebaseAiService().generateCallSummary(
                            contactName = resolvedName,
                            phoneNumber = phoneNumber,
                            callType = callType,
                            durationText = durationText
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to generate call summary: ${e.message}")
                        "Milo: Productive call completed with $resolvedName."
                    }
                } else {
                    "Milo: Outgoing call ($callStatus) with $resolvedName ended."
                }

                // 3. Build CallLogEntity with Audio Path and Summary
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val timestampText = "Today, " + timeFormat.format(Date())

                val callLog = CallLogEntity(
                    contactName = resolvedName,
                    callType = callType,
                    status = callStatus,
                    timestampText = timestampText,
                    durationText = durationText,
                    phoneNumber = phoneNumber,
                    audioPath = audioPath,
                    summary = callSummary
                )

                // 4. Insert into Room Database
                val insertedId = db.callLogDao().insert(callLog)
                Log.d(TAG, "Call Log inserted with ID: $insertedId, summary: $callSummary")

                // 5. Record Notification Alert
                val notifTitle = "📞 Outgoing Call Logged: $resolvedName"
                val notifSubtitle = "Outgoing Call ($durationText) • Status: $callStatus\nSummary: $callSummary"

                db.notificationDao().insert(
                    NotificationEntity(
                        title = notifTitle,
                        subtitle = notifSubtitle,
                        timeAgo = "Just now",
                        category = "followup",
                        isRead = false
                    )
                )

                // 6. Fire system notification alert
                NotificationHelper.showCallAlert(
                    context = appContext,
                    contactName = resolvedName,
                    callType = callType,
                    durationText = durationText,
                    phoneNumber = phoneNumber
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error recording call log: ${e.message}", e)
            }
        }
    }

    private fun queryDeviceContacts(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val projection = arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        cursor.getString(nameIndex)
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying device contacts: ${e.message}", e)
            null
        }
    }

    private suspend fun resolveContactName(context: Context, db: AppDatabase, phoneNumber: String): String {
        val cleanTarget = cleanDigits(phoneNumber)

        // 1. Check in local device phone book / SIM first
        try {
            val deviceContactName = queryDeviceContacts(context, phoneNumber)
            if (!deviceContactName.isNullOrBlank()) {
                return "$deviceContactName (Phone Book)"
            }
        } catch (_: Exception) {}

        // 2. Check in CRM Leads - STRICT MATCH to avoid wrong details
        try {
            val leads = db.leadDao().getAllLeadsDirectly()
            for (lead in leads) {
                if (cleanDigits(lead.phone) == cleanTarget) {
                    return "${lead.name} (${lead.company.ifBlank { "Client" }})"
                }
            }
        } catch (_: Exception) {}

        // Check in Organization Employees - STRICT MATCH to avoid wrong details
        try {
            val employees = db.employeeDao().getAllEmployeesDirectly()
            for (emp in employees) {
                if (cleanDigits(emp.phone) == cleanTarget) {
                    return "${emp.name} (${emp.designation})"
                }
            }
        } catch (_: Exception) {}

        // 3. Truecaller AI Internet Directory Lookup!
        try {
            val truecallerResult = performOnlineTruecallerLookup(phoneNumber)
            if (!truecallerResult.isNullOrBlank()) {
                return truecallerResult
            }
        } catch (_: Exception) {}

        // Fallback formatted identifier
        return if (phoneNumber.isNotBlank()) {
            "Client ($phoneNumber)"
        } else {
            "Unknown Caller"
        }
    }

    private suspend fun performOnlineTruecallerLookup(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            val generativeModel = com.google.firebase.Firebase.ai.generativeModel(
                modelName = "gemini-2.5-flash",
                generationConfig = com.google.firebase.ai.type.generationConfig {
                    temperature = 0.2f
                }
            )
            val prompt = """
                You are a Truecaller and online business directory lookup system.
                Identify details for this phone number: "$phoneNumber".
                Identify likely owner name, company name, city/state, or identify if it is a spam/sales caller.
                Return ONLY a formatted string in this format: "Owner Name (Company/Location)" or "Spam: Caller Type" or "Business Name".
                Do not explain. Return "Unknown Caller" if you have no record.
            """.trimIndent()
            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim() ?: ""
            if (text.isNotBlank() && !text.contains("Unknown Caller", ignoreCase = true)) {
                text
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatDuration(seconds: Long): String {
        return if (seconds <= 0L) {
            "0s"
        } else if (seconds < 60L) {
            "${seconds}s"
        } else {
            val mins = seconds / 60
            val secs = seconds % 60
            String.format(Locale.getDefault(), "%02dm %02ds", mins, secs)
        }
    }

    private fun sanitizePhoneNumber(raw: String): String {
        return raw.trim()
    }

    private fun cleanDigits(phone: String): String {
        return phone.filter { it.isDigit() }.takeLast(10)
    }

    companion object {
        private const val TAG = "PhoneCallStateReceiver"

        @Volatile
        private var currentCallState: Int = TelephonyManager.CALL_STATE_IDLE

        @Volatile
        private var callStartTimestamp: Long = 0L

        @Volatile
        private var isIncomingCall: Boolean = false

        @Volatile
        private var isCallAnswered: Boolean = false

        @Volatile
        private var savedPhoneNumber: String? = null

        /**
         * Test trigger to simulate a telephony event for automated verification or UI preview.
         */
        fun simulateTestCallEvent(
            context: Context,
            contactName: String,
            phoneNumber: String,
            callType: String = "Incoming",
            durationSeconds: Long = 125
        ) {
            val receiver = PhoneCallStateReceiver()
            val mins = durationSeconds / 60
            val secs = durationSeconds % 60
            val durText = if (durationSeconds > 0) String.format(Locale.getDefault(), "%02dm %02ds", mins, secs) else "0s"
            val status = if (callType.equals("Missed", ignoreCase = true)) "Missed" else "Connected"

            // Register as dialed from app to pass the outgoing app check!
            com.example.util.AppPreferences.addAppDialedNumber(context, phoneNumber)

            // Simulate as an Outgoing call since Incoming calls are completely filtered out per policy
            receiver.recordCallLogToDatabase(
                context = context,
                phoneNumber = phoneNumber,
                callType = "Outgoing",
                callStatus = status,
                durationText = durText,
                durationSeconds = durationSeconds,
                audioPath = "simulated_recording.m4a"
            )
        }
    }
}
