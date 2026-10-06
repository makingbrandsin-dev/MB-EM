package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DailyTrendDataPoint
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.ThirtyDayTrendsState
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlin.math.roundToInt

enum class RechartsViewMetric {
    COMBINED,
    ATTENDANCE,
    TASK_COMPLETION
}

enum class RechartsTimeframe(val days: Int, val label: String) {
    LAST_7_DAYS(7, "7 Days"),
    LAST_14_DAYS(14, "14 Days"),
    LAST_30_DAYS(30, "30 Days")
}

/**
 * Recharts-inspired interactive dashboard widget for visualizing:
 * 1. Daily Attendance Trends (hours logged, presence status, overtime)
 * 2. Task Completion Rates (% completed, total vs finished tasks)
 * across the last 30 days with CartesianGrid, dual axes, smooth Bezier curves,
 * interactive scrubber tooltip, and KPI summary cards.
 */
@Composable
fun RechartsTrendDashboardWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val trendsState by viewModel.thirtyDayTrendsState.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    var selectedMetric by remember { mutableStateOf(RechartsViewMetric.COMBINED) }
    var selectedTimeframe by remember { mutableStateOf(RechartsTimeframe.LAST_30_DAYS) }
    var showAttendanceSeries by remember { mutableStateOf(true) }
    var showTasksSeries by remember { mutableStateOf(true) }

    // Active hovered / scrubbed data point
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Filter points based on timeframe
    val filteredPoints = remember(trendsState.points, selectedTimeframe) {
        if (trendsState.points.isEmpty()) emptyList()
        else trendsState.points.takeLast(selectedTimeframe.days)
    }

    // Default to last point if none selected
    val activePoint = remember(filteredPoints, selectedIndex) {
        if (filteredPoints.isEmpty()) null
        else if (selectedIndex in filteredPoints.indices) filteredPoints[selectedIndex]
        else filteredPoints.lastOrNull()
    }

    // Animation progress for smooth line & area drawing
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedTimeframe, selectedMetric) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_trend_dashboard_widget")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 🏷️ Top Header with Recharts Icon & Timeframe Filter Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF10B981))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Recharts Analytics",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "30-Day Trends & Analytics",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEEF2FF),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE))
                            ) {
                                Text(
                                    text = "Recharts",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4F46E5),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Daily Attendance vs. Task Completion Rates",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Timeframe Chips (7D, 14D, 30D)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RechartsTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandDarkBlue else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                if (selectedTimeframe != tf) {
                                    MiloHaptics.performButtonClick(context, hapticFeedback)
                                    selectedTimeframe = tf
                                    selectedIndex = -1
                                }
                            }
                        ) {
                            Text(
                                text = tf.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🔀 Metric Switcher Tabs (Combined, Attendance, Tasks)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricTabButton(
                        title = "Combined (Dual Axis)",
                        icon = Icons.Default.Insights,
                        isSelected = selectedMetric == RechartsViewMetric.COMBINED,
                        accentColor = Color(0xFF4F46E5),
                        onClick = {
                            MiloHaptics.performButtonClick(context, hapticFeedback)
                            selectedMetric = RechartsViewMetric.COMBINED
                            showAttendanceSeries = true
                            showTasksSeries = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTabButton(
                        title = "Attendance Hours",
                        icon = Icons.Default.AccessTime,
                        isSelected = selectedMetric == RechartsViewMetric.ATTENDANCE,
                        accentColor = Color(0xFF10B981),
                        onClick = {
                            MiloHaptics.performButtonClick(context, hapticFeedback)
                            selectedMetric = RechartsViewMetric.ATTENDANCE
                            showAttendanceSeries = true
                            showTasksSeries = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTabButton(
                        title = "Task Rate (%)",
                        icon = Icons.Default.CheckCircle,
                        isSelected = selectedMetric == RechartsViewMetric.TASK_COMPLETION,
                        accentColor = Color(0xFF6366F1),
                        onClick = {
                            MiloHaptics.performButtonClick(context, hapticFeedback)
                            selectedMetric = RechartsViewMetric.TASK_COMPLETION
                            showAttendanceSeries = false
                            showTasksSeries = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 📊 30-Day Aggregated Summary KPI Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KpiPillCard(
                    title = "Avg. Attendance",
                    value = "${trendsState.summary.averageDailyAttendanceHours}h",
                    subtitle = "${trendsState.summary.attendanceRatePercentage}% Presence",
                    tintColor = Color(0xFF10B981),
                    bgColor = Color(0xFFECFDF5),
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
                KpiPillCard(
                    title = "Task Completion",
                    value = "${trendsState.summary.averageTaskCompletionRate}%",
                    subtitle = "${trendsState.summary.totalTasksCompleted}/${trendsState.summary.totalTasksAssigned} Tasks",
                    tintColor = Color(0xFF6366F1),
                    bgColor = Color(0xFFEEF2FF),
                    icon = Icons.Default.TaskAlt,
                    modifier = Modifier.weight(1f)
                )
                KpiPillCard(
                    title = "Peak Day",
                    value = trendsState.summary.highestCompletionDay,
                    subtitle = "${trendsState.summary.bestAttendanceStreak}d Streak",
                    tintColor = Color(0xFFD97706),
                    bgColor = Color(0xFFFFFBEB),
                    icon = Icons.Default.ElectricBolt,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 🎯 Recharts Interactive Scrubber Tooltip (<Tooltip />)
            activePoint?.let { point ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${point.dayOfWeek}, ${point.displayDay}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showAttendanceSeries) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${point.attendanceHours}h (${point.attendanceStatus})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }
                            if (showTasksSeries) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF818CF8))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${point.taskCompletionRate.roundToInt()}% (${point.tasksCompleted}/${point.tasksTotal} tasks)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFA5B4FC)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 📈 Main Recharts Canvas Viewport (<ResponsiveContainer>, <CartesianGrid>, <AreaChart>, <LineChart>)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .pointerInput(filteredPoints) {
                        detectTapGestures { offset ->
                            if (filteredPoints.isNotEmpty()) {
                                val chartWidth = size.width - 70.dp.toPx()
                                val startX = 35.dp.toPx()
                                val relativeX = (offset.x - startX).coerceIn(0f, chartWidth)
                                val stepX = chartWidth / (filteredPoints.size - 1).coerceAtLeast(1)
                                val idx = (relativeX / stepX).roundToInt().coerceIn(0, filteredPoints.size - 1)
                                if (selectedIndex != idx) {
                                    MiloHaptics.performReactionTick(context, hapticFeedback)
                                    selectedIndex = idx
                                }
                            }
                        }
                    }
                    .pointerInput(filteredPoints) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            if (filteredPoints.isNotEmpty()) {
                                val chartWidth = size.width - 70.dp.toPx()
                                val startX = 35.dp.toPx()
                                val relativeX = (change.position.x - startX).coerceIn(0f, chartWidth)
                                val stepX = chartWidth / (filteredPoints.size - 1).coerceAtLeast(1)
                                val idx = (relativeX / stepX).roundToInt().coerceIn(0, filteredPoints.size - 1)
                                if (selectedIndex != idx) {
                                    MiloHaptics.performReactionTick(context, hapticFeedback)
                                    selectedIndex = idx
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (filteredPoints.isEmpty()) return@Canvas

                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val paddingLeft = 36.dp.toPx()
                    val paddingRight = 36.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 28.dp.toPx()

                    val chartWidth = canvasWidth - paddingLeft - paddingRight
                    val chartHeight = canvasHeight - paddingTop - paddingBottom

                    // 1. Cartesian Grid (<CartesianGrid strokeDasharray="3 3" />)
                    val gridLines = 4
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                    for (i in 0..gridLines) {
                        val y = paddingTop + (chartHeight / gridLines) * i
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(paddingLeft, y),
                            end = Offset(canvasWidth - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    val pointsCount = filteredPoints.size
                    val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

                    // Normalized bounds:
                    // Attendance: 0 to 10 hours
                    val maxAttendanceHours = 10f
                    // Tasks: 0 to 100%
                    val maxTaskRate = 100f

                    val attendanceCoords = mutableListOf<Offset>()
                    val taskCoords = mutableListOf<Offset>()

                    for (i in filteredPoints.indices) {
                        val pt = filteredPoints[i]
                        val x = paddingLeft + i * stepX

                        // Attendance Y coordinate
                        val attRatio = (pt.attendanceHours / maxAttendanceHours).coerceIn(0f, 1f)
                        val attY = paddingTop + chartHeight * (1f - attRatio * animProgress.value)
                        attendanceCoords.add(Offset(x, attY))

                        // Task Completion Y coordinate
                        val taskRatio = (pt.taskCompletionRate / maxTaskRate).coerceIn(0f, 1f)
                        val taskY = paddingTop + chartHeight * (1f - taskRatio * animProgress.value)
                        taskCoords.add(Offset(x, taskY))
                    }

                    // 2. Draw Task Completion Bars / Volume (if Task series active)
                    if (showTasksSeries && selectedMetric == RechartsViewMetric.TASK_COMPLETION) {
                        for (i in filteredPoints.indices) {
                            val pt = filteredPoints[i]
                            val x = paddingLeft + i * stepX
                            val barW = (stepX * 0.45f).coerceIn(4.dp.toPx(), 14.dp.toPx())
                            val barRatio = (pt.tasksCompleted.toFloat() / 7f).coerceIn(0f, 1f) * animProgress.value
                            val barH = chartHeight * barRatio
                            val barTop = paddingTop + chartHeight - barH

                            drawRoundRect(
                                color = Color(0xFFC7D2FE).copy(alpha = 0.5f),
                                topLeft = Offset(x - barW / 2, barTop),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }
                    }

                    // 3. Draw Attendance Trend Curve with Gradient Area Fill
                    if (showAttendanceSeries && attendanceCoords.size > 1) {
                        val attPath = Path()
                        val attAreaPath = Path()

                        attPath.moveTo(attendanceCoords[0].x, attendanceCoords[0].y)
                        attAreaPath.moveTo(attendanceCoords[0].x, paddingTop + chartHeight)
                        attAreaPath.lineTo(attendanceCoords[0].x, attendanceCoords[0].y)

                        for (i in 0 until attendanceCoords.size - 1) {
                            val p0 = attendanceCoords[i]
                            val p1 = attendanceCoords[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            attPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            attAreaPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }

                        attAreaPath.lineTo(attendanceCoords.last().x, paddingTop + chartHeight)
                        attAreaPath.close()

                        // Gradient Area Fill (<Area />)
                        drawPath(
                            path = attAreaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.28f),
                                    Color(0xFF10B981).copy(alpha = 0.02f)
                                ),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Outer Glowing Line Stroke
                        drawPath(
                            path = attPath,
                            color = Color(0xFF10B981),
                            style = Stroke(
                                width = 3.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 4. Draw Task Completion Rate Curve with Gradient Area Fill
                    if (showTasksSeries && taskCoords.size > 1) {
                        val taskPath = Path()
                        val taskAreaPath = Path()

                        taskPath.moveTo(taskCoords[0].x, taskCoords[0].y)
                        taskAreaPath.moveTo(taskCoords[0].x, paddingTop + chartHeight)
                        taskAreaPath.lineTo(taskCoords[0].x, taskCoords[0].y)

                        for (i in 0 until taskCoords.size - 1) {
                            val p0 = taskCoords[i]
                            val p1 = taskCoords[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            taskPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            taskAreaPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }

                        taskAreaPath.lineTo(taskCoords.last().x, paddingTop + chartHeight)
                        taskAreaPath.close()

                        if (selectedMetric != RechartsViewMetric.COMBINED) {
                            drawPath(
                                path = taskAreaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1).copy(alpha = 0.24f),
                                        Color(0xFF6366F1).copy(alpha = 0.01f)
                                    ),
                                    startY = paddingTop,
                                    endY = paddingTop + chartHeight
                                )
                            )
                        }

                        drawPath(
                            path = taskPath,
                            color = Color(0xFF6366F1),
                            style = Stroke(
                                width = 3.2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 5. Draw Interactive Crosshair & Target Rings for Active Point
                    val curIdx = if (selectedIndex in filteredPoints.indices) selectedIndex else (filteredPoints.size - 1)
                    if (curIdx in filteredPoints.indices) {
                        val targetX = paddingLeft + curIdx * stepX

                        // Vertical dashed crosshair line
                        drawLine(
                            color = Color(0xFF475569).copy(alpha = 0.6f),
                            start = Offset(targetX, paddingTop),
                            end = Offset(targetX, paddingTop + chartHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )

                        // Attendance Active Ring
                        if (showAttendanceSeries && curIdx in attendanceCoords.indices) {
                            val attPos = attendanceCoords[curIdx]
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = attPos
                            )
                            drawCircle(
                                color = Color(0xFF10B981),
                                radius = 6.dp.toPx(),
                                center = attPos,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF10B981).copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = attPos
                            )
                        }

                        // Task Active Ring
                        if (showTasksSeries && curIdx in taskCoords.indices) {
                            val taskPos = taskCoords[curIdx]
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = taskPos
                            )
                            drawCircle(
                                color = Color(0xFF6366F1),
                                radius = 6.dp.toPx(),
                                center = taskPos,
                                style = Stroke(width = 3.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF6366F1).copy(alpha = 0.35f),
                                radius = 10.dp.toPx(),
                                center = taskPos
                            )
                        }
                    }
                }

                // Left Y-Axis Labels (Attendance Hours)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp, top = 14.dp, bottom = 26.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    Text("7.5h", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                    Text("5h", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                    Text("2.5h", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF059669))
                    Text("0h", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                }

                // Right Y-Axis Labels (Task Completion Rate %)
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp, top = 14.dp, bottom = 26.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text("100%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                    Text("75%", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4F46E5))
                    Text("50%", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4F46E5))
                    Text("25%", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4F46E5))
                    Text("0%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                }

                // Bottom X-Axis Date Labels
                if (filteredPoints.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 36.dp, end = 36.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val step = (filteredPoints.size / 5).coerceAtLeast(1)
                        val sampledIndices = (0 until filteredPoints.size step step).take(5)
                        sampledIndices.forEach { idx ->
                            Text(
                                text = filteredPoints[idx].displayDay,
                                fontSize = 9.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🏷️ Interactive Recharts Legend (<Legend />) with Toggleable Series
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attendance Legend Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showAttendanceSeries) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showAttendanceSeries) Color(0xFF86EFAC) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.clickable {
                        MiloHaptics.performButtonClick(context, hapticFeedback)
                        showAttendanceSeries = !showAttendanceSeries
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (showAttendanceSeries) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Attendance (Hours)",
                            fontSize = 11.sp,
                            fontWeight = if (showAttendanceSeries) FontWeight.Bold else FontWeight.Normal,
                            color = if (showAttendanceSeries) Color(0xFF065F46) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Task Completion Legend Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showTasksSeries) Color(0xFFEEF2FF) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (showTasksSeries) Color(0xFFA5B4FC) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.clickable {
                        MiloHaptics.performButtonClick(context, hapticFeedback)
                        showTasksSeries = !showTasksSeries
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (showTasksSeries) Color(0xFF6366F1) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Task Completion Rate (%)",
                            fontSize = 11.sp,
                            fontWeight = if (showTasksSeries) FontWeight.Bold else FontWeight.Normal,
                            color = if (showTasksSeries) Color(0xFF3730A3) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🚀 Quick Deep-Dive Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onNavigateToAttendance,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = BrandDarkBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Attendance Logs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandDarkBlue
                    )
                }

                TextButton(
                    onClick = onNavigateToTasks,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Manage Tasks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color.White else Color.Transparent,
        shadowElevation = if (isSelected) 1.5.dp else 0.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else Color(0xFF64748B),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TextPrimary else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KpiPillCard(
    title: String,
    value: String,
    subtitle: String,
    tintColor: Color,
    bgColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF475569)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tintColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
