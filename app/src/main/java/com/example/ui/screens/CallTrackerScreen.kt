package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntity
import com.example.receiver.PhoneCallStateReceiver
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

/**
 * Screen displaying the complete list of logged phone calls retrieved from Room database,
 * sorted in descending order (newest calls first), with search, category filtering,
 * call metrics statistics, and 1-tap Redial / WhatsApp dispatch actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallTrackerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val rawCallLogs by viewModel.callLogs.collectAsState()

    // Database items sorted by ID/date descending (newest calls at the top)
    // Enforce Policy: only show if the call is made using this app and is Outgoing
    val callLogs = remember(rawCallLogs) {
        rawCallLogs
            .filter { it.callType.equals("Outgoing", ignoreCase = true) && com.example.util.AppPreferences.isNumberDialedFromApp(context, it.phoneNumber) }
            .sortedByDescending { it.id }
    }

    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All")

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var showQuickLogDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<CallLogEntity?>(null) }
    var showDeleteAllLogsDialog by remember { mutableStateOf(false) }
    val isAutoCallRecordingEnabled by viewModel.isAutoCallRecordingEnabled.collectAsState()
    var currentlyPlayingLogId by remember { mutableStateOf<Long?>(null) }

    // Dialog: Delete All Call Logs Confirmation
    if (showDeleteAllLogsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllLogsDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete All Call Logs?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to permanently delete all call logs and recorded call audio? These will not reproduce once deleted.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllCallLogs()
                        showDeleteAllLogsDialog = false
                        Toast.makeText(context, "All call logs and recordings deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete All", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllLogsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Metrics calculations
    val totalCount = callLogs.size
    val incomingCount = remember(callLogs) { callLogs.count { it.callType.equals("Incoming", ignoreCase = true) } }
    val outgoingCount = remember(callLogs) { callLogs.count { it.callType.equals("Outgoing", ignoreCase = true) } }
    val missedCount = remember(callLogs) {
        callLogs.count {
            it.callType.equals("Missed", ignoreCase = true) ||
            it.status.equals("Missed", ignoreCase = true) ||
            it.status.equals("No Answer", ignoreCase = true)
        }
    }

    // Filtered logs
    val filteredLogs = remember(callLogs, selectedFilter, searchQuery) {
        callLogs.filter { log ->
            val matchesFilter = when (selectedFilter) {
                "Incoming" -> log.callType.equals("Incoming", ignoreCase = true)
                "Outgoing" -> log.callType.equals("Outgoing", ignoreCase = true)
                "Missed" -> log.callType.equals("Missed", ignoreCase = true) ||
                            log.status.equals("Missed", ignoreCase = true) ||
                            log.status.equals("No Answer", ignoreCase = true)
                else -> true
            }

            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                log.contactName.contains(searchQuery.trim(), ignoreCase = true) ||
                log.phoneNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                log.status.contains(searchQuery.trim(), ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Phone Call Logs",
                subMenuSubtitle = "${filteredLogs.size} of $totalCount Calls (Newest First)",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "Toggle Search",
                            tint = BrandBlue
                        )
                    }
                    IconButton(onClick = { showQuickLogDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddIcCall,
                            contentDescription = "Log Call",
                            tint = BrandBlue
                        )
                    }
                    if (callLogs.isNotEmpty()) {
                        IconButton(onClick = { showDeleteAllLogsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Delete All Call Logs",
                                tint = StatusRed
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickLogDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Default.AddIcCall, contentDescription = "Log Manual Call")
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // 0. Auto Call Recording Configuration Card (Option ON/OFF)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAutoCallRecordingEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(1.dp, if (isAutoCallRecordingEnabled) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isAutoCallRecordingEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isAutoCallRecordingEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = if (isAutoCallRecordingEnabled) Color(0xFF16A34A) else Color(0xFF64748B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Auto Call Recording",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isAutoCallRecordingEnabled) Color(0xFF16A34A) else Color(0xFF64748B)
                                    ) {
                                        Text(
                                            if (isAutoCallRecordingEnabled) "ON" else "OFF",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (isAutoCallRecordingEnabled)
                                        "Auto-records in-call audio & logs with CRM clients"
                                    else
                                        "Call audio recording is turned OFF",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isAutoCallRecordingEnabled,
                            onCheckedChange = { isChecked ->
                                viewModel.setAutoCallRecordingEnabled(isChecked)
                                Toast.makeText(
                                    context,
                                    if (isChecked) "Auto Call Recording turned ON" else "Auto Call Recording turned OFF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }
            }

            // 1. Telephony BroadcastReceiver Live Status Banner
            item {
                TelephonyStatusCard(
                    onSimulateClick = {
                        PhoneCallStateReceiver.simulateTestCallEvent(
                            context = context,
                            contactName = "Vikram Malhotra (Apex Tech)",
                            phoneNumber = "+91 98112 34567",
                            callType = "Incoming",
                            durationSeconds = 165
                        )
                        Toast.makeText(context, "Simulated incoming call logged successfully", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 2. Overview Metrics Cards
            item {
                CallMetricsOverviewSection(
                    total = totalCount,
                    incoming = incomingCount,
                    outgoing = outgoingCount,
                    missed = missedCount
                )
            }

            // 3. Search Bar (Expandable)
            if (isSearchActive) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by contact name or phone number...", fontSize = 13.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }
            }

            // 4. Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(filters) { filter ->
                        val countLabel = when (filter) {
                            "Incoming" -> " ($incomingCount)"
                            "Outgoing" -> " ($outgoingCount)"
                            "Missed" -> " ($missedCount)"
                            else -> " ($totalCount)"
                        }
                        val isSelected = selectedFilter == filter

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    "$filter$countLabel",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) BrandBlue else Color(0xFFE2E8F0),
                                borderWidth = 1.dp
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // 5. Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Call History (Sorted by Date)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredLogs.size} logs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
            }

            // 6. Call Log Items List (or Empty State)
            if (filteredLogs.isEmpty()) {
                item {
                    EmptyCallLogsView(
                        searchQuery = searchQuery,
                        onClearSearch = {
                            searchQuery = ""
                            selectedFilter = "All"
                        },
                        onSimulate = {
                            PhoneCallStateReceiver.simulateTestCallEvent(
                                context = context,
                                contactName = "Rohan Verma (Cloud Corp)",
                                phoneNumber = "+91 98765 43210",
                                callType = "Outgoing",
                                durationSeconds = 90
                            )
                            Toast.makeText(context, "Sample call logged to database", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    CallLogItemCard(
                        log = log,
                        isAudioPlaying = currentlyPlayingLogId == log.id,
                        onPlayAudio = {
                            if (currentlyPlayingLogId == log.id) {
                                com.example.util.AudioRecorderHelper.stopPlaying()
                                currentlyPlayingLogId = null
                            } else {
                                com.example.util.AudioRecorderHelper.stopPlaying()
                                val audioSource = log.audioPath
                                if (!audioSource.isNullOrBlank()) {
                                    currentlyPlayingLogId = log.id
                                    Toast.makeText(context, "Playing call recording for ${log.contactName}", Toast.LENGTH_SHORT).show()
                                    com.example.util.AudioRecorderHelper.playAudio(
                                        filePathOrUrl = audioSource,
                                        onPrepared = {},
                                        onCompletion = {
                                            if (currentlyPlayingLogId == log.id) {
                                                currentlyPlayingLogId = null
                                            }
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "No audio recording file associated with this call log", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onCallClick = {
                            WhatsAppHelper.dialPhoneNumber(context, log.phoneNumber)
                        },
                        onWhatsAppClick = {
                            WhatsAppHelper.openWhatsAppDirectChat(
                                context = context,
                                phoneNumber = log.phoneNumber,
                                initialMessage = "Hello ${log.contactName.takeWhile { it != '(' }.trim()}, following up regarding our recent phone conversation."
                            )
                        },
                        onDeleteClick = {
                            logToDelete = log
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    logToDelete?.let { targetLog ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Delete Call Log", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete the call record for '${targetLog.contactName}' (${targetLog.timestampText})?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCallLog(targetLog)
                        logToDelete = null
                        Toast.makeText(context, "Call log removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Quick Manual Call Entry Dialog
    if (showQuickLogDialog) {
        QuickLogCallDialog(
            onDismiss = { showQuickLogDialog = false },
            onSave = { name, phone, type, duration, status ->
                viewModel.addCallLog(
                    contactName = name,
                    phone = phone,
                    type = type,
                    duration = duration
                )
                showQuickLogDialog = false
                Toast.makeText(context, "Call log saved successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Live Telephony & BroadcastReceiver Status Indicator
 */
