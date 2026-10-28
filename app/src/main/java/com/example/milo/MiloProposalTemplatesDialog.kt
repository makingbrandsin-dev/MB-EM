package com.example.milo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.LeadEntity
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import com.example.util.WhatsAppHelper

data class ProposalTemplate(
    val title: String,
    val description: String,
    val iconEmoji: String,
    val generateContent: (MainViewModel) -> String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloProposalTemplatesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSelectTemplate: (String, String) -> Unit // returns (templateTitle, filledContent)
) {
    val context = LocalContext.current
    val leads by viewModel.leads.collectAsState()
    val projects by viewModel.projects.collectAsState()

    var activeTemplateTitle by remember { mutableStateOf<String?>(null) }
    var activeTemplateContent by remember { mutableStateOf<String?>(null) }
    var selectedLead by remember { mutableStateOf<LeadEntity?>(null) }

    val templates = remember(leads, projects) {
        listOf(
            ProposalTemplate(
                title = "Sales Pitch Structure",
                description = "Customized B2B sales pitch tailored to the latest CRM lead's requirement.",
                iconEmoji = "🤝",
                generateContent = { vm ->
                    val lead = selectedLead
                        ?: leads.firstOrNull { !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) }
                        ?: leads.firstOrNull()
                    if (lead != null) {
                        """
                        🦁 *MAKING BRANDS — EXECUTIVE SALES PITCH*
                        --------------------------------------------
                        Client: ${lead.name} (${lead.company})
                        Contact: ${lead.phone} | ${lead.email}
                        Projected Value: ${lead.potentialValue}
                        Lead Stage: ${lead.stage}
                        Source: ${lead.source}

                        Hello ${lead.name},

                        I hope you're having an inspiring week!

                        I am reaching out from Making Brands regarding your inquiry for *${lead.requirement.ifBlank { "Digital Transformation & Business Growth" }}*. 

                        Our team has analyzed your market sector and identified 3 direct opportunities to amplify your sales and reach:
                        1. High-converting modern web & mobile architecture tailored for your customers.
                        2. Direct pipeline automation that captures high-intent prospects 24/7.
                        3. Strategic brand positioning to outpace industry competitors.

                        We would love to share a bespoke 15-minute live walkthrough of how we solved this for comparable brands.

                        Would you be open for a short call tomorrow at 11:30 AM or 3:00 PM?

                        Warm regards,
                        ${lead.assignedTo.ifBlank { "Client Relations Team" }}
                        Making Brands • makingbrands.in
                        """.trimIndent()
                    } else {
                        """
                        🦁 *MAKING BRANDS — EXECUTIVE SALES PITCH*
                        --------------------------------------------
                        Hello Partner,

                        I'm reaching out from Making Brands regarding scaling your customer reach and brand identity. 

                        We specialize in custom web/app architecture and ROI-driven marketing campaigns. Would you be open for a brief 10-minute discovery call this week?

                        Warm regards,
                        Client Partnership Team • Making Brands
                        """.trimIndent()
                    }
                }
            ),
            ProposalTemplate(
                title = "Formal Quotation & Scope",
                description = "Standard commercial breakdown with payment schedule & milestones.",
                iconEmoji = "📑",
                generateContent = { vm ->
                    val lead = selectedLead
                        ?: leads.firstOrNull { !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) }
                        ?: leads.firstOrNull()
                    val targetClient = lead?.name ?: "Executive Client"
                    val targetCompany = lead?.company ?: "Client Enterprise"
                    val req = lead?.requirement?.ifBlank { "Full-Stack Branding & Tech Architecture" } ?: "Full-Stack Branding & Tech Architecture"
                    """
                    🦁 *MAKING BRANDS — COMMERCIAL QUOTATION*
                    -----------------------------------------
                    Client: $targetClient ($targetCompany)
                    Requirement: $req
                    Date: Auto-Generated Live Record

                    *1. Deliverables & Scope:*
                    • Complete UI/UX High-Fidelity Prototyping
                    • Mobile-Optimized Responsive Web/App Stack
                    • Real-Time Cloud Telemetry & Automated CRM Sync
                    • 30-Day Post-Launch Hypercare Support

                    *2. Commercial Schedule:*
                    • Total Estimate: ₹1,25,000 + 18% GST
                    • Payment Terms: 50% Kickoff | 30% Beta Review | 20% Production Go-Live

                    *3. Target Timeline:*
                    • 3-4 Weeks from architecture sign-off

                    Prepared with dedication,
                    Making Brands Technical Leadership
                    """.trimIndent()
                }
            ),
            ProposalTemplate(
                title = "Project Milestone Schedule",
                description = "Multi-phase delivery schedule based on active enterprise projects.",
                iconEmoji = "🎯",
                generateContent = { vm ->
                    val activeProject = projects.firstOrNull { it.status.equals("Active", true) }
                        ?: projects.firstOrNull()
                    if (activeProject != null) {
                        """
                        🦁 *MAKING BRANDS — PROJECT MILESTONE SCHEDULE*
                        -------------------------------------------------
                        Project Title: ${activeProject.name}
                        Client Partner: ${activeProject.clientName}
                        Budget Allocated: ₹${String.format("%,.0f", activeProject.budget)}
                        Project Deadline: ${activeProject.deadline}

                        Dear ${activeProject.clientName} Team,

                        Here is our proposed milestone schedule for *${activeProject.name}* to ensure seamless delivery:
                        • Phase 1 (Architecture & UX Review): Target completion within 7 days.
                        • Phase 2 (Core Feature Build): Target completion ${activeProject.startDate}.
                        • Phase 3 (Production Deployment & Launch): Handover by ${activeProject.deadline}.

                        Total Contract Value: ₹${String.format("%,.0f", activeProject.budget)}
                        Lead Project Manager: ${activeProject.managerName}
                        """.trimIndent()
                    } else {
                        "🦁 No active projects currently found. Create an active project in Projects screen to populate this proposal."
                    }
                }
            ),
            ProposalTemplate(
                title = "Client Onboarding Checklist",
                description = "Professional welcome checklist structure populated with active employee assignments.",
                iconEmoji = "📋",
                generateContent = { vm ->
                    val lead = selectedLead ?: leads.firstOrNull()
                    val assignedTo = lead?.assignedTo ?: "Arjun Mehta (Lead Engineering)"
                    """
                    🦁 *MAKING BRANDS — CLIENT ONBOARDING CHECKLIST*
                    --------------------------------------------------
                    Partner: ${lead?.company ?: "Client Enterprise"}
                    Contact: ${lead?.name ?: "Client Partner"}
                    Assigned Lead: $assignedTo

                    1. [x] Telemetry Workspace & Project Channel Initialized
                    2. [ ] Kickoff Strategy & Requirement Alignment Call
                    3. [ ] Brand Assets & Domain Credentials Collected
                    4. [ ] Beta Milestone Target Set (14 Days)

                    We look forward to an exceptional partnership together!
                    Making Brands Team
                    """.trimIndent()
                }
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeTemplateTitle != null) {
                            IconButton(
                                onClick = {
                                    activeTemplateTitle = null
                                    activeTemplateContent = null
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = ButtonPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = activeTemplateTitle ?: "Milo Drafting Templates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // If a template is active, show the rich live working template editor & actions
                if (activeTemplateTitle != null && activeTemplateContent != null) {
                    var editableContent by remember(activeTemplateContent) {
                        mutableStateOf(activeTemplateContent ?: "")
                    }

                    Text(
                        text = "Real working draft populated from live CRM records. Edit or send directly:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editableContent,
                        onValueChange = { editableContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = TextPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ButtonPrimary,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real Working Action Buttons (Copy, WhatsApp, Send to Milo Assistant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Copy button
                        OutlinedButton(
                            onClick = {
                                MiloHaptics.performButtonTap(context)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Template", editableContent))
                                Toast.makeText(context, "Copied template to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // 2. WhatsApp Direct Share
                        Button(
                            onClick = {
                                MiloHaptics.performButtonTap(context)
                                val targetPhone = selectedLead?.phone ?: leads.firstOrNull()?.phone ?: ""
                                WhatsAppHelper.sendWhatsAppMessage(
                                    context = context,
                                    phoneNumber = targetPhone,
                                    message = editableContent
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("WhatsApp", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // 3. Ask Milo button (passes exact prompt into Milo)
                        Button(
                            onClick = {
                                MiloHaptics.performButtonTap(context)
                                onSelectTemplate(activeTemplateTitle!!, editableContent)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Use in Milo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    // Templates List View
                    Text(
                        "Select any business proposal or pitch structure. Milo connects to your live database to generate a real, ready-to-send draft.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates) { template ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        MiloHaptics.performButtonTap(context)
                                        val content = template.generateContent(viewModel)
                                        activeTemplateTitle = template.title
                                        activeTemplateContent = content
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFEFF6FF),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(template.iconEmoji, fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = template.description,
                                            fontSize = 11.5.sp,
                                            color = TextSecondary,
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(20.dp)
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
