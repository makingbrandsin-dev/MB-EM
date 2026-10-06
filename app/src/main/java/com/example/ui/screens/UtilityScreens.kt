package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppLanguage
import com.example.util.LanguageManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.data.model.NotificationEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.MetricBadge
import com.example.ui.theme.*

// ---------------- Screen 15: Profile Screen ----------------
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showDeleteAllFieldsDialog by remember { mutableStateOf(false) }
    var showResetAccountDialog by remember { mutableStateOf(false) }
    var showClearEmployeeDataDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val firebaseUser by viewModel.firebaseUserRecord.collectAsState()
    val userSession by viewModel.userSession.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val empName = userProfile?.name?.ifBlank { userSession.effectiveDisplayName } ?: userSession.effectiveDisplayName
    val empRole = userProfile?.role?.ifBlank { userSession.effectiveRole } ?: userSession.effectiveRole
    val email = userProfile?.email?.ifBlank { userSession.effectiveEmail } ?: userSession.effectiveEmail
    val phone = userProfile?.phone?.ifBlank { userSession.effectivePhone } ?: userSession.effectivePhone
    val department = userProfile?.department?.ifBlank { userSession.effectiveDepartment } ?: userSession.effectiveDepartment
    val joiningDate = userProfile?.joiningDate?.ifBlank { userSession.effectiveJoiningDate } ?: userSession.effectiveJoiningDate
    val emergencyContact = userProfile?.emergencyContact?.ifBlank { userSession.effectiveEmergencyContact } ?: userSession.effectiveEmergencyContact
    val address = userProfile?.address?.ifBlank { userSession.effectiveAddress } ?: userSession.effectiveAddress
    val skills = userProfile?.skills?.ifBlank { userSession.effectiveSkills } ?: userSession.effectiveSkills
    val bio = userProfile?.bio?.ifBlank { userSession.effectiveBio } ?: userSession.effectiveBio

    var showDeleteAllEnterpriseDataDialog by remember { mutableStateOf(false) }

    // Edit Profile Dialog with all fields (90% of screen width & height according to device)
    if (showEditProfileDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEditProfileDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .fillMaxHeight(0.90f)
            ) {
                var editName by remember(showEditProfileDialog) { mutableStateOf(empName) }
                var editRole by remember(showEditProfileDialog) { mutableStateOf(empRole) }
                var editEmail by remember(showEditProfileDialog) { mutableStateOf(email) }
                var editPhone by remember(showEditProfileDialog) { mutableStateOf(phone) }
                var editDepartment by remember(showEditProfileDialog) { mutableStateOf(department) }
                var editJoiningDate by remember(showEditProfileDialog) { mutableStateOf(joiningDate) }
                var editEmergencyContact by remember(showEditProfileDialog) { mutableStateOf(emergencyContact) }
                var editAddress by remember(showEditProfileDialog) { mutableStateOf(address) }
                var editSkills by remember(showEditProfileDialog) { mutableStateOf(skills) }
                var editBio by remember(showEditProfileDialog) { mutableStateOf(bio) }

                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Edit Profile Fields", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextPrimary)
                            Text("90% Screen Responsive • Persists in Room & Cloud", fontSize = 11.sp, color = ElectricBlue)
                        }
                        // Clear all inputs inside dialog
                        TextButton(
                            onClick = {
                                editName = ""
                                editRole = ""
                                editEmail = ""
                                editPhone = ""
                                editDepartment = ""
                                editJoiningDate = ""
                                editEmergencyContact = ""
                                editAddress = ""
                                editSkills = ""
                                editBio = ""
                            }
                        ) {
                            Icon(Icons.Default.ClearAll, contentDescription = null, tint = StatusRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 12.sp, color = StatusRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editRole,
                                onValueChange = { editRole = it },
                                label = { Text("Role / Designation") },
                                leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editDepartment,
                                onValueChange = { editDepartment = it },
                                label = { Text("Department") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Official Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Mobile / Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editEmergencyContact,
                                onValueChange = { editEmergencyContact = it },
                                label = { Text("Emergency Contact") },
                                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editJoiningDate,
                                onValueChange = { editJoiningDate = it },
                                label = { Text("Joining Date") },
                                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editAddress,
                                onValueChange = { editAddress = it },
                                label = { Text("Work / Residential Address") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editSkills,
                                onValueChange = { editSkills = it },
                                label = { Text("Skills & Expertise (comma separated)") },
                                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { editBio = it },
                                label = { Text("Bio / Status Note") },
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = ElectricBlue) },
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                colors = appTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditProfileDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateEmployeeProfile(
                                    name = editName,
                                    role = editRole,
                                    email = editEmail,
                                    phone = editPhone,
                                    department = editDepartment,
                                    joiningDate = editJoiningDate,
                                    emergencyContact = editEmergencyContact,
                                    address = editAddress,
                                    skills = editSkills,
                                    bio = editBio
                                )
                                showEditProfileDialog = false
                                Toast.makeText(context, "Employee Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Save All Fields", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Delete All Application & Enterprise Data
    if (showDeleteAllEnterpriseDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllEnterpriseDataDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(40.dp)) },
            title = { Text("Delete All Application Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "⚠️ WARNING: This will permanently delete ALL leads, customer contacts, tasks, chat histories, attendance records, expense claims, call logs, quotations, and employee fields across the entire database. This action CANNOT be undone.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllEnterpriseData {
                            Toast.makeText(context, "All application data deleted completely!", Toast.LENGTH_LONG).show()
                        }
                        showDeleteAllEnterpriseDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Permanently Delete All Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllEnterpriseDataDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog for Delete / Clear All Profile Fields
    if (showDeleteAllFieldsDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllFieldsDialog = false },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Delete All Profile Fields?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete and clear all fields in your employee profile? " +
                    "This will reset your name, role, email, phone number, emergency contact, skills, and address to empty values.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllUserProfileFields()
                        showDeleteAllFieldsDialog = false
                        Toast.makeText(context, "All profile fields cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Yes, Delete All Fields", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllFieldsDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Confirmation Dialog for Clear All Employee Data
    if (showClearEmployeeDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearEmployeeDataDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = StatusOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Clear All Employee Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will delete your local dummy tasks, chat logs, call logs, and attendance history, keeping only official records assigned to you.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearEmployeeData()
                        showClearEmployeeDataDialog = false
                        Toast.makeText(context, "Employee data and dummy records cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOrange)
                ) {
                    Text("Clear Data", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearEmployeeDataDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Employee Profile",
                subMenuSubtitle = "$empName • $empRole",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TextPrimary)
                    }
                    IconButton(onClick = { showDeleteAllFieldsDialog = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete All Fields", tint = StatusRed)
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                shape = CircleShape,
                                color = BrandBlue.copy(alpha = 0.15f),
                                modifier = Modifier.size(86.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(48.dp))
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = ElectricBlue,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { showEditProfileDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = empName.ifEmpty { "No Name Assigned" },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (empName.isEmpty()) TextMuted else TextPrimary
                        )
                        Text(
                            text = empRole.ifEmpty { "No Role Assigned" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (empRole.isEmpty()) TextMuted else ElectricBlue
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Department: $department",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Firebase Auth & Firestore Data Persistence Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFFF7ED),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Firebase & Firestore Sync", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text("Live Cloud Identity & User Persistence", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = CircleShape, color = Color(0xFF16A34A), modifier = Modifier.size(6.dp)) {}
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Connected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Identity Provider", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(firebaseUser?.providerId ?: "Google Sign-in", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Firebase UID", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text(
                                        (firebaseUser?.uid ?: "user_${Math.abs(email.hashCode()).toString().take(8)}"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ElectricBlue
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Firestore Path", fontSize = 11.sp, color = Color(0xFF64748B))
                                    Text("users/${firebaseUser?.uid ?: "current"}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        var isSyncingNow by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = {
                                isSyncingNow = true
                                viewModel.syncUserProfileToFirestore(
                                    name = empName,
                                    designation = empRole,
                                    department = department,
                                    email = email
                                ) { success ->
                                    isSyncingNow = false
                                    Toast.makeText(
                                        context,
                                        if (success) "User profile persisted & synced with Firestore!" else "User data queued for Firestore sync",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricBlue),
                            enabled = !isSyncingNow
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ElectricBlue, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Persisting to Firestore...", fontSize = 12.sp, color = ElectricBlue)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync User Data to Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                            }
                        }
                    }
                }
            }

            // Summary Metric Badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBadge(
                        label = "Projects",
                        value = "3",
                        backgroundColor = Color(0xFFE0F2FE),
                        textColor = Color(0xFF0369A1),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Tasks",
                        value = "27",
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFFB45309),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Attendance",
                        value = "98%",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF15803D),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Profile Fields Detail Card
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
                            Text("Profile Fields & Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            TextButton(onClick = { showEditProfileDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        ProfileFieldRow(Icons.Default.Email, "Email Address", email)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.Phone, "Phone Number", phone)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.ContactPhone, "Emergency Contact", emergencyContact)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.CalendarToday, "Joining Date", joiningDate)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.LocationOn, "Office / Address", address)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.Stars, "Skills", skills)
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        ProfileFieldRow(Icons.Default.Notes, "Bio / Status", bio)
                    }
                }
            }

            // Field Management & Clear Actions Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Field & Data Actions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Text("Manage and reset your profile fields", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Delete All Profile Fields Button
                        Button(
                            onClick = { showDeleteAllFieldsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete / Clear All Profile Fields", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StatusRed)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Edit Fields Button
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit & Update All Fields", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricBlue)
                        }
                    }
                }
            }

            // Settings Navigation Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ProfileNavRow(Icons.Default.Person, "Edit Profile Fields", onClick = { showEditProfileDialog = true })
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.Settings, "Settings & Preferences", onClick = onNavigateToSettings)
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.HelpOutline, "Help & Support", onClick = onNavigateToHelp)
                        Divider(color = BorderLight)
                        ProfileNavRow(
                            icon = Icons.Default.CleaningServices,
                            label = "Clear All Employee Data",
                            textColor = StatusOrange,
                            iconTint = StatusOrange,
                            onClick = { showClearEmployeeDataDialog = true }
                        )
                        Divider(color = BorderLight)
                        ProfileNavRow(
                            icon = Icons.Default.DeleteForever,
                            label = "Delete All Data (Permanent Reset)",
                            textColor = StatusRed,
                            iconTint = StatusRed,
                            onClick = { showDeleteAllEnterpriseDataDialog = true }
                        )
                        Divider(color = BorderLight)
                        ProfileNavRow(Icons.Default.Logout, "Logout Account", textColor = StatusRed, iconTint = StatusRed, onClick = onLogout)
                    }
                }
            }

            // Version 5 Badge Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MB Traker Enterprise Suite", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Version 5.0.0 (Enterprise Build 500)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Making Brands © 2026. All Systems Operational.", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileFieldRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFFF1F5F9),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Text(
                text = value.ifEmpty { "Not specified (Empty)" },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (value.isEmpty()) TextMuted else TextPrimary
            )
        }
    }
}

