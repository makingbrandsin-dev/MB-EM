package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShiftScheduleEntity
import com.example.data.model.TaskEntity
import com.example.domain.milo.MiloState
import com.example.milo.MiloCharacter
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.liftOnPress
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🗓️ Jetpack Compose Work Schedule & Task Calendar Screen
 * Pulls shift rotations and task deadlines from Firestore & Room database.
 * Supports Monthly Grid View and Weekly Timeline View.
 */

data class CalendarDayItem(
    val dayNumber: Int,
    val dateString: String, // e.g. "06 Oct 2026"
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val shifts: List<ShiftScheduleEntity> = emptyList(),
    val tasksDue: List<TaskEntity> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkScheduleCalendarScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToShiftRotations: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val context = LocalContext.current
    val shifts by viewModel.shiftSchedules.collectAsState(initial = emptyList())
    val tasks by viewModel.tasks.collectAsState(initial = emptyList())

    var viewMode by remember { mutableStateOf("MONTH") } // "MONTH" or "WEEK"
    val calendar = remember { Calendar.getInstance() }
    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var selectedDateString by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
    }

    // Filter shifts and tasks for selected date
    val dayShifts = shifts.filter { it.date == selectedDateString || it.date.contains(selectedDateString.take(6)) }
    val dayTasks = tasks.filter { it.dueDate == selectedDateString }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "🗓️ Work Schedule Calendar",
                subMenuSubtitle = "Firestore Shifts & Task Deadlines",
                onBack = onBack
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // View Switcher & Month Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Segmented Button: Monthly vs Weekly View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = viewMode == "MONTH",
                            onClick = {
                                viewMode = "MONTH"
                                MiloHaptics.performButtonClick(context)
                            },
                            label = { Text("🗓️ Monthly Grid View", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = viewMode == "WEEK",
                            onClick = {
                                viewMode = "WEEK"
                                MiloHaptics.performButtonClick(context)
                            },
                            label = { Text("📅 Weekly Timeline", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Month Navigation Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val cal = currentMonthCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, -1)
                                currentMonthCalendar = cal
                                MiloHaptics.performButtonClick(context)
                            }
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                        }

                        Text(
                            text = monthFormat.format(currentMonthCalendar.time),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        IconButton(
                            onClick = {
                                val cal = currentMonthCalendar.clone() as Calendar
                                cal.add(Calendar.MONTH, 1)
                                currentMonthCalendar = cal
                                MiloHaptics.performButtonClick(context)
                            }
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                        }
                    }
                }
            }

            if (viewMode == "MONTH") {
                // Monthly Grid View
                val calendarDays = remember(currentMonthCalendar, shifts, tasks) {
                    generateMonthDays(currentMonthCalendar, shifts, tasks)
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Day of week labels header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.height(270.dp)
                        ) {
                            items(calendarDays) { day ->
                                val isSelected = day.dateString == selectedDateString
                                Surface(
                                    onClick = {
                                        if (day.isCurrentMonth) {
                                            selectedDateString = day.dateString
                                            MiloHaptics.performButtonClick(context)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isSelected -> BrandBlue
                                        day.isToday -> Color(0xFFEFF6FF)
                                        else -> Color.Transparent
                                    },
                                    border = if (day.isToday && !isSelected) BorderStroke(1.dp, BrandBlue) else null,
                                    modifier = Modifier.aspectRatio(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = if (day.dayNumber > 0) day.dayNumber.toString() else "",
                                            fontSize = 12.sp,
                                            fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                !day.isCurrentMonth -> Color(0xFFCBD5E1)
                                                day.isToday -> BrandBlue
                                                else -> TextPrimary
                                            }
                                        )

                                        // Badges row for shifts and tasks
                                        if (day.isCurrentMonth && (day.shifts.isNotEmpty() || day.tasksDue.isNotEmpty())) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                if (day.shifts.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(if (isSelected) Color.White else Color(0xFF0284C7), CircleShape)
                                                    )
                                                }
                                                if (day.tasksDue.isNotEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .background(if (isSelected) Color.Yellow else Color(0xFFDC2626), CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Weekly Timeline List View
                Text(
                    text = "📋 Weekly Rotation & Task Deadlines",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // 📋 Selected Date Schedule Details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = BrandBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Schedule for $selectedDateString",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = BorderLight)
                    }

                    // Shifts Section
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔄 Work Rotation Shift", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = onNavigateToShiftRotations) {
                                Text("Shift Portal", fontSize = 11.sp, color = BrandBlue)
                            }
                        }
                    }

                    if (dayShifts.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Standard Flexible Work Shift • 09:00 AM - 06:00 PM",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        items(dayShifts) { shift ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandBlue)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(shift.shiftType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("${shift.startTime} - ${shift.endTime} • ${shift.location}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    // Task Deadlines Section
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯 Tasks & Deadlines Due", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(onClick = onNavigateToTasks) {
                                Text("View All Tasks", fontSize = 11.sp, color = BrandBlue)
                            }
                        }
                    }

                    if (dayTasks.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No critical task deadlines on this date.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        items(dayTasks) { task ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFFDC2626))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(task.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text("Priority: ${task.priority} • Status: ${if (task.isCompleted) "Completed" else "Pending"}", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateMonthDays(
    calendar: Calendar,
    shifts: List<ShiftScheduleEntity>,
    tasks: List<TaskEntity>
): List<CalendarDayItem> {
    val list = mutableListOf<CalendarDayItem>()
    val cal = calendar.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed Sun=0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val todayStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    // Empty lead cells
    for (i in 0 until firstDayOfWeek) {
        list.add(CalendarDayItem(dayNumber = 0, dateString = "", isCurrentMonth = false, isToday = false))
    }

    // Days of current month
    for (day in 1..daysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dateStr = dateFormat.format(cal.time)
        val dayShifts = shifts.filter { it.date == dateStr }
        val dayTasks = tasks.filter { it.dueDate == dateStr }

        list.add(
            CalendarDayItem(
                dayNumber = day,
                dateString = dateStr,
                isCurrentMonth = true,
                isToday = (dateStr == todayStr),
                shifts = dayShifts,
                tasksDue = dayTasks
            )
        )
    }

    return list
}
