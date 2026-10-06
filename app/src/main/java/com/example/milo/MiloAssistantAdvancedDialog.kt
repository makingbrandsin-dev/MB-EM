package com.example.milo

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Unified Advanced Milo Assistant Dialog (MiloAssistantAdvancedDialog.kt)
 * Combines all 5 Advanced MILO 20X Features into a single, interactive Jetpack Compose sheet:
 * 1. Proactive Pipeline Pulse
 * 2. Hands-Free Voice Mode
 * 3. One-Shot Macro Orchestration
 * 4. Inbound Lead Sentiment & Urgency AI Scanner
 * 5. AI Subtask Breakdown & Smart Estimator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAssistantAdvancedDialog(
    miloViewModel: MiloViewModel,
    onDismiss: () -> Unit,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val miloState by miloViewModel.state.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Macros & Voice, 1: Sentiment Scanner, 2: Task Decomposer, 3: Pipeline Pulse
    var commandInput by remember { mutableStateOf("") }
    var macroResult by remember { mutableStateOf<MacroExecutionResult?>(null) }
    var isExecutingMacro by remember { mutableStateOf(false) }

    // Feature 2: Voice Helper Integration
    val (voiceState, toggleVoice) = rememberMiloVoiceState { recognizedText ->
        commandInput = recognizedText
    }

    // Feature 3: Macro Orchestrator
    val macroOrchestrator = remember { MiloMacroOrchestrator(context) }

    // Feature 4: Lead Sentiment State
    var leadRequirementInput by remember { mutableStateOf("Need urgent pricing quote for 200 items today, budget approved!") }
    var estimatedLeadValue by remember { mutableStateOf("35000") }
    val sentimentAnalysisResult = remember(leadRequirementInput, estimatedLeadValue) {
        val valDouble = estimatedLeadValue.toDoubleOrNull() ?: 0.0
        MiloLeadAnalyzer.analyzeLead(leadRequirementInput, valDouble)
    }

    // Feature 5: Task Decomposer State
    var taskTitleInput by remember { mutableStateOf("Prepare formal proposal for Zenith Retail") }
    var taskDecompositionResult by remember(taskTitleInput) {
        mutableStateOf(MiloTaskDecomposer.decomposeTask(taskTitleInput))
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.50f),
        dragHandle = null,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .imePadding()
                    .padding(16.dp)
            ) {
            // Header: Milo Avatar & Live Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiloCharacter(
                        state = macroResult?.miloState ?: miloState,
                        size = 100.dp,
                        showStateBadge = true
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "MILO 20X AI Assistant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = macroResult?.miloSpeech ?: "🦁 \"Ready for 20X field efficiency!\"",
                            fontSize = 12.sp,
                            color = ButtonPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs for 5 Features
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = BrandBlue,
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("⚡ Voice & Macros", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("🔍 Lead AI Scanner", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("📋 Task Estimator", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    text = { Text("⚠️ Pipeline Pulse", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Contents
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    0 -> MacroAndVoiceTabContent(
                        commandInput = commandInput,
                        onCommandChange = { commandInput = it },
                        voiceState = voiceState,
                        onToggleVoice = toggleVoice,
                        isExecuting = isExecutingMacro,
                        macroResult = macroResult,
                        onExecuteMacro = { cmd ->
                            scope.launch {
                                isExecutingMacro = true
                                val res = macroOrchestrator.executeMacro(cmd)
                                macroResult = res
                                isExecutingMacro = false
                                if (res.triggerConfetti) {
                                    Toast.makeText(context, "🎉 Celebration! Deal Closed!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    1 -> LeadSentimentTabContent(
                        requirement = leadRequirementInput,
                        onRequirementChange = { leadRequirementInput = it },
                        value = estimatedLeadValue,
                        onValueChange = { estimatedLeadValue = it },
                        result = sentimentAnalysisResult,
                        onOpenLeads = onNavigateToLeads
                    )

                    2 -> TaskDecomposerTabContent(
                        taskTitle = taskTitleInput,
                        onTaskTitleChange = { taskTitleInput = it },
                        decompositionResult = taskDecompositionResult,
                        onUpdateResult = { taskDecompositionResult = it },
                        onOpenTasks = onNavigateToTasks
                    )

                    3 -> PipelinePulseTabContent()
                }
            }
        }
    }
}
}

@Composable
private fun MacroAndVoiceTabContent(
    commandInput: String,
    onCommandChange: (String) -> Unit,
    voiceState: MiloVoiceState,
    onToggleVoice: () -> Unit,
    isExecuting: Boolean,
    macroResult: MacroExecutionResult?,
    onExecuteMacro: (String) -> Unit
) {
    val quickMacros = listOf(
        "We closed the deal with Zenith Retail for ₹50,000",
        "Add lead Apex Corp phone 9876543210 requirement Urgent",
        "Schedule follow up with Metro Supplies tomorrow",
        "Broadcast update to sales team"
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Feature 2 & 3: Hands-Free Voice & One-Shot Macro",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = commandInput,
                        onValueChange = onCommandChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type or speak command (e.g. 'We closed deal with...')", fontSize = 13.sp) },
                        trailingIcon = {
                            IconButton(onClick = onToggleVoice) {
                                Icon(
                                    imageVector = if (voiceState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (voiceState.isListening) StatusOrange else BrandBlue
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBlue,
                            unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    if (voiceState.isListening) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🎙️ Listening... Speak your field command",
                            fontSize = 12.sp,
                            color = StatusOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (voiceState.error != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "⚠️ ${voiceState.error}",
                            fontSize = 12.sp,
                            color = StatusRed
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (commandInput.isNotBlank()) {
                                onExecuteMacro(commandInput)
                            }
                        },
                        enabled = commandInput.isNotBlank() && !isExecuting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        if (isExecuting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Orchestrating Workflow...")
                        } else {
                            Icon(Icons.Default.FlashOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run One-Shot Macro")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "⚡ Quick One-Shot Shortcuts",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
        }

        items(quickMacros) { macro ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCommandChange(macro)
                        onExecuteMacro(macro)
                    },
                colors = CardDefaults.cardColors(containerColor = SurfaceBg),
                border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = macro, fontSize = 12.sp, color = TextPrimary)
                }
            }
        }

        macroResult?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.macroName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (result.triggerConfetti) {
                                Text("🎉 WINNER!", fontSize = 12.sp, color = StatusGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        result.actionsPerformed.forEach { action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = action.actionType, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                    Text(text = action.description, fontSize = 11.sp, color = TextSecondary)
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
private fun LeadSentimentTabContent(
    requirement: String,
    onRequirementChange: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    result: LeadAnalysisResult,
    onOpenLeads: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Feature 4: Lead Sentiment & Urgency AI Scanner",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = requirement,
                    onValueChange = onRequirementChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Requirement Text", fontSize = 12.sp) },
                    singleLine = false,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Estimated Deal Value (₹)", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Live Scanner Result Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when (result.sentiment) {
                    LeadSentiment.URGENT, LeadSentiment.HIGH_RISK -> StatusOrange.copy(alpha = 0.1f)
                    LeadSentiment.POSITIVE -> StatusGreen.copy(alpha = 0.1f)
                    LeadSentiment.NEUTRAL -> CardBg
                }
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = 1.dp,
                color = when (result.sentiment) {
                    LeadSentiment.URGENT, LeadSentiment.HIGH_RISK -> StatusOrange
                    LeadSentiment.POSITIVE -> StatusGreen
                    LeadSentiment.NEUTRAL -> TextSecondary.copy(alpha = 0.2f)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Urgency Score: ${result.urgencyScore}/100",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        color = when (result.suggestedPriority) {
                            "URGENT" -> StatusRed
                            "HIGH" -> StatusOrange
                            "MEDIUM" -> BrandBlue
                            else -> TextSecondary
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = result.suggestedPriority,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = result.actionRecommendation,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                if (result.detectedKeywords.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "Keywords Detected:", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        result.detectedKeywords.forEach { kw ->
                            Surface(
                                color = BrandBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = kw,
                                    fontSize = 10.sp,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOpenLeads,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Apply Scanner to All CRM Leads")
                }
            }
        }
    }
}

@Composable
private fun TaskDecomposerTabContent(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    decompositionResult: TaskDecompositionResult,
    onUpdateResult: (TaskDecompositionResult) -> Unit,
    onOpenTasks: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Feature 5: AI Subtask Breakdown & Smart Estimator",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = taskTitle,
                    onValueChange = onTaskTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Main Task Title", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Decomposed Subtasks (${decompositionResult.subtasks.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        color = BrandBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "⏱️ ${decompositionResult.totalEstimatedMinutes} mins total",
                            fontSize = 11.sp,
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                decompositionResult.subtasks.forEachIndexed { index, subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                val updated = decompositionResult.subtasks.toMutableList()
                                updated[index] = subtask.copy(isCompleted = !subtask.isCompleted)
                                onUpdateResult(decompositionResult.copy(subtasks = updated))
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { isChecked ->
                                val updated = decompositionResult.subtasks.toMutableList()
                                updated[index] = subtask.copy(isCompleted = isChecked)
                                onUpdateResult(decompositionResult.copy(subtasks = updated))
                            },
                            colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = subtask.title,
                            fontSize = 12.sp,
                            color = if (subtask.isCompleted) TextSecondary else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${subtask.estimatedMinutes}m",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOpenTasks,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("Add Subtasks to Task Board")
                }
            }
        }
    }
}

@Composable
private fun PipelinePulseTabContent() {
    val context = LocalContext.current
    var isScheduling by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Feature 1: Proactive Pipeline Pulse",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Background WorkManager scans database daily, flags stale high-value leads (> ₹10,000 uncontacted for 2+ hours), and dispatches proactive notifications with Milo's WARNING pose.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = StatusGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("WorkManager Background Job Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Interval: Every 24 hours | Auto Pulse Guard", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        isScheduling = true
                        MiloPipelinePulseWorker.schedulePeriodicPulse(context)
                        Toast.makeText(context, "Milo Pipeline Pulse Enqueued Successfully!", Toast.LENGTH_SHORT).show()
                        isScheduling = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Daily Pulse WorkManager Job")
                }
            }
        }
    }
}
