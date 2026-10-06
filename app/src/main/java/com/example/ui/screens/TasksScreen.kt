package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.KanbanBoardView
import com.example.ui.components.MiloAssistantDialog
import com.example.ui.components.PriorityBadge
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.getCategoryConfig
import com.example.ui.components.standardCategories
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCrm: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val tasks by viewModel.tasks.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val activeEmployeeNames = remember(allAttendance) {
        allAttendance.filter { it.isWorking }.map { it.employeeName }.toSet()
    }
    var selectedScopeTab by remember { mutableIntStateOf(0) } // 0: All, 1: My Tasks
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var isKanbanMode by remember { mutableStateOf(false) }

    val statusFilters = listOf("All", "Backlog", "In Progress", "Completed")

    val filteredTasks = tasks.filter { task ->
        val matchesCategory = if (selectedCategoryFilter == "All") true else task.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesStatus = when (selectedStatusFilter) {
            "Backlog" -> task.status.equals("Backlog", ignoreCase = true)
            "In Progress" -> task.status.equals("In Progress", ignoreCase = true)
            "Completed" -> task.isCompleted || task.status.equals("Completed", ignoreCase = true)
            else -> true
        }
        val matchesScope = if (selectedScopeTab == 1) task.assignee == "Rahul Sharma" else true
        matchesCategory && matchesStatus && matchesScope
    }.sortedByDescending { it.id }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Tasks & Workflow",
                subMenuSubtitle = "${filteredTasks.size} Tasks · ${tasks.count { it.isCompleted }} Done",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(onClick = { isKanbanMode = !isKanbanMode }) {
                        Icon(
                            if (isKanbanMode) Icons.Default.ViewList else Icons.Default.ViewKanban,
                            contentDescription = "Toggle Kanban",
                            tint = BrandBlue
                        )
                    }
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Task",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        val isRefreshing by viewModel.isRefreshing.collectAsState()

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshAll() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "tasks",
                    onNavigateToCrm = onNavigateToCrm,
                    onNavigateToTasks = { },
                    onNavigateToAttendance = onNavigateToAttendance,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            // Scope Tabs & View Mode Switcher
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabRow(
                        selectedTabIndex = selectedScopeTab,
                        containerColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedScopeTab == 0,
                            onClick = { selectedScopeTab = 0 },
                            text = { Text("All", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedScopeTab == 1,
                            onClick = { selectedScopeTab = 1 },
                            text = { Text("My Tasks", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.clickable { isKanbanMode = !isKanbanMode }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isKanbanMode) Icons.Default.ViewList else Icons.Default.ViewKanban,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isKanbanMode) "List View" else "Kanban",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(statusFilters) { filter ->
                        val isSel = selectedStatusFilter == filter
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStatusFilter = filter },
                            label = {
                                Text(
                                    filter,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else Color(0xFF0F172A)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE2E8F0),
                                labelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        val isAll = selectedCategoryFilter == "All"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isAll) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isAll) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedCategoryFilter = "All" }
                        ) {
                            Text(
                                "All Categories",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAll) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(standardCategories) { catName ->
                        val config = getCategoryConfig(catName)
                        val isSel = selectedCategoryFilter.equals(catName, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) config.leftAccentColor else config.tagBgColor,
                            border = BorderStroke(1.dp, if (isSel) config.leftAccentColor else config.cardBorderColor),
                            modifier = Modifier.clickable { selectedCategoryFilter = catName }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    config.icon,
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else config.tagTextColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    catName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else config.tagTextColor
                                )
                            }
                        }
                    }
                }
            }

            // Tasks List or Kanban Board
            if (isKanbanMode) {
                item {
                    KanbanBoardView(
                        tasks = filteredTasks,
                        onUpdateStatus = { task, newStatus ->
                            viewModel.updateTaskStatus(task, newStatus)
                        }
                    )
                }
            } else {
                items(filteredTasks) { task ->
                    TaskCardItem(
                        task = task,
                        employees = employees,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) },
                        onReassign = { newAssignee -> viewModel.reassignTask(task, newAssignee) },
                        onDelete = { viewModel.deleteTask(task) }
                    )
                }
            }
        }
        }
    }

    if (showAddTaskDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf(if (selectedCategoryFilter != "All") selectedCategoryFilter else "Work") }
        var newProject by remember { mutableStateOf("Website Revamp") }
        var newPriority by remember { mutableStateOf("High") }
        var newDueDate by remember { mutableStateOf("30 Sep 2026") }
        var newEstimatedTime by remember { mutableStateOf("4 Hours") }
        var selectedAssignee by remember { mutableStateOf("Rahul Sharma") }
        var filterActiveOnly by remember { mutableStateOf(false) }
        var selectedDependencyTaskId by remember { mutableStateOf<Long?>(null) }
        var selectedDependencyTitle by remember { mutableStateOf<String?>(null) }

        val candidateEmployees = remember(employees, filterActiveOnly, activeEmployeeNames) {
            if (filterActiveOnly) {
                employees.filter { it.name in activeEmployeeNames || it.status == com.example.data.model.EmployeeStatus.ACTIVE }
            } else {
                employees
            }
        }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ElectricBlueBg,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create New Task", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Task Title *") },
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Assignee Selection Section with Active Staff Filter
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Assign To Employee:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (!filterActiveOnly) ElectricBlueBg else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { filterActiveOnly = false }
                                ) {
                                    Text(
                                        "All (${employees.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!filterActiveOnly) ElectricBlue else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (filterActiveOnly) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { filterActiveOnly = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(shape = CircleShape, color = Color(0xFF16A34A), modifier = Modifier.size(5.dp)) {}
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            "Active Clocked-In",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (filterActiveOnly) Color(0xFF15803D) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            candidateEmployees.forEach { emp ->
                                val isSelected = selectedAssignee.equals(emp.name, ignoreCase = true)
                                val isActive = emp.name in activeEmployeeNames || emp.status == com.example.data.model.EmployeeStatus.ACTIVE
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ElectricBlue else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isSelected) ElectricBlue else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable { selectedAssignee = emp.name }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isActive) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(7.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Column {
                                            Text(
                                                emp.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF0F172A)
                                            )
                                            Text(
                                                emp.designation,
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Category Selection Chips
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
                                val isSel = newCategory.equals(catName, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) config.leftAccentColor else config.tagBgColor,
                                    border = BorderStroke(1.dp, if (isSel) config.leftAccentColor else config.cardBorderColor),
                                    modifier = Modifier.clickable { newCategory = catName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            config.icon,
                                            contentDescription = null,
                                            tint = if (isSel) Color.White else config.tagTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            catName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.White else config.tagTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Priority Selection Chips
                    Column {
                        Text(
                            "Priority Level:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("High", "Medium", "Low").forEach { p ->
                                val isSel = newPriority.equals(p, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) when (p) {
                                        "High" -> Color(0xFFEF4444)
                                        "Medium" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF10B981)
                                    } else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable { newPriority = p }
                                ) {
                                    Text(
                                        p,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newProject,
                        onValueChange = { newProject = it },
                        label = { Text("Project Name") },
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                        colors = appTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newDueDate,
                            onValueChange = { newDueDate = it },
                            label = { Text("Deadline / Due") },
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.sp),
                            colors = appTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newEstimatedTime,
                            onValueChange = { newEstimatedTime = it },
                            label = { Text("Est. Time") },
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.sp),
                            colors = appTextFieldColors(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Task Dependencies Selector
                    Column {
                        Text(
                            "Prerequisite Dependency (Optional):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedDependencyTaskId == null) ElectricBlue else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    selectedDependencyTaskId = null
                                    selectedDependencyTitle = null
                                }
                            ) {
                                Text(
                                    "No Dependency",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedDependencyTaskId == null) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                            tasks.filter { !it.isCompleted }.take(5).forEach { depTask ->
                                val isSelected = selectedDependencyTaskId == depTask.id
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ElectricBlue else Color(0xFFF1F5F9),
                                    modifier = Modifier.clickable {
                                        selectedDependencyTaskId = depTask.id
                                        selectedDependencyTitle = depTask.title
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Link,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            depTask.title.take(18) + if (depTask.title.length > 18) "..." else "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addTask(
                                title = newTitle,
                                projectName = newProject,
                                priority = newPriority,
                                dueDate = newDueDate,
                                category = newCategory,
                                estimatedTimeNeeded = newEstimatedTime,
                                assignee = selectedAssignee,
                                dependsOnTaskId = selectedDependencyTaskId,
                                dependsOnTaskTitle = selectedDependencyTitle
                            )
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Task", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskEntity,
    employees: List<com.example.data.model.EmployeeEntity>,
    onToggle: () -> Unit,
    onStatusChange: (String) -> Unit = {},
    onReassign: (String) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showReassignDialog by remember { mutableStateOf(false) }
    val availableStatuses = listOf("Backlog", "In Progress", "In Review", "Completed")
    val categoryConfig = getCategoryConfig(task.category)
    val context = androidx.compose.ui.platform.LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (task.isCompleted) Color(0xFFE2E8F0) else categoryConfig.cardBorderColor.copy(alpha = 0.8f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (task.isCompleted) Color(0xFFCBD5E1) else categoryConfig.leftAccentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggle() },
                        colors = CheckboxDefaults.colors(checkedColor = BrandBlue, uncheckedColor = Color(0xFF64748B)),
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (task.isCompleted) TextMuted else Color(0xFF0F172A),
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    PriorityBadge(priority = task.priority)

                    Box {
                        IconButton(
                            onClick = { showStatusMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Task options",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            Text(
                                "Quick Actions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            DropdownMenuItem(
                                text = { Text("Reassign Employee", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    showReassignDialog = true
                                    showStatusMenu = false
                                }
                            )
                            Divider(color = BorderLight, thickness = 1.dp)
                            DropdownMenuItem(
                                text = { Text("Delete Task", fontWeight = FontWeight.Bold, color = StatusRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    onDelete()
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(task.projectName, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (task.isCompleted) Color(0xFFF1F5F9) else categoryConfig.tagBgColor,
                        border = BorderStroke(1.dp, if (task.isCompleted) Color(0xFFE2E8F0) else categoryConfig.cardBorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                categoryConfig.icon,
                                contentDescription = null,
                                tint = if (task.isCompleted) Color(0xFF94A3B8) else categoryConfig.tagTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                categoryConfig.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) Color(0xFF94A3B8) else categoryConfig.tagTextColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Due: ${task.dueDate}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Est: ${task.estimatedTimeNeeded}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("ASSIGNED TO EMPLOYEE:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = if (task.assignee.isNotBlank()) task.assignee else "Unassigned",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricBlue
                            )
                        }
                    }

                    Button(
                        onClick = { showReassignDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlueBg, contentColor = ElectricBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reassign", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!task.dependsOnTaskTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(0.5.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Depends on: ${task.dependsOnTaskTitle}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                HorizontalDivider(color = BorderLight, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Status:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    availableStatuses.forEach { statusOption ->
                        val isSelected = task.status.equals(statusOption, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BrandBlue else Color(0xFFE2E8F0),
                            modifier = Modifier.clickable { onStatusChange(statusOption) }
                        ) {
                            Text(
                                text = statusOption,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color(0xFF0F172A),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReassignDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredEmployees = employees.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.designation.contains(searchQuery, ignoreCase = true) 
        }
        AlertDialog(
            onDismissRequest = { showReassignDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = BrandBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reassign Task", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Select a team member to assign this task to. The reassignment will immediately sync to their device and Firestore.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name or role...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Box(modifier = Modifier.heightIn(max = 240.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredEmployees) { emp ->
                                val isCurrent = emp.name.equals(task.assignee, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) ElectricBlueBg else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isCurrent) ElectricBlue else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onReassign(emp.name)
                                            showReassignDialog = false
                                            Toast.makeText(context, "Task reassigned to ${emp.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (emp.status == com.example.data.model.EmployeeStatus.ACTIVE) Color(0xFF22C55E) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                            Text(emp.designation, fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (isCurrent) {
                                            Text("Current", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReassignDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
