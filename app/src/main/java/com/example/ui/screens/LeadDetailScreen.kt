package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LeadEntity
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper

@Composable
fun LeadDetailScreen(
    leadId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val leads by viewModel.leads.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val lead = leads.find { it.id == leadId } ?: leads.firstOrNull()
    val context = LocalContext.current
    
    val leadCallLogs = remember(lead, callLogs) {
        lead?.let { l ->
            callLogs.filter { it.phoneNumber.contains(l.phone.takeLast(5)) || l.phone.contains(it.phoneNumber.takeLast(5)) }
        } ?: emptyList()
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = mutableListOf("Overview", "Activity", "Notes", "Files")
    if (leadCallLogs.any { !it.transcription.isNullOrBlank() }) {
        tabs.add("AI Insights")
    }
    
    var newNoteText by remember { mutableStateOf("") }
    var isAddingNote by remember { mutableStateOf(false) }
    var showSendQuoteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = lead?.name ?: "Lead Details",
                subMenuSubtitle = "${lead?.company ?: "Client"} · ${lead?.stage ?: "New"}",
                onBack = onBack,
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = {
                            if (lead != null) {
                                viewModel.sendCompanyProfileBrochure(
                                    context = context,
                                    recipientName = lead.name,
                                    recipientPhone = lead.phone,
                                    companyName = lead.company
                                )
                            }
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share Profile",
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        if (lead != null) {
            val (avatarBg, avatarText) = getAvatarColor(lead.name)
            val (stageBg, stageText) = getStageBadgeColors(lead.stage)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Card 1: Lead Hero Header ─────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Avatar + Name + Stage Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = avatarBg,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = getInitials(lead.name),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 20.sp,
                                                color = avatarText
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = lead.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
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

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = stageBg,
                                    border = BorderStroke(1.dp, stageText.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = lead.stage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = stageText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Contact Channels with Direct Tap Affordances
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // Phone Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .clickable {
                                            WhatsAppHelper.dialPhoneNumber(context, lead.phone)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(lead.phone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                    }
                                    Text("Tap to call", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Medium)
                                }

                                // Email Row (if available)
                                if (lead.email.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF8FAFC))
                                            .clickable {
                                                val mailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${lead.email}"))
                                                try { context.startActivity(mailIntent) } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(lead.email, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                                        }
                                        Text("Tap to email", fontSize = 11.sp, color = ElectricBlue, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Card 2: Quick Action Grid (Call, WhatsApp, Send Quote, Brochure) ───
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Call
                        LeadDetailActionButton(
                            icon = Icons.Default.Phone,
                            label = "Call",
                            bgColor = Color(0xFFDCFCE7),
                            tintColor = Color(0xFF15803D),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                WhatsAppHelper.dialPhoneNumber(context, lead.phone)
                                viewModel.addCallLog(lead.name, lead.phone, "Outgoing", "Initiated")
                            }
                        )
                        // WhatsApp
                        LeadDetailActionButton(
                            icon = Icons.Default.Chat,
                            label = "WhatsApp",
                            bgColor = Color(0xFFD1FAE5),
                            tintColor = Color(0xFF059669),
                            modifier = Modifier.weight(1.1f),
                            onClick = {
                                WhatsAppHelper.sendWhatsAppMessage(
                                    context = context,
                                    phoneNumber = lead.phone,
                                    message = "Hello ${lead.name}, connecting with you from Making Brands regarding your project.",
                                    showSuccessToast = true
                                )
                            }
                        )
                        // Send Quotation
                        LeadDetailActionButton(
                            icon = Icons.Default.Description,
                            label = "Send Quote",
                            bgColor = Color(0xFFFAF5FF),
                            tintColor = Color(0xFF9333EA),
                            modifier = Modifier.weight(1.1f),
                            onClick = {
                                showSendQuoteDialog = true
                            }
                        )
                        // Send Brochure
                        LeadDetailActionButton(
                            icon = Icons.Default.Share,
                            label = "Profile",
                            bgColor = Color(0xFFEFF6FF),
                            tintColor = Color(0xFF2563EB),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.sendCompanyProfileBrochure(
                                    context = context,
                                    recipientName = lead.name,
                                    recipientPhone = lead.phone,
                                    companyName = lead.company
                                )
                            }
                        )
                    }
                }

                // ── Card 3: Deal Metrics & Pipeline Health ─────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Deal Financials & Pipeline Scope", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Potential Value
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Deal Value", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(lead.potentialValue, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    }
                                }

                                // Lead Score
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Quality Score", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (lead.leadScore >= 80) Icons.Default.LocalFireDepartment else Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = if (lead.leadScore >= 80) Color(0xFFEA580C) else ElectricBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${lead.leadScore}%", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                        }
                                    }
                                }
                            }

                            // Requirement Scope Box
                            if (lead.requirement.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Client Requirement", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(lead.requirement, fontSize = 13.sp, color = Color(0xFF334155), lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Subview Tabs & Content ─────────────────────────────
                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = ElectricBlue,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp))
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Tab Content Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (selectedTab) {
                                0 -> { // Overview
                                    Text("Lead Origin & Assignment", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Source Platform", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(lead.source, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Assigned Representative", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text(lead.assignedTo.ifBlank { "Unassigned" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Lead Record ID", fontSize = 13.sp, color = Color(0xFF64748B))
                                        Text("#LEAD-${lead.id}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                                    }
                                }
                                1 -> { // Activity
                                    Text("Recent Engagement Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = CircleShape, color = Color(0xFFDCFCE7), modifier = Modifier.size(24.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Lead Created in CRM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Source: ${lead.source} · Stage: ${lead.stage}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(24.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(13.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Company Profile Dispatch Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("Automated brochure available for WhatsApp delivery", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                        }
                                    }
                                }
                                2 -> { // Notes
                                    Text("Client Notes & Records", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Text(
                                        lead.notes.ifBlank { "No notes added yet for this client." },
                                        fontSize = 13.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 18.sp
                                    )
                                }
                                3 -> { // Files
                                    Text("Attached Documents & Media", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth().clickable {
                                            viewModel.sendCompanyProfileBrochure(
                                                context = context,
                                                recipientName = lead.name,
                                                recipientPhone = lead.phone,
                                                companyName = lead.company
                                            )
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Making_Brands_Company_Profile.pdf", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("2.4 MB · Ready to share on WhatsApp", fontSize = 11.sp, color = Color(0xFF64748B))
                                            }
                                            Icon(Icons.Default.Share, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                4 -> { // AI Insights
                                    Text("AI Call Insights", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    Column(
                                        modifier = Modifier.heightIn(max = 300.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        leadCallLogs.filter { !it.transcription.isNullOrBlank() }.forEach { callLog ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Call on ${callLog.timestampText}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricBlue)
                                                    
                                                    Text("Transcription:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF475569))
                                                    Text(callLog.transcription!!, fontSize = 13.sp, color = Color(0xFF0F172A), lineHeight = 18.sp)
                                                    
                                                    if (!callLog.aiInsights.isNullOrBlank()) {
                                                        HorizontalDivider(color = Color(0xFFE2E8F0))
                                                        Text("AI Summary & Takeaways:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = ElectricBlue)
                                                        callLog.aiInsights!!.split("\n").forEach { line ->
                                                            if (line.isNotBlank()) {
                                                                Row(verticalAlignment = Alignment.Top) {
                                                                    Text("•", fontSize = 14.sp, color = ElectricBlue, modifier = Modifier.padding(end = 4.dp))
                                                                    Text(line.trim('-').trim(), fontSize = 13.sp, color = Color(0xFF0F172A), lineHeight = 18.sp)
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
                    }
                }
            }
        }

        if (showSendQuoteDialog && lead != null) {
            LeadSendQuotationDialog(
                lead = lead,
                onDismiss = { showSendQuoteDialog = false },
                onSendWhatsAppQuote = { scope, amount, terms ->
                    showSendQuoteDialog = false
                    val quoteNo = "MB-QT-${System.currentTimeMillis().toString().takeLast(4)}"
                    WhatsAppHelper.sendQuotationEstimate(
                        context = context,
                        phoneNumber = lead.phone,
                        clientName = lead.name,
                        quotationNumber = quoteNo,
                        totalAmount = amount,
                        scopeOfWork = scope
                    )
                    viewModel.addQuotation(
                        clientName = lead.name,
                        clientCompany = lead.company.ifBlank { lead.name },
                        clientEmail = lead.email.ifBlank { "client@example.com" },
                        clientPhone = lead.phone,
                        validUntil = "30 Days from Issue",
                        subtotal = amount,
                        discountPercent = 0.0,
                        taxPercent = 18.0,
                        scopeOfWork = scope,
                        termsAndConditions = terms
                    )
                    viewModel.addCallLog(lead.name, lead.phone, "Quotation Sent", "₹$amount ($quoteNo)")
                }
            )
        }
    }
}

@Composable
fun LeadSendQuotationDialog(
    lead: LeadEntity,
    onDismiss: () -> Unit,
    onSendWhatsAppQuote: (scope: String, amount: Double, terms: String) -> Unit
) {
    val initialAmount = remember(lead.potentialValue) {
        val clean = lead.potentialValue.replace("₹", "").replace("$", "").replace(",", "").trim()
        clean.toDoubleOrNull() ?: 50000.0
    }
    var scopeOfWork by remember {
        mutableStateOf(lead.requirement.ifBlank { "Custom Web & Mobile App Development with Cloud Database & Automated CRM Suite" })
    }
    var amountStr by remember { mutableStateOf(initialAmount.toInt().toString()) }
    var paymentTerms by remember { mutableStateOf("50% Advance on kickoff, 50% upon final milestone UAT sign-off.") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF5FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Send Quotation", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                            Text(lead.name, fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                OutlinedTextField(
                    value = scopeOfWork,
                    onValueChange = { scopeOfWork = it },
                    label = { Text("Scope of Work / Deliverables") },
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Quotation Amount (₹)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paymentTerms,
                    onValueChange = { paymentTerms = it },
                    label = { Text("Payment Terms") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Directly formatted and dispatched to ${lead.phone} via WhatsApp + saved to Invoices.",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsed = amountStr.toDoubleOrNull() ?: 0.0
                            if (parsed > 0 && scopeOfWork.isNotBlank()) {
                                onSendWhatsAppQuote(scopeOfWork, parsed, paymentTerms)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Quote", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LeadDetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, tintColor.copy(alpha = 0.3f)),
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(icon, contentDescription = label, tint = tintColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tintColor)
        }
    }
}
