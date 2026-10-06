package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProjectEntity
import com.example.data.model.TaskEntity
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*

enum class OverviewTabFilter {
    ALL,
    PROJECTS,
    TASKS
}

/**
 * Main Dashboard Overview Component displaying ongoing projects and active tasks
 * customized for the currently logged-in user.
 */
@Composable
fun OngoingProjectsTasksOverviewCard(
    viewModel: MainViewModel,
    onNavigateToProjects: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loggedInUser by viewModel.currentEmployeeName.collectAsState()
    val allProjects by viewModel.projects.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()

    var selectedFilter by remember { mutableStateOf(OverviewTabFilter.ALL) }

    // Filter projects relevant to logged-in user or active in organization
    val ongoingProjects = remember(allProjects, loggedInUser) {
        val userProjects = allProjects.filter { proj ->
            proj.status.equals("Active", ignoreCase = true) ||
            proj.status.equals("In Progress", ignoreCase = true) ||
            proj.managerName.contains(loggedInUser, ignoreCase = true)
        }
        if (userProjects.isNotEmpty()) userProjects else allProjects.filter { !it.status.equals("Completed", ignoreCase = true) }
    }

    // Filter active tasks assigned to logged-in user or active in organization
    val activeTasks = remember(allTasks, loggedInUser) {
        val userActiveTasks = allTasks.filter { task ->
            !task.isCompleted && !task.status.equals("Completed", ignoreCase = true) &&
            (task.assignee.contains(loggedInUser, ignoreCase = true) || task.assignee.isBlank())
        }
        if (userActiveTasks.isNotEmpty()) userActiveTasks else allTasks.filter { !it.isCompleted }
    }

    val totalOngoingCount = ongoingProjects.size
    val activeTasksCount = activeTasks.size
    val highPriorityTasksCount = activeTasks.count { it.priority.equals("High", ignoreCase = true) }

    val avgProjectProgress = remember(ongoingProjects) {
        if (ongoingProjects.isEmpty()) 0
        else ongoingProjects.map { it.progressPercent }.average().toInt()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("ongoing_projects_tasks_overview_card")
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEFF6FF),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = "Overview Dashboard",
                                tint = BrandBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Ongoing Projects & Tasks",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Work Overview • $loggedInUser",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onNavigateToProjects,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderSpecial,
                            contentDescription = "Projects",
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToTasks,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = "Tasks",
                            tint = StatusOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Filter Tabs (All / Projects / Tasks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OverviewTabFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val label = when (filter) {
                        OverviewTabFilter.ALL -> "Overview"
                        OverviewTabFilter.PROJECTS -> "Projects ($totalOngoingCount)"
                        OverviewTabFilter.TASKS -> "Active Tasks ($activeTasksCount)"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BrandBlue else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Summary Metric Pill Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary Metric 1: Ongoing Projects Progress
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToProjects() }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ongoing Projects", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$totalOngoingCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("active", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { avgProjectProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = BrandBlue,
                            trackColor = Color(0xFFDBEAFE)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$avgProjectProgress% Avg Complete", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                    }
                }

                // Summary Metric 2: Active Tasks
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToTasks() }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Tasks", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusOrange)
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$activeTasksCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("pending", fontSize = 10.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "$highPriorityTasksCount High Priority",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content Display (All / Projects / Tasks)
            AnimatedContent(
                targetState = selectedFilter,
                label = "OverviewTabTransition"
            ) { filterState ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (filterState) {
                        OverviewTabFilter.ALL -> {
                            // Section 1: Ongoing Projects Carousel / Quick Cards
                            if (ongoingProjects.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active Projects (${ongoingProjects.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "See All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue,
                                        modifier = Modifier.clickable { onNavigateToProjects() }
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(ongoingProjects.take(4), key = { it.id }) { project ->
                                        DashboardProjectCard(
                                            project = project,
                                            onClick = onNavigateToProjects
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Section 2: Active User Tasks List
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Assigned Active Tasks (${activeTasks.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "View Tasks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusOrange,
                                    modifier = Modifier.clickable { onNavigateToTasks() }
                                )
                            }

                            if (activeTasks.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("All tasks completed! Great job.", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activeTasks.take(3).forEach { task ->
                                        DashboardActiveTaskRow(
                                            task = task,
                                            onToggleComplete = {
                                                viewModel.toggleTaskCompletion(task)
                                                Toast.makeText(context, "Task '${task.title}' updated!", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OverviewTabFilter.PROJECTS -> {
                            if (ongoingProjects.isEmpty()) {
                                Text("No ongoing projects currently assigned.", fontSize = 12.sp, color = TextSecondary)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ongoingProjects.forEach { project ->
                                        FullDashboardProjectCard(
                                            project = project,
                                            onClick = onNavigateToProjects
                                        )
                                    }
                                }
                            }
                        }

                        OverviewTabFilter.TASKS -> {
                            if (activeTasks.isEmpty()) {
                                Text("No active pending tasks.", fontSize = 12.sp, color = TextSecondary)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    activeTasks.forEach { task ->
                                        DashboardActiveTaskRow(
                                            task = task,
                                            onToggleComplete = {
                                                viewModel.toggleTaskCompletion(task)
                                                Toast.makeText(context, "Task '${task.title}' updated!", Toast.LENGTH_SHORT).show()
                                            }
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
}

/**
 * Compact Horizontal Project Card for Carousel View
 */
@Composable
private fun DashboardProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (project.priority) {
                        "High" -> Color(0xFFFEE2E2)
                        "Medium" -> Color(0xFFFEF3C7)
                        else -> Color(0xFFEFF6FF)
                    }
                ) {
                    Text(
                        text = project.priority,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (project.priority) {
                            "High" -> StatusRed
                            "Medium" -> Color(0xFFB45309)
                            else -> BrandBlue
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${project.progressPercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandBlue
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = project.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Client: ${project.clientName}",
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { project.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandBlue,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("${project.completedTasks}/${project.totalTasks} Tasks", fontSize = 10.sp, color = TextSecondary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(project.deadline, fontSize = 10.sp, color = TextSecondary)
                }
            }
        }
    }
}

/**
 * Full Width Detailed Project Card
 */
@Composable
private fun FullDashboardProjectCard(
    project: ProjectEntity,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(project.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Client: ${project.clientName} • Manager: ${project.managerName}", fontSize = 11.sp, color = TextSecondary)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF)
                ) {
                    Text(
                        "${project.progressPercent}% DONE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { project.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandBlue,
                trackColor = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${project.completedTasks} of ${project.totalTasks} Tasks Completed", fontSize = 10.sp, color = TextSecondary)
                Text("Deadline: ${project.deadline}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = StatusOrange)
            }
        }
    }
}

/**
 * Single Active Task Item Row with Checkbox Toggle
 */
@Composable
private fun DashboardActiveTaskRow(
    task: TaskEntity,
    onToggleComplete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(checkedColor = StatusGreen, checkmarkColor = Color.White)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = task.projectName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue
                    )

                    Text("•", fontSize = 10.sp, color = TextMuted)

                    Text(
                        text = "Due: ${task.dueDate}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = when (task.priority) {
                    "High" -> Color(0xFFFEE2E2)
                    "Medium" -> Color(0xFFFEF3C7)
                    else -> Color(0xFFEFF6FF)
                }
            ) {
                Text(
                    text = task.priority,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (task.priority) {
                        "High" -> StatusRed
                        "Medium" -> Color(0xFFB45309)
                        else -> BrandBlue
                    },
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
