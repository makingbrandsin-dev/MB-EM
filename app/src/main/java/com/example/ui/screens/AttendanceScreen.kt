package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.WhatsAppQuickChatDialog
import com.example.ui.components.MiloAssistantDialog
import com.example.ui.components.formatLiveSeconds
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCrm: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null,
    onNavigateToChat: () -> Unit = {}
) {
    val attendance by viewModel.latestAttendance.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val liveSeconds by viewModel.liveActiveDurationSeconds.collectAsState()
    val isOnBreak by viewModel.isOnBreak.collectAsState()
    val currentBreakType by viewModel.currentBreakType.collectAsState()
    val liveBreakSeconds by viewModel.liveBreakDurationSeconds.collectAsState()
    val isQuickChatOpen by viewModel.isQuickChatOpen.collectAsState()
    val unreadChatCount by viewModel.unreadChatCount.collectAsState()
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val employeeRole by viewModel.currentEmployeeRole.collectAsState()
    val regularizations by viewModel.attendanceRegularizations.collectAsState()
    val unsyncedAttendanceCount by viewModel.unsyncedAttendanceCount.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()

    val isWorking = attendance?.isWorking ?: false

    // Realtime Live Clock & Date for Attendance dynamically adjusted with ViewModel clock
    val liveClockMillis by viewModel.liveClockTimeMillis.collectAsState()
    val liveTimeString = remember(liveClockMillis) {
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(liveClockMillis))
    }
    val liveDateString = remember(liveClockMillis) {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(liveClockMillis))
    }

    val context = LocalContext.current
    var selectedViewMode by remember { mutableStateOf("visual_chart") } // "visual_chart", "daily_logs", "date_sheet", "monthly_sheet"
    var selectedMonth by remember { mutableStateOf("September 2026") }
    var selectedDateForSheet by remember { mutableStateOf("2026-09-17") }
    var showBreakDialog by remember { mutableStateOf(false) }
    var showGeofencePunchDialog by remember { mutableStateOf(false) }
    var showRegularizationDialog by remember { mutableStateOf(false) }

    val monthsList = listOf("September 2026", "August 2026", "July 2026")

    // Pulsing circle animation when working
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseBorder by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorder"
    )

    val installDate = remember { com.example.util.AppPreferences.getAppInstallDate(context) }

    // Filter attendance records by selected month and starting strictly from the app install date
    val filteredRecords = remember(allAttendance, selectedMonth, installDate) {
        val monthPrefix = when (selectedMonth) {
            "September 2026" -> "2026-09"
            "August 2026" -> "2026-08"
            "July 2026" -> "2026-07"
            else -> "2026-09"
        }
        allAttendance
            .filter { it.date.startsWith(monthPrefix) && it.date >= installDate }
            .sortedWith(compareByDescending<AttendanceRecord> { it.date }.thenByDescending { it.id })
    }

    // Monthly Aggregated Stats
    val totalMinutesLogged = filteredRecords.sumOf { it.durationMinutes }
    val totalHoursLogged = totalMinutesLogged / 60
    val totalHoursLoggedRemMins = totalMinutesLogged % 60
    val totalWorkingDays = filteredRecords.size
    val totalOvertimeMins = filteredRecords.sumOf { it.overtimeMinutes }
    val avgDailyMinutes = if (totalWorkingDays > 0) totalMinutesLogged / totalWorkingDays else 0

    // WhatsApp Quick Chat Modal Overlay
    if (isQuickChatOpen) {
        WhatsAppQuickChatDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeQuickChat() }
        )
    }

    // Break Dialog
    if (showBreakDialog) {
        BreakOptionsDialog(
            onDismiss = { showBreakDialog = false },
            onSelectBreak = { breakType ->
                viewModel.startBreak(breakType)
                showBreakDialog = false
            }
        )
    }

    if (showRegularizationDialog) {
        AttendanceRegularizationDialog(
            onDismiss = { showRegularizationDialog = false },
            onSubmit = { date, inTime, outTime, reason, remarks ->
                viewModel.submitAttendanceRegularization(date, inTime, outTime, reason, remarks)
            }
        )
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Attendance & Performance",
                subMenuSubtitle = if (isWorking) "Shift In Progress · Live Tracking" else "Off-Clock · Ready to Punch",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToChat = onNavigateToChat
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "attendance",
                    onNavigateToCrm = onNavigateToCrm,
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = { },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // 💾 Offline Room Database Local Cache & Sync Status Banner
            if (!isDeviceOnline || unsyncedAttendanceCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isDeviceOnline) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (!isDeviceOnline) Color(0xFFFCD34D) else Color(0xFF86EFAC)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            if (!isDeviceOnline) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (!isDeviceOnline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                        contentDescription = "Room Local Cache",
                                        tint = if (!isDeviceOnline) Color(0xFFD97706) else Color(0xFF16A34A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (!isDeviceOnline) "Offline Mode · Room Cache Active" else "Room Cache: $unsyncedAttendanceCount Pending Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (!isDeviceOnline) Color(0xFF92400E) else Color(0xFF166534)
                                    )
                                    Text(
                                        text = if (!isDeviceOnline) 
                                            "All punches and logs persist safely in local Room DB and auto-sync when online."
                                        else 
                                            "Changes safely stored locally in Room. Ready to sync with Firestore.",
                                        fontSize = 11.sp,
                                        color = if (!isDeviceOnline) Color(0xFFB45309) else Color(0xFF15803D),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                            if (isDeviceOnline && unsyncedAttendanceCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.syncCachedRoomDataNow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // ⏱️ Live Punch & Break Action Card (Clean Stacked Vertical Layout One Below the Other)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandDarkBlue),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Top Section: Live Clock & Date + Live Status Pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = liveTimeString,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = liveDateString,
                                        color = Color(0xFF93C5FD),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF22C55E).copy(alpha = 0.2f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4ADE80))
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                "Firestore Synced",
                                                color = Color(0xFF86EFAC),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isWorking) {
                                    if (isOnBreak) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF22C55E).copy(alpha = 0.25f)
                                } else Color.White.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isWorking) {
                                                    if (isOnBreak) Color(0xFFFBBF24) else Color(0xFF4ADE80)
                                                } else Color(0xFF94A3B8)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isWorking) {
                                            if (isOnBreak) "On $currentBreakType" else "Shift Active"
                                        } else "Off-Clock",
                                        color = if (isWorking) {
                                            if (isOnBreak) Color(0xFFFDE68A) else Color(0xFF86EFAC)
                                        } else Color(0xFFCBD5E1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFF1E3A8A).copy(alpha = 0.7f), thickness = 1.dp)

                        // 2. Active Worked Today Time (Prominently Displayed)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isOnBreak) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF10B981).copy(alpha = 0.25f),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (isOnBreak) Icons.Default.Coffee else Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = if (isOnBreak) Color(0xFFFBBF24) else Color(0xFF34D399),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isOnBreak) "Break Time Elapsed" else "Active Worked Today",
                                            color = if (isOnBreak) Color(0xFFFDE68A) else Color(0xFF93C5FD),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isWorking) (if (isOnBreak) "Shift Timer Paused" else "${liveSeconds / 3600}h ${(liveSeconds % 3600) / 60}m logged · Goal 8.0h") else "Off-Clock",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isWorking) {
                                            if (isOnBreak) "${liveBreakSeconds / 60}m ${liveBreakSeconds % 60}s"
                                            else formatLiveSeconds(liveSeconds)
                                        } else if ((attendance?.durationMinutes ?: 0) > 0) {
                                            "${(attendance?.durationMinutes ?: 0) / 60}h ${(attendance?.durationMinutes ?: 0) % 60}m"
                                        } else "00:00:00",
                                        color = if (isOnBreak) Color(0xFFFBBF24) else Color(0xFFFDE047),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = if (isWorking) (if (isOnBreak) "Paused" else "Live Counting") else "Total Logged",
                                        color = if (isWorking) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 3. Punch In Time & Check Out Time (In The Same Single Box for Space Efficiency)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left Column: Punch In Time
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF3B82F6).copy(alpha = 0.25f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Login,
                                                contentDescription = null,
                                                tint = Color(0xFF93C5FD),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Punch In", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (attendance?.checkInTime != null) attendance?.checkInTime!! else if (isWorking) "09:00 AM" else "--:--",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (isWorking) "On-Time" else "Not In",
                                            color = if (isWorking) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Center Subtle Divider
                                Divider(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .width(1.dp)
                                )

                                // Right Column: Check Out Time
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF6366F1).copy(alpha = 0.25f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Logout,
                                                contentDescription = null,
                                                tint = Color(0xFFA5B4FC),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Check Out", color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = when {
                                                attendance?.checkOutTime != null -> attendance?.checkOutTime!!
                                                isWorking -> "06:00 PM"
                                                else -> "--:--"
                                            },
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = if (attendance?.checkOutTime != null) "Departed" else if (isWorking) "Estimated" else "Off-Clock",
                                            color = if (attendance?.checkOutTime != null) Color(0xFF4ADE80) else Color(0xFF93C5FD),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Break Action Controls (No borders/strokes)
                        if (isWorking) {
                            if (isOnBreak) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Coffee, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Currently on $currentBreakType", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Shift timer paused temporarily", color = Color(0xFFFDE68A), fontSize = 10.sp)
                                            }
                                        }

                                        Button(
                                            onClick = { viewModel.resumeFromBreak() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Resume Work", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showBreakDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                ) {
                                    Icon(Icons.Default.FreeBreakfast, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("☕ Take Break (Tea / Lunch / Custom Break)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // Big Punch In / Out Button (No border, no stroke)
                        Button(
                            onClick = {
                                if (isWorking) {
                                    viewModel.checkOutUser(context = context)
                                } else {
                                    showGeofencePunchDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isWorking) Color(0xFFEF4444) else BrandBlue,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(
                                if (isWorking) Icons.Default.Logout else Icons.Default.Login,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isWorking) "Punch Out for the Day" else "Punch In (Geofenced & Selfie)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Regularization Button
                        OutlinedButton(
                            onClick = { showRegularizationDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF93C5FD)
                            )
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Missed Punch? Request Regularization", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF93C5FD))
                        }
                    }
                }
            }

            // Pending Regularization Requests
            if (regularizations.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Regularization Workflow (${regularizations.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlue)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            regularizations.take(3).forEach { req ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${req.date} (${req.requestedInTime} - ${req.requestedOutTime})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("${req.reason} • ${req.remarks.ifBlank { "Pending approval" }}", fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when(req.status) {
                                            "APPROVED" -> Color(0xFFDCFCE7)
                                            "REJECTED" -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    ) {
                                        Text(
                                            req.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when(req.status) {
                                                "APPROVED" -> Color(0xFF166534)
                                                "REJECTED" -> Color(0xFF991B1B)
                                                else -> Color(0xFF92400E)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 📅 Monthly Performance Section Header (Months placed below the title for clean spacing)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column {
                        Text(
                            "Monthly Performance",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            "Interactive visual hours tracking & monthly appraisal",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    // Month Picker Pills (Placed below the title)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(monthsList) { mName ->
                            val isSel = selectedMonth == mName
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) BrandBlue else Color.White,
                                border = BorderStroke(1.dp, if (isSel) BrandBlue else BorderLight),
                                modifier = Modifier.clickable { selectedMonth = mName }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isSel) Color.White else BrandBlue,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Overview Summary 4-Capsule Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceMetricMiniCard(
                        title = "Hours Logged",
                        value = "${totalHoursLogged}h ${totalHoursLoggedRemMins}m",
                        subtext = "in $selectedMonth",
                        color = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricMiniCard(
                        title = "Days Present",
                        value = "$totalWorkingDays Days",
                        subtext = "Attendance 95%",
                        color = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricMiniCard(
                        title = "Daily Avg",
                        value = "${avgDailyMinutes / 60}h ${avgDailyMinutes % 60}m",
                        subtext = "Target: 8.0h",
                        color = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB),
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceMetricMiniCard(
                        title = "Overtime",
                        value = "${totalOvertimeMins / 60}h ${totalOvertimeMins % 60}m",
                        subtext = "Accumulated",
                        color = Color(0xFF9333EA),
                        bgColor = Color(0xFFFAF5FF),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 🔀 4-Mode View Switcher (Bar Graph / Day-by-Day Log / Date Performance Sheet / Monthly Performance Sheet)
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AttendanceTabButton(
                            title = "📊 Bar Graph",
                            isSelected = selectedViewMode == "visual_chart",
                            onClick = { selectedViewMode = "visual_chart" },
                            modifier = Modifier.weight(1.1f)
                        )
                        AttendanceTabButton(
                            title = "📅 Logs",
                            isSelected = selectedViewMode == "daily_logs",
                            onClick = { selectedViewMode = "daily_logs" },
                            modifier = Modifier.weight(0.9f)
                        )
                        AttendanceTabButton(
                            title = "📋 Date Sheet",
                            isSelected = selectedViewMode == "date_sheet",
                            onClick = { selectedViewMode = "date_sheet" },
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceTabButton(
                            title = "📈 Monthly",
                            isSelected = selectedViewMode == "monthly_sheet",
                            onClick = { selectedViewMode = "monthly_sheet" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === VIEW MODE 0: VISUAL DASHBOARD WITH RECHARTS-STYLE BAR GRAPH ===
            if (selectedViewMode == "visual_chart") {
                item {
                    MonthlyAttendanceBarChartDashboard(
                        selectedMonth = selectedMonth,
                        records = filteredRecords,
                        totalHoursLogged = totalHoursLogged,
                        totalHoursRemMins = totalHoursLoggedRemMins,
                        avgDailyMinutes = avgDailyMinutes,
                        totalOvertimeMins = totalOvertimeMins,
                        onSelectDay = { dateStr ->
                            selectedDateForSheet = dateStr
                            selectedViewMode = "date_sheet"
                        }
                    )
                }
            }

            // === VIEW MODE 1: DAY-BY-DAY ATTENDANCE LOG ===
            else if (selectedViewMode == "daily_logs") {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Day & Hours Spent Breakdown",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            "${filteredRecords.size} records",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                items(filteredRecords) { record ->
                    DayAttendanceCard(
                        record = record,
                        onViewDateSheet = {
                            selectedDateForSheet = record.date
                            selectedViewMode = "date_sheet"
                        }
                    )
                }
            }

            // === VIEW MODE 2: DATE PERFORMANCE SHEET ===
            else if (selectedViewMode == "date_sheet") {
                item {
                    DatePerformanceSheetView(
                        selectedDate = selectedDateForSheet,
                        allRecords = filteredRecords,
                        employeeName = employeeName,
                        employeeRole = employeeRole,
                        onSelectDate = { selectedDateForSheet = it },
                        onExportSlip = { dateStr ->
                            viewModel.exportDailyPerformanceSlip(dateStr)
                        }
                    )
                }
            }

            // === VIEW MODE 3: MONTHLY PERFORMANCE SHEET ===
            else if (selectedViewMode == "monthly_sheet") {
                item {
                    MonthlyPerformanceSheetView(
                        selectedMonth = selectedMonth,
                        records = filteredRecords,
                        employeeName = employeeName,
                        employeeRole = employeeRole,
                        totalHoursLogged = totalHoursLogged,
                        totalHoursRemMins = totalHoursLoggedRemMins,
                        totalWorkingDays = totalWorkingDays,
                        totalOvertimeMins = totalOvertimeMins,
                        avgDailyMinutes = avgDailyMinutes,
                        onExportMonthly = { monthStr ->
                            viewModel.exportMonthlyPerformanceReport(monthStr)
                        }
                    )
                }
            }
        }

        if (showGeofencePunchDialog) {
            GeofenceSelfiePunchDialog(
                onDismiss = { showGeofencePunchDialog = false },
                onConfirmPunchIn = { isGeofenced, selfieUri ->
                    viewModel.checkInUser(context = context, selfieUri = selfieUri)
                    showGeofencePunchDialog = false
                }
            )
        }
    }
}

// -------------------------------------------------------------
// Day-by-Day Attendance Card Component
// -------------------------------------------------------------
@Composable
fun DayAttendanceCard(
    record: AttendanceRecord,
    onViewDateSheet: () -> Unit
) {
    // Parse Date into Day name (e.g. Fri, 18 Sep 2026)
    val formattedDateText = remember(record.date) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdfInput.parse(record.date)
            val sdfOutput = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            if (d != null) sdfOutput.format(d) else record.date
        } catch (e: Exception) {
            record.date
        }
    }

    val dayNameOnly = remember(record.date) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdfInput.parse(record.date)
            val sdfOutput = SimpleDateFormat("EEE", Locale.getDefault())
            if (d != null) sdfOutput.format(d).uppercase() else "DAY"
        } catch (e: Exception) {
            "DAY"
        }
    }

    val dayNumOnly = remember(record.date) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdfInput.parse(record.date)
            val sdfOutput = SimpleDateFormat("dd", Locale.getDefault())
            if (d != null) sdfOutput.format(d) else "00"
        } catch (e: Exception) {
            "00"
        }
    }

    val hrs = record.durationMinutes / 60
    val mins = record.durationMinutes % 60
    val isHalfDay = record.status.equals("Half Day", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day & Date Pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHalfDay) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                        modifier = Modifier.size(width = 46.dp, height = 48.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = dayNameOnly,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHalfDay) Color(0xFFD97706) else BrandBlue
                            )
                            Text(
                                text = dayNumOnly,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isHalfDay) Color(0xFFB45309) else BrandDarkBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = formattedDateText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "In: ${record.checkInTime} · Out: ${record.checkOutTime ?: "In Progress"}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        if (!record.isSynced) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = "Room Cached",
                                    tint = Color(0xFFEA580C),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Room Cached (Offline)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                        }
                    }
                }

                // Hours Spent Highlight
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHalfDay) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isHalfDay) Color(0xFFFCD34D) else Color(0xFF86EFAC))
                    ) {
                        Text(
                            text = "${hrs}h ${mins}m spent",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isHalfDay) Color(0xFFB45309) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (record.overtimeMinutes > 0) "+${record.overtimeMinutes}m Overtime" else if (isHalfDay) "Half Day Shift" else "Full Day (8h+)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (record.overtimeMinutes > 0) Color(0xFF7C3AED) else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = BorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            // Break info and View Performance button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Coffee,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Break: ${if (record.breakMinutes > 0) "${record.breakMinutes}m" else "45m"} (${record.breakType})",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(
                    onClick = onViewDateSheet,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("View Date Performance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// VIEW 2: Comprehensive Date Performance Sheet
// -------------------------------------------------------------
@Composable
fun DatePerformanceSheetView(
    selectedDate: String,
    allRecords: List<AttendanceRecord>,
    employeeName: String,
    employeeRole: String,
    onSelectDate: (String) -> Unit,
    onExportSlip: (String) -> Unit
) {
    val currentRecord = allRecords.firstOrNull { it.date == selectedDate } ?: allRecords.firstOrNull()
    val recordDate = currentRecord?.date ?: selectedDate

    val formattedHeaderDate = remember(recordDate) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdfInput.parse(recordDate)
            val sdfOutput = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            if (d != null) sdfOutput.format(d) else recordDate
        } catch (e: Exception) {
            recordDate
        }
    }

    val durationMins = currentRecord?.durationMinutes ?: 540L
    val workHours = durationMins / 60
    val workMins = durationMins % 60
    val overtimeMins = currentRecord?.overtimeMinutes ?: 45L
    val breakMins = currentRecord?.breakMinutes ?: 45L
    val efficiencyPercent = ((durationMins.toFloat() / 500f) * 100).toInt().coerceIn(75, 100)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date Selector Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Select Date for Performance Evaluation",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allRecords.take(12)) { rec ->
                        val isSel = rec.date == recordDate
                        val dayLabel = try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val d = sdf.parse(rec.date)
                            SimpleDateFormat("dd MMM", Locale.getDefault()).format(d!!)
                        } catch (e: Exception) {
                            rec.date
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSel) BrandDarkBlue else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onSelectDate(rec.date) }
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Executive Date Performance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header of the sheet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E3A8A)
                            ) {
                                Text(
                                    "DAILY PERFORMANCE SLIP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MB-EMP-104", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formattedHeaderDate,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${efficiencyPercent}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderLight)
                Spacer(modifier = Modifier.height(14.dp))

                // Employee Details Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Employee Name", fontSize = 10.sp, color = TextSecondary)
                        Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    Column {
                        Text("Designation", fontSize = 10.sp, color = TextSecondary)
                        Text(employeeRole, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Punctuality Score", fontSize = 10.sp, color = TextSecondary)
                        Text("100 / 100", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF16A34A))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed Metrics Grid
                Text("Shift & Activity Metrics", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PerformanceRowItem(label = "Shift Timings", value = "${currentRecord?.checkInTime ?: "09:00 AM"} – ${currentRecord?.checkOutTime ?: "06:15 PM"}", icon = Icons.Default.Schedule)
                    PerformanceRowItem(label = "Gross Hours Spent", value = "${workHours} Hours ${workMins} Mins", icon = Icons.Default.Timer, highlightColor = BrandBlue)
                    PerformanceRowItem(label = "Break Time Logged", value = "${breakMins} Mins (Within 45m policy)", icon = Icons.Default.Coffee, highlightColor = Color(0xFFD97706))
                    PerformanceRowItem(label = "Overtime Accrued", value = "+${overtimeMins} Mins (+0.75h)", icon = Icons.Default.TrendingUp, highlightColor = Color(0xFF7C3AED))
                    PerformanceRowItem(label = "Tasks Completed", value = "3 Delivered (Sprint Alpha, API Sync)", icon = Icons.Default.CheckCircle, highlightColor = Color(0xFF16A34A))
                    PerformanceRowItem(label = "Quality / Downtime", value = "0% Unauthorized idle time", icon = Icons.Default.VerifiedUser, highlightColor = Color(0xFF0284C7))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar vs 8 Hour Target
                Text("Target Achievement (8.0 Hours Standard)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (durationMins.toFloat() / 480f).coerceAtMost(1.2f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = BrandBlue,
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Standard: 8h 00m", fontSize = 10.sp, color = TextSecondary)
                    Text("Achieved: ${workHours}h ${workMins}m (${((durationMins.toFloat()/480f)*100).toInt()}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manager Feedback Remarks
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7).copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.RateReview, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Manager Evaluation Note", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF92400E))
                            Text("Consistent execution, zero delay in punch times. Sprint backlog tasks cleared on time.", fontSize = 11.sp, color = Color(0xFF78350F))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Button(
                    onClick = { onExportSlip(formattedHeaderDate) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandDarkBlue)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export & Share Daily Performance Slip", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// VIEW 3: Monthly Comprehensive Performance Sheet
// -------------------------------------------------------------
@Composable
fun MonthlyPerformanceSheetView(
    selectedMonth: String,
    records: List<AttendanceRecord>,
    employeeName: String,
    employeeRole: String,
    totalHoursLogged: Long,
    totalHoursRemMins: Long,
    totalWorkingDays: Int,
    totalOvertimeMins: Long,
    avgDailyMinutes: Long,
    onExportMonthly: (String) -> Unit
) {
    val totalHalfDays = records.count { it.status.equals("Half Day", ignoreCase = true) }
    val fullDaysPresent = totalWorkingDays - totalHalfDays
    val totalLeavesTaken = 1
    val attendanceRate = if (totalWorkingDays > 0) ((totalWorkingDays.toFloat() / (totalWorkingDays + totalLeavesTaken)) * 100).toInt() else 95

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Monthly Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BrandDarkBlue
                        ) {
                            Text(
                                "MONTHLY PERFORMANCE APPRAISAL SHEET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$selectedMonth Report",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Making Brands HQ · Employee Assessment Matrix",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Text(
                                "Grade A+",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text("Outstanding", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderLight)
                Spacer(modifier = Modifier.height(14.dp))

                // Employee Profile Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Employee Name", fontSize = 10.sp, color = TextSecondary)
                        Text(employeeName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("ID: MB-EMP-104", fontSize = 10.sp, color = TextSecondary)
                    }
                    Column {
                        Text("Department", fontSize = 10.sp, color = TextSecondary)
                        Text("Product & Tech", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text(employeeRole, fontSize = 10.sp, color = TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Attendance %", fontSize = 10.sp, color = TextSecondary)
                        Text("$attendanceRate%", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF16A34A))
                        Text("21 Working Days", fontSize = 10.sp, color = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4-Card Performance Indicators
                Text("Monthly Performance Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthlyMetricTile(title = "Total Time Logged", value = "${totalHoursLogged}h ${totalHoursRemMins}m", subtitle = "Avg ${avgDailyMinutes/60}h/day", bgColor = Color(0xFFEFF6FF), tint = Color(0xFF2563EB), modifier = Modifier.weight(1f))
                    MonthlyMetricTile(title = "Overtime Accrued", value = "${totalOvertimeMins / 60}h ${totalOvertimeMins % 60}m", subtitle = "Approved extra", bgColor = Color(0xFFFAF5FF), tint = Color(0xFF9333EA), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonthlyMetricTile(title = "Present / Half Days", value = "$fullDaysPresent / $totalHalfDays", subtitle = "1 Approved Leave", bgColor = Color(0xFFF0FDF4), tint = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                    MonthlyMetricTile(title = "Tasks Delivered", value = "24 Completed", subtitle = "100% On-Time SLA", bgColor = Color(0xFFFFFBEB), tint = Color(0xFFD97706), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detailed KPI Table
                Text("KPI Evaluation Checklist", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    KpiEvaluationRow("Punctuality & Shift Discipline", "98%", "Excellent (Max 2 minor delays)")
                    KpiEvaluationRow("Break Compliance (<45 mins)", "99%", "Strict adherence to lunch policies")
                    KpiEvaluationRow("Sprint Task Productivity", "96%", "Delivered all planned features")
                    KpiEvaluationRow("Overtime Value Delivered", "100%", "Client website and mobile app releases")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manager & HR Verification Stamp
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Manager Signoff", fontSize = 10.sp, color = TextSecondary)
                            Text("Arjun Mehta (Tech Lead)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                            Text("Status: Approved & Verified", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Export Button
                Button(
                    onClick = { onExportMonthly(selectedMonth) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download Monthly Performance Report (PDF)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Small Helper Composables
// -------------------------------------------------------------
@Composable
fun MonthlyMetricTile(
    title: String,
    value: String,
    subtitle: String,
    bgColor: Color,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = tint)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun KpiEvaluationRow(kpi: String, score: String, remarks: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(kpi, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(remarks, fontSize = 10.sp, color = TextSecondary)
        }
        Text(score, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
    }
}

@Composable
fun PerformanceRowItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    highlightColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = highlightColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = highlightColor)
    }
}

@Composable
fun AttendanceMetricMiniCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 10.sp, color = TextSecondary, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtext, fontSize = 9.sp, color = Color(0xFF64748B), maxLines = 1)
        }
    }
}

@Composable
fun AttendanceTabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) BrandBlue else Color.Transparent,
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

// -------------------------------------------------------------
// Visual Dashboard: Recharts-Style Interactive Bar Graph
// -------------------------------------------------------------
data class DailyChartBarData(
    val dayNumber: Int,
    val dayName: String,
    val dateString: String,
    val hoursWorked: Float,
    val durationMinutes: Long,
    val isWeekend: Boolean,
    val status: String,
    val overtimeMinutes: Long,
    val breakMinutes: Long,
    val checkInTime: String,
    val checkOutTime: String
)

@Composable
fun MonthlyAttendanceBarChartDashboard(
    selectedMonth: String,
    records: List<AttendanceRecord>,
    totalHoursLogged: Long,
    totalHoursRemMins: Long,
    avgDailyMinutes: Long,
    totalOvertimeMins: Long,
    onSelectDay: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") } // "all", "workdays", "overtime"
    var selectedDayNumber by remember { mutableStateOf(18) }

    // Generate full month days (e.g. 30 days for September)
    val totalDaysInMonth = remember(selectedMonth) {
        when {
            selectedMonth.startsWith("September") -> 30
            selectedMonth.startsWith("August") -> 31
            selectedMonth.startsWith("July") -> 31
            else -> 30
        }
    }

    val monthPrefix = remember(selectedMonth) {
        when {
            selectedMonth.startsWith("September") -> "2026-09"
            selectedMonth.startsWith("August") -> "2026-08"
            selectedMonth.startsWith("July") -> "2026-07"
            else -> "2026-09"
        }
    }

    // Build day-by-day bar chart dataset
    val fullMonthBars = remember(selectedMonth, records) {
        val list = mutableListOf<DailyChartBarData>()
        val cal = Calendar.getInstance()

        for (day in 1..totalDaysInMonth) {
            val dayStr = if (day < 10) "0$day" else "$day"
            val fullDateStr = "$monthPrefix-$dayStr"

            // Determine Day of Week
            cal.set(2026, when {
                selectedMonth.startsWith("September") -> Calendar.SEPTEMBER
                selectedMonth.startsWith("August") -> Calendar.AUGUST
                else -> Calendar.JULY
            }, day)

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val isSunday = dayOfWeek == Calendar.SUNDAY
            val isSecondFourthSat = dayOfWeek == Calendar.SATURDAY && ((day in 8..14) || (day in 22..28))
            val isWeekend = isSunday || isSecondFourthSat

            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Day"
            }

            val matchingRecord = records.firstOrNull { it.date == fullDateStr }

            if (matchingRecord != null) {
                val hrs = matchingRecord.durationMinutes / 60f
                list.add(
                    DailyChartBarData(
                        dayNumber = day,
                        dayName = dayName,
                        dateString = fullDateStr,
                        hoursWorked = hrs,
                        durationMinutes = matchingRecord.durationMinutes,
                        isWeekend = false,
                        status = matchingRecord.status,
                        overtimeMinutes = matchingRecord.overtimeMinutes,
                        breakMinutes = matchingRecord.breakMinutes,
                        checkInTime = matchingRecord.checkInTime ?: "09:00 AM",
                        checkOutTime = matchingRecord.checkOutTime ?: "06:15 PM"
                    )
                )
            } else if (isWeekend) {
                list.add(
                    DailyChartBarData(
                        dayNumber = day,
                        dayName = dayName,
                        dateString = fullDateStr,
                        hoursWorked = 0f,
                        durationMinutes = 0,
                        isWeekend = true,
                        status = "Weekend Off",
                        overtimeMinutes = 0,
                        breakMinutes = 0,
                        checkInTime = "--",
                        checkOutTime = "--"
                    )
                )
            } else if (day <= 18) {
                // Past workday baseline
                val defaultHrs = 8.5f
                list.add(
                    DailyChartBarData(
                        dayNumber = day,
                        dayName = dayName,
                        dateString = fullDateStr,
                        hoursWorked = defaultHrs,
                        durationMinutes = (defaultHrs * 60).toLong(),
                        isWeekend = false,
                        status = "Present",
                        overtimeMinutes = 30,
                        breakMinutes = 45,
                        checkInTime = "09:00 AM",
                        checkOutTime = "06:00 PM"
                    )
                )
            } else {
                // Upcoming day
                list.add(
                    DailyChartBarData(
                        dayNumber = day,
                        dayName = dayName,
                        dateString = fullDateStr,
                        hoursWorked = 0f,
                        durationMinutes = 0,
                        isWeekend = false,
                        status = "Upcoming",
                        overtimeMinutes = 0,
                        breakMinutes = 0,
                        checkInTime = "--",
                        checkOutTime = "--"
                    )
                )
            }
        }
        list
    }

    // Filtered bars according to user selection
    val displayedBars = remember(fullMonthBars, selectedFilter) {
        when (selectedFilter) {
            "workdays" -> fullMonthBars.filter { !it.isWeekend && it.hoursWorked > 0f }
            "overtime" -> fullMonthBars.filter { it.overtimeMinutes > 0 }
            else -> fullMonthBars
        }
    }

    val selectedBar = fullMonthBars.firstOrNull { it.dayNumber == selectedDayNumber } ?: fullMonthBars.firstOrNull { it.hoursWorked > 0 } ?: fullMonthBars.first()

    val formattedSelectedDate = remember(selectedBar.dateString) {
        try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdfInput.parse(selectedBar.dateString)
            val sdfOutput = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
            if (d != null) sdfOutput.format(d) else selectedBar.dateString
        } catch (e: Exception) {
            selectedBar.dateString
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Visual Graph Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderLight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Header & Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "DAILY PERFORMANCE BAR GRAPH",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    "Live Recharts Engine",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daily Worked Hours ($selectedMonth)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap on any day bar to inspect exact hours & shift logs",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipPill(
                        title = "All 30 Days",
                        isSelected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" }
                    )
                    FilterChipPill(
                        title = "Workdays Only",
                        isSelected = selectedFilter == "workdays",
                        onClick = { selectedFilter = "workdays" }
                    )
                    FilterChipPill(
                        title = "Overtime Days (>8.5h)",
                        isSelected = selectedFilter == "overtime",
                        onClick = { selectedFilter = "overtime" }
                    )
                }

                // 📊 THE BAR GRAPH CANVAS & INTERACTIVE BARS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Graph Target Indicator Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Y-Axis: Daily Hours Logged (0h – 12h)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 16.dp, height = 2.dp)
                                        .background(Color(0xFFEF4444))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "8.0h Daily Target",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chart Layout: Left Y-Axis Labels + Scrollable Bar Columns
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Left Y-Axis Scale
                            Column(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(160.dp)
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("12h", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("8h", fontSize = 9.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold)
                                Text("4h", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("0h", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Scrollable Bars Row with 8.0h Target Benchmark Line
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(160.dp)
                            ) {
                                // Red Dashed 8.0h Target Line (at 8h / 12h = 66.6% from bottom -> top ~ 33%)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 24.dp + (136.dp * (8f / 12f)))
                                        .height(1.dp)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.7f))
                                )

                                // Scrollable Bars
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    items(displayedBars) { bar ->
                                        val isSelected = bar.dayNumber == selectedDayNumber
                                        val maxChartHeight = 110f
                                        val barHeightDp = ((bar.hoursWorked / 12f) * maxChartHeight).coerceIn(4f, maxChartHeight).dp

                                        val barColor = when {
                                            bar.isWeekend -> Color(0xFFE2E8F0)
                                            bar.hoursWorked <= 0f -> Color(0xFFE2E8F0)
                                            bar.overtimeMinutes > 30 || bar.hoursWorked >= 9.0f -> Color(0xFF10B981) // Overtime Green
                                            bar.hoursWorked in 7.0f..8.99f -> BrandBlue // Standard Blue
                                            else -> Color(0xFFF59E0B) // Half Day Amber
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .width(26.dp)
                                                .clickable { selectedDayNumber = bar.dayNumber }
                                        ) {
                                            // Top hour indicator (e.g. 9.3h)
                                            if (bar.hoursWorked > 0f) {
                                                Text(
                                                    text = String.format(Locale.US, "%.1f", bar.hoursWorked),
                                                    fontSize = 8.sp,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isSelected) BrandDarkBlue else Color(0xFF64748B)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                            } else {
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }

                                            // The Bar Column
                                            Box(
                                                modifier = Modifier
                                                    .width(if (isSelected) 22.dp else 18.dp)
                                                    .height(barHeightDp)
                                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                    .background(
                                                        if (isSelected) barColor else barColor.copy(alpha = 0.85f)
                                                    )
                                                    .then(
                                                        if (isSelected) Modifier.border(2.dp, BrandDarkBlue, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        else Modifier
                                                    )
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Bottom Day Label (e.g. 18)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) BrandDarkBlue else Color.Transparent
                                            ) {
                                                Text(
                                                    text = "${bar.dayNumber}",
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                    color = if (isSelected) Color.White else TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }

                                            // Day Initials (M, T, W, T, F, S, S)
                                            Text(
                                                text = bar.dayName.take(1),
                                                fontSize = 8.sp,
                                                color = if (bar.isWeekend) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Graph Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ChartLegendItem(color = Color(0xFF10B981), label = "Overtime (>8.5h)")
                            ChartLegendItem(color = BrandBlue, label = "Standard (8.0h Met)")
                            ChartLegendItem(color = Color(0xFFF59E0B), label = "Half-Day (<6h)")
                            ChartLegendItem(color = Color(0xFF94A3B8), label = "Weekend / Off")
                        }
                    }
                }

                // 🔍 INTERACTIVE SELECTED DAY INSPECTOR CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${selectedBar.dayNumber}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = formattedSelectedDate,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = BrandDarkBlue
                                    )
                                    Text(
                                        text = if (selectedBar.isWeekend) "Official Weekend Off" else "${selectedBar.status} · Shift Logged",
                                        fontSize = 11.sp,
                                        color = if (selectedBar.isWeekend) Color(0xFF64748B) else Color(0xFF16A34A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Total Worked Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selectedBar.hoursWorked >= 8.0f) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, if (selectedBar.hoursWorked >= 8.0f) Color(0xFF86EFAC) else Color(0xFFFDE68A))
                            ) {
                                Text(
                                    text = if (selectedBar.hoursWorked > 0) "${selectedBar.durationMinutes / 60}h ${selectedBar.durationMinutes % 60}m" else "0h Logged",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (selectedBar.hoursWorked >= 8.0f) Color(0xFF15803D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = Color(0xFFBFDBFE))

                        // Metrics 3-Item Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InspectorMetricItem(
                                label = "Shift Timing",
                                value = if (selectedBar.hoursWorked > 0) "${selectedBar.checkInTime} – ${selectedBar.checkOutTime}" else "Off-Clock",
                                icon = Icons.Default.Schedule
                            )
                            InspectorMetricItem(
                                label = "Break Logged",
                                value = if (selectedBar.breakMinutes > 0) "${selectedBar.breakMinutes}m" else "45m",
                                icon = Icons.Default.Coffee
                            )
                            InspectorMetricItem(
                                label = "Overtime Accrued",
                                value = if (selectedBar.overtimeMinutes > 0) "+${selectedBar.overtimeMinutes}m" else "0m",
                                icon = Icons.Default.TrendingUp,
                                color = if (selectedBar.overtimeMinutes > 0) Color(0xFF7C3AED) else TextSecondary
                            )
                        }

                        // CTA Button to Open Date Performance Slip
                        Button(
                            onClick = { onSelectDay(selectedBar.dateString) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandDarkBlue),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("Open Detailed Date Performance Slip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun ChartLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF475569), fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InspectorMetricItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = TextPrimary
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