@Composable
private fun TelephonyStatusCard(onSimulateClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF16A34A), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Telephony Auto-Logger Active",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "BroadcastReceiver auto-records incoming & outgoing SIM calls",
                        fontSize = 11.sp,
                        color = Color(0xFF166534),
                        lineHeight = 15.sp
                    )
                }
            }

            FilledTonalButton(
                onClick = onSimulateClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFDCFCE7),
                    contentColor = Color(0xFF15803D)
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test Event", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 4-Grid Overview of Call Metrics
 */
@Composable
private fun CallMetricsOverviewSection(
    total: Int,
    incoming: Int,
    outgoing: Int,
    missed: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricMiniCard(
            title = "App Outgoing",
            count = total.toString(),
            color = Color(0xFF2563EB),
            bgColor = Color(0xFFEEF2FF),
            icon = Icons.AutoMirrored.Filled.CallMade,
            modifier = Modifier.weight(1f)
        )
        MetricMiniCard(
            title = "Connected",
            count = total.toString(),
            color = StatusGreen,
            bgColor = StatusGreenBg,
            icon = Icons.Default.Phone,
            modifier = Modifier.weight(1f)
        )
        MetricMiniCard(
            title = "Recorded",
            count = total.toString(),
            color = BrandBlue,
            bgColor = Color(0xFFEFF6FF),
            icon = Icons.Default.Mic,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    count: String,
    color: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(count, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, maxLines = 1)
        }
    }
}