@Composable
fun ProfileNavRow(
    icon: ImageVector,
    label: String,
    textColor: Color = TextPrimary,
    iconTint: Color = BrandBlue,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

// ---------------- Screen 16: Notifications Screen ----------------
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Notifications",
                subMenuSubtitle = "${notifications.size} Alerts",
                onBack = onBack,
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = { viewModel.markAllNotificationsAsRead() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read", tint = ElectricBlue)
                        }
                        IconButton(onClick = { viewModel.clearAllNotifications() }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all", tint = StatusRed)
                        }
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricBlueBg,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsNone,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notifications yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You're all caught up!",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { item ->
                    AnimatedVisibility(
                        visible = !item.isRead, // Fade-up disappearance when marked as read
                        enter = slideInVertically(initialOffsetY = { -40 }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -40 }) + fadeOut(),
                        modifier = Modifier.animateItem()
                    ) {
                        NotificationCardItem(
                            item = item,
                            onClick = { viewModel.markNotificationAsRead(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    item: NotificationEntity,
    onClick: () -> Unit
) {
    val (icon, bgColor, tintColor) = when (item.category.lowercase()) {
        "followup" -> Triple(Icons.Default.PhoneCallback, Color(0xFFFEE2E2), Color(0xFFDC2626))
        "task" -> Triple(Icons.Default.TaskAlt, Color(0xFFE0E7FF), Color(0xFF4F46E5))
        "lead", "crm" -> Triple(Icons.Default.TrendingUp, Color(0xFFDCFCE7), Color(0xFF16A34A))
        "message", "chat", "team_chat" -> Triple(Icons.Default.Forum, Color(0xFFE0F2FE), Color(0xFF0284C7))
        "attendance" -> Triple(Icons.Default.PunchClock, Color(0xFFFFEDD5), Color(0xFFEA580C))
        "shift", "calendar", "rotation" -> Triple(Icons.Default.CalendarMonth, Color(0xFFE0F2FE), Color(0xFF0284C7))
        "approval", "user_registration" -> Triple(Icons.Default.VerifiedUser, Color(0xFFD1FAE5), Color(0xFF059669))
        "security" -> Triple(Icons.Default.Shield, Color(0xFFFEE2E2), Color(0xFFDC2626))
        "leave" -> Triple(Icons.Default.EventBusy, Color(0xFFFEF3C7), Color(0xFFB45309))
        "broadcast", "announcement", "change" -> Triple(Icons.Default.Campaign, Color(0xFFFFF7ED), Color(0xFFEA580C))
        else -> Triple(Icons.Default.Notifications, Color(0xFFEDE9FE), Color(0xFF7C3AED))
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) ElectricBlueBg.copy(alpha = 0.5f) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = bgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        fontWeight = if (!item.isRead) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(item.timeAgo, fontSize = 11.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(item.subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

// ---------------- Screen 17: Holidays & Leave Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysLeaveScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Calendar & Apply", "My Leaves (${viewModel.leaves.collectAsState().value.size})", "Holidays")

    val empName by viewModel.currentEmployeeName.collectAsState()
    val leaves by viewModel.leaves.collectAsState()

    val currentNow = remember { Calendar.getInstance() }
    var displayedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }
    var selectedDay by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
    }
    var showApplyLeaveModal by remember { mutableStateOf(false) }
    var applicationSuccessMsg by remember { mutableStateOf<String?>(null) }

    // Dynamic month/year data
    val monthYearTitle = remember(displayedCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(displayedCalendar.time)
    }
    val daysInMonth = remember(displayedCalendar) {
        displayedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val firstDayOfWeek = remember(displayedCalendar) {
        displayedCalendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    }
    val totalSlots = remember(daysInMonth, firstDayOfWeek) {
        firstDayOfWeek + daysInMonth
    }
    val selectedDateFormatted = remember(displayedCalendar, selectedDay) {
        val cal = (displayedCalendar.clone() as Calendar).apply {
            val validDay = selectedDay.coerceIn(1, displayedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.DAY_OF_MONTH, validDay)
        }
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // State for the leave application form
    var selectedLeaveType by remember { mutableStateOf("Casual Leave (CL)") }
    var leaveReason by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val leaveTypes = listOf("Casual Leave (CL)", "Sick Leave (SL)", "Earned Leave (EL)", "Half Day")

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Holidays & Leave",
                subMenuSubtitle = "$empName • ${leaves.size} Records",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab Selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = ElectricBlue
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) ElectricBlue else Color(0xFF64748B),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }

            // Success Confirmation Banner if applied
            if (applicationSuccessMsg != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                applicationSuccessMsg ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { applicationSuccessMsg = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: Interactive Calendar & Immediate Apply Action
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ElectricBlueBg,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(monthYearTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                                add(Calendar.MONTH, -1)
                                            }
                                        }) { Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month", tint = Color(0xFF64748B)) }
                                        IconButton(onClick = {
                                            displayedCalendar = (displayedCalendar.clone() as Calendar).apply {
                                                add(Calendar.MONTH, 1)
                                            }
                                        }) { Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = Color(0xFF64748B)) }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Day Labels
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { day ->
                                        Text(day, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Days Grid dynamically rendering current month & year
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier.height(210.dp)
                                ) {
                                    items(totalSlots) { slot ->
                                        if (slot < firstDayOfWeek) {
                                            Box(modifier = Modifier.padding(3.dp).aspectRatio(1f))
                                        } else {
                                            val dayNum = slot - firstDayOfWeek + 1
                                            val isSelected = dayNum == selectedDay
                                            val isToday = displayedCalendar.get(Calendar.YEAR) == currentNow.get(Calendar.YEAR) &&
                                                    displayedCalendar.get(Calendar.MONTH) == currentNow.get(Calendar.MONTH) &&
                                                    dayNum == currentNow.get(Calendar.DAY_OF_MONTH)
                                            val isSunday = (slot % 7) == 0

                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        when {
                                                            isSelected -> ElectricBlue
                                                            isToday -> ElectricBlueBg
                                                            isSunday -> Color(0xFFFEE2E2)
                                                            else -> Color(0xFFF8FAFC)
                                                        }
                                                    )
                                                    .then(
                                                        if (isToday && !isSelected) {
                                                            Modifier.border(1.dp, ElectricBlue, RoundedCornerShape(10.dp))
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedDay = dayNum
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        "$dayNum",
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected || isToday || isSunday) FontWeight.Bold else FontWeight.Medium,
                                                        color = when {
                                                            isSelected -> Color.White
                                                            isToday -> ElectricBlue
                                                            isSunday -> StatusRed
                                                            else -> TextPrimary
                                                        }
                                                    )
                                                    if (isToday && !isSelected) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(ElectricBlue))
                                                    } else if (isSunday && !isSelected) {
                                                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(StatusRed))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dynamic Apply Action Card shown upon selecting a date
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Selected Date",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            selectedDateFormatted,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricBlue
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                empName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    "Ready to request leave for this date? Click Apply to submit and record directly in your account database.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )

                                // Direct Apply Button
                                Button(
                                    onClick = { showApplyLeaveModal = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElectricBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.PostAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Apply for ${if (selectedDay < 10) "0$selectedDay" else "$selectedDay"} Sep 2025",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Summary of Leave Balance
                    item {
                        Text("Leave Balance Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricBadge("Casual (CL)", "8 Days", Color(0xFFDBEAFE), Color(0xFF1D4ED8), Modifier.weight(1f))
                            MetricBadge("Sick (SL)", "5 Days", Color(0xFFDCFCE7), Color(0xFF15803D), Modifier.weight(1f))
                            MetricBadge("Earned (EL)", "12 Days", Color(0xFFEDE9FE), Color(0xFF6D28D9), Modifier.weight(1f))
                        }
                    }
                }

                1 -> {
                    // TAB 1: My Leaves History (from Room database)
                    if (leaves.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("No Leave Applications Found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF334155))
                                    Text("Select a date from the calendar to submit your first leave application.", fontSize = 13.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        item {
                            Text("Your Applied Leaves (${leaves.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                            Text("Stored persistently in database under username: $empName", fontSize = 12.sp, color = ElectricBlue)
                        }

                        items(leaves) { leaveItem ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = ElectricBlueBg,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(leaveItem.startDate, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                                Text("Applicant: ${leaveItem.username}", fontSize = 12.sp, color = Color(0xFF64748B))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (leaveItem.status == "Approved") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                leaveItem.status,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (leaveItem.status == "Approved") Color(0xFF15803D) else Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF8FAFC),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Type: ${leaveItem.leaveType}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ElectricBlue)
                                            if (leaveItem.reason.isNotBlank()) {
                                                Text("Reason: ${leaveItem.reason}", fontSize = 12.sp, color = Color(0xFF475569))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.deleteLeave(leaveItem) }
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Cancel Leave", tint = StatusRed, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel Request", color = StatusRed, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Upcoming Official Holidays
                    item {
                        Text("Official Holidays (2025)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                HolidayRow("Gandhi Jayanti", "02 Oct 2025", "Thursday")
                                Divider(color = BorderLight)
                                HolidayRow("Dussehra", "12 Oct 2025", "Sunday")
                                Divider(color = BorderLight)
                                HolidayRow("Diwali", "20 Oct 2025", "Monday")
                                Divider(color = BorderLight)
                                HolidayRow("Guru Nanak Jayanti", "05 Nov 2025", "Wednesday")
                                Divider(color = BorderLight)
                                HolidayRow("Christmas", "25 Dec 2025", "Thursday")
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet / Dialog for Applying Leave
    if (showApplyLeaveModal) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showApplyLeaveModal = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with creative blue badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ElectricBlueBg,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Apply for Leave",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Persists under username: $empName",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = { showApplyLeaveModal = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Applicant details badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Applicant Username", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(empName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Date", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(selectedDateFormatted, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                    }
                }

                // Leave Type Selection
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Leave Type", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leaveTypes.take(2).forEach { type ->
                            val isSelected = selectedLeaveType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLeaveType = type },
                                label = { Text(type, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        leaveTypes.drop(2).forEach { type ->
                            val isSelected = selectedLeaveType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLeaveType = type },
                                label = { Text(type, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // Reason for Leave
                OutlinedTextField(
                    value = leaveReason,
                    onValueChange = { leaveReason = it },
                    label = { Text("Reason for Leave *") },
                    placeholder = { Text("e.g., Medical checkup, family function, personal work") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                    colors = appTextFieldColors(),
                    minLines = 2,
                    maxLines = 4
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showApplyLeaveModal = false },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            val dateStr = selectedDateFormatted
                            val reason = leaveReason.ifBlank { "Personal leave request" }
                            isSubmitting = true
                            viewModel.applyLeave(
                                leaveType = selectedLeaveType,
                                startDate = dateStr,
                                endDate = dateStr,
                                reason = reason
                            )
                            isSubmitting = false
                            showApplyLeaveModal = false
                            applicationSuccessMsg = "Leave application for $dateStr recorded in database for $empName!"
                            selectedTab = 1 // Switch to My Leaves tab so user sees their new entry immediately!
                        },
                        modifier = Modifier.weight(1.5f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit & Store", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HolidayRow(title: String, date: String, day: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(day, fontSize = 12.sp, color = TextSecondary)
        }
        Text(date, fontSize = 13.sp, color = ElectricBlue, fontWeight = FontWeight.Bold)
    }
}

// ---------------- Screen 17: Settings Screen ----------------
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToMiloDebug: () -> Unit = {},
    onNavigateToMiloOnboarding: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val currencyCode by viewModel.currencyCode.collectAsState()
    val fcmTokenVal by viewModel.fcmToken.collectAsState()
    var pushNotificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(com.example.util.BiometricHelper.isBiometricSettingEnabled(context)) }
    var biometricEmployeeLogin by remember { mutableStateOf(com.example.util.BiometricHelper.isBiometricForEmployeeLoginEnabled(context)) }
    var biometricDashboard by remember { mutableStateOf(com.example.util.BiometricHelper.isBiometricForDashboardEnabled(context)) }
    var biometricChat by remember { mutableStateOf(com.example.util.BiometricHelper.isBiometricForChatEnabled(context)) }
    val sensorInfo = remember { com.example.util.BiometricHelper.getBiometricSensorInfo(context) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    if (showPasswordDialog) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        var oldPass by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPasswordDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth(if (isTablet) 0.75f else 1f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it },
                        label = { Text("Current Password") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showPasswordDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                showPasswordDialog = false
                                toastMessage = "Password updated successfully!"
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Settings",
                subMenuSubtitle = "Preferences & Security",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Toast / Confirmation Snackbar
            if (toastMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(toastMessage!!, color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(onClick = { toastMessage = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF166534))
                            }
                        }
                    }
                }
            }

            // Currency Selection Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Currency Preference", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Select display currency for leads & financials", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val isInr = currencyCode == "INR"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isInr) ElectricBlueBg else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isInr) ElectricBlue else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setCurrency("INR")
                                        toastMessage = "Currency set to Indian Rupee (₹)"
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("₹ INR", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isInr) ElectricBlue else TextPrimary)
                                    Text("Indian Rupee", fontSize = 11.sp, color = TextSecondary)
                                    if (isInr) {
                                        Text("(Default)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                    }
                                }
                            }

                            val isUsd = currencyCode == "USD"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUsd) ElectricBlueBg else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isUsd) ElectricBlue else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.setCurrency("USD")
                                        toastMessage = "Currency set to US Dollar ($)"
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("$ USD", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isUsd) ElectricBlue else TextPrimary)
                                    Text("US Dollar", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // 🌐 Multi-Language Architecture (English / Telugu / Tamil)
            item {
                val currentLanguage by LanguageManager.currentLanguage.collectAsState()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Language Preference / భాష / மொழி", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Text("Select display language for Milo & app notifications", fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                val isSelected = currentLanguage == lang
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) ElectricBlueBg else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) BrandBlue else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            LanguageManager.setLanguage(lang)
                                            toastMessage = "Language switched to ${lang.displayName} (${lang.nativeName})"
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            lang.nativeName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) BrandBlue else TextPrimary,
                                            maxLines = 1
                                        )
                                        Text(
                                            lang.displayName,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🔋 Battery Saver & Power Optimization Card
            item {
                val batteryState by viewModel.batteryState.collectAsState()
                val isSaverActive by viewModel.isBatterySaverActive.collectAsState()

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSaverActive) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.BatterySaver,
                                            contentDescription = null,
                                            tint = if (isSaverActive) Color(0xFFD97706) else Color(0xFF16A34A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Battery Saver & Power Optimization", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Throttle background sync & listeners on low battery", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSaverActive) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (isSaverActive) "⚡ SAVER ACTIVE" else "🔋 NORMAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSaverActive) Color(0xFF92400E) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Real-time Battery Status Bar
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Device Battery Level:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${batteryState.levelPercent}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                        if (batteryState.isCharging) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("🔌 (Charging)", fontSize = 11.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "Sync: ${batteryState.syncIntervalSeconds}s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSaverActive) StatusOrange else StatusGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (batteryState.levelPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = when {
                                        batteryState.levelPercent <= 15 -> StatusRed
                                        batteryState.levelPercent <= 30 -> StatusOrange
                                        else -> StatusGreen
                                    },
                                    trackColor = Color(0xFFE2E8F0)
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = batteryState.statusDescription,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Battery Saver Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.util.BatterySaverMode.values().forEach { mode ->
                                val isSelected = batteryState.mode == mode
                                val modeLabel = when (mode) {
                                    com.example.util.BatterySaverMode.AUTO -> "Auto (Low)"
                                    com.example.util.BatterySaverMode.ALWAYS_ON -> "Force On"
                                    com.example.util.BatterySaverMode.OFF -> "Disabled"
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) ElectricBlueBg else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) ElectricBlue else Color.Transparent),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.setBatterySaverMode(mode)
                                            toastMessage = "Battery Saver mode set to $modeLabel"
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = modeLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) ElectricBlue else TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (batteryState.mode == com.example.util.BatterySaverMode.AUTO) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Low Battery Threshold:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(15, 20, 25, 30).forEach { pct ->
                                    val isThreshSelected = batteryState.lowThresholdPercent == pct
                                    FilterChip(
                                        selected = isThreshSelected,
                                        onClick = {
                                            viewModel.setBatterySaverThreshold(pct)
                                            toastMessage = "Battery Saver threshold set to $pct%"
                                        },
                                        label = { Text("≤ $pct%", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricBlueBg,
                                            selectedLabelColor = ElectricBlue
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Firestore Cloud Auth Provider & Role Redirection Card
            item {
                val currentRoleStr by viewModel.userRole.collectAsState()
                val firestoreMsg by viewModel.firestoreAuthStatus.collectAsState()
                val isCurrentAdmin = currentRoleStr.contains("admin", ignoreCase = true) || currentRoleStr.contains("manager", ignoreCase = true)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Firestore Auth & Role Provider", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Live role sync from collection 'users'", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCurrentAdmin) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (isCurrentAdmin) "MB Admin" else "Employee",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentAdmin) Color(0xFF92400E) else Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Active UI Destination:", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        text = if (isCurrentAdmin) "Admin Dashboard (manager)" else "Employee Workspace (home)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentAdmin) Color(0xFFB45309) else Color(0xFF15803D)
                                    )
                                }
                                if (firestoreMsg != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Cloud Status: $firestoreMsg",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Test Dynamic Cloud Role Switching:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateRoleInFirestore("admin") { ok ->
                                        toastMessage = if (ok) "Updated Firestore role to ADMIN! Next login routes to Admin Dashboard." else "Failed to update Firestore"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isCurrentAdmin) Color(0xFFB45309) else TextPrimary
                                )
                            ) {
                                Text("Set Admin in Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.updateRoleInFirestore("employee") { ok ->
                                        toastMessage = if (ok) "Updated Firestore role to EMPLOYEE! Next login routes to Employee Workspace." else "Failed to update Firestore"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (!isCurrentAdmin) Color(0xFF15803D) else TextPrimary
                                )
                            ) {
                                Text("Set Employee in Cloud", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Firebase Cloud Messaging (FCM) & Push Alerts Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFFBEB),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Firebase Cloud Messaging (FCM)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Background chat & task alerts", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    "Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "FCM delivers real-time notifications even when MB Traker is closed or running in the background.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testTriggerChatNotification()
                                    toastMessage = "🔔 Sent Test Team Chat Push Alert!"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Chat Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    viewModel.testTriggerTaskNotification()
                                    toastMessage = "⚡ Sent Test Task Update Alert!"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Task Alert", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 🦁 Milo AI & Character Voice Settings Card
            item {
                val (isMiloMuted, toggleMiloMute) = com.example.milo.rememberMiloMuteState()
                var voiceOpt by remember { mutableStateOf(com.example.milo.MiloVoiceHelper.getVoiceOption(context)) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ElectricBlueBg,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🦁", fontSize = 18.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Milo AI & Voice Assistant", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Your 24/7 Live Workplace Companion", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isMiloMuted) Color(0xFFFEF2F2) else Color(0xFFDBEAFE)
                            ) {
                                Text(
                                    text = if (isMiloMuted) "Muted" else "Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMiloMuted) Color(0xFFDC2626) else ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Milo manages your projects, tasks, invoices, quotations, lead funnels, and real-time reminders.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = BorderLight)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Voice Mute Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleMiloMute() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Mute Milo Spoken Voice", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                Text("Silence proactive audio briefings and voice read-outs", fontSize = 11.sp, color = TextSecondary)
                            }
                            Switch(
                                checked = isMiloMuted,
                                onCheckedChange = { toggleMiloMute() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFDC2626),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Voice Accent & Tone:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(6.dp))

                        val voiceOptions = listOf(
                            "INDIAN_MALE" to "Indian Male",
                            "INDIAN_FEMALE" to "Indian Female",
                            "UK_ACCENT" to "UK Accent",
                            "US_ACCENT" to "US Accent"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            voiceOptions.forEach { (code, label) ->
                                val isSelected = voiceOpt == code
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        voiceOpt = code
                                        com.example.milo.MiloVoiceHelper.setVoiceOption(context, code)
                                        toastMessage = "Set voice to $label"
                                    },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlueBg,
                                        selectedLabelColor = ElectricBlue
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isMiloMuted) {
                                        toastMessage = "Milo is muted. Unmute to test voice audio."
                                    } else {
                                        com.example.milo.MiloVoiceHelper.speakText(context, "Hello! I am Milo, your AI workplace copilot. Ready to conquer today's targets!")
                                        toastMessage = "🦁 Playing voice sample..."
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Voice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onNavigateToMiloOnboarding,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Milo Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Notification Toggles Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notifications", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        SettingToggleRow("Push Notifications", "Receive real-time lead & task updates", pushNotificationsEnabled) {
                            pushNotificationsEnabled = it
                        }
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        SettingToggleRow("Sound & Vibration", "Play audio alert on new chat messages", soundEnabled) {
                            soundEnabled = it
                        }
                    }
                }
            }

            // Security Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Security & Privacy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        SettingActionRow(Icons.Default.Lock, "Change Password", "Update your account password") {
                            showPasswordDialog = true
                        }
                        Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                        SettingToggleRow("Biometric Security (Master)", "Use Fingerprint or Face ID for authentication", biometricEnabled) { isChecked ->
                            if (isChecked && activity != null) {
                                com.example.util.BiometricHelper.promptBiometricAuth(
                                    activity = activity,
                                    title = "Enable Biometric Security",
                                    subtitle = "Authenticate to enable fingerprint/face unlock for MB Traker",
                                    onSuccess = {
                                        biometricEnabled = true
                                        com.example.util.BiometricHelper.setBiometricSettingEnabled(context, true)
                                    },
                                    onError = { err ->
                                        toastMessage = "Biometric setup: $err"
                                    }
                                )
                            } else {
                                biometricEnabled = isChecked
                                com.example.util.BiometricHelper.setBiometricSettingEnabled(context, isChecked)
                                toastMessage = if (isChecked) "Biometric unlock enabled" else "Biometric unlock disabled"
                            }
                        }

                        if (biometricEnabled) {
                            Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                            SettingToggleRow(
                                "Employee Login Scan",
                                "Require biometric scan on employee login",
                                biometricEmployeeLogin
                            ) { isChecked ->
                                biometricEmployeeLogin = isChecked
                                com.example.util.BiometricHelper.setBiometricForEmployeeLoginEnabled(context, isChecked)
                                toastMessage = if (isChecked) "Employee Login Biometrics Enabled" else "Employee Login Biometrics Disabled"
                            }

                            Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                            SettingToggleRow(
                                "Employee Dashboard Lock",
                                "Require scan before viewing attendance trends & KPIs",
                                biometricDashboard
                            ) { isChecked ->
                                biometricDashboard = isChecked
                                com.example.util.BiometricHelper.setBiometricForDashboardEnabled(context, isChecked)
                                toastMessage = if (isChecked) "Dashboard Biometric Gate Enabled" else "Dashboard Biometric Gate Disabled"
                            }

                            Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                            SettingToggleRow(
                                "Team Chat Biometric Gate",
                                "Require scan before viewing confidential communications",
                                biometricChat
                            ) { isChecked ->
                                biometricChat = isChecked
                                com.example.util.BiometricHelper.setBiometricForChatEnabled(context, isChecked)
                                toastMessage = if (isChecked) "Team Chat Biometric Gate Enabled" else "Team Chat Biometric Gate Disabled"
                            }

                            Divider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                            SettingActionRow(
                                Icons.Default.Fingerprint,
                                "Test Biometric Scanner",
                                "Test native androidx.biometric prompt & sensor response"
                            ) {
                                if (activity != null) {
                                    com.example.util.BiometricHelper.promptBiometricAuth(
                                        activity = activity,
                                        title = "Biometric Sensor Test",
                                        subtitle = "Testing sensor response using androidx.biometric",
                                        onSuccess = {
                                            // Sensor responsive test succeeded, no success note shown per policy
                                        },
                                        onError = { err ->
                                            toastMessage = "Biometric test: $err"
                                        },
                                        onCancel = {
                                            toastMessage = "Biometric test cancelled"
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sensor Hardware Diagnostics Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = if (sensorInfo.isSupported) Color(0xFF059669) else Color(0xFF3B82F6),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = sensorInfo.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = sensorInfo.description,
                                            fontSize = 11.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🚀 App Version & In-App Update Management Card
            item {
                com.example.util.AppVersionAndUpdatesCard(
                    onShowToast = { toastMessage = it }
                )
            }

            // 🔔 Notification Sound Preview Card
            item {
                NotificationSoundSettingsCard()
            }

            // 🗑️ Data Management & System Wipe Card
            item {
                var showSettingsDeleteAllDialog by remember { mutableStateOf(false) }

                if (showSettingsDeleteAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDeleteAllDialog = false },
                        icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(40.dp)) },
                        title = { Text("Delete All Application Data?", fontWeight = FontWeight.Bold) },
                        text = {
                            Text(
                                "⚠️ This will permanently erase ALL data across all modules (leads, clients, tasks, attendance records, chat logs, call logs, expenses, and employee fields) from local storage and offline cache.",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                lineHeight = 18.sp
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.deleteAllEnterpriseData {
                                        toastMessage = "All application data deleted completely!"
                                    }
                                    showSettingsDeleteAllDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                            ) {
                                Text("Permanently Delete All Data", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSettingsDeleteAllDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                        }
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF2F2),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = StatusRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Data Management & Reset", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Text("Wipe CRM, tasks, chats, and records", fontSize = 12.sp, color = TextSecondary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showSettingsDeleteAllDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete All Data", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Session & Logout Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Session & Account", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out & Lock App", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // 🔔 Notification Sound Preview Card
            item {
                NotificationSoundSettingsCard()
            }

            // App Version Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MB Traker Enterprise Suite", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("Version 5.0.0 (Enterprise Build 500)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BrandBlue)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("© 2026 Making Brands. All Rights Reserved.", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
            Text(subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
        )
    }
}

@Composable
fun SettingActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

// ---------------- Screen 18: Help & Support Screen ----------------
@Composable
fun HelpSupportScreen(
    viewModel: MainViewModel? = null,
    onBack: () -> Unit
) {
    var ticketSubject by remember { mutableStateOf("") }
    var ticketMessage by remember { mutableStateOf("") }
    var submittedMessage by remember { mutableStateOf<String?>(null) }
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }

    val faqs = listOf(
        "How do I check in or check out?" to "Navigate to the Home screen and tap the 'Check In' gold button. Your live working timer will immediately start. When finishing your shift, tap 'Check Out' to log your total hours.",
        "How do I apply for leave?" to "Go to the Attendance tab, scroll to the bottom or tap 'Apply Leave'. Select leave type, choose dates, enter your reason, and click 'Submit & Store'. Your manager will receive the request instantly.",
        "How do I change currency between INR and USD?" to "Open Profile -> Settings -> Currency Preference. Select '₹ INR' or '$ USD'. All lead potential values and project financial figures will update accordingly.",
        "How do I manage CRM leads and follow-ups?" to "Tap the CRM tab on the bottom bar. From there, you can view assigned leads, filter by pipeline stage, add new leads, schedule follow-up calls, and update lead notes."
    )

    Scaffold(
        topBar = {
            if (viewModel != null) {
                StandardScreenHeader(
                    viewModel = viewModel,
                    subMenuTitle = "Help & Support",
                    subMenuSubtitle = "24/7 Corporate Assistance",
                    onBack = onBack
                )
            } else {
                AppHeader(
                    title = "Help & Support",
                    onBack = onBack
                )
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Direct Contact Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Need Immediate Help?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        Text("Our support team is available 24/7 for assistance", fontSize = 12.sp, color = Color(0xFFCBD5E1))

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Call Us", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("+91 98765 43210", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Email Support", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text("makingbrands.in@gmail.com", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // FAQ Section
            item {
                Text("Frequently Asked Questions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            }

            items(faqs.size) { index ->
                val (question, answer) = faqs[index]
                val isExpanded = expandedFaqIndex == index

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedFaqIndex = if (isExpanded) null else index }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(question, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ElectricBlue
                            )
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = BorderLight)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(answer, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                        }
                    }
                }
            }

            // Submit Ticket Form Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Submit Support Ticket", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Text("Send a message to our technical support team", fontSize = 12.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(14.dp))

                        if (submittedMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDCFCE7),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(submittedMessage!!, color = Color(0xFF166534), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    IconButton(onClick = { submittedMessage = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF166534))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = ticketSubject,
                            onValueChange = { ticketSubject = it },
                            label = { Text("Subject / Issue Title") },
                            placeholder = { Text("e.g. Attendance sync delay") },
                            singleLine = true,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = ticketMessage,
                            onValueChange = { ticketMessage = it },
                            label = { Text("Detailed Description") },
                            placeholder = { Text("Describe what happened...") },
                            minLines = 3,
                            colors = appTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (ticketSubject.isNotBlank()) {
                                    submittedMessage = "Ticket '#MB-${(1000..9999).random()}' submitted successfully! Our team will respond shortly."
                                    ticketSubject = ""
                                    ticketMessage = ""
                                }
                            },
                            enabled = ticketSubject.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit Ticket", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Preview and test custom notification sounds for different app events.
 */
@Composable
fun NotificationSoundSettingsCard() {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Notification Sound Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoundPreviewButton("Task", com.example.R.raw.sound_task, Modifier.weight(1f))
                SoundPreviewButton("Chat", com.example.R.raw.sound_chat, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoundPreviewButton("Lead", com.example.R.raw.sound_lead, Modifier.weight(1f))
                SoundPreviewButton("Attendance", com.example.R.raw.sound_attendance, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SoundPreviewButton(label: String, resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            com.example.util.MiloHaptics.performButtonClick(context)
            com.example.util.NotificationSoundManager.previewSoundAsset(context, resId)
        },
        modifier = modifier,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(label, fontSize = 12.sp)
    }
}

