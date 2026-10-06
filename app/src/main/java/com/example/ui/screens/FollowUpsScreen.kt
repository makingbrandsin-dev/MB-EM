package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FollowUpEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*

@Composable
fun FollowUpsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val followUps by viewModel.followUps.collectAsState()
    val leads by viewModel.leads.collectAsState()
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    val filters = listOf("Today", "Upcoming", "Completed")

    val filteredFollowUps = remember(followUps, selectedFilterIndex) {
        val category = filters[selectedFilterIndex]
        followUps.filter { item ->
            if (category == "Completed") {
                item.isCompleted || item.scheduledDateCategory.equals("Completed", ignoreCase = true)
            } else {
                item.scheduledDateCategory.equals(category, ignoreCase = true) && !item.isCompleted
            }
        }.sortedByDescending { it.id }
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Client Follow-ups",
                subMenuSubtitle = "${filteredFollowUps.size} Scheduled Reminders",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Follow-up", tint = BrandBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Schedule Follow-up")
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Segmented Filter Tabs
            item {
                TabRow(
                    selectedTabIndex = selectedFilterIndex,
                    containerColor = Color.White
                ) {
                    filters.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Follow-up Items
            if (filteredFollowUps.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No Follow-ups in ${filters[selectedFilterIndex]}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text("All scheduled customer follow-ups will appear here.", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
            } else {
                items(filteredFollowUps) { item ->
                    val matchingLead = leads.firstOrNull { it.id == item.leadId } ?: leads.firstOrNull { it.name.equals(item.clientName, ignoreCase = true) }
                    val clientPhone = matchingLead?.phone ?: "+91 98765 43210"
                    val clientEmail = matchingLead?.email?.ifBlank { "client@makingbrands.in" } ?: "client@makingbrands.in"

                    FollowUpCardItem(
                        item = item,
                        clientPhone = clientPhone,
                        clientEmail = clientEmail,
                        onComplete = { viewModel.toggleFollowUpCompletion(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var clientName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var scheduledTime by remember { mutableStateOf("04:30 PM") }
        var actionType by remember { mutableStateOf("Call") }
        var dateCategory by remember { mutableStateOf(filters[selectedFilterIndex].let { if (it == "Completed") "Today" else it }) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("Schedule Follow-Up", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Lead Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Follow-up Note / Objective *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("Scheduled Time") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Call", "WhatsApp", "Email", "Meeting").forEach { type ->
                            FilterChip(
                                selected = actionType == type,
                                onClick = { actionType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank() && description.isNotBlank()) {
                            viewModel.addFollowUp(
                                clientName = clientName.trim(),
                                taskDescription = description.trim(),
                                scheduledTime = scheduledTime,
                                actionType = actionType,
                                category = dateCategory
                            )
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Schedule", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun FollowUpCardItem(
    item: FollowUpEntity,
    clientPhone: String = "+91 98765 43210",
    clientEmail: String = "client@makingbrands.in",
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandBlue.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            when (item.actionType.lowercase()) {
                                "call" -> Icons.Default.Phone
                                "whatsapp" -> Icons.Default.Chat
                                "email" -> Icons.Default.Email
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(item.clientName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                    Text(item.taskDescription, fontSize = 12.sp, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(item.scheduledTime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            when (item.actionType.lowercase()) {
                                "call" -> {
                                    com.example.util.WhatsAppHelper.dialPhoneNumber(context, clientPhone)
                                }
                                "whatsapp" -> {
                                    com.example.util.WhatsAppHelper.sendWhatsAppMessage(
                                        context = context,
                                        phoneNumber = clientPhone,
                                        message = "Hello ${item.clientName}, following up regarding: ${item.taskDescription}"
                                    )
                                }
                                "email" -> {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$clientEmail")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "Follow-up: ${item.clientName}")
                                        putExtra(Intent.EXTRA_TEXT, "Hello ${item.clientName},\n\nFollowing up regarding ${item.taskDescription}.\n\nBest regards,\nMaking Brands Team")
                                    }
                                    try {
                                        context.startActivity(emailIntent)
                                    } catch (_: Exception) {}
                                }
                                else -> onComplete()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.actionType == "Call") StatusGreen else BrandBlue
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(item.actionType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.Done,
                            contentDescription = "Done",
                            tint = if (item.isCompleted) StatusGreen else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
