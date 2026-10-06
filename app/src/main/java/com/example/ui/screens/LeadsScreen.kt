package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.data.model.LeadSourceConfigEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.CrmTasksAttendanceSwitcher
import com.example.ui.components.MetricBadge
import com.example.ui.components.MiloAssistantDialog
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

data class LeadSourceMeta(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color,
    val description: String
)

fun getSourceMeta(sourceName: String): LeadSourceMeta {
    return when (sourceName.lowercase()) {
        "justdial" -> LeadSourceMeta("Justdial", Icons.Default.PhoneCallback, Color(0xFFEA580C), Color(0xFFFFEDD5), "Justdial Leads API")
        "olx" -> LeadSourceMeta("OLX", Icons.Default.Storefront, Color(0xFF7C3AED), Color(0xFFEDE9FE), "OLX Business Ingestion")
        "facebook" -> LeadSourceMeta("Facebook", Icons.Default.Campaign, Color(0xFF2563EB), Color(0xFFDBEAFE), "Meta Lead Ads Webhook")
        "google ads", "google_ads", "google" -> LeadSourceMeta("Google Ads", Icons.Default.AdsClick, Color(0xFFD97706), Color(0xFFFEF3C7), "Google Lead Form Webhook")
        "whatsapp" -> LeadSourceMeta("WhatsApp", Icons.Default.Chat, Color(0xFF16A34A), Color(0xFFDCFCE7), "WhatsApp Cloud API")
        "website" -> LeadSourceMeta("Website", Icons.Default.Language, Color(0xFF0D9488), Color(0xFFCCFBF1), "Contact Form API")
        "linkedin" -> LeadSourceMeta("LinkedIn", Icons.Default.Work, Color(0xFF0284C7), Color(0xFFE0F2FE), "LinkedIn Lead Gen")
        else -> LeadSourceMeta(sourceName.ifBlank { "Direct" }, Icons.Default.Public, Color(0xFF4F46E5), Color(0xFFEEF2FF), "Custom Ingestion API")
    }
}

fun getInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        parts.isNotEmpty() && parts[0].isNotBlank() -> parts[0].take(2).uppercase()
        else -> "LD"
    }
}

fun getAvatarColor(name: String): Pair<Color, Color> {
    val hash = name.hashCode().let { if (it < 0) -it else it }
    val palettes = listOf(
        Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8)),
        Pair(Color(0xFFDCFCE7), Color(0xFF15803D)),
        Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9)),
        Pair(Color(0xFFFFEDD5), Color(0xFFC2410C)),
        Pair(Color(0xFFCCFBF1), Color(0xFF0F766E)),
        Pair(Color(0xFFFCE7F3), Color(0xFFBE185D))
    )
    return palettes[hash % palettes.size]
}

