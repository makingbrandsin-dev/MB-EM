package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.EmployeeEntity
import com.example.data.model.EmployeeRole
import com.example.data.model.EmployeeStatus
import com.example.data.model.PresenceStatus
import com.example.ui.components.AppHeader
import com.example.ui.components.MetricBadge
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*

val Department.displayName: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEmployeeClick: ((Long) -> Unit)? = null,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null
) {
    val employees by viewModel.employees.collectAsState()
    val employeeCount by viewModel.employeeCount.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedDepartment by remember { mutableStateOf<Department?>(null) }
    var selectedStatus by remember { mutableStateOf<EmployeeStatus?>(null) }
    var showAddEmployeeDialog by remember { mutableStateOf(false) }
    var selectedEmployeeForDetail by remember { mutableStateOf<EmployeeEntity?>(null) }

    val departments = Department.values()
    val statuses = EmployeeStatus.values()

    val filteredEmployees = remember(employees, searchQuery, selectedDepartment, selectedStatus) {
        employees.filter { emp ->
            val matchesSearch = searchQuery.isBlank() ||
                    emp.name.contains(searchQuery, ignoreCase = true) ||
                    emp.designation.contains(searchQuery, ignoreCase = true) ||
                    emp.email.contains(searchQuery, ignoreCase = true) ||
                    emp.skills.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesDept = selectedDepartment == null || emp.department == selectedDepartment
            val matchesStatus = selectedStatus == null || emp.status == selectedStatus

            matchesSearch && matchesDept && matchesStatus
        }
    }

    val activeCount = remember(employees) { employees.count { it.status == EmployeeStatus.ACTIVE } }
    val onLeaveCount = remember(employees) { employees.count { it.status == EmployeeStatus.ON_LEAVE } }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Team Directory",
                subMenuSubtitle = "${employees.size} Members · $activeCount Active",
                onBack = onBack,
                onNavigateToChat = onNavigateToChat,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(
                        onClick = { showAddEmployeeDialog = true },
                        modifier = Modifier.testTag("add_employee_top_button")
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = "Add Employee",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEmployeeDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("add_employee_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Employee")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBadge(
                        label = "Total Staff",
                        value = employeeCount.toString(),
                        backgroundColor = Color(0xFFE0F2FE),
                        textColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Active Now",
                        value = activeCount.toString(),
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "On Leave",
                        value = onLeaveCount.toString(),
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, role, email, skill...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("employee_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // Department Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Department",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedDepartment == null,
                                onClick = { selectedDepartment = null },
                                label = { Text("All (${employees.size})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        items(departments) { dept ->
                            val count = employees.count { it.department == dept }
                            FilterChip(
                                selected = selectedDepartment == dept,
                                onClick = {
                                    selectedDepartment = if (selectedDepartment == dept) null else dept
                                },
                                label = { Text("${dept.displayName} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Status Filter Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Status:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    statuses.forEach { status ->
                        val isSelected = selectedStatus == status
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BrandLightBlue.copy(alpha = 0.2f) else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BrandBlue else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .clickable {
                                    selectedStatus = if (selectedStatus == status) null else status
                                }
                        ) {
                            Text(
                                text = status.name.replace("_", " "),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrandBlue else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Results count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Members (${filteredEmployees.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (selectedDepartment != null || selectedStatus != null || searchQuery.isNotBlank()) {
                        TextButton(onClick = {
                            selectedDepartment = null
                            selectedStatus = null
                            searchQuery = ""
                        }) {
                            Text("Reset Filters", fontSize = 12.sp, color = BrandBlue)
                        }
                    }
                }
            }

            // Employee List Cards
            if (filteredEmployees.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No members found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Try adjusting your search query or department filters.",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(filteredEmployees, key = { it.id }) { employee ->
                    EmployeeCard(
                        employee = employee,
                        onClick = {
                            selectedEmployeeForDetail = employee
                            onEmployeeClick?.invoke(employee.id)
                        },
                        onDelete = {
                            viewModel.deleteEmployee(employee)
                            Toast.makeText(context, "Removed ${employee.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Detail & Quick Action Dialog
    selectedEmployeeForDetail?.let { emp ->
        EmployeeDetailDialog(
            employee = emp,
            onDismiss = { selectedEmployeeForDetail = null },
            onDelete = {
                viewModel.deleteEmployee(emp)
                selectedEmployeeForDetail = null
                Toast.makeText(context, "Removed ${emp.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Employee Dialog
    if (showAddEmployeeDialog) {
        AddEmployeeDialog(
            onDismiss = { showAddEmployeeDialog = false },
            onAdd = { name, email, phone, designation, dept, role, skillsList ->
                viewModel.addEmployee(
                    name = name,
                    email = email,
                    phone = phone,
                    designation = designation,
                    department = dept,
                    role = role,
                    skills = skillsList
                )
                showAddEmployeeDialog = false
                Toast.makeText(context, "Added $name to organization", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun EmployeeCard(
    employee: EmployeeEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("employee_card_${employee.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with Presence indicator
                Box {
                    Surface(
                        shape = CircleShape,
                        color = BrandLightBlue,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = employee.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                fontSize = 18.sp
                            )
                        }
                    }
                    val statusColor = when (employee.presenceStatus) {
                        PresenceStatus.ONLINE -> StatusGreen
                        PresenceStatus.IN_MEETING -> StatusPurple
                        PresenceStatus.OFFLINE -> Color(0xFF94A3B8)
                    }
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .align(Alignment.BottomEnd)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = employee.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${employee.designation} · ${employee.department.displayName}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = employee.email,
                        fontSize = 11.sp,
                        color = BrandBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (employee.status == EmployeeStatus.ACTIVE) StatusGreenBg else StatusOrangeBg
                ) {
                    Text(
                        text = employee.status.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (employee.status == EmployeeStatus.ACTIVE) StatusGreen else StatusOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Skills & Project Tags
            if (employee.skills.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(employee.skills.take(4)) { skill ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = skill,
                                fontSize = 10.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Quick Actions: Phone, Email, WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            com.example.util.WhatsAppHelper.dialPhoneNumber(context, employee.phone)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${employee.email}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Text("Delete", color = StatusRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Employee") },
            text = { Text("Are you sure you want to remove ${employee.name} from the directory?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmployeeDetailDialog(
    employee: EmployeeEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("${employee.designation} (${employee.role.name})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrandBlue)
                Text("Department: ${employee.department.displayName}", fontSize = 13.sp, color = TextSecondary)
                Text("Email: ${employee.email}", fontSize = 13.sp, color = TextSecondary)
                Text("Phone: ${employee.phone}", fontSize = 13.sp, color = TextSecondary)
                if (employee.skills.isNotEmpty()) {
                    Text("Skills: ${employee.skills.joinToString(", ")}", fontSize = 13.sp, color = TextSecondary)
                }
                Text("Status: ${employee.status.name} (${employee.presenceStatus.name})", fontSize = 13.sp, color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEmployeeDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, email: String, phone: String, designation: String, dept: Department, role: EmployeeRole, skills: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf(Department.ENGINEERING) }
    var selectedRole by remember { mutableStateOf(EmployeeRole.DEVELOPER) }
    var skillsText by remember { mutableStateOf("Kotlin, Compose, Android") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = SurfaceBg,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
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
                                color = ElectricBlueBg,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = BrandBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Add Team Member",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    "Assign department, role & skills",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
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
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Personal Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name *") },
                                placeholder = { Text("e.g. Rahul Sharma") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address *") },
                                placeholder = { Text("e.g. rahul@makingbrands.in") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+91 9876543210") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Role & Department", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)

                            OutlinedTextField(
                                value = designation,
                                onValueChange = { designation = it },
                                label = { Text("Designation") },
                                placeholder = { Text("e.g. Senior Android Engineer") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Department:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(Department.values()) { dept ->
                                    FilterChip(
                                        selected = selectedDept == dept,
                                        onClick = { selectedDept = dept },
                                        label = { Text(dept.name.lowercase().capitalize(java.util.Locale.ROOT), fontSize = 12.sp) }
                                    )
                                }
                            }

                            Text("Role:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(EmployeeRole.values()) { role ->
                                    FilterChip(
                                        selected = selectedRole == role,
                                        onClick = { selectedRole = role },
                                        label = { Text(role.name.lowercase().capitalize(java.util.Locale.ROOT), fontSize = 12.sp) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = skillsText,
                                onValueChange = { skillsText = it },
                                label = { Text("Skills (comma separated)") },
                                placeholder = { Text("Kotlin, Jetpack Compose, UI/UX") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = TextSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (name.isNotBlank() && email.isNotBlank()) {
                                    val skillsList = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onAdd(name, email, phone, designation, selectedDept, selectedRole, skillsList)
                                }
                            },
                            enabled = name.isNotBlank() && email.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Member", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
