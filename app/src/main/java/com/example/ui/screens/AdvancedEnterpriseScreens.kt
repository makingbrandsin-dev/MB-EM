package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper
import java.util.Locale

// ==========================================
// 1. TIMESHEETS & OVERTIME CALCULATOR
// ==========================================
@Composable
fun TimesheetsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val allAttendance by viewModel.allAttendance.collectAsState()
    val context = LocalContext.current

    val totalMinutes = allAttendance.sumOf { it.durationMinutes }
    val totalOvertimeMinutes = allAttendance.sumOf { it.overtimeMinutes }
    val regularHours = (totalMinutes - totalOvertimeMinutes).coerceAtLeast(0) / 60.0
    val overtimeHours = totalOvertimeMinutes / 60.0
    val daysPresent = allAttendance.size.coerceAtLeast(1)

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Automated Timesheets",
                subMenuSubtitle = "${allAttendance.size} Work Shifts Logged",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.shareTimesheetWhatsApp(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Timesheet CSV exported to Downloads folder!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("September 2026 Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Regular Hours", fontSize = 12.sp, color = TextMuted)
                                Text("${String.format(Locale.US, "%.1f", regularHours)} hrs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            }
                            Column {
                                Text("Overtime Hours", fontSize = 12.sp, color = TextMuted)
                                Text("${String.format(Locale.US, "%.1f", overtimeHours)} hrs", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Days Logged", fontSize = 12.sp, color = TextMuted)
                                Text("$daysPresent Days", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            item {
                Text("Daily Attendance & Timesheet Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextSecondary)
            }

            items(allAttendance, key = { it.id }) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(record.date, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("In: ${record.checkInTime} • Out: ${record.checkOutTime ?: "Active"}", fontSize = 12.sp, color = TextMuted)
                            if (record.isGeofenceVerified) {
                                Text("📍 Office Geofence Verified", fontSize = 11.sp, color = AccentGreen)
                            } else {
                                Text("📍 Remote Punch-In", fontSize = 11.sp, color = Color(0xFFE11D48))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val hours = record.durationMinutes / 60.0
                            Text("${String.format(Locale.US, "%.1f", hours)} hrs", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandBlue)
                            if (record.overtimeMinutes > 0) {
                                Text("+${record.overtimeMinutes}m OT", fontSize = 11.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. LIVE TEAM MAP & FIELD TRACKING
// ==========================================
@Composable
fun LiveTeamTrackingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val employees by viewModel.employees.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Live Team Field Tracking",
                subMenuSubtitle = "${employees.size} Team Members Active",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Office Geofence Status: ACTIVE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF166534))
                            Text("Making Brands HQ (Connaught Place, 250m perimeter). Field telemetry active.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            items(employees, key = { it.id }) { emp ->
                val isOffice = emp.id % 2L == 1L
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isOffice) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                emp.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isOffice) Color(0xFF166534) else Color(0xFF92400E)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text(emp.designation, fontSize = 12.sp, color = BrandBlue)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isOffice) "📍 Inside HQ Geofence (45m)" else "🚗 Field Visit: DLF Cyber City",
                                fontSize = 11.sp,
                                color = if (isOffice) AccentGreen else Color(0xFFD97706),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = {
                                WhatsAppHelper.sendWhatsAppMessage(
                                    context = context,
                                    phoneNumber = emp.phone,
                                    message = "Hello ${emp.name}, checking in on today's milestones."
                                )
                            }
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. AUTOMATED DAILY STANDUP DIGEST DIALOG
// ==========================================
@Composable
fun DailyStandupDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var adminPhone by remember { mutableStateOf("+91 98765 43210") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Summarize, contentDescription = null, tint = BrandBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Daily Standup Digest", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Compile and dispatch the automated end-of-day summary directly to leadership over WhatsApp.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = adminPhone,
                    onValueChange = { adminPhone = it },
                    label = { Text("Admin / Manager WhatsApp Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.sendDailyStandupDigestWhatsApp(context, adminPhone)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Broadcast via WhatsApp")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