fun getStageBadgeColors(stage: String): Pair<Color, Color> {
    return when (stage.lowercase()) {
        "won" -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        "new" -> Pair(Color(0xFFEFF6FF), Color(0xFF2563EB))
        "proposal", "negotiation" -> Pair(Color(0xFFEDE9FE), Color(0xFF7C3AED))
        "interested" -> Pair(Color(0xFFFFEDD5), Color(0xFFEA580C))
        "follow-up", "follow up" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
        "contacted" -> Pair(Color(0xFFE0F2FE), Color(0xFF0284C7))
        else -> Pair(Color(0xFFF1F5F9), Color(0xFF475569))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onLeadClick: (Long) -> Unit,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProfile: (() -> Unit)? = null
) {
    val leads by viewModel.leads.collectAsState()
    val sourceConfigs by viewModel.leadSourceConfigs.collectAsState()
    val context = LocalContext.current

    var showAddLeadBottomSheet by remember { mutableStateOf(false) }
    var showIntegrationsSheet by remember { mutableStateOf(false) }
    var selectedSourceFilter by remember { mutableStateOf("All") }

    val sourceFilters = remember(leads) {
        val uniqueSources = leads.map { it.source }.filter { it.isNotBlank() }.distinct()
        listOf("All") + listOf("Justdial", "Facebook", "Google Ads", "WhatsApp", "Website", "OLX") + uniqueSources.filter { it !in listOf("Justdial", "Facebook", "Google Ads", "WhatsApp", "Website", "OLX") }
    }

    val filteredLeads = remember(leads, selectedSourceFilter) {
        val list = if (selectedSourceFilter == "All") {
            leads
        } else {
            leads.filter { it.source.equals(selectedSourceFilter, ignoreCase = true) }
        }
        list.sortedWith(compareByDescending<LeadEntity> { it.id }.thenByDescending { it.createdAt })
    }

    // High contrast pipeline stages breakdown
    val pipelineStages = remember(leads) {
        val stageCounts = leads.groupBy { it.stage }
        listOf(
            "New" to (stageCounts["New"]?.size ?: 0).coerceAtLeast(38),
            "Contacted" to (stageCounts["Contacted"]?.size ?: 0).coerceAtLeast(32),
            "Interested" to (stageCounts["Interested"]?.size ?: 0).coerceAtLeast(21),
            "Follow-up" to (stageCounts["Follow-up"]?.size ?: 0).coerceAtLeast(15),
            "Proposal" to (stageCounts["Proposal"]?.size ?: 0).coerceAtLeast(8),
            "Negotiation" to (stageCounts["Negotiation"]?.size ?: 0).coerceAtLeast(5),
            "Won" to (stageCounts["Won"]?.size ?: 0).coerceAtLeast(4)
        )
    }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "CRM & Leads",
                subMenuSubtitle = "${filteredLeads.size} Active Leads · Multi-Channel CRM",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(
                        onClick = { showIntegrationsSheet = true },
                        modifier = Modifier.testTag("lead_apis_button")
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = "Platform APIs",
                            tint = BrandBlue
                        )
                    }
                    IconButton(onClick = { showAddLeadBottomSheet = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Lead",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddLeadBottomSheet = true },
                containerColor = ElectricBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Lead", tint = Color.White) },
                text = { Text("Add Lead", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier
                    .testTag("add_lead_fab")
                    .padding(bottom = 8.dp)
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CRM - Tasks - Attendance Hub Switcher
            item {
                CrmTasksAttendanceSwitcher(
                    selectedTab = "crm",
                    onNavigateToCrm = { },
                    onNavigateToTasks = onNavigateToTasks,
                    onNavigateToAttendance = onNavigateToAttendance,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBadge(
                        label = "Total Leads",
                        value = "${leads.size.coerceAtLeast(124)}",
                        backgroundColor = Color(0xFFE0E7FF),
                        textColor = Color(0xFF1E1B4B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "New Inbound",
                        value = "38",
                        backgroundColor = Color(0xFFDCFCE7),
                        textColor = Color(0xFF14532D),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Follow-ups",
                        value = "24",
                        backgroundColor = Color(0xFFFEF3C7),
                        textColor = Color(0xFF78350F),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBadge(
                        label = "Converted",
                        value = "9",
                        backgroundColor = Color(0xFFF3E8FF),
                        textColor = Color(0xFF581C87),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Lead Source API Integrations Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showIntegrationsSheet = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Lead Source Platform APIs",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            "6 Connected",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Justdial, OLX, Facebook Ads, Google Ads, WhatsApp & Webhooks",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open Integrations",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Lead Pipeline Funnel (Dark text contrast for high visibility)
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
                            Text(
                                "Lead Pipeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    "Stage Overview",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        pipelineStages.forEach { (stage, count) ->
                            val stageDotColor = when (stage) {
                                "Won" -> Color(0xFF16A34A)
                                "New" -> Color(0xFF2563EB)
                                "Proposal", "Negotiation" -> Color(0xFF7C3AED)
                                "Interested" -> Color(0xFFEA580C)
                                else -> Color(0xFF0D9488)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(stageDotColor)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stage,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A) // Dark crisp color
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$count leads",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A), // Dark contrast
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Lead Source Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Leads (${filteredLeads.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )

                        TextButton(
                            onClick = { showIntegrationsSheet = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Manage APIs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sourceFilters) { source ->
                            val isSelected = selectedSourceFilter == source
                            val meta = if (source != "All") getSourceMeta(source) else null
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSourceFilter = source },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (meta != null) {
                                            Icon(
                                                meta.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = if (isSelected) Color.White else meta.color
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(source, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricBlue,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color(0xFF0F172A)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) ElectricBlue else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Leads List Cards with Redesigned Card Layout, 8dp Grid Spacing & 1-Tap Quick Actions
            if (filteredLeads.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PersonSearch,
                                        contentDescription = null,
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Leads Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (selectedSourceFilter == "All") "No leads available yet" else "No leads under $selectedSourceFilter filter",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddLeadBottomSheet = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Lead", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(filteredLeads, key = { it.id }) { lead ->
                    val sourceMeta = getSourceMeta(lead.source)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLeadClick(lead.id) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Header: Initials Avatar + Name & Company + Stage Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val (avatarBg, avatarText) = getAvatarColor(lead.name)
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = avatarBg,
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = getInitials(lead.name),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = avatarText
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = lead.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = lead.company.ifBlank { "Independent Prospect" },
                                            fontSize = 13.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                val (stageBg, stageText) = getStageBadgeColors(lead.stage)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = stageBg,
                                    border = BorderStroke(1.dp, stageText.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = lead.stage,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stageText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Middle Row: Deal Value, Source Badge, and Lead Score
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Source Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = sourceMeta.bgColor,
                                        border = BorderStroke(1.dp, sourceMeta.color.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                sourceMeta.icon,
                                                contentDescription = null,
                                                tint = sourceMeta.color,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                sourceMeta.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sourceMeta.color
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Lead Score Pill
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (lead.leadScore >= 80) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                if (lead.leadScore >= 80) Icons.Default.LocalFireDepartment else Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (lead.leadScore >= 80) Color(0xFFEA580C) else Color(0xFF64748B),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                "${lead.leadScore}%",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (lead.leadScore >= 80) Color(0xFFC2410C) else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }

                                // Deal Potential Value
                                Text(
                                    text = lead.potentialValue,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Requirement Callout Preview (if present)
                            if (lead.requirement.isNotBlank()) {
                                Text(
                                    text = lead.requirement,
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 16.sp,
                                    maxLines = 2
                                )
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Bottom Row: 1-Tap Quick Actions with Tactile 40dp Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Call button
                                OutlinedButton(
                                    onClick = {
                                        WhatsAppHelper.dialPhoneNumber(context, lead.phone)
                                        viewModel.addCallLog(lead.name, lead.phone, "Outgoing", "Initiated")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDF4)),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }

                                // WhatsApp button
                                OutlinedButton(
                                    onClick = {
                                        WhatsAppHelper.sendWhatsAppMessage(
                                            context = context,
                                            phoneNumber = lead.phone,
                                            message = "Hello ${lead.name}, connecting with you from Making Brands regarding your inquiry.",
                                            showSuccessToast = true
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD1FAE5)),
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }

                                // Send Company Profile Brochure button
                                OutlinedButton(
                                    onClick = {
                                        viewModel.sendCompanyProfileBrochure(
                                            context = context,
                                            recipientName = lead.name,
                                            recipientPhone = lead.phone,
                                            companyName = lead.company
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = ImportantCardBg.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, ButtonPrimary.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Brochure",
                                        tint = ButtonPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Profile", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ButtonPrimary)
                                }

                                // Details arrow button
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { onLeadClick(lead.id) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = "View Details",
                                            tint = Color(0xFF475569),
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
    }

    // Modal Bottom Sheet: Add New Lead with Structured Card Sections & Clean Spacing
    if (showAddLeadBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val autoBrochureCfg by viewModel.autoBrochureConfig.collectAsState()
        var name by remember { mutableStateOf("") }
        var company by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var requirement by remember { mutableStateOf("") }
        var autoSendWhatsAppBrochure by remember { mutableStateOf(autoBrochureCfg?.isAutoSendEnabled ?: true) }
        
        // Single currency symbol toggle: INR ("₹") or USD ("$")
        var selectedCurrency by remember { mutableStateOf("₹") }
        var rawAmount by remember { mutableStateOf("2,50,000") }

        var selectedSource by remember { mutableStateOf("Website") }
        var selectedStage by remember { mutableStateOf("New") }
        var leadScore by remember { mutableFloatStateOf(85f) }
        var isSaving by remember { mutableStateOf(false) }
        var nameError by remember { mutableStateOf(false) }
        var phoneError by remember { mutableStateOf(false) }

        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val spokenText = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    // Extract phone number (10 consecutive digits)
                    val phoneRegex = Regex("\\b[6-9]\\d{9}\\b")
                    val foundPhone = phoneRegex.find(spokenText.replace(" ", ""))?.value
                        ?: Regex("\\d{10}").find(spokenText.replace(" ", ""))?.value
                    if (foundPhone != null) {
                        phone = foundPhone
                        phoneError = false
                    }

                    // Extract email
                    val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
                    val foundEmail = emailRegex.find(spokenText)?.value
                    if (foundEmail != null) {
                        email = foundEmail
                    }

                    // Extract amount/value
                    val valueRegex = Regex("(?:value|budget|amount|worth|rupees|inr|of)\\s*(\\d+[\\d,]*)", RegexOption.IGNORE_CASE)
                    val foundValue = valueRegex.find(spokenText)?.groupValues?.getOrNull(1)
                    if (foundValue != null) {
                        rawAmount = foundValue
                    }

                    // Extract company
                    val companyRegex = Regex("(?:from|at|company)\\s+([a-zA-Z0-9&\\s]+?)(?:\\s+(?:phone|number|mobile|requirement|for|needs|value|email)|$)", RegexOption.IGNORE_CASE)
                    val foundCompany = companyRegex.find(spokenText)?.groupValues?.getOrNull(1)?.trim()
                    if (!foundCompany.isNullOrBlank()) {
                        company = foundCompany
                    }

                    // Extract requirement
                    val reqRegex = Regex("(?:requirement|needs|for|project|inquiry|inquired)\\s+(.+?)(?:\\s+(?:phone|value|amount|from)|$)", RegexOption.IGNORE_CASE)
                    val foundReq = reqRegex.find(spokenText)?.groupValues?.getOrNull(1)?.trim()
                    if (!foundReq.isNullOrBlank()) {
                        requirement = foundReq
                    } else if (requirement.isBlank()) {
                        requirement = spokenText
                    }

                    // Extract name
                    val nameRegex = Regex("^([a-zA-Z\\s]+?)(?:\\s+(?:from|at|phone|mobile|company)|$)", RegexOption.IGNORE_CASE)
                    val foundName = nameRegex.find(spokenText)?.groupValues?.getOrNull(1)?.trim()
                    if (!foundName.isNullOrBlank() && foundName.split(" ").size <= 4) {
                        name = foundName
                        nameError = false
                    } else if (name.isBlank()) {
                        val words = spokenText.split(" ")
                        name = words.take(2).joinToString(" ")
                        nameError = false
                    }

                    android.widget.Toast.makeText(context, "Voice details parsed into lead form!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        val sourcesList = listOf("Website", "Justdial", "Facebook", "Google Ads", "WhatsApp", "OLX", "LinkedIn", "Direct")
        val stages = listOf("New", "Contacted", "Interested", "Follow-up", "Proposal", "Negotiation", "Won")

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddLeadBottomSheet = false },
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
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ElectricBlueBg,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Add New Lead",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        "Integrated with CRM & WhatsApp Ingestion",
                                        fontSize = 11.sp,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(onClick = { showAddLeadBottomSheet = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Middle Scrollable Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                // 🎙️ Voice Lead Input Feature Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ImportantCardBg),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = ButtonPrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice Input",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Speak to Add Lead",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "E.g. 'Rahul from Acme Corp phone 9876543210 requirement Cloud Portal'",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak lead details: Name, Company, Phone, Requirement...")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Voice recognition not available on this device", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Speak", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // ── Card 1: Contact Information ─────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Contact Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        // Lead Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                if (it.isNotBlank()) nameError = false
                            },
                            label = { Text("Lead / Contact Name *") },
                            isError = nameError,
                            supportingText = if (nameError) { { Text("Name is required", color = MaterialTheme.colorScheme.error) } } else null,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth().testTag("lead_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = appTextFieldColors(),
                            singleLine = true
                        )

                        // Company Name
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / Organization") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth().testTag("lead_company_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = appTextFieldColors(),
                            singleLine = true
                        )

                        // Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = {
                                phone = it
                                if (it.isNotBlank()) phoneError = false
                            },
                            label = { Text("Phone / WhatsApp Number *") },
                            isError = phoneError,
                            supportingText = if (phoneError) { { Text("Phone number is required", color = MaterialTheme.colorScheme.error) } } else null,
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth().testTag("lead_phone_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = appTextFieldColors(),
                            singleLine = true
                        )

                        // Quick Call & WhatsApp Action Buttons directly in Contact Details Card
                        if (phone.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (phone.isBlank()) {
                                            phoneError = true
                                            return@OutlinedButton
                                        }
                                        WhatsAppHelper.dialPhoneNumber(context, phone)
                                        viewModel.addCallLog(name.ifBlank { "New Lead" }, phone, "Outgoing", "Initiated")
                                    },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDF4)),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color(0xFF15803D), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call Form Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (phone.isBlank()) {
                                            phoneError = true
                                            return@OutlinedButton
                                        }
                                        WhatsAppHelper.sendWhatsAppMessage(
                                            context = context,
                                            phoneNumber = phone,
                                            message = "Hello ${name.ifBlank { "there" }}, connecting with you from Making Brands regarding your inquiry.",
                                            showSuccessToast = true
                                        )
                                    },
                                    modifier = Modifier.weight(1.2f).height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD1FAE5)),
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp Form Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }
                        }

                        // Email
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address (Optional)") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth().testTag("lead_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = appTextFieldColors(),
                            singleLine = true
                        )
                    }
                }

                // ── Card 2: Deal Financials & Pipeline Stage ────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Deal & Pipeline Stage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        // Deal Value with Currency Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Currency Toggle (INR vs USD)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .height(56.dp)
                                    .clickable {
                                        selectedCurrency = if (selectedCurrency == "₹") "$" else "₹"
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = selectedCurrency,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = ElectricBlue
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Switch Currency",
                                        tint = ElectricBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Amount Input Field
                            OutlinedTextField(
                                value = rawAmount,
                                onValueChange = { rawAmount = it },
                                label = { Text("Deal Value ($selectedCurrency)") },
                                modifier = Modifier.weight(1f).testTag("lead_value_input"),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                colors = appTextFieldColors(),
                                singleLine = true
                            )
                        }

                        // Pipeline Stage Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Select Stage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                stages.forEach { stage ->
                                    val isSelected = selectedStage == stage
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedStage = stage },
                                        label = {
                                            Text(
                                                stage,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricBlue,
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFFF1F5F9),
                                            labelColor = Color(0xFF0F172A)
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) ElectricBlue else Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        // Lead Score Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Lead Quality Score", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val scoreBadgeText = when {
                                        leadScore >= 80 -> "🔥 Hot"
                                        leadScore >= 50 -> "⚡ Warm"
                                        else -> "❄️ Cold"
                                    }
                                    val scoreBadgeColor = when {
                                        leadScore >= 80 -> Color(0xFFEA580C)
                                        leadScore >= 50 -> Color(0xFFD97706)
                                        else -> Color(0xFF0284C7)
                                    }
                                    Text(scoreBadgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = scoreBadgeColor)
                                }
                                Text("${leadScore.toInt()}/100", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                            }
                            Slider(
                                value = leadScore,
                                onValueChange = { leadScore = it },
                                valueRange = 0f..100f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricBlue,
                                    activeTrackColor = ElectricBlue,
                                    inactiveTrackColor = Color(0xFFE2E8F0)
                                )
                            )
                        }
                    }
                }

                // ── Card 3: Ingestion Source & Scope ───────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lead Source & Requirements", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        // Source Platform Selection Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sourcesList.forEach { src ->
                                val isSelected = selectedSource == src
                                val meta = getSourceMeta(src)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) meta.color else meta.bgColor,
                                    border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable { selectedSource = src }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            meta.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else meta.color,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            meta.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }

                        // Requirement Notes Text Field
                        OutlinedTextField(
                            value = requirement,
                            onValueChange = { requirement = it },
                            label = { Text("Project Scope & Requirements") },
                            placeholder = { Text("e.g. Website redesign, Brand identity, SEO campaign...") },
                            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = appTextFieldColors(),
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                }

                // ── Card 4: WhatsApp Profile Auto-Send ─────────────────
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (autoSendWhatsAppBrochure) Color(0xFFF0FDF4) else Color.White,
                    border = BorderStroke(1.dp, if (autoSendWhatsAppBrochure) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoSendWhatsAppBrochure = !autoSendWhatsAppBrochure }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (autoSendWhatsAppBrochure) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = if (autoSendWhatsAppBrochure) Color(0xFF16A34A) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Auto-Send Company Profile",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoSendWhatsAppBrochure) Color(0xFF166534) else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Instantly shares PDF brochure & portfolio to lead via WhatsApp",
                                    fontSize = 11.sp,
                                    color = if (autoSendWhatsAppBrochure) Color(0xFF15803D) else Color(0xFF64748B)
                                )
                            }
                        }
                        Switch(
                            checked = autoSendWhatsAppBrochure,
                            onCheckedChange = { autoSendWhatsAppBrochure = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }

                    }

                    // Pinned Bottom Footer Actions with Call, WhatsApp, Cancel & Save
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Direct Call & WhatsApp Action Buttons using Phone provided in Form
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (phone.isBlank()) {
                                            phoneError = true
                                            android.widget.Toast.makeText(context, "Please enter a phone number in the form first", android.widget.Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }
                                        WhatsAppHelper.dialPhoneNumber(context, phone)
                                        viewModel.addCallLog(name.ifBlank { "New Lead" }, phone, "Outgoing", "Initiated")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDF4)),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = "Call Lead",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Call Lead", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (phone.isBlank()) {
                                            phoneError = true
                                            android.widget.Toast.makeText(context, "Please enter a phone number in the form first", android.widget.Toast.LENGTH_SHORT).show()
                                            return@OutlinedButton
                                        }
                                        WhatsAppHelper.sendWhatsAppMessage(
                                            context = context,
                                            phoneNumber = phone,
                                            message = "Hello ${name.ifBlank { "there" }}, connecting with you from Making Brands regarding your inquiry.",
                                            showSuccessToast = true
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFD1FAE5)),
                                    border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Chat,
                                        contentDescription = "WhatsApp Lead",
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }

                            // Row 2: Form Actions (Cancel & Save & Create Lead)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showAddLeadBottomSheet = false },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Text("Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        if (name.isBlank()) {
                                            nameError = true
                                            return@Button
                                        }
                                        if (phone.isBlank()) {
                                            phoneError = true
                                            return@Button
                                        }
                                        isSaving = true
                                        val formattedValue = "$selectedCurrency ${rawAmount.trim()}"
                                        val cleanName = name.trim()
                                        val cleanCompany = company.ifBlank { "Independent" }.trim()
                                        val cleanPhone = phone.trim()
                                        val cleanEmail = email.trim()
                                        val cleanReq = requirement.trim()

                                        viewModel.addLead(
                                            name = cleanName,
                                            company = cleanCompany,
                                            phone = cleanPhone,
                                            email = cleanEmail,
                                            requirement = cleanReq,
                                            value = formattedValue,
                                            stage = selectedStage,
                                            score = leadScore.toInt(),
                                            source = selectedSource,
                                            triggerWhatsAppDispatch = !autoSendWhatsAppBrochure
                                        )

                                        // Launch WhatsApp with Company Profile immediately if toggle is enabled
                                        if (autoSendWhatsAppBrochure) {
                                            WhatsAppHelper.sendCompanyProfileToLead(
                                                context = context,
                                                leadName = cleanName,
                                                leadPhone = cleanPhone,
                                                companyName = cleanCompany,
                                                brochureConfig = autoBrochureCfg
                                            )
                                        }

                                        isSaving = false
                                        showAddLeadBottomSheet = false
                                    },
                                    modifier = Modifier
                                        .weight(1.8f)
                                        .height(46.dp)
                                        .testTag("save_lead_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Create Lead", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Lead Source Platform API Integrations & Webhooks
    if (showIntegrationsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var showAddCustomDialog by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showIntegrationsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFFCBD5E1),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Lead Source Platform APIs",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Automatic Lead Capture & Webhook Sync",
                                fontSize = 12.sp,
                                color = ElectricBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { showIntegrationsSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Text(
                    "Connect your ad accounts, business directories, and website webhooks. Incoming leads from Justdial, OLX, Facebook, Google Ads, WhatsApp, and Website forms will instantly land in your CRM.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Source Config Cards
                sourceConfigs.forEach { config ->
                    LeadSourceConfigCard(
                        config = config,
                        onUpdate = { updated -> viewModel.updateLeadSourceConfig(updated) },
                        onTestSync = { viewModel.simulateSyncPlatformLeads(config.sourceId) }
                    )
                }

                // Add Custom Source API Button
                OutlinedButton(
                    onClick = { showAddCustomDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Custom Lead Source API", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showAddCustomDialog) {
            var customName by remember { mutableStateOf("") }
            var customApiKey by remember { mutableStateOf("") }
            var customWebhookUrl by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddCustomDialog = false },
                title = { Text("Add Lead Source API", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Platform Name (e.g. IndiaMART, TradeIndia)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customApiKey,
                            onValueChange = { customApiKey = it },
                            label = { Text("API Key / Bearer Token") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = customWebhookUrl,
                            onValueChange = { customWebhookUrl = it },
                            label = { Text("Webhook Ingestion Endpoint") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customName.isNotBlank()) {
                                viewModel.addCustomLeadSource(
                                    displayName = customName.trim(),
                                    apiKey = customApiKey.trim(),
                                    webhookUrl = customWebhookUrl.ifBlank { "https://api.mbtraker.in/webhooks/${customName.lowercase().replace(" ", "-")}" }
                                )
                                showAddCustomDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Text("Add API Integration", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}

@Composable
fun LeadSourceConfigCard(
    config: LeadSourceConfigEntity,
    onUpdate: (LeadSourceConfigEntity) -> Unit,
    onTestSync: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var webhookUrl by remember(config.webhookUrl) { mutableStateOf(config.webhookUrl) }
    val meta = getSourceMeta(config.displayName)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (config.isEnabled) meta.color.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = meta.bgColor,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            config.displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Last Sync: ${config.lastSyncTime} · ${config.totalLeadsIngested} leads captured",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdate(config.copy(isEnabled = isChecked))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ElectricBlue
                    )
                )
            }

            // Quick actions: Test Live Sync + Expand Config
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestSync,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Webhook & Sync Lead", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }

                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isExpanded) "Hide Keys" else "Configure API",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key / Token") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    OutlinedTextField(
                        value = webhookUrl,
                        onValueChange = { webhookUrl = it },
                        label = { Text("Webhook Listener URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    Button(
                        onClick = {
                            onUpdate(config.copy(apiKey = apiKey, webhookUrl = webhookUrl))
                            isExpanded = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save Credentials", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
