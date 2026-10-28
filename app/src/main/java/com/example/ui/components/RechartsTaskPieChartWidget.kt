package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.MiloHaptics
import kotlin.math.atan2

enum class TaskPieSliceType {
    COMPLETED,
    PENDING
}

data class TaskPieSlice(
    val type: TaskPieSliceType,
    val label: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val gradientColors: List<Color>,
    val startAngle: Float,
    val sweepAngle: Float
)

/**
 * Visual Recharts-style interactive Pie / Donut Chart component for displaying
 * the employee's pending vs completed task distribution.
 *
 * Implements Recharts interactive aesthetics:
 * - Animated SVG-like sweep angle rendering
 * - Active slice highlight / hover elevation effect
 * - Center KPI metric display (Completion Rate %)
 * - Recharts legend and tooltip pill
 * - Quick breakdown indicators for pending vs finished tasks
 */
@Composable
fun RechartsTaskPieChartWidget(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTasks: () -> Unit = {}
) {
    val tasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current

    val completedCount = remember(tasks) { tasks.count { it.isCompleted || it.status.equals("Completed", ignoreCase = true) } }
    val totalCount = remember(tasks) { tasks.size }
    val pendingCount = remember(completedCount, totalCount) { (totalCount - completedCount).coerceAtLeast(0) }

    val completionRate = remember(completedCount, totalCount) {
        if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()) * 100f else 0f
    }

    // Interactive selected slice state
    var selectedSlice by remember { mutableStateOf<TaskPieSliceType?>(null) }

    // Recharts sweep-in animation
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(completedCount, pendingCount) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    val completedColor = Color(0xFF10B981) // Emerald Green
    val pendingColor = Color(0xFF3B82F6) // Recharts Blue
    val pendingWarmColor = Color(0xFFF59E0B) // Amber for backlog urgency

    // Build slice geometries (startAngle, sweepAngle)
    val slices = remember(completedCount, pendingCount, totalCount) {
        if (totalCount == 0) {
            emptyList()
        } else {
            val completedSweep = (completedCount.toFloat() / totalCount.toFloat()) * 360f
            val pendingSweep = 360f - completedSweep

            listOf(
                TaskPieSlice(
                    type = TaskPieSliceType.COMPLETED,
                    label = "Completed",
                    count = completedCount,
                    percentage = if (totalCount > 0) (completedCount.toFloat() / totalCount) * 100f else 0f,
                    color = completedColor,
                    gradientColors = listOf(Color(0xFF34D399), Color(0xFF059669)),
                    startAngle = -90f,
                    sweepAngle = completedSweep
                ),
                TaskPieSlice(
                    type = TaskPieSliceType.PENDING,
                    label = "Pending",
                    count = pendingCount,
                    percentage = if (totalCount > 0) (pendingCount.toFloat() / totalCount) * 100f else 0f,
                    color = pendingColor,
                    gradientColors = listOf(Color(0xFF60A5FA), Color(0xFF2563EB)),
                    startAngle = -90f + completedSweep,
                    sweepAngle = pendingSweep
                )
            )
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recharts_task_pie_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title + Recharts Badge + Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Task Distribution",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "Recharts",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Pending vs Completed Tasks Ratio",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                TextButton(
                    onClick = onNavigateToTasks,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Tasks",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (totalCount == 0) {
                // Empty state if no tasks exist
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No tasks assigned yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Interactive Donut Chart + Center Label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Pie Canvas Box with Recharts tap interaction
                    Box(
                        modifier = Modifier
                            .size(175.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(slices) {
                                    detectTapGestures { offset ->
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val dx = offset.x - centerX
                                        val dy = offset.y - centerY
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        if (angle < -90f) angle += 360f

                                        val clickedSlice = slices.find { slice ->
                                            val normStart = slice.startAngle
                                            val normEnd = slice.startAngle + slice.sweepAngle
                                            angle in normStart..normEnd
                                        }

                                        selectedSlice = if (selectedSlice == clickedSlice?.type) null else clickedSlice?.type
                                        MiloHaptics.performButtonTap(context)
                                    }
                                }
                        ) {
                            val strokeWidth = 32.dp.toPx()
                            val chartSize = size.minDimension - strokeWidth
                            val topLeft = Offset((size.width - chartSize) / 2f, (size.height - chartSize) / 2f)
                            val arcSize = Size(chartSize, chartSize)

                            // Background guide track
                            drawArc(
                                color = Color(0xFFF1F5F9),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )

                            // Draw slices with animation progress
                            slices.forEach { slice ->
                                val isHovered = selectedSlice == slice.type
                                val animatedSweep = slice.sweepAngle * animationProgress.value

                                if (animatedSweep > 0.5f) {
                                    val brush = Brush.sweepGradient(
                                        colors = slice.gradientColors,
                                        center = Offset(size.width / 2f, size.height / 2f)
                                    )

                                    drawArc(
                                        brush = brush,
                                        startAngle = slice.startAngle,
                                        sweepAngle = (animatedSweep - 2f).coerceAtLeast(1f), // Small slice gap
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(
                                            width = if (isHovered) strokeWidth + 6.dp.toPx() else strokeWidth
                                        )
                                    )
                                }
                            }
                        }

                        // Center Metric Label (Recharts center text)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${completionRate.toInt()}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    completionRate >= 75f -> Color(0xFF059669)
                                    completionRate >= 40f -> Color(0xFF2563EB)
                                    else -> Color(0xFFD97706)
                                }
                            )
                            Text(
                                text = "Done Rate",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                        }
                    }

                    // Legend and Detail Cards
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Completed Slice Legend
                        RechartsSliceLegendPill(
                            title = "Completed",
                            count = completedCount,
                            total = totalCount,
                            color = completedColor,
                            icon = Icons.Default.CheckCircle,
                            isSelected = selectedSlice == TaskPieSliceType.COMPLETED,
                            onClick = {
                                selectedSlice = if (selectedSlice == TaskPieSliceType.COMPLETED) null else TaskPieSliceType.COMPLETED
                                MiloHaptics.performButtonTap(context)
                            }
                        )

                        // Pending Slice Legend
                        RechartsSliceLegendPill(
                            title = "Pending",
                            count = pendingCount,
                            total = totalCount,
                            color = pendingColor,
                            icon = Icons.Default.PendingActions,
                            isSelected = selectedSlice == TaskPieSliceType.PENDING,
                            onClick = {
                                selectedSlice = if (selectedSlice == TaskPieSliceType.PENDING) null else TaskPieSliceType.PENDING
                                MiloHaptics.performButtonTap(context)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Recharts Tooltip / Active Selection Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (selectedSlice) {
                                            TaskPieSliceType.COMPLETED -> completedColor
                                            TaskPieSliceType.PENDING -> pendingColor
                                            null -> Color(0xFF64748B)
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (selectedSlice) {
                                    TaskPieSliceType.COMPLETED -> "Filtered: $completedCount of $totalCount completed"
                                    TaskPieSliceType.PENDING -> "Filtered: $pendingCount remaining tasks"
                                    null -> "Tap slice or legend to highlight metric"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "$totalCount Total",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RechartsSliceLegendPill(
    title: String,
    count: Int,
    total: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val percentage = if (total > 0) ((count.toFloat() / total.toFloat()) * 100).roundToInt() else 0

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$count tasks",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
