package com.example.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*

@Composable
fun ProjectDetailScreen(
    projectId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onViewTasks: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val context = LocalContext.current
    val project = projects.find { it.id == projectId } ?: projects.firstOrNull()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Tasks", "Team", "Files", "Activity")

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = project?.name ?: "Project Details",
                subMenuSubtitle = "${project?.clientName ?: "Client"} · ${project?.progressPercent ?: 0}% Complete",
                onBack = onBack
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onViewTasks,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("View Tasks", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (project != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    project.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = StatusGreenBg
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(StatusGreen)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(project.status, color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(project.clientName, color = TextSecondary, fontSize = 13.sp)
                                Text("${project.progressPercent}% Complete", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { project.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BrandLightBlue,
                                trackColor = BorderLight
                            )
                        }
                    }
                }

                // Subview Tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) BrandBlue else TextSecondary
                                    )
                                }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> { // Overview Tab
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text("Project Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    ProjectDetailRow(Icons.Default.CalendarToday, "Start Date", project.startDate)
                                    Divider(color = BorderLight)
                                    ProjectDetailRow(Icons.Default.Event, "Deadline", project.deadline)
                                    Divider(color = BorderLight)
                                    ProjectDetailRow(Icons.Default.Flag, "Priority", project.priority, isBadge = true)
                                    Divider(color = BorderLight)
                                    ProjectDetailRow(Icons.Default.Person, "Project Manager", project.managerName)
                                    Divider(color = BorderLight)
                                    ProjectDetailRow(Icons.Default.Business, "Client", project.clientName)
                                    Divider(color = BorderLight)
                                    ProjectDetailRow(Icons.Default.Group, "Team", "${project.teamSize} Members")
                                }
                            }
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Milestones & Delivery", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Phase 1: Architecture & UI", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Completed on schedule", fontSize = 11.sp, color = StatusGreen)
                                        }
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                    }
                                    Divider(color = BorderLight)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Phase 2: Core Integration & QA", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("In Progress · Due 15 Oct", fontSize = 11.sp, color = BrandBlue)
                                        }
                                        CircularProgressIndicator(progress = { 0.65f }, modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp, color = BrandBlue)
                                    }
                                    Divider(color = BorderLight)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Phase 3: Production Deployment", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Scheduled · Due 30 Oct", fontSize = 11.sp, color = TextMuted)
                                        }
                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Tasks Tab
                        val projName = project?.name ?: ""
                        val projectTasks = allTasks.filter {
                            it.projectName.contains(projName, ignoreCase = true) ||
                            (projName.isNotBlank() && projName.contains(it.projectName, ignoreCase = true))
                        }

                        if (projectTasks.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.AssignmentLate, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No Specific Tasks Assigned", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Tap 'View Tasks' below to view and assign new tasks.", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            }
                        } else {
                            items(projectTasks) { task ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = task.isCompleted,
                                            onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                                            colors = CheckboxDefaults.colors(checkedColor = StatusGreen, checkmarkColor = Color.White)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                            Text("Assignee: ${task.assignee} · Due: ${task.dueDate}", fontSize = 11.sp, color = TextSecondary)
                                        }
                                        PriorityBadge(priority = task.priority)
                                    }
                                }
                            }
                        }
                    }
                    2 -> { // Team Tab
                        val manager = project?.managerName ?: "Rahul Sharma"
                        val teamMembers = listOf(
                            Triple(manager, "Project Lead & Manager", "+91 98765 43210"),
                            Triple("Vikram Patel", "Lead Android Developer", "+91 98112 34567"),
                            Triple("Priya Nair", "Senior UI/UX Designer", "+91 98223 45678"),
                            Triple("Arjun Mehta", "Backend Architect", "+91 98334 56789")
                        )
                        items(teamMembers) { (name, role, phone) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(name.take(2).uppercase(), fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 14.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        Text(role, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = {
                                            com.example.util.WhatsAppHelper.sendWhatsAppMessage(
                                                context = context,
                                                phoneNumber = phone,
                                                message = "Hi $name, regarding ${project?.name ?: "project"} update:"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF25D366))
                                    }
                                }
                            }
                        }
                    }
                    3 -> { // Files Tab
                        val files = listOf(
                            Triple("SOW & Architecture Doc.pdf", "2.4 MB · Updated 2 days ago", Icons.Default.Description),
                            Triple("Figma Design Specs.zip", "14.8 MB · Updated yesterday", Icons.Default.FolderZip),
                            Triple("Client Quotation & SOW.pdf", "1.1 MB · Approved", Icons.Default.ReceiptLong)
                        )
                        items(files) { (fileName, fileMeta, icon) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                        Text(fileMeta, fontSize = 11.sp, color = TextSecondary)
                                    }
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = TextMuted)
                                }
                            }
                        }
                    }
                    4 -> { // Activity Tab
                        val activities = listOf(
                            Triple("Phase 1 Deliverables Signed Off", "Client approved UI & DB schemas", "Yesterday at 04:30 PM"),
                            Triple("Sprint 3 Deployed to Staging", "Rahul pushed build v1.0.4", "17 Sep at 06:15 PM"),
                            Triple("Project Initiated & Team Assigned", "Created by MB Admin", "01 Sep at 09:00 AM")
                        )
                        items(activities) { (title, subtitle, time) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BrandBlue)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                        Text(subtitle, fontSize = 11.sp, color = TextSecondary)
                                        Text(time, fontSize = 10.sp, color = TextMuted)
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

@Composable
fun ProjectDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isBadge: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 13.sp, color = TextSecondary)
        }
        if (isBadge) {
            PriorityBadge(priority = value)
        } else {
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}