/**
 * Individual Call Log Card with 1-tap Actions
 */
@Composable
fun CallLogItemCard(
    log: CallLogEntity,
    isAudioPlaying: Boolean = false,
    onPlayAudio: () -> Unit = {},
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAnalyzeCall: () -> Unit = {},
    onDeleteClick: () -> Unit
) {
    val isIncoming = log.callType.equals("Incoming", ignoreCase = true)
    val isMissed = log.callType.equals("Missed", ignoreCase = true) ||
                   log.status.equals("Missed", ignoreCase = true) ||
                   log.status.equals("No Answer", ignoreCase = true)

    val (icon, iconTint, bgTint) = when {
        isMissed -> Triple(Icons.Default.PhoneMissed, StatusRed, StatusRedBg)
        isIncoming -> Triple(Icons.AutoMirrored.Filled.CallReceived, StatusGreen, StatusGreenBg)
        else -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF2563EB), Color(0xFFEEF2FF))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Call Direction Avatar Badge
                    Surface(
                        shape = CircleShape,
                        color = bgTint,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = log.callType,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.contactName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.phoneNumber,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (log.status.equals("Connected", ignoreCase = true)) StatusGreenBg else StatusRedBg
                ) {
                    Text(
                        text = log.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (log.status.equals("Connected", ignoreCase = true)) StatusGreen else StatusRed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // AI Call Summary Card section if present
            if (!log.summary.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB), // Soft golden-amber background for Milo's insights
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "🦁",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Milo's Call Summary",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = log.summary,
                                fontSize = 12.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Audio Player Controls if recording is present
            if (!log.audioPath.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPlayAudio,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isAudioPlaying) StatusRed else BrandBlue, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Stop Recording" else "Play Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isAudioPlaying) "Playing Call Recording..." else "Call Recording Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = if (log.audioPath == "simulated_recording.m4a") "Simulated HD Audio (.m4a)" else "Recorded via MIC Source",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isAudioPlaying) StatusRed else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Metadata row & 1-tap Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time & Duration
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.timestampText,
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(3.dp).background(TextMuted, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.durationText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iconTint
                    )
                }

                // Quick Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 1-tap Call Dial
                    FilledTonalIconButton(
                        onClick = onCallClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = BrandBlue
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    }

                    // 1-tap WhatsApp Dispatch
                    FilledTonalIconButton(
                        onClick = onWhatsAppClick,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFDCFCE7),
                            contentColor = Color(0xFF15803D)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                    }
                    
                    // AI Analyze
                    FilledTonalIconButton(
                        onClick = onAnalyzeCall,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xFFF3E8FF),
                            contentColor = Color(0xFF7E22CE)
                        ),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Analyze", modifier = Modifier.size(16.dp))
                    }

                    // Delete Log
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Empty Call Logs Placeholder
 */
@Composable
private fun EmptyCallLogsView(
    searchQuery: String,
    onClearSearch: () -> Unit,
    onSimulate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEFF6FF),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "No Matching Call Logs" else "No Phone Calls Logged Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No calls found matching '$searchQuery'. Try adjusting your filter."
                } else {
                    "Incoming and outgoing phone calls on this device will automatically be recorded here by the background BroadcastReceiver."
                },
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (searchQuery.isNotBlank()) {
                OutlinedButton(onClick = onClearSearch) {
                    Text("Clear Filter")
                }
            } else {
                Button(
                    onClick = onSimulate,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Simulate Sample Call")
                }
            }
        }
    }
}

/**
 * Quick Manual Call Log Dialog
 */
@Composable
private fun QuickLogCallDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, type: String, duration: String, status: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("02m 45s") }
    var selectedType by remember { mutableStateOf("Outgoing") }
    var selectedStatus by remember { mutableStateOf("Connected") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Phone Call", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact / Client Name") },
                    placeholder = { Text("e.g. Ramesh Patel (TechCorp)") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+91 98765 43210") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Type Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Call Type", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Incoming", "Outgoing", "Missed").forEach { type ->
                                val isSel = selectedType == type
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) BrandBlue else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedType = type
                                            if (type == "Missed") {
                                                selectedStatus = "Missed"
                                                duration = "0s"
                                            }
                                        }
                                ) {
                                    Text(
                                        text = type.take(3),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextSecondary,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    placeholder = { Text("02m 45s") },
                    singleLine = true,
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalPhone = phone.ifBlank { "+91 98765 43210" }
                        onSave(name.trim(), finalPhone.trim(), selectedType, duration.trim(), selectedStatus)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Text("Save to CRM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
