package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloAiResponse
import com.example.domain.milo.MiloFirebaseAiService
import com.example.domain.milo.MiloState
import com.example.domain.milo.MiloViewModel
import com.example.presentation.components.milo.MiloCharacter
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Dedicated debug and testing screen for the Milo Character component and FirebaseAI / Gemini integration.
 * Allows direct prompt testing, state inspection, and raw JSON schema validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloDebugScreen(
    viewModel: MainViewModel,
    miloViewModel: MiloViewModel = androidx.compose.runtime.remember { MiloViewModel() },
    onBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { MiloFirebaseAiService() }

    val currentState by miloViewModel.state.collectAsState()
    val speechText by miloViewModel.speechText.collectAsState()
    val subSpeechText by miloViewModel.subSpeechText.collectAsState()
    val suggestedAction by miloViewModel.suggestedAction.collectAsState()

    var testPrompt by remember { mutableStateOf("12 new leads imported from Meta Ads, 4 need quick follow up") }
    var isLoadingAi by remember { mutableStateOf(false) }
    var lastAiResponse by remember { mutableStateOf<MiloAiResponse?>(null) }
    var rawJsonOutput by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Live AI Test, 1: State Matrix (13 States), 2: Schema Spec

    val presetPrompts = listOf(
        "12 new leads imported from Justdial" to MiloState.LEAD_IMPORTED,
        "Won ₹ 1,50,000 deal with Apex Solutions!" to MiloState.CONVERTED,
        "8 client follow-up calls due today" to MiloState.FOLLOW_UP,
        "Completed SEO & Google Ads setup task" to MiloState.SUCCESS,
        "3 high-value leads inactive for 4 days" to MiloState.WARNING,
        "Employee logged in for morning shift" to MiloState.WELCOME,
        "Team target exceeded 100% this month!" to MiloState.CELEBRATION
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricBlueBg,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🦁", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Milo AI Debug Lab", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("FirebaseAI & Gemini Model Inspector", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("milo_debug_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { miloViewModel.reset() },
                        modifier = Modifier.testTag("milo_debug_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset State", tint = ElectricBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = SurfaceBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // Live Milo Preview Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = currentState.primaryColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = currentState.primaryColor,
                                        modifier = Modifier.size(6.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "STATE: ${currentState.name}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentState.primaryColor
                                    )
                                }
                            }

                            Text(
                                text = "Interactive Canvas",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Character Render
                        MiloCharacter(
                            state = currentState,
                            size = 130.dp,
                            modifier = Modifier.testTag("milo_debug_character_canvas")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Speech Bubble
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = speechText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = subSpeechText,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569)
                                )
                                if (suggestedAction != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = ElectricBlueBg
                                    ) {
                                        Text(
                                            text = "Suggested Action: $suggestedAction",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElectricBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Selector: Live Test vs State Matrix vs Schema Spec
            item {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White,
                    contentColor = ElectricBlue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Prompt Test", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("milo_tab_prompt_test")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("13 State Matrix", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("milo_tab_state_matrix")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Schema & Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        modifier = Modifier.testTag("milo_tab_schema_logs")
                    )
                }
            }

            // Tab 0: Direct Prompt & FirebaseAI Generator
            if (activeTab == 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Test Custom Prompt on FirebaseAI", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            Text("Sends prompt to Gemini API with JSON structured schema enforcement", fontSize = 11.sp, color = TextSecondary)

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = testPrompt,
                                onValueChange = { testPrompt = it },
                                label = { Text("User Prompt / Event Description") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("milo_prompt_input"),
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    isLoadingAi = true
                                    coroutineScope.launch {
                                        val response = aiService.generateMiloResponse(
                                            userPrompt = testPrompt,
                                            currentMiloState = currentState
                                        )
                                        lastAiResponse = response
                                        rawJsonOutput = response.rawJson
                                        isLoadingAi = false

                                        // Apply to Milo state machine
                                        miloViewModel.setState(response.state)
                                    }
                                },
                                enabled = !isLoadingAi && testPrompt.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("milo_run_ai_prompt_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                            ) {
                                if (isLoadingAi) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Calling Gemini Model...", fontSize = 13.sp)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate AI Response", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Preset Test Scenarios
                item {
                    Text("Quick Preset Scenarios", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }

                items(presetPrompts) { (presetText, targetState) ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                testPrompt = presetText
                                isLoadingAi = true
                                coroutineScope.launch {
                                    val response = aiService.generateMiloResponse(
                                        userPrompt = presetText,
                                        currentMiloState = currentState
                                    )
                                    lastAiResponse = response
                                    rawJsonOutput = response.rawJson
                                    isLoadingAi = false
                                    miloViewModel.setState(response.state)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(presetText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Expected State: ${targetState.name}", fontSize = 11.sp, color = targetState.primaryColor)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = ElectricBlue)
                        }
                    }
                }
            }

            // Tab 1: Direct 13-State Matrix Switcher
            if (activeTab == 1) {
                item {
                    Text("Direct State Switcher (13 Emotional Expressions)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                }

                items(MiloState.values()) { state ->
                    val isCurrent = state == currentState
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) state.primaryColor.copy(alpha = 0.08f) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isCurrent) state.primaryColor else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                miloViewModel.setState(state)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(state.emoji, fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = state.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) state.primaryColor else TextPrimary
                                    )
                                    Text(
                                        text = state.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            if (isCurrent) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = state.primaryColor
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Schema Spec & Raw JSON Inspector
            if (activeTab == 2) {
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
                                Text("JSON Output & Reasoning", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (lastAiResponse?.isSuccess == true) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (lastAiResponse?.isSuccess == true) "Schema Valid" else "Standby",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lastAiResponse?.isSuccess == true) Color(0xFF166534) else Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (lastAiResponse?.reasoning != null) {
                                Text("AI Reasoning:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                Text(lastAiResponse!!.reasoning, fontSize = 12.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            val jsonTextToShow = rawJsonOutput ?: """
                                {
                                  "state": "${currentState.name}",
                                  "speech": "$speechText",
                                  "subSpeech": "$subSpeechText",
                                  "suggestedAction": ${if (suggestedAction != null) "\"$suggestedAction\"" else "null"},
                                  "reasoning": "Live default state configuration"
                                }
                            """.trimIndent()

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .horizontalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = jsonTextToShow,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
