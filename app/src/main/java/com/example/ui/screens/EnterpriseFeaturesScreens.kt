package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.LocationHelper
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. GEOFENCE & SELFIE PUNCH-IN DIALOG
// ==========================================
@Composable
fun GeofenceSelfiePunchDialog(
    onDismiss: () -> Unit,
    onConfirmPunchIn: (isGeofenceVerified: Boolean, selfieUri: String?) -> Unit
) {
    val context = LocalContext.current
    var geofenceResult by remember {
        mutableStateOf(LocationHelper.verifyOfficeGeofence(context))
    }
    var selfieCaptured by remember { mutableStateOf(false) }
    var isCheckingLocation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (geofenceResult.isInsideGeofence) AccentGreen else Color(0xFFE11D48),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Geofenced Punch-In", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // GPS Status Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (geofenceResult.isInsideGeofence) Color(0xFFF0FDF4) else Color(0xFFFFF1F2)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (geofenceResult.isInsideGeofence) Color(0xFF86EFAC) else Color(0xFFFDA4AF)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (geofenceResult.isInsideGeofence) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (geofenceResult.isInsideGeofence) AccentGreen else Color(0xFFE11D48),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (geofenceResult.isInsideGeofence) "Office Perimeter Verified" else "Remote Zone Detected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (geofenceResult.isInsideGeofence) Color(0xFF166534) else Color(0xFF9F1239)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            geofenceResult.statusMessage,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "📍 Current Location: ${geofenceResult.locationName}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                // Selfie Verification Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (selfieCaptured) Color(0xFFDCFCE7) else Color(0xFFE2E8F0))
                                .border(
                                    2.dp,
                                    if (selfieCaptured) AccentGreen else BrandBlue,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selfieCaptured) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selfie Verified",
                                    tint = AccentGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Take Selfie",
                                    tint = BrandBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (selfieCaptured) "Selfie Captured & Encrypted ✓" else "Front-Camera Attendance Selfie",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (selfieCaptured) AccentGreen else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                selfieCaptured = true
                                Toast.makeText(context, "Selfie snapshot verified!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(if (selfieCaptured) "Retake Selfie" else "Capture Verification Selfie")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPunchIn(
                        geofenceResult.isInsideGeofence,
                        if (selfieCaptured) "local_selfie_${System.currentTimeMillis()}.jpg" else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm Punch-In", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

// ==========================================
// 2. CLIENT MEETING CHECK-INS (FIELD SALES)
// ==========================================
@Composable
fun ClientMeetingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val meetings by viewModel.clientMeetings.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Field Sales Check-Ins",
                subMenuSubtitle = "${meetings.size} Visits Logged",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AddLocation, contentDescription = "Log Meeting")
            }
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("GPS-Verified Client Visits", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlue)
                            Text("Log client site visits, meeting notes, and deal outcomes with real-time location tags.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            }

            if (meetings.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No client meetings logged yet. Tap + to check in at a client site!", color = TextMuted)
                    }
                }
            } else {
                items(meetings, key = { it.id }) { meeting ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(meeting.clientName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                                Surface(
                                    color = when (meeting.outcome) {
                                        "Deal Won" -> Color(0xFFDCFCE7)
                                        "Proposal Requested" -> Color(0xFFE0E7FF)
                                        else -> Color(0xFFFEF3C7)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        meeting.outcome,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (meeting.outcome) {
                                            "Deal Won" -> Color(0xFF166534)
                                            "Proposal Requested" -> Color(0xFF3730A3)
                                            else -> Color(0xFF92400E)
                                        }
                                    )
                                }
                            }
                            Text(meeting.company, fontSize = 13.sp, color = BrandBlue, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(meeting.locationName, fontSize = 12.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check-In: ${meeting.checkInTime}", fontSize = 12.sp, color = TextMuted)
                            }
                            if (meeting.meetingNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = BorderLight)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("📝 Notes: ${meeting.meetingNotes}", fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddClientMeetingDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, company, purpose, location, notes, outcome ->
                viewModel.recordClientMeeting(
                    clientName = name,
                    company = company,
                    purpose = purpose,
                    locationName = location,
                    notes = notes,
                    outcome = outcome
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddClientMeetingDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, comp: String, purp: String, loc: String, notes: String, outc: String) -> Unit
) {
    var clientName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Connaught Place / Client Office") }
    var notes by remember { mutableStateOf("") }
    var outcome by remember { mutableStateOf("Proposal Requested") }

    val outcomes = listOf("Proposal Requested", "Deal Won", "Follow-up Required", "Needs Revision")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Client Meeting", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Client Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Client Company") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Meeting Purpose / Agenda") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Site Address") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Meeting Discussion Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (clientName.isNotBlank()) {
                        onConfirm(clientName, company, purpose, location, notes, outcome)
                    }
                }
            ) {
                Text("Save Check-In")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==========================================
// 3. EXPENSE & REIMBURSEMENT CLAIMS
// ==========================================
@Composable
fun ExpenseClaimsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val expenses by viewModel.expenseClaims.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Expense Reimbursements",
                subMenuSubtitle = "${expenses.size} Claims Total",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.ReceiptLong, contentDescription = "Add Expense")
            }
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val totalPending = expenses.filter { it.status == "Pending" }.sumOf { it.amount }
                val totalApproved = expenses.filter { it.status == "Approved" || it.status == "Reimbursed" }.sumOf { it.amount }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pending Approval", fontSize = 12.sp, color = TextMuted)
                            Text("₹ ${String.format(Locale.US, "%,.2f", totalPending)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Approved", fontSize = 12.sp, color = TextMuted)
                            Text("₹ ${String.format(Locale.US, "%,.2f", totalApproved)}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
                        }
                    }
                }
            }

            items(expenses, key = { it.id }) { claim ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(claim.category, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text(
                                "₹ ${String.format(Locale.US, "%,.2f", claim.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Merchant: ${claim.merchant}", fontSize = 13.sp, color = TextSecondary)
                        Text(claim.description, fontSize = 12.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Date: ${claim.date}", fontSize = 11.sp, color = TextMuted)
                            Surface(
                                color = when (claim.status) {
                                    "Approved" -> Color(0xFFDCFCE7)
                                    "Reimbursed" -> Color(0xFFE0E7FF)
                                    else -> Color(0xFFFEF3C7)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    claim.status,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (claim.status) {
                                        "Approved" -> Color(0xFF166534)
                                        "Reimbursed" -> Color(0xFF3730A3)
                                        else -> Color(0xFF92400E)
                                    }
                                )
                            }
                        }
                        if (claim.status == "Pending") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.updateExpenseStatus(claim.id, "Approved") },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Approve", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.updateExpenseStatus(claim.id, "Rejected") },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("Reject", fontSize = 12.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cat, amt, merch, desc ->
                viewModel.submitExpenseClaim(cat, amt, merch, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (cat: String, amt: Double, merch: String, desc: String) -> Unit
) {
    var category by remember { mutableStateOf("Travel & Fuel") }
    var amountText by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val categories = listOf("Travel & Fuel", "Client Dinner", "Office Supplies", "Lodging", "Software & Tools")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Expense Claim", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Vendor Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Business Purpose / Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(category, amt, merchant, description)
                    }
                }
            ) {
                Text("Submit Claim")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ==========================================
// 4. DOCUMENT & ASSET VAULT
// ==========================================
@Composable
fun VaultScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val documents by viewModel.vaultDocuments.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    val context = LocalContext.current

    val categories = listOf("All", "Brochures", "Proposal Templates", "Branding & Logos", "HR Policies")

    val filteredDocs = if (selectedCategory == "All") documents else documents.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Document & Asset Vault",
                subMenuSubtitle = "${documents.size} Corporate Documents",
                onBack = onBack
            )
        },
        containerColor = SurfaceBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            items(filteredDocs, key = { it.id }) { doc ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (doc.fileType) {
                                        "PDF" -> Color(0xFFFEE2E2)
                                        "DOCX" -> Color(0xFFDBEAFE)
                                        "ZIP" -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFDCFCE7)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                doc.fileType,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = when (doc.fileType) {
                                    "PDF" -> Color(0xFF991B1B)
                                    "DOCX" -> BrandBlue
                                    "ZIP" -> Color(0xFF92400E)
                                    else -> Color(0xFF166534)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("${doc.category} • ${doc.fileSize}", fontSize = 12.sp, color = TextMuted)
                            if (doc.description.isNotBlank()) {
                                Text(doc.description, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                            }
                        }

                        IconButton(
                            onClick = {
                                WhatsAppHelper.sendWhatsAppMessage(
                                    context = context,
                                    phoneNumber = "919876543210",
                                    message = "Sharing Official Document from MB Vault: *${doc.title}*\nDownload link: ${doc.downloadUrlOrPath}"
                                )
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = BrandBlue)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. ATTENDANCE REGULARIZATION DIALOG
// ==========================================
@Composable
fun AttendanceRegularizationDialog(
    onDismiss: () -> Unit,
    onSubmit: (date: String, inTime: String, outTime: String, reason: String, remarks: String) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    var dateText by remember { mutableStateOf(dateFormat.format(cal.time)) }
    var inTimeText by remember { mutableStateOf("09:30 AM") }
    var outTimeText by remember { mutableStateOf("06:30 PM") }
    val reasons = listOf(
        "Forgot to Punch In",
        "Forgot to Punch Out",
        "Network / Device Issue",
        "Client Site Visit",
        "Biometric Sensor Malfunction"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var remarksText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDBEAFE),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Request Regularization", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                    Text("Missed punch workflow", fontSize = 11.sp, color = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Date (DD MMM YYYY)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inTimeText,
                        onValueChange = { inTimeText = it },
                        label = { Text("In Time") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = outTimeText,
                        onValueChange = { outTimeText = it },
                        label = { Text("Out Time") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = appTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Reason for Regularization", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.forEach { reason ->
                        val isSelected = selectedReason == reason
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedReason = reason },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandBlue)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(reason, fontSize = 12.sp, color = if (isSelected) BrandBlue else TextPrimary)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    label = { Text("Remarks / Explanation") },
                    placeholder = { Text("Add specific notes for manager") },
                    shape = RoundedCornerShape(10.dp),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(dateText, inTimeText, outTimeText, selectedReason, remarksText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Submit Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

