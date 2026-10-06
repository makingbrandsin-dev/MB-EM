package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MiloMessage(
    val id: String,
    val sender: String,
    val text: String,
    val actionExecuted: Boolean = false
)

fun executeMiloIntent(rawInput: String, viewModel: MainViewModel): String {
    val trimmed = rawInput.trim()
    val lower = trimmed.lowercase()

    return when {
        // 1. ADD LEAD INTENT: "Add lead Ramesh | ABC Corp | +91 9876543210 | Website" or "Add lead Rahul"
        lower.startsWith("add lead") || lower.startsWith("new lead") || lower.startsWith("create lead") -> {
            val content = trimmed.substringAfter("lead", "").trim().removePrefix(":").trim()
            if (content.isEmpty()) {
                "🦁 Roar! To hatch a lead, use format:\n`Add lead Name | Company | Phone | Requirement`\nExample: `Add lead Ramesh | ABC Corp | +91 9876543210 | Website`"
            } else {
                val parts = content.split("|").map { it.trim() }
                val name = parts.getOrNull(0)?.ifEmpty { "New Customer" } ?: "New Customer"
                val company = parts.getOrNull(1)?.ifEmpty { "Making Brands Client" } ?: "Making Brands Client"
                val phone = parts.getOrNull(2)?.ifEmpty { "+91 98765 43210" } ?: "+91 98765 43210"
                val requirement = parts.getOrNull(3)?.ifEmpty { "Branding & Digital Marketing" } ?: "Branding & Digital Marketing"

                viewModel.addLead(
                    name = name,
                    company = company,
                    phone = phone,
                    requirement = requirement,
                    value = "₹ 2,50,000",
                    stage = "New",
                    score = (75..95).random(),
                    source = "MILO AI Assistant"
                )

                "🦁 ROAR! Lead hatched and locked into CRM!\n\n📋 **Customer:** $name\n🏢 **Company:** $company\n📞 **Phone:** $phone\n🎯 **Requirement:** $requirement\n💼 **Stage:** New (Score: 88)\n\nSynced to Firebase & ready for follow-up! ⚡"
            }
        }

        // 2. CHECK LEAD STATUS INTENT: "Check status of lead Ramesh" or "lead status Ramesh"
        lower.contains("status of lead") || (lower.contains("lead") && lower.contains("status")) -> {
            val query = trimmed.substringAfter("lead", "").replace("status", "").replace("of", "").replace("for", "").trim().lowercase()
            val allLeads = viewModel.leads.value
            val match = if (query.isEmpty()) {
                allLeads.firstOrNull()
            } else {
                allLeads.firstOrNull { it.name.lowercase().contains(query) || it.company.lowercase().contains(query) }
            }

            if (match != null) {
                "🦁 Found CRM Record for **${match.name}**!\n\n🏢 **Company:** ${match.company}\n📊 **Stage:** ${match.stage}\n⭐ **Lead Score:** ${match.leadScore}/100\n💰 **Potential Value:** ${match.potentialValue}\n📅 **Next Follow-Up:** ${match.nextFollowUp}\n🎯 **Requirement:** ${match.requirement}"
            } else {
                "🦁 Couldn't spot an active lead matching '$query' in the pride CRM. You can add one anytime by saying: `Add lead Name | Company | Phone`!"
            }
        }

        // 3. HOLIDAYS INTENT: "Next month holidays" or "holidays" or "leave"
        lower.contains("holiday") || lower.contains("holidays") || lower.contains("vacation") -> {
            "🦁 Here are the upcoming official Making Brands holidays:\n\n🎉 **02 Oct 2026:** Mahatma Gandhi Jayanti (Friday)\n🪔 **20 Oct 2026:** Dussehra / Vijayadashami (Tuesday)\n✨ **08 Nov 2026:** Diwali / Deepavali (Sunday)\n🌟 **09 Nov 2026:** Diwali Balipratipada (Monday)\n\nPlan your deliverables and recharge with pride! 🦁🌴"
        }

        // 4. TASK / REMINDER INTENT: "Remind me: ..." or "Add task ..." or "Create task ..."
        lower.startsWith("remind me") || lower.startsWith("add task") || lower.startsWith("create task") || lower.startsWith("task:") -> {
            val title = when {
                lower.startsWith("remind me:") -> trimmed.substringAfter("remind me:").trim()
                lower.startsWith("remind me") -> trimmed.substringAfter("remind me").trim()
                lower.startsWith("add task:") -> trimmed.substringAfter("add task:").trim()
                lower.startsWith("add task") -> trimmed.substringAfter("add task").trim()
                lower.startsWith("create task") -> trimmed.substringAfter("create task").trim()
                else -> trimmed.substringAfter("task:").trim()
            }

            if (title.isBlank()) {
                "🦁 What would you like me to lock into your task queue? Example: `Remind me: Call technical director at 4 PM`"
            } else {
                viewModel.addTask(
                    title = title,
                    projectName = "Priority Operations",
                    priority = "High",
                    dueDate = "Today",
                    category = "Work",
                    estimatedTimeNeeded = "2 Hours",
                    assignee = viewModel.currentEmployeeName.value
                )
                "🦁 ROAR! Task locked in!\n\n📌 **Task:** $title\n⚡ **Priority:** High\n📅 **Due:** Today\n👤 **Assignee:** ${viewModel.currentEmployeeName.value}\n\nAdded to your daily task list and synced!"
            }
        }

        // 5. ATTENDANCE INTENT: "Attendance" or "Check in status" or "hours"
        lower.contains("attendance") || lower.contains("check in") || lower.contains("check out") || lower.contains("hours") -> {
            val att = viewModel.latestAttendance.value
            val isWorking = att?.isWorking ?: false
            val checkIn = att?.checkInTime ?: "--:--"
            val duration = com.example.ui.components.formatLiveSeconds(viewModel.liveActiveDurationSeconds.value)
            val status = if (isWorking) "Active • Checked In at $checkIn ($duration)" else "Off Duty • Ready to Check In"
            "🦁 Attendance Telemetry for **${viewModel.currentEmployeeName.value}**:\n\n⏱️ **Status:** $status\n🏢 **Office Geofence:** 500m MB HQ\n🛡️ **Sync:** Live Cloud Synced"
        }

        // 6. MOTIVATION / ROAR
        lower.contains("roar") || lower.contains("motivat") || lower.contains("energy") || lower.contains("cheer") -> {
            "🦁 **ROARRRR!** You belong to the Making Brands Pride! Great brands aren't built on average days — let's close those deals, ship pixel-perfect code, and make this a 20X productive day! ⚡🔥"
        }

        // 7. DEFAULT HELPFUL EXECUTIVE ASSISTANT
        else -> {
            "🦁 MILO heard: \"$trimmed\"\n\nI can help you right away! Try commands like:\n• `Add lead Ramesh | ABC Corp | +91 9876543210 | Website`\n• `Check status of lead Ramesh`\n• `Next month holidays`\n• `Remind me: Review client presentation at 5 PM`\n• `Attendance status`\n\nWhat shall we conquer next, teammate? ⚡"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAssistantDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var miloMood by remember { mutableStateOf(MiloMood.AWAKE_IDLE) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Initial greeting haptic pulse on dialog opening
    LaunchedEffect(Unit) {
        MiloHaptics.performWakeUp(context, hapticFeedback)
    }

    // Auto-sleep timer: if no chat action for 15 seconds, Milo goes to SLEEPING
    LaunchedEffect(lastInteractionTime) {
        delay(15000)
        if (System.currentTimeMillis() - lastInteractionTime >= 15000 &&
            miloMood != MiloMood.LISTENING &&
            miloMood != MiloMood.THINKING &&
            miloMood != MiloMood.SPEAKING
        ) {
            miloMood = MiloMood.SLEEPING
            MiloHaptics.performSleep(context, hapticFeedback)
        }
    }

    val messages = remember {
        mutableStateListOf(
            MiloMessage(
                id = "1",
                sender = "MILO",
                text = "🦁 **ROAR! MILO is in the Pride!**\n\nI'm your 20X AI Executive Companion. What can I do for you right now?\n\n• Hatch or check CRM Leads\n• Check upcoming company holidays\n• Lock tasks & reminders into your queue\n• Check your live attendance telemetry"
            )
        )
    }

    val quickPrompts = remember {
        listOf(
            "Add lead Ramesh | ABC Corp | +91 9876543210 | Website",
            "Check status of lead Ramesh",
            "Next month holidays",
            "Remind me: Call technical director at 4 PM",
            "Attendance status",
            "Give me a motivation roar!"
        )
    }

    val processMiloCommand = { rawInput: String ->
        val text = rawInput.trim()
        if (text.isNotEmpty()) {
            MiloHaptics.performButtonTap(context, hapticFeedback)
            lastInteractionTime = System.currentTimeMillis()
            miloMood = MiloMood.LISTENING
            messages.add(MiloMessage(id = System.currentTimeMillis().toString(), sender = "You", text = text))
            inputText = ""

            coroutineScope.launch {
                miloMood = MiloMood.THINKING
                MiloHaptics.performThinking(context, hapticFeedback)
                delay(500) // Realistic processing pulse
                miloMood = MiloMood.SPEAKING
                val responseText = executeMiloIntent(text, viewModel)
                val isActionExecution = responseText.contains("🦁 **Milo Synced") || responseText.contains("🦁 **Task Locked")
                if (isActionExecution) {
                    MiloHaptics.performActionSuccess(context, hapticFeedback)
                } else {
                    MiloHaptics.performRoarResponse(context, hapticFeedback)
                }
                messages.add(
                    MiloMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        sender = "MILO",
                        text = responseText,
                        actionExecuted = isActionExecution
                    )
                )
                delay(100)
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
                delay(1200)
                miloMood = MiloMood.AWAKE_IDLE
                lastInteractionTime = System.currentTimeMillis()
            }
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.70f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SurfaceBg,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar with Milo Mascot & Dynamic State Badge
                    Surface(
                        color = BrandDarkBlue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Top Drag Indicator Pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 8.dp)
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Animated Milo Avatar / Video Widget based on live status
                                    val matchingState = when (miloMood) {
                                        MiloMood.SLEEPING -> com.example.domain.milo.MiloState.GOODBYE
                                        MiloMood.THINKING -> com.example.domain.milo.MiloState.THINKING
                                        MiloMood.LISTENING -> com.example.domain.milo.MiloState.WORKING
                                        MiloMood.SPEAKING -> com.example.domain.milo.MiloState.CELEBRATION
                                        MiloMood.AWAKE_IDLE -> com.example.domain.milo.MiloState.WELCOME
                                    }
                                    val popupVideoUri = remember(miloMood) {
                                        com.example.milo.MiloVideoHelper.getMiloVideoUri(context, matchingState)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clickable {
                                                lastInteractionTime = System.currentTimeMillis()
                                                miloMood = if (miloMood == MiloMood.SLEEPING) MiloMood.AWAKE_IDLE else MiloMood.LISTENING
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (popupVideoUri != null) {
                                            com.example.milo.MiloVideoSurface(
                                                videoUri = popupVideoUri,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            MiloAnimatedAvatar(
                                                mood = miloMood,
                                                sizeDp = 50,
                                                onClick = {
                                                    lastInteractionTime = System.currentTimeMillis()
                                                    miloMood = if (miloMood == MiloMood.SLEEPING) MiloMood.AWAKE_IDLE else MiloMood.LISTENING
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                "Milo Smart Assistant",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = Color.White,
                                                letterSpacing = 0.3.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = BrandAccent
                                            ) {
                                                Text(
                                                    text = "20X AI",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BrandDarkBlue,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = when (miloMood) {
                                                MiloMood.SLEEPING -> Color(0xFF475569)
                                                MiloMood.LISTENING -> Color(0xFF065F46)
                                                MiloMood.THINKING -> Color(0xFF1E3A8A)
                                                MiloMood.SPEAKING -> Color(0xFFB45309)
                                                MiloMood.AWAKE_IDLE -> Color(0xFF1E293B)
                                            }
                                        ) {
                                            Text(
                                                text = when (miloMood) {
                                                    MiloMood.SLEEPING -> "💤 Sleeping · Tap mascot to wake"
                                                    MiloMood.LISTENING -> "👂 Listening to command..."
                                                    MiloMood.THINKING -> "⚡ Thinking & crunching CRM..."
                                                    MiloMood.SPEAKING -> "🦁 Roaring executive reply..."
                                                    MiloMood.AWAKE_IDLE -> "⚡ Active Companion · Online"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when (miloMood) {
                                                    MiloMood.SLEEPING -> Color(0xFFCBD5E1)
                                                    MiloMood.LISTENING -> Color(0xFF6EE7B7)
                                                    MiloMood.THINKING -> Color(0xFF93C5FD)
                                                    MiloMood.SPEAKING -> Color(0xFFFDE68A)
                                                    MiloMood.AWAKE_IDLE -> BrandAccent
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close Milo Assistant",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Chat Messages Feed
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMilo = msg.sender == "MILO"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        lastInteractionTime = System.currentTimeMillis()
                                        if (miloMood == MiloMood.SLEEPING) {
                                            MiloHaptics.performWakeUp(context, hapticFeedback)
                                            miloMood = MiloMood.AWAKE_IDLE
                                        }
                                    },
                                horizontalArrangement = if (isMilo) Arrangement.Start else Arrangement.End,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isMilo) {
                                    MiloMiniAvatar(
                                        sizeDp = 32,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(
                                    modifier = Modifier.widthIn(max = 300.dp),
                                    horizontalAlignment = if (isMilo) Alignment.Start else Alignment.End
                                ) {
                                    var isExpanded by remember { mutableStateOf(false) }
                                    val isLongMessage = isMilo && (msg.text.length > 150 || msg.text.contains("Proposal", ignoreCase = true) || msg.text.contains("Template", ignoreCase = true))

                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMilo) 4.dp else 16.dp,
                                            bottomEnd = if (isMilo) 16.dp else 4.dp
                                        ),
                                        color = if (isMilo) Color.White else BrandBlue,
                                        border = if (isMilo) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                                        shadowElevation = 2.dp
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = if (isMilo) "MILO AI" else "You",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = if (isMilo) BrandDarkBlue else Color.White.copy(alpha = 0.85f)
                                                )
                                                if (isMilo && msg.actionExecuted) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFFDCFCE7)
                                                    ) {
                                                        Text(
                                                            text = "✓ SYNCED",
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF15803D),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            if (isLongMessage) {
                                                val displayText = if (isExpanded) msg.text else msg.text.take(130) + "..."
                                                Text(
                                                    text = displayText,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    lineHeight = 19.sp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = if (isExpanded) "Collapse Draft ▴" else "Expand Draft Accordion ▾",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BrandBlue,
                                                    modifier = Modifier
                                                        .clickable { isExpanded = !isExpanded }
                                                        .padding(vertical = 4.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = msg.text,
                                                    fontSize = 13.sp,
                                                    color = if (isMilo) TextPrimary else Color.White,
                                                    lineHeight = 19.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Thinking Indicator Bubble
                        if (miloMood == MiloMood.THINKING) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MiloMiniAvatar(sizeDp = 28)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shadowElevation = 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "🦁 Milo is crunching...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = BrandBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Quick Action Suggestion Chips
                    Surface(
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(quickPrompts) { prompt ->
                                    val shortLabel = when {
                                        prompt.startsWith("Add lead") -> "🦁 Add Lead"
                                        prompt.startsWith("Check status") -> "📋 Lead Status"
                                        prompt.startsWith("Next month") -> "🏖️ Next Holidays"
                                        prompt.startsWith("Remind me") -> "⏰ Add Task"
                                        prompt.startsWith("Attendance") -> "⏱️ Attendance"
                                        else -> "⚡ Roar Motivation"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.clickable { processMiloCommand(prompt) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = shortLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandDarkBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Input Bar with clean actions & keyboard send handling
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = {
                                    if (miloMood == MiloMood.SLEEPING) {
                                        MiloHaptics.performWakeUp(context, hapticFeedback)
                                        miloMood = MiloMood.LISTENING
                                    }
                                    inputText = it
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                placeholder = {
                                    Text(
                                        "Ask Milo (e.g. Add lead, Holidays)...",
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = appTextFieldColors(),
                                trailingIcon = {
                                    if (inputText.isNotBlank()) {
                                        IconButton(onClick = { inputText = "" }) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Send
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSend = {
                                        if (inputText.isNotBlank()) {
                                            processMiloCommand(inputText)
                                        }
                                    }
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            FloatingActionButton(
                                onClick = { processMiloCommand(inputText) },
                                containerColor = if (inputText.isNotBlank()) BrandBlue else BrandDarkBlue,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

