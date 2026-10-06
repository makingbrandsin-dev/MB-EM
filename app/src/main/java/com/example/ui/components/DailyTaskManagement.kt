package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics

/**
 * Visual styling and metadata configuration for Task Categories
 */
data class TaskCategoryConfig(
    val name: String,
    val icon: ImageVector,
    val tagBgColor: Color,
    val tagTextColor: Color,
    val leftAccentColor: Color,
    val cardBorderColor: Color
)

fun getCategoryConfig(category: String): TaskCategoryConfig {
    return when (category.trim().lowercase()) {
        "work" -> TaskCategoryConfig(
            name = "Work",
            icon = Icons.Default.Work,
            tagBgColor = Color(0xFFC8E8D0), // Soft Mint Sage
            tagTextColor = Color(0xFF4A7C59), // Deep Emerald Green
            leftAccentColor = Color(0xFF705C30), // Warm Luxury Bronze
            cardBorderColor = Color(0xFFE8DEC8)
        )
        "personal" -> TaskCategoryConfig(
            name = "Personal",
            icon = Icons.Default.Person,
            tagBgColor = Color(0xFFF5F3FF), // Soft Purple / Lavender
            tagTextColor = Color(0xFF7C3AED),
            leftAccentColor = Color(0xFF8B5CF6),
            cardBorderColor = Color(0xFFDDD6FE)
        )
        "urgent" -> TaskCategoryConfig(
            name = "Urgent",
            icon = Icons.Default.LocalFireDepartment,
            tagBgColor = Color(0xFFFFF1F2), // Soft Crimson / Rose
            tagTextColor = Color(0xFFE11D48),
            leftAccentColor = Color(0xFFF43F5E),
            cardBorderColor = Color(0xFFFECDD3)
        )
        "meeting" -> TaskCategoryConfig(
            name = "Meeting",
            icon = Icons.Default.Groups,
            tagBgColor = Color(0xFFFFFBEB), // Soft Amber
            tagTextColor = Color(0xFFD97706),
            leftAccentColor = Color(0xFFF59E0B),
            cardBorderColor = Color(0xFFFDE68A)
        )
        "review" -> TaskCategoryConfig(
            name = "Review",
            icon = Icons.Default.RateReview,
            tagBgColor = Color(0xFFECFDF5), // Soft Emerald
            tagTextColor = Color(0xFF059669),
            leftAccentColor = Color(0xFF10B981),
            cardBorderColor = Color(0xFFA7F3D0)
        )
        else -> TaskCategoryConfig(
            name = category.ifBlank { "General" },
            icon = Icons.Default.Label,
            tagBgColor = Color(0xFFF8FAFC),
            tagTextColor = Color(0xFF475569),
            leftAccentColor = Color(0xFF64748B),
            cardBorderColor = Color(0xFFE2E8F0)
        )
    }
}

val standardCategories = listOf("Work", "Personal", "Urgent", "Meeting", "Review")

