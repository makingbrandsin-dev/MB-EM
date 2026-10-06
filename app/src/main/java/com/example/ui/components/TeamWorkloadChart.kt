package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.ProjectWorkloadItem
import com.example.ui.screens.TeamMemberWorkloadItem
import com.example.ui.screens.WorkloadSummaryStats
import com.example.ui.theme.*

enum class WorkloadViewMode {
    BY_PROJECT,
    BY_MEMBER
}

@Composable
fun TeamWorkloadChartCard(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val projectWorkloads by viewModel.projectWorkloadDistribution.collectAsState()
    val memberWorkloads by viewModel.teamMemberWorkloadDistribution.collectAsState()
    val summaryStats by viewModel.workloadSummaryStats.collectAsState()

    var viewMode by remember { mutableStateOf(WorkloadViewMode.BY_PROJECT) }
    var selectedProjectName by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("team_workload_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricBlueBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Workload Chart",
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Team Workload Visualizer",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Project-based workload & task analytics",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // View Mode Toggle (Projects vs Members)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewMode == WorkloadViewMode.BY_PROJECT) BrandBlue else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewMode = WorkloadViewMode.BY_PROJECT }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("workload_toggle_projects")
                        ) {
                            Text(
                                text = "Projects",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == WorkloadViewMode.BY_PROJECT) Color.White else TextSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewMode == WorkloadViewMode.BY_MEMBER) BrandBlue else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewMode = WorkloadViewMode.BY_MEMBER }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("workload_toggle_members")
                        ) {
                            Text(
                                text = "Members",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (viewMode == WorkloadViewMode.BY_MEMBER) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Workload Metric Summary Banner (Recharts KPI style)
            WorkloadKpiBanner(summaryStats = summaryStats)

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Legend
            WorkloadChartLegend()

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Chart Content
            AnimatedContent(
                targetState = viewMode,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "WorkloadViewModeTransition"
            ) { mode ->
                when (mode) {
                    WorkloadViewMode.BY_PROJECT -> {
                        if (projectWorkloads.isEmpty()) {
                            WorkloadEmptyState(
                                message = "No project workload data yet",
                                onAction = onNavigateToTasks
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.animateContentSize()
                            ) {
                                projectWorkloads.forEach { projectItem ->
                                    val isSelected = selectedProjectName == projectItem.projectName
                                    ProjectWorkloadBarItem(
                                        item = projectItem,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedProjectName = if (isSelected) null else projectItem.projectName
                                        }
                                    )
                                }
                            }
                        }
                    }
                    WorkloadViewMode.BY_MEMBER -> {
                        if (memberWorkloads.isEmpty()) {
                            WorkloadEmptyState(
                                message = "No team member workload data yet",
                                onAction = onNavigateToTasks
                            )
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.animateContentSize()
                            ) {
                                memberWorkloads.forEach { memberItem ->
                                    TeamMemberWorkloadRow(item = memberItem)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Busiest: ${summaryStats.busiestProject}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                TextButton(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.testTag("workload_view_all_tasks_btn")
                ) {
                    Text(
                        text = "Manage Tasks →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkloadKpiBanner(summaryStats: WorkloadSummaryStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8FAFC))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        KpiItem(
            count = "${summaryStats.totalTasks}",
            label = "Total Tasks",
            color = BrandBlue
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(Color(0xFFCBD5E1))
        )
        KpiItem(
            count = "${summaryStats.activeTasks}",
            label = "Active",
            color = Color(0xFFD97706)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(Color(0xFFCBD5E1))
        )
        KpiItem(
            count = "${summaryStats.completedTasks}",
            label = "Done",
            color = Color(0xFF10B981)
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(28.dp)
                .background(Color(0xFFCBD5E1))
        )
        KpiItem(
            count = "${summaryStats.highPriorityTasks}",
            label = "Critical",
            color = Color(0xFFEF4444)
        )
    }
}

@Composable
private fun KpiItem(
    count: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun WorkloadChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendIndicator(color = Color(0xFF705C30), label = "Pending")
        LegendIndicator(color = Color(0xFFD97706), label = "In Progress")
        LegendIndicator(color = Color(0xFF10B981), label = "Completed")
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ProjectWorkloadBarItem(
    item: ProjectWorkloadItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val animatedWorkloadPct by animateFloatAsState(
        targetValue = item.workloadPercentage,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "animatedWorkloadPct"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFF0F9FF) else Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) BrandBlue else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("project_workload_bar_${item.projectName}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Project Name & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.projectName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.highPriorityTasks > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusRedBg
                        ) {
                            Text(
                                text = "⚠️ ${item.highPriorityTasks} High",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = "${item.totalTasks} Tasks",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Custom D3/Recharts-inspired Stacked Workload Bar
            val total = item.totalTasks.coerceAtLeast(1)
            val pendingRatio = item.pendingTasks.toFloat() / total.toFloat()
            val inProgressRatio = item.inProgressTasks.toFloat() / total.toFloat()
            val completedRatio = item.completedTasks.toFloat() / total.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())

                // Background track
                drawRoundRect(
                    color = Color(0xFFE2E8F0),
                    size = size,
                    cornerRadius = cornerRadius
                )

                var currentX = 0f

                // Completed segment (Green)
                if (completedRatio > 0f) {
                    val width = canvasWidth * completedRatio
                    drawRoundRect(
                        color = Color(0xFF10B981),
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, canvasHeight),
                        cornerRadius = cornerRadius
                    )
                    currentX += width
                }

                // In Progress segment (Amber)
                if (inProgressRatio > 0f) {
                    val width = canvasWidth * inProgressRatio
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, canvasHeight),
                        cornerRadius = cornerRadius
                    )
                    currentX += width
                }

                // Pending segment (Dark Bronze)
                if (pendingRatio > 0f) {
                    val width = canvasWidth * pendingRatio
                    drawRoundRect(
                        color = Color(0xFF705C30),
                        topLeft = Offset(currentX, 0f),
                        size = Size(width, canvasHeight),
                        cornerRadius = cornerRadius
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Percentage and Distribution Meta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.workloadPercentage.toInt()}% of total workload",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.completionPercentage.toInt()}% completed",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.completionPercentage >= 80f) Color(0xFF10B981) else BrandBlue
                )
            }

            // Expanded Breakdown Details
            AnimatedVisibility(visible = isSelected) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Assigned Team (${item.assignees.size}):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${item.pendingTasks} pending · ${item.completedTasks} done",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (item.assignees.isEmpty()) {
                        Text(
                            text = "No direct assignees yet",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item.assignees.take(4).forEach { assignee ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFBFDBFE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(BrandBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = assignee.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = assignee,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BrandBlue
                                        )
                                    }
                                }
                            }
                            if (item.assignees.size > 4) {
                                Text(
                                    text = "+${item.assignees.size - 4}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMemberWorkloadRow(item: TeamMemberWorkloadItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.memberName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.memberName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = if (item.assignedProjects.isNotEmpty()) {
                            item.assignedProjects.joinToString(", ")
                        } else "General Tasks",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (item.pendingTasks > 3) Color(0xFFFEF3C7) else Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = "${item.pendingTasks} Pending",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.pendingTasks > 3) Color(0xFFD97706) else BrandBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "${item.completedTasks} Done",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${item.workloadPercentage.toInt()}% team capacity",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun WorkloadEmptyState(
    message: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.TaskAlt,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("Create First Task", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
