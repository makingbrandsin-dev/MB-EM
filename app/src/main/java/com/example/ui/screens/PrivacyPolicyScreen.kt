package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*

/**
 * 🛡️ Google Play Policy-Compliant In-App Privacy & Data Safety Center.
 *
 * Provides comprehensive disclosures on data collection, permissions,
 * encryption standards, retention periods, and user account deletion controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteAccountDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = StatusRed,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Request Account & Data Deletion?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action will permanently delete your user profile, authentication credentials, and personal records from Making Brands servers and local storage.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                    Text(
                        text = "• Company audit records (e.g. historical invoices or past finalized attendance logs) may be retained by your employer organization as required by statutory law.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        viewModel.deleteUserAccount { success, errorMsg ->
                            isDeleting = false
                            showDeleteAccountDialog = false
                            if (success) {
                                Toast.makeText(context, "Account and data successfully deleted", Toast.LENGTH_LONG).show()
                                onBack()
                            } else {
                                Toast.makeText(context, errorMsg ?: "Deletion failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Permanently Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Privacy & Data Safety",
                subMenuSubtitle = "Google Play Policy & Compliance",
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
            // Header Hero Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ElectricBlueBg,
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                "Your Privacy is Protected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlue
                            )
                            Text(
                                "MB EM complies with Google Play Privacy, Permissions, and Data Safety standards.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // 1. Data Collected
            item {
                PolicySectionCard(
                    icon = Icons.Default.Inventory,
                    iconColor = BrandBlue,
                    title = "1. Data We Collect & Access",
                    items = listOf(
                        "User Identity" to "Employee Name, corporate email, phone number, designation, and company department to manage role-based workplace access.",
                        "Foreground Attendance Location" to "Approximate & precise GPS coordinates collected strictly at clock-in to verify presence within the office geofence (250m). NO background continuous tracking.",
                        "Attendance Selfie" to "Single front-camera snapshot taken upon clock-in to confirm physical presence. No continuous video or surveillance.",
                        "Team Communications" to "Workplace chat messages, task assignments, and user-initiated voice notes recorded on demand.",
                        "Business Records" to "CRM leads, invoices, quotations, and expense receipt images uploaded via Android Photo Picker."
                    )
                )
            }

            // 2. High-Risk Permissions Strictly Prohibited
            item {
                PolicySectionCard(
                    icon = Icons.Default.GppGood,
                    iconColor = Color(0xFF059669),
                    title = "2. Permissions Audit & Prohibitions",
                    items = listOf(
                        "No Call Log Access" to "MB EM does NOT request READ_CALL_LOG or WRITE_CALL_LOG. We do not inspect your phone call history.",
                        "No Telephony Interception" to "PROCESS_OUTGOING_CALLS and READ_PHONE_STATE are excluded. Calls to leads are user-initiated via the Android system dialer (Intent.ACTION_DIAL).",
                        "No Contact Book Uploads" to "READ_CONTACTS is removed. The app only accesses client leads saved inside the enterprise database.",
                        "No Background Audio" to "Microphone is accessed ONLY while you actively press the voice note button in Team Chat. Background recording is strictly disabled."
                    )
                )
            }

            // 3. Security & Cloud Safeguards
            item {
                PolicySectionCard(
                    icon = Icons.Default.Lock,
                    iconColor = Color(0xFFD97706),
                    title = "3. Security & Encryption",
                    items = listOf(
                        "Encryption in Transit" to "All network requests and database synchronization use TLS 1.3 with HTTPS.",
                        "App Check Protection" to "Firebase App Check validates device and app integrity to prevent unauthorized bots or modified binaries.",
                        "Android Backup Security" to "Sensitive authentication tokens and Room offline databases are excluded from cloud backup and device transfer.",
                        "Biometric Protection" to "Local app gate supports Android native BiometricPrompt (fingerprint and face unlock)."
                    )
                )
            }

            // 4. Account Deletion Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBox, contentDescription = null, tint = StatusRed, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("4. Account & Data Deletion", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "In accordance with Google Play Account Deletion requirements, you have the right to request deletion of your account and all associated personal data.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://makingbrands.com/privacy/delete-account")
                                    )
                                    try {
                                        context.startActivity(browserIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Web portal: https://makingbrands.com/privacy/delete-account", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Web Portal", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showDeleteAccountDialog = true },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 5. Contact & Data Protection Officer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("5. Data Protection Contact", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "For privacy inquiries, GDPR/CCPA data export requests, or concerns, contact our Data Protection Office at:\nprivacy@makingbrands.com\nMaking Brands HQ, Connaught Place, New Delhi 110001",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    items: List<Pair<String, String>>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEach { (label, desc) ->
                    Column {
                        Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}
