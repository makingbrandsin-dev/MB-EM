package com.example.presentation.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloEvent
import com.example.domain.milo.MiloState
import com.example.domain.milo.MiloViewModel
import com.example.presentation.components.banner.OfferBannerSlider
import com.example.presentation.components.milo.MiloCharacter
import com.example.ui.components.RechartsTrendDashboardWidget
import com.example.milo.MiloXpWidgetCard
import com.example.milo.MiloWardrobeSheet
import com.example.milo.MiloLevelUpCelebrationDialog
import com.example.milo.MiloDailyInsightCard
import com.example.milo.MiloXpManager
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*

/**
 * Main Dashboard Screen for MB Tracker.
 * Features Overview Stats, Recent Leads, Quick CRM Actions, and the interactive Milo Character Companion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    miloViewModel: MiloViewModel = remember { MiloViewModel() },
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToLeadDetail: (Long) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current

    // State observation from ViewModels
    val leads by viewModel.leads.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val latestAttendance by viewModel.latestAttendance.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val currentEmpName by viewModel.currentEmployeeName.collectAsState()
    val currentEmpRole by viewModel.currentEmployeeRole.collectAsState()

    val miloState by miloViewModel.state.collectAsState()
    val miloSpeech by miloViewModel.speechText.collectAsState()
    val miloSubSpeech by miloViewModel.subSpeechText.collectAsState()
    val miloAction by miloViewModel.suggestedAction.collectAsState()

    val isShiftActive = latestAttendance?.isWorking == true
    val employeeDisplayName = userProfile?.name?.ifBlank { currentEmpName } ?: currentEmpName
    val employeeDisplayRole = userProfile?.role?.ifBlank { currentEmpRole } ?: currentEmpRole

    var selectedLeadTab by remember { mutableStateOf("All") }
    var showMiloAiDialog by remember { mutableStateOf(false) }
    var showMiloWardrobeSheet by remember { mutableStateOf(false) }

    val xpState by MiloXpManager.xpState.collectAsState()

    val activeLeadsCount = leads.size
    val pendingTasksCount = tasks.count { !it.isCompleted }
    val completedTasksCount = tasks.count { it.isCompleted }
    val hotLeadsCount = leads.count { it.leadScore >= 80 }

    Scaffold(
        topBar = {
            DashboardTopBar(
                employeeName = employeeDisplayName,
                role = employeeDisplayRole,
                onNotificationClick = onNavigateToNotifications,
                onChatClick = onNavigateToChat
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // 1. Interactive Milo Character Hero Section
            item {
                MiloDashboardHeroCard(
                    miloViewModel = miloViewModel,
                    miloState = miloState,
                    speechText = miloSpeech,
                    subSpeechText = miloSubSpeech,
                    suggestedAction = miloAction,
                    onMiloClick = {
                        showMiloAiDialog = true
                        miloViewModel.handleEvent(MiloEvent.Thinking("daily pipeline insights"))
                    },
                    onActionClick = {
                        when (miloAction) {
                            "View Leads", "Open Lead" -> onNavigateToLeads()
                            "Today's Tasks", "Next Task", "View Tasks" -> onNavigateToTasks()
                            "View Attendance" -> onNavigateToAttendance()
                            "Celebrate" -> miloViewModel.handleEvent(MiloEvent.Celebration("Monthly Target Reached!"))
                            else -> {
                                showMiloAiDialog = true
                                miloViewModel.handleEvent(MiloEvent.Thinking("recommendations"))
                            }
                        }
                    }
                )
            }

            // 🌟 Milo XP Level & Wardrobe Rewards Widget
            item {
                MiloXpWidgetCard(
                    onOpenWardrobe = { showMiloWardrobeSheet = true }
                )
            }

            // 💡 Milo Daily Insight Card
            item {
                MiloDailyInsightCard()
            }

            // 🎁 Exclusive App Offers & Promotional Banner Slider
            item {
                OfferBannerSlider(
                    onBannerClick = { banner ->
                        when (banner.routeAction) {
                            "leads" -> onNavigateToLeads()
                            "tasks" -> onNavigateToTasks()
                            "invoices" -> {
                                Toast.makeText(context, "Opening Instant Quotations & Invoices", Toast.LENGTH_SHORT).show()
                            }
                            "milo_ai" -> {
                                showMiloAiDialog = true
                                miloViewModel.handleEvent(MiloEvent.Thinking("Special Offers & Deals"))
                            }
                        }
                    }
                )
            }

            // 2. Overview Stats Section
            item {
                OverviewStatsSection(
                    totalLeads = activeLeadsCount,
                    pendingTasks = pendingTasksCount,
                    completedTasks = completedTasksCount,
                    hotLeads = hotLeadsCount,
                    isShiftActive = isShiftActive,
                    onStatClick = { statType ->
                        when (statType) {
                            "leads" -> onNavigateToLeads()
                            "tasks" -> onNavigateToTasks()
                            "attendance" -> onNavigateToAttendance()
                            "projects" -> onNavigateToProjects()
                        }
                    }
                )
            }

            // 3. Quick Action Hub
            item {
                QuickActionHub(
                    onNewLead = onNavigateToLeads,
                    onNewTask = onNavigateToTasks,
                    onPunchInOut = {
                        if (isShiftActive) {
                            viewModel.checkOutUser(context)
                            miloViewModel.handleEvent(MiloEvent.EmployeeLoggedOut)
                            Toast.makeText(context, "Checked out successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.checkInUser(context)
                            miloViewModel.handleEvent(MiloEvent.EmployeeLoggedIn)
                            Toast.makeText(context, "Checked in & attendance logged", Toast.LENGTH_SHORT).show()
                        }
                    },
                    isShiftActive = isShiftActive,
                    onAskMilo = {
                        showMiloAiDialog = true
                        miloViewModel.handleEvent(MiloEvent.Thinking("sales assistant"))
                    }
                )
            }

            // 📈 Recharts 30-Day Trends & Analytics Widget (Attendance vs. Task Completion Rates)
            item {
                RechartsTrendDashboardWidget(
                    viewModel = viewModel,
                    onNavigateToAttendance = onNavigateToAttendance,
                    onNavigateToTasks = onNavigateToTasks
                )
            }

            // 4. Recent Leads Header & Filter Tabs
            item {
                RecentLeadsHeaderSection(
                    totalLeads = leads.size,
                    selectedTab = selectedLeadTab,
                    onTabSelected = { selectedLeadTab = it },
                    onViewAllClick = onNavigateToLeads
                )
            }

            // 5. Recent Leads List Items
            val filteredLeads = when (selectedLeadTab) {
                "Hot" -> leads.filter { it.leadScore >= 80 }
                "New" -> leads.filter { it.stage.equals("New", ignoreCase = true) }
                "Follow-up" -> leads.filter { it.stage.contains("Follow", ignoreCase = true) }
                "Won" -> leads.filter { it.stage.equals("Won", ignoreCase = true) || it.stage.equals("Converted", ignoreCase = true) }
                else -> leads
            }.take(6)

            if (filteredLeads.isEmpty()) {
                item {
                    EmptyLeadsCard(onAddLeadClick = onNavigateToLeads)
                }
            } else {
                items(filteredLeads, key = { it.id }) { lead ->
                    DashboardLeadCard(
                        lead = lead,
                        onLeadClick = { onNavigateToLeadDetail(lead.id) },
                        onCallClick = {
                            com.example.util.WhatsAppHelper.dialPhoneNumber(context, lead.phone)
                            miloViewModel.handleEvent(MiloEvent.FollowUpDue(count = 1, clientName = lead.name))
                        },
                        onWhatsAppClick = {
                            val message = "Hello ${lead.name}, this is $employeeDisplayName from Making Brands following up on your ${lead.requirement} inquiry."
                            com.example.util.WhatsAppHelper.sendWhatsAppMessage(context, lead.phone, message)
                            miloViewModel.handleEvent(MiloEvent.TaskCompleted("WhatsApp Follow-up"))
                        }
                    )
                }
            }
        }
    }

    // Milo AI Copilot Assistant Dialog
    if (showMiloAiDialog) {
        MiloAssistantBottomSheet(
            miloViewModel = miloViewModel,
            onDismiss = {
                showMiloAiDialog = false
                miloViewModel.closeAiAssistant()
            },
            onNavigateToRoute = { route ->
                showMiloAiDialog = false
                when (route) {
                    "leads" -> onNavigateToLeads()
                    "tasks" -> onNavigateToTasks()
                    "attendance" -> onNavigateToAttendance()
                    "projects" -> onNavigateToProjects()
                }
            }
        )
    }

    // Milo XP Wardrobe & Rewards Sheet
    if (showMiloWardrobeSheet) {
        MiloWardrobeSheet(
            onDismiss = { showMiloWardrobeSheet = false }
        )
    }

    // Milo Level Up Celebratory Dialog
    xpState.recentLevelUp?.let { newLevel ->
        MiloLevelUpCelebrationDialog(
            newLevel = newLevel,
            onDismiss = { MiloXpManager.dismissLevelUpNotice() },
            onOpenWardrobe = {
                MiloXpManager.dismissLevelUpNotice()
                showMiloWardrobeSheet = true
            }
        )
    }
}

/**
 * Top App Bar with Branding and Employee Profile Snippet.
 */
