package com.example.ui.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeaveApplicationEntity
import com.example.data.model.ShiftScheduleEntity
import com.example.domain.milo.MiloState
import com.example.milo.MiloCharacter
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.liftOnPress
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🗓️ Automated Shift Scheduling & Employee Work Rotations Module.
 * Features:
 * - Real-time Firebase & Room database sync for employee shift rotations.
 * - Interactive Day/Week rotation cards with shift codes, timings, and locations.
 * - Milo AI Assistant integration for 1-tap automated time-off requests.
 * - Custom Leave Application modal with real-time Firebase telemetry updates.
 * - Manager / Admin shift rotation assignment sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftScheduleScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenAskMilo: () -> Unit = {}
) {
    val context = LocalContext.current
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val shifts by viewModel.shiftSchedules.collectAsState()
    val leaves by viewModel.leaves.collectAsState()

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All Shifts, 1: My Shifts, 2: Time-Off Requests
    var showTimeOffDialog by remember { mutableStateOf(false) }
    var showAssignShiftDialog by remember { mutableStateOf(false) }

    // Dialog Input State for Time Off
    var selectedLeaveType by remember { mutableStateOf("Casual Leave") }
    var startDateInput by remember { mutableStateOf("08 Oct 2026") }
    var endDateInput by remember { mutableStateOf("08 Oct 2026") }
    var timeOffReason by remember { mutableStateOf("") }

    // Filtered shifts based on user selection
    val filteredShifts = remember(shifts, selectedFilterTab, employeeName) {
        when (selectedFilterTab) {
            1 -> shifts.filter { it.employeeName.contains(employeeName, ignoreCase = true) || employeeName.contains(it.employeeName, ignoreCase = true) }
            else -> shifts
        }
    }

    // Filtered leaves
    val myLeaves = remember(leaves, employeeName) {
        leaves.filter { it.username.contains(employeeName, ignoreCase = true) || employeeName.contains(it.username, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Shift Schedules",
                subMenuSubtitle = "Work Rotations & Milo Time-Off Sync",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showTimeOffDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Time Off", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // 1. Rotation Overview Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        text = "Q4 ROTATION · WEEK 41",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Automated Work Rotations",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Real-time Firebase scheduling & Milo AI time-off automation",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Box(modifier = Modifier.size(54.dp)) {
                                MiloCharacter(state = MiloState.IDLE, size = 54.dp, showStateBadge = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderLight)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats metrics summary row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            RotationStatItem(
                                title = "Scheduled Shifts",
                                value = "${shifts.size}",
                                icon = Icons.Default.CalendarMonth,
                                color = BrandBlue
                            )
                            RotationStatItem(
                                title = "Your Shifts",
                                value = "${shifts.count { it.employeeName.contains(employeeName, ignoreCase = true) }}",
                                icon = Icons.Default.Badge,
                                color = ElectricBlue
                            )
                            RotationStatItem(
                                title = "Time Off Ledger",
                                value = "${myLeaves.size}",
                                icon = Icons.Default.EventAvailable,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }

            // 2. Milo AI Quick Time-Off Assistant Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🦁", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Milo AI Quick Time-Off Assistant",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }

                            TextButton(onClick = onOpenAskMilo) {
                                Text("Ask Milo ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap any quick prompt chip below to request instant leave or time-off via Milo AI Copilot:",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                QuickMiloTimeOffChip("☀️ Request Time Off Tomorrow") {
                                    timeOffReason = "Needed urgent day off tomorrow"
                                    showTimeOffDialog = true
                                }
                            }
                            item {
                                QuickMiloTimeOffChip("🤒 Apply Sick Leave Friday") {
                                    selectedLeaveType = "Sick Leave"
                                    timeOffReason = "Health checkup and recovery"
                                    showTimeOffDialog = true
                                }
                            }
                            item {
                                QuickMiloTimeOffChip("🏖️ Apply Casual Leave Monday") {
                                    selectedLeaveType = "Casual Leave"
                                    timeOffReason = "Family function commitment"
                                    showTimeOffDialog = true
                                }
                            }
                            item {
                                QuickMiloTimeOffChip("🏠 Request Work From Home") {
                                    selectedLeaveType = "Work From Home"
                                    timeOffReason = "Remote work for internet installation"
                                    showTimeOffDialog = true
                                }
                            }
                        }
                    }
                }
            }

            // 3. Tab Selectors (All Shifts, My Shifts, Time-Off Ledger)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All Shifts (${shifts.size})", "My Shifts", "Time Off (${leaves.size})")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedFilterTab == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandBlue else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable {
                                    MiloHaptics.performButtonTap(context)
                                    selectedFilterTab = index
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Content List based on selected tab
            if (selectedFilterTab == 2) {
                // Time-Off Ledger Applications
                if (myLeaves.isEmpty()) {
                    item {
                        EmptyStateCard("No Time-Off Requests", "Tap 'Request Time Off' below or ask Milo AI to submit a leave request!")
                    }
                } else {
                    items(myLeaves) { leave ->
                        TimeOffApplicationCard(leave = leave)
                    }
                }
            } else {
                // Shift Rotation Schedule Cards
                if (filteredShifts.isEmpty()) {
                    item {
                        EmptyStateCard("No Shifts Found", "No work rotations currently scheduled for this view.")
                    }
                } else {
                    items(filteredShifts) { shift ->
                        ShiftScheduleCard(
                            shift = shift,
                            onRequestTimeOff = {
                                selectedLeaveType = "Casual Leave"
                                timeOffReason = "Time off for shift on ${shift.date}"
                                showTimeOffDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 5. Time-Off Application Modal Dialog
    if (showTimeOffDialog) {
        AlertDialog(
            onDismissRequest = { showTimeOffDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Request Time Off", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit your time-off or leave request. Milo will notify your manager and sync to Firebase instantly.", fontSize = 12.sp, color = TextSecondary)

                    Text("Leave Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    val leaveTypes = listOf("Casual Leave", "Sick Leave", "Earned Leave", "Work From Home")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(leaveTypes) { type ->
                            FilterChip(
                                selected = selectedLeaveType == type,
                                onClick = { selectedLeaveType = type },
                                label = { Text(type, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEFF6FF),
                                    selectedLabelColor = BrandBlue
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = startDateInput,
                        onValueChange = { startDateInput = it },
                        label = { Text("Start Date") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = timeOffReason,
                        onValueChange = { timeOffReason = it },
                        label = { Text("Reason for Time Off") },
                        placeholder = { Text("e.g. Urgent family matter or doctor appointment") },
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addLeave(
                            username = employeeName,
                            leaveType = selectedLeaveType,
                            startDate = startDateInput,
                            endDate = endDateInput,
                            totalDays = 1,
                            reason = timeOffReason.ifBlank { "Requested time off" },
                            onComplete = {
                                Toast.makeText(context, "✅ Time-Off request submitted and synced!", Toast.LENGTH_SHORT).show()
                                showTimeOffDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Request", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeOffDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RotationStatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }
        Text(title, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun QuickMiloTimeOffChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier
            .liftOnPress()
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandBlue,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ShiftScheduleCard(
    shift: ShiftScheduleEntity,
    onRequestTimeOff: () -> Unit
) {
    val context = LocalContext.current
    val shiftBadgeColor = when (shift.shiftType) {
        "Morning Shift" -> Color(0xFF2563EB)
        "Afternoon Shift" -> Color(0xFFD97706)
        "Night Shift" -> Color(0xFF7C3AED)
        "General Day Shift" -> Color(0xFF059669)
        else -> Color(0xFF475569)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .liftOnPress()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = shiftBadgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = shift.shiftCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = shiftBadgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = shift.shiftType,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${shift.dayOfWeek}, ${shift.date}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (shift.status) {
                        "In Progress" -> Color(0xFFDCFCE7)
                        "Scheduled" -> Color(0xFFEFF6FF)
                        else -> Color(0xFFF1F5F9)
                    }
                ) {
                    Text(
                        text = shift.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (shift.status) {
                            "In Progress" -> StatusGreen
                            "Scheduled" -> BrandBlue
                            else -> TextMuted
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${shift.startTime} - ${shift.endTime}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(shift.location, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(shift.employeeName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(" (${shift.employeeRole})", fontSize = 10.sp, color = TextSecondary)
                }

                OutlinedButton(
                    onClick = onRequestTimeOff,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Request Time-Off", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                }
            }
        }
    }
}

@Composable
private fun TimeOffApplicationCard(leave: LeaveApplicationEntity) {
    val statusColor = when (leave.status) {
        "Approved" -> StatusGreen
        "Rejected" -> StatusRed
        else -> Color(0xFFD97706)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(leave.leaveType, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("${leave.startDate} - ${leave.endDate} (${leave.totalDays} Day)", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = leave.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (leave.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reason: ${leave.reason}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