@Composable
fun DailyTaskManagementSection(
    viewModel: MainViewModel,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val tasks by viewModel.tasks.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Work", "Personal", "Urgent", "Meeting", "Review"
    var selectedStatusFilter by remember { mutableStateOf("All") } // "All", "Pending", "Completed"
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }

    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted || it.status.equals("Completed", ignoreCase = true) }
    val pendingCount = totalCount - completedCount
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    // Filter by both Category and Status
    val filteredTasks = tasks.filter { task ->
        val matchesCategory = if (selectedCategoryFilter == "All") {
            true
        } else {
            task.category.equals(selectedCategoryFilter, ignoreCase = true)
        }

        val matchesStatus = when (selectedStatusFilter) {
            "Pending" -> !task.isCompleted && !task.status.equals("Completed", ignoreCase = true)
            "Completed" -> task.isCompleted || task.status.equals("Completed", ignoreCase = true)
            else -> true
        }

        matchesCategory && matchesStatus
    }.sortedByDescending { it.id }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_task_management_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Assigned Tasks",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF4A7C59),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Firestore",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF705C30)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (totalCount > 0) "$pendingCount pending · $completedCount completed (${(progress * 100).toInt()}% done)" else "No tasks assigned from Firestore yet",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Add Task Button
                Button(
                    onClick = { showAddTaskDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("daily_task_add_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. Progress Bar
            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = StatusGreen,
                    trackColor = Color(0xFFF1F5F9),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Category Color-Coded Filter Chips (Horizontally Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "All Categories" Chip
                val isAllCatSelected = selectedCategoryFilter == "All"
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAllCatSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                    border = if (isAllCatSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .clickable {
                            MiloHaptics.performReactionTick(context, hapticFeedback)
                            selectedCategoryFilter = "All"
                        }
                        .testTag("category_filter_All")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All ($totalCount)",
                            fontSize = 11.sp,
                            fontWeight = if (isAllCatSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isAllCatSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }

                // Color-coded category chips
                standardCategories.forEach { catName ->
                    val config = getCategoryConfig(catName)
                    val isSelected = selectedCategoryFilter.equals(catName, ignoreCase = true)
                    val catCount = tasks.count { it.category.equals(catName, ignoreCase = true) }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) config.leftAccentColor else config.tagBgColor,
                        border = BorderStroke(1.dp, if (isSelected) config.leftAccentColor else config.cardBorderColor),
                        modifier = Modifier
                            .clickable {
                                MiloHaptics.performReactionTick(context, hapticFeedback)
                                selectedCategoryFilter = catName
                            }
                            .testTag("category_filter_$catName")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = config.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else config.tagTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$catName ($catCount)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Bold,
                                color = if (isSelected) Color.White else config.tagTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Status Filter Tabs (All / Pending / Completed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("All", "All Status", Color(0xFF64748B)),
                    Triple("Pending", "Pending ($pendingCount)", StatusOrange),
                    Triple("Completed", "Completed ($completedCount)", StatusGreen)
                ).forEach { (key, label, activeColor) ->
                    val isSelected = selectedStatusFilter == key
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, activeColor) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedStatusFilter = key }
                            .testTag("status_filter_$key")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) activeColor else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Tasks List
            if (filteredTasks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (selectedStatusFilter == "Completed") Icons.Default.CheckCircleOutline else Icons.Default.TaskAlt,
                            contentDescription = null,
                            tint = if (selectedStatusFilter == "Completed") StatusGreen else Color(0xFF94A3B8),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when {
                                selectedCategoryFilter != "All" && selectedStatusFilter != "All" ->
                                    "No $selectedStatusFilter tasks in $selectedCategoryFilter"
                                selectedCategoryFilter != "All" ->
                                    "No tasks in $selectedCategoryFilter category"
                                selectedStatusFilter == "Completed" ->
                                    "No completed tasks yet"
                                selectedStatusFilter == "Pending" ->
                                    "Awesome! All tasks are completed 🎉"
                                else -> "No tasks found"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Task' to add a task with custom category",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredTasks.forEach { task ->
                        DailyTaskItemRow(
                            task = task,
                            onToggle = {
                                MiloHaptics.performClockInFeedback(context, hapticFeedback)
                                viewModel.toggleTaskCompletion(task)
                            },
                            onEdit = { taskToEdit = task },
                            onStatusChange = { newStatus ->
                                MiloHaptics.performReactionTick(context, hapticFeedback)
                                viewModel.updateTaskStatus(task, newStatus)
                            },
                            onDelete = {
                                MiloHaptics.performActionWarning(context, hapticFeedback)
                                viewModel.deleteTask(task)
                            }
                        )
                    }
                }
            }

            // 6. Footer / Open Full Task Board Link
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToTasks() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Open Full Task Board",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Navigate to full tasks",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        AddDailyTaskDialog(
            initialCategory = if (selectedCategoryFilter != "All") selectedCategoryFilter else "Work",
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { title, project, priority, dueDate, category, timeNeeded ->
                viewModel.addTask(title, project, priority, dueDate, category, timeNeeded)
                showAddTaskDialog = false
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { task ->
        EditDailyTaskDialog(
            task = task,
            onDismiss = { taskToEdit = null },
            onSaveTask = { updatedTask ->
                viewModel.updateTask(updatedTask)
                taskToEdit = null
            }
        )
    }
}

/**
 * Color-Coded Daily Task Item Card with Category Indicator & Badges
 */
@Composable
fun DailyTaskItemRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    val isDone = task.isCompleted || task.status.equals("Completed", ignoreCase = true)
    val categoryConfig = getCategoryConfig(task.category)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDone) Color(0xFFF8FAFC) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDone) Color(0xFFE2E8F0) else categoryConfig.cardBorderColor.copy(alpha = 0.8f)
        ),
        shadowElevation = if (isDone) 0.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left color-coded vertical accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (isDone) Color(0xFFCBD5E1) else categoryConfig.leftAccentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Top Row: Checkbox, Title, and Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Checkbox & Task Title
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Category visual accent icon
                        Surface(
                            shape = CircleShape,
                            color = categoryConfig.tagBgColor,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = categoryConfig.icon,
                                    contentDescription = null,
                                    tint = categoryConfig.leftAccentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDone) Color(0xFF94A3B8) else Color(0xFF0F172A),
                                textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Badges Row: Category Color-Coded Pill + Time Needed + Project + Due Date
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                // 🎨 Category Color-Coded Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDone) Color(0xFFF1F5F9) else categoryConfig.tagBgColor,
                                    border = BorderStroke(1.dp, if (isDone) Color(0xFFE2E8F0) else categoryConfig.cardBorderColor)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = categoryConfig.icon,
                                            contentDescription = null,
                                            tint = if (isDone) Color(0xFF94A3B8) else categoryConfig.tagTextColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = categoryConfig.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDone) Color(0xFF94A3B8) else categoryConfig.tagTextColor
                                        )
                                    }
                                }

                                // ⏱️ Time Needed for Project Badge
                                if (task.estimatedTimeNeeded.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFF475569),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = task.estimatedTimeNeeded,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }

                                // Project Badge
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = task.projectName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                // 👤 Assignee Tag
                                if (task.assignee.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = task.assignee,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                }

                                // Due Date
                                Text(
                                    text = "· ${task.dueDate}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Action Buttons (Priority, Edit, Status Dropdown)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        PriorityBadge(priority = task.priority)

                        // Edit Button
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("daily_task_edit_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit task",
                                tint = ElectricBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Status Dropdown & Menu
                        Box {
                            IconButton(
                                onClick = { showStatusMenu = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("daily_task_more_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                Text(
                                    "Change Status:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                                listOf("Backlog", "In Progress", "In Review", "Completed").forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                statusOption,
                                                fontWeight = if (task.status.equals(statusOption, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal,
                                                color = if (task.status.equals(statusOption, ignoreCase = true)) ElectricBlue else Color(0xFF0F172A),
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            onStatusChange(statusOption)
                                            showStatusMenu = false
                                        }
                                    )
                                }
                                Divider(color = Color(0xFFE2E8F0))
                                DropdownMenuItem(
                                    text = {
                                        Text("Delete Task", fontWeight = FontWeight.Bold, color = StatusRed, fontSize = 13.sp)
                                    },
                                    onClick = {
                                        onDelete()
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = StatusRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Row: Status Tag & Helper Note
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (task.status.lowercase()) {
                            "completed" -> StatusGreenBg
                            "in progress" -> StatusBlueBg
                            "in review" -> StatusPurpleBg
                            else -> StatusOrangeBg
                        }
                    ) {
                        Text(
                            text = "Status: ${task.status}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (task.status.lowercase()) {
                                "completed" -> StatusGreen
                                "in progress" -> StatusBlue
                                "in review" -> StatusPurple
                                else -> StatusOrange
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = if (isDone) "Completed" else "Tap checkbox when done",
                        fontSize = 10.sp,
                        color = if (isDone) StatusGreen else Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Add Daily Task Dialog with Color-Coded Category Picker
 */
@Composable
fun AddDailyTaskDialog(
    initialCategory: String = "Work",
    onDismiss: () -> Unit,
    onAddTask: (title: String, project: String, priority: String, dueDate: String, category: String, timeNeeded: String) -> Unit
) {
    val currentDateFormatted = remember {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var project by remember { mutableStateOf("Website Revamp") }
    var priority by remember { mutableStateOf("High") }
    var dueDate by remember { mutableStateOf("Today ($currentDateFormatted)") }
    var estimatedTimeNeeded by remember { mutableStateOf("4 Hours") }

    val priorities = listOf("High", "Medium", "Low")
    val projectOptions = listOf("Website Revamp", "MB Traker App", "Client Acquisition", "Operations", "General")
    val timeOptions = listOf("1 Hour", "2 Hours", "4 Hours", "1 Day", "2 Days", "1 Week")

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDBEAFE),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AddCircle,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Add Daily Task",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Plan your daily priority & estimated time",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Task Title
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Task Title *") },
                                placeholder = { Text("e.g. Prepare client deck") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_task_title_input")
                            )

                            // 🎨 Category Selection (Color-coded chips)
                            Column {
                                Text(
                                    "Category (Color-Coded):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    standardCategories.forEach { catName ->
                                        val config = getCategoryConfig(catName)
                                        val isSel = selectedCategory.equals(catName, ignoreCase = true)

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) config.leftAccentColor else config.tagBgColor,
                                            border = BorderStroke(1.dp, if (isSel) config.leftAccentColor else config.cardBorderColor),
                                            modifier = Modifier
                                                .clickable { selectedCategory = catName }
                                                .testTag("add_task_category_$catName")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = config.icon,
                                                    contentDescription = null,
                                                    tint = if (isSel) Color.White else config.tagTextColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = catName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else config.tagTextColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ⏱️ Time Needed for Project Completion
                            Column {
                                Text(
                                    "Time Needed for Completion:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    timeOptions.forEach { tOpt ->
                                        val isSel = estimatedTimeNeeded.equals(tOpt, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) ElectricBlue else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSel) ElectricBlue else Color(0xFFE2E8F0)),
                                            modifier = Modifier.clickable { estimatedTimeNeeded = tOpt }
                                        ) {
                                            Text(
                                                text = tOpt,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = estimatedTimeNeeded,
                                    onValueChange = { estimatedTimeNeeded = it },
                                    label = { Text("Estimated Duration (e.g. 4 Hours)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Project Field
                            OutlinedTextField(
                                value = project,
                                onValueChange = { project = it },
                                label = { Text("Project / Context") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_task_project_input")
                            )

                            // Quick Project Suggestions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                projectOptions.take(3).forEach { option ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (project == option) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                        modifier = Modifier.clickable { project = option }
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 10.sp,
                                            fontWeight = if (project == option) FontWeight.Bold else FontWeight.Medium,
                                            color = if (project == option) ElectricBlue else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Priority Selection
                            Column {
                                Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    priorities.forEach { p ->
                                        val isSel = priority == p
                                        val pColor = when (p) {
                                            "High" -> StatusRed
                                            "Medium" -> StatusOrange
                                            else -> StatusGreen
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) pColor else Color(0xFFF1F5F9),
                                            border = if (isSel) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { priority = p }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = p,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Due Date Field
                            OutlinedTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                label = { Text("Due Date") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Pinned Bottom Footer Actions
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
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onAddTask(title.trim(), project.trim(), priority, dueDate.trim(), selectedCategory, estimatedTimeNeeded.trim())
                                }
                            },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("add_task_confirm_button")
                        ) {
                            Text("Add Task", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Edit Daily Task Dialog with Color-Coded Category Picker
 */
@Composable
fun EditDailyTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSaveTask: (TaskEntity) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var category by remember { mutableStateOf(task.category) }
    var project by remember { mutableStateOf(task.projectName) }
    var priority by remember { mutableStateOf(task.priority) }
    var dueDate by remember { mutableStateOf(task.dueDate) }
    var status by remember { mutableStateOf(task.status) }
    var estimatedTimeNeeded by remember { mutableStateOf(task.estimatedTimeNeeded) }

    val priorities = listOf("High", "Medium", "Low")
    val statuses = listOf("Backlog", "In Progress", "In Review", "Completed")
    val timeOptions = listOf("1 Hour", "2 Hours", "4 Hours", "1 Day", "2 Days", "1 Week")

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEDE9FE),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = VibrantPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Edit Daily Task",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    "Modify status, duration & project",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }
                }

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Task Title
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("Task Title *") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("edit_task_title_input")
                            )

                            // Status Selection
                            Column {
                                Text("Status:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    statuses.forEach { s ->
                                        val isSel = status.equals(s, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) ElectricBlue else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { status = s }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = s,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else Color(0xFF475569)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ⏱️ Time Needed for Project Completion
                            Column {
                                Text(
                                    "Time Needed for Completion:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    timeOptions.forEach { tOpt ->
                                        val isSel = estimatedTimeNeeded.equals(tOpt, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) ElectricBlue else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSel) ElectricBlue else Color(0xFFE2E8F0)),
                                            modifier = Modifier.clickable { estimatedTimeNeeded = tOpt }
                                        ) {
                                            Text(
                                                text = tOpt,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF334155),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = estimatedTimeNeeded,
                                    onValueChange = { estimatedTimeNeeded = it },
                                    label = { Text("Estimated Duration") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Project Field
                            OutlinedTextField(
                                value = project,
                                onValueChange = { project = it },
                                label = { Text("Project / Category") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Priority Selection
                            Column {
                                Text("Priority:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    priorities.forEach { p ->
                                        val isSel = priority.equals(p, ignoreCase = true)
                                        val pColor = when (p) {
                                            "High" -> StatusRed
                                            "Medium" -> StatusOrange
                                            else -> StatusGreen
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSel) pColor else Color(0xFFF1F5F9),
                                            border = if (isSel) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { priority = p }
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = p,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Due Date Field
                            OutlinedTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                label = { Text("Due Date") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Pinned Bottom Footer Actions
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
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val isComp = status.equals("Completed", ignoreCase = true)
                                    val updated = task.copy(
                                        title = title.trim(),
                                        category = category,
                                        projectName = project.trim(),
                                        priority = priority,
                                        dueDate = dueDate.trim(),
                                        status = status,
                                        isCompleted = isComp,
                                        estimatedTimeNeeded = estimatedTimeNeeded.trim()
                                    )
                                    onSaveTask(updated)
                                }
                            },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("edit_task_save_button")
                        ) {
                            Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
