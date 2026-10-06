package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.theme.*

@Composable
fun KanbanBoardView(
    tasks: List<TaskEntity>,
    onUpdateStatus: (TaskEntity, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val backlogTasks = tasks.filter { it.status.equals("Backlog", ignoreCase = true) }
    val inProgressTasks = tasks.filter { it.status.equals("In Progress", ignoreCase = true) }
    val completedTasks = tasks.filter { it.isCompleted || it.status.equals("Completed", ignoreCase = true) }

    val horizontalScrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(horizontalScrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Column 1: Backlog
        KanbanColumn(
            title = "Backlog",
            tasks = backlogTasks,
            columnColor = Color(0xFFFEF3C7),
            titleColor = Color(0xFF92400E),
            nextStatus = "In Progress",
            nextStatusLabel = "Start →",
            onMoveTask = { task -> onUpdateStatus(task, "In Progress") }
        )

        // Column 2: In Progress
        KanbanColumn(
            title = "In Progress",
            tasks = inProgressTasks,
            columnColor = Color(0xFFDBEAFE),
            titleColor = BrandBlue,
            nextStatus = "Completed",
            nextStatusLabel = "Complete ✓",
            onMoveTask = { task -> onUpdateStatus(task, "Completed") }
        )

        // Column 3: Completed
        KanbanColumn(
            title = "Completed",
            tasks = completedTasks,
            columnColor = Color(0xFFDCFCE7),
            titleColor = Color(0xFF166534),
            nextStatus = null,
            nextStatusLabel = "",
            onMoveTask = {}
        )
    }
}

@Composable
fun KanbanColumn(
    title: String,
    tasks: List<TaskEntity>,
    columnColor: Color,
    titleColor: Color,
    nextStatus: String?,
    nextStatusLabel: String,
    onMoveTask: (TaskEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Column Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(titleColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                }

                Surface(
                    color = columnColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "${tasks.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks", color = TextMuted, fontSize = 12.sp)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tasks.forEach { task ->
                        KanbanTaskCard(
                            task = task,
                            nextStatusLabel = nextStatusLabel,
                            onAdvance = { onMoveTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KanbanTaskCard(
    task: TaskEntity,
    nextStatusLabel: String,
    onAdvance: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityBadge(priority = task.priority)

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        task.category,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                task.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(task.projectName, fontSize = 11.sp, color = BrandBlue)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            task.assignee.take(1).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(task.dueDate, fontSize = 11.sp, color = TextMuted)
                }

                if (nextStatusLabel.isNotBlank()) {
                    Surface(
                        color = BrandBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.clickable { onAdvance() }
                    ) {
                        Text(
                            nextStatusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