@Composable
private fun DashboardTopBar(
    employeeName: String,
    role: String,
    onNotificationClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // MB Brand Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(BrandBlue, ElectricBlue)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Column {
                    Text(
                        text = "MB Tracker",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "$employeeName • $role",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onChatClick) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Team Chat",
                        tint = BrandBlue
                    )
                }

                IconButton(onClick = onNotificationClick) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = StatusRed) {
                                Text("3", color = Color.White, fontSize = 9.sp)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Milo Character Hero Section with Speech Bubble, Animated Status, and Action Triggers.
 */
@Composable
private fun MiloDashboardHeroCard(
    miloViewModel: MiloViewModel,
    miloState: MiloState,
    speechText: String,
    subSpeechText: String,
    suggestedAction: String?,
    onMiloClick: () -> Unit,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, miloState.primaryColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            miloState.primaryColor.copy(alpha = 0.08f),
                            Color.White
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Interactive Milo Animated Lion
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clickable { onMiloClick() }
                ) {
                    MiloCharacter(
                        state = miloState,
                        size = 92.dp,
                        showStateBadge = true,
                        onClick = onMiloClick
                    )
                }

                // Milo Reactive Speech Balloon
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = miloState.primaryColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Milo Assistant • ${miloState.title}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = speechText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = subSpeechText,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row & AI Assistant Prompt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick State Selector Pills for interactive testing
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val sampleStates = listOf(
                        MiloState.IDLE,
                        MiloState.WELCOME,
                        MiloState.WORKING,
                        MiloState.THINKING,
                        MiloState.LEAD_IMPORTED,
                        MiloState.SUCCESS,
                        MiloState.CONVERTED
                    )
                    items(sampleStates) { state ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (miloState == state) state.primaryColor else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { miloViewModel.setState(state) }
                        ) {
                            Text(
                                text = "${state.emoji} ${state.title}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (miloState == state) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (suggestedAction != null) {
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = miloState.primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = suggestedAction,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-Impact Overview Metric Cards (Leads, Tasks, Shift Status, Conversions).
 */
@Composable
private fun OverviewStatsSection(
    totalLeads: Int,
    pendingTasks: Int,
    completedTasks: Int,
    hotLeads: Int,
    isShiftActive: Boolean,
    onStatClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overview Performance",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Live Today",
                fontSize = 12.sp,
                color = ElectricBlue,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Total Leads Card
            StatMetricCard(
                modifier = Modifier.weight(1f),
                title = "Total Leads",
                value = "$totalLeads",
                subtitle = "$hotLeads Hot Pipeline",
                icon = Icons.Default.Groups,
                accentColor = BrandBlue,
                onClick = { onStatClick("leads") }
            )

            // Pending Tasks Card
            StatMetricCard(
                modifier = Modifier.weight(1f),
                title = "Pending Tasks",
                value = "$pendingTasks",
                subtitle = "$completedTasks Completed",
                icon = Icons.Default.Assignment,
                accentColor = Color(0xFFF59E0B),
                onClick = { onStatClick("tasks") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Shift / Attendance Card
            StatMetricCard(
                modifier = Modifier.weight(1f),
                title = "Shift Status",
                value = if (isShiftActive) "Active" else "Offline",
                subtitle = if (isShiftActive) "Logged In" else "Tap to Punch In",
                icon = Icons.Default.AccessTime,
                accentColor = if (isShiftActive) AccentGreen else Color(0xFF64748B),
                onClick = { onStatClick("attendance") }
            )

            // Converted Deals / Revenue
            StatMetricCard(
                modifier = Modifier.weight(1f),
                title = "Target Progress",
                value = "78%",
                subtitle = "₹ 4.8L Converted",
                icon = Icons.Default.TrendingUp,
                accentColor = Color(0xFF8B5CF6),
                onClick = { onStatClick("projects") }
            )
        }
    }
}

/**
 * Individual Reusable Metric Card.
 */
@Composable
private fun StatMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Quick Action Hub for 1-tap workflows.
 */
@Composable
private fun QuickActionHub(
    onNewLead: () -> Unit,
    onNewTask: () -> Unit,
    onPunchInOut: () -> Unit,
    isShiftActive: Boolean,
    onAskMilo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuickActionButton(
                label = "New Lead",
                icon = Icons.Default.PersonAdd,
                color = BrandBlue,
                onClick = onNewLead
            )
            QuickActionButton(
                label = "Add Task",
                icon = Icons.Default.AddTask,
                color = Color(0xFFD97706),
                onClick = onNewTask
            )
            QuickActionButton(
                label = if (isShiftActive) "Punch Out" else "Punch In",
                icon = if (isShiftActive) Icons.Default.TimerOff else Icons.Default.Fingerprint,
                color = if (isShiftActive) StatusRed else AccentGreen,
                onClick = onPunchInOut
            )
            QuickActionButton(
                label = "Milo AI",
                icon = Icons.Default.Psychology,
                color = Color(0xFF7C3AED),
                onClick = onAskMilo
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/**
 * Filter header for Recent Leads.
 */
@Composable
private fun RecentLeadsHeaderSection(
    totalLeads: Int,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Recent CRM Leads",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    color = BrandBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$totalLeads",
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "View All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricBlue
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View All",
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Filter Tabs (All, Hot, New, Follow-up, Won)
        val tabs = listOf("All", "Hot", "New", "Follow-up", "Won")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs) { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) BrandBlue else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                    modifier = Modifier.clickable { onTabSelected(tab) }
                ) {
                    Text(
                        text = tab,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Rich Card displaying an individual Lead with quick communication triggers.
 */
@Composable
private fun DashboardLeadCard(
    lead: com.example.data.model.LeadEntity,
    onLeadClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { onLeadClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lead.name.take(1).uppercase(),
                            color = BrandBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Text(
                            text = lead.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = lead.company.ifBlank { "Individual Client" },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Lead Score Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        lead.leadScore >= 80 -> AccentGreen.copy(alpha = 0.15f)
                        lead.leadScore >= 60 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFF64748B).copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        text = "Score: ${lead.leadScore}",
                        color = when {
                            lead.leadScore >= 80 -> AccentGreen
                            lead.leadScore >= 60 -> Color(0xFFD97706)
                            else -> Color(0xFF475569)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Stage & Source info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = lead.source.ifBlank { "Website" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFE0E7FF)
                    ) {
                        Text(
                            text = lead.stage,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3730A3),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = lead.potentialValue.ifBlank { "₹ 50,000" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandBlue
                )
            }

            Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Action Buttons: Call & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next: ${lead.nextFollowUp.ifBlank { "Today" }}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Call Button
                    FilledTonalButton(
                        onClick = onCallClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFEFF6FF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = BrandBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    }

                    // WhatsApp Button
                    Button(
                        onClick = onWhatsAppClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Text("💬 WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Empty State placeholder if no leads match filter.
 */
@Composable
private fun EmptyLeadsCard(onAddLeadClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🦁", fontSize = 36.sp)
            Text(
                text = "No leads in this category",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = "Add fresh leads or import from Justdial & Meta Ads.",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAddLeadClick,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add New Lead", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Milo AI Assistant Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiloAssistantBottomSheet(
    miloViewModel: MiloViewModel,
    onDismiss: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val aiInsights by miloViewModel.aiInsights.collectAsState()
    val isThinking by miloViewModel.isAiThinking.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiloCharacter(
                        state = MiloState.THINKING,
                        size = 48.dp
                    )
                    Column {
                        Text(
                            text = "Milo AI Assistant",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Sales Co-pilot & Daily Recommendations",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Quick Query Chips
            val quickQueries = listOf("What to focus on today?", "Check new leads", "Attendance status", "Draft follow-up")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickQueries) { chipText ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.clickable { miloViewModel.askMiloAiQuery(chipText) }
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // AI Insights List
            if (isThinking) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aiInsights.forEach { insight ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = insight.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandBlue
                                )
                                Text(
                                    text = insight.summary,
                                    fontSize = 12.sp,
                                    color = TextPrimary
                                )
                                if (insight.actionLabel != null && insight.actionRoute != null) {
                                    TextButton(
                                        onClick = { onNavigateToRoute(insight.actionRoute) },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "${insight.actionLabel} →",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ElectricBlue
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
}
}
