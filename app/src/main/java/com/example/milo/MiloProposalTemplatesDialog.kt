package com.example.milo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*

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
    val context = androidx.compose.ui.platform.LocalContext.current
    val leads by viewModel.leads.collectAsState()
    val projects by viewModel.projects.collectAsState()

    val templates = remember(leads, projects) {
        listOf(
            ProposalTemplate(
                title = "Sales Pitch Structure",
                description = "Customized B2B sales pitch tailored to the latest CRM lead's requirement.",
                iconEmoji = "🤝",
                generateContent = { vm ->
                    val latestLead = leads.firstOrNull { !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) }
                        ?: leads.firstOrNull()
                    if (latestLead != null) {
                        """
                        🦁 **Milo AI Sales Pitch Proposal Template**
                        --------------------------------------------
                        **Client Name:** ${latestLead.name}
                        **Company:** ${latestLead.company}
                        **Project Value:** ${latestLead.potentialValue}
                        **Source Platform:** ${latestLead.source}

                        Dear ${latestLead.name},

                        We reviewed your core requirement for *${latestLead.requirement}* and are excited to draft this comprehensive roadmap. Our executive team at Making Brands proposes a robust alignment strategy tailored to optimize your CRM workflow.

                        With a projected budget of *${latestLead.potentialValue}*, we guarantee:
                        1. 20X Faster Lead Engagement Funnel Setup
                        2. Native Biometric Verification Gateways
                        3. Automated Call Tracking & CRM Syncing

                        Let's grow your brand with pride! 🦁
                        """.trimIndent()
                    } else {
                        "🦁 No CRM Leads currently present in the database. Please add a lead first to generate a context-aware pitch."
                    }
                }
            ),
            ProposalTemplate(
                title = "Project Milestone Structure",
                description = "Detailed milestone timeline proposal based on active enterprise projects.",
                iconEmoji = "🎯",
                generateContent = { vm ->
                    val activeProject = projects.firstOrNull { it.status.equals("Active", true) }
                        ?: projects.firstOrNull()
                    if (activeProject != null) {
                        """
                        🦁 **Milo AI Project Milestone Proposal**
                        -----------------------------------------
                        **Project Title:** ${activeProject.name}
                        **Client Partner:** ${activeProject.clientName}
                        **Budget Allocated:** ₹${String.format("%,.0f", activeProject.budget)}
                        **Project Deadline:** ${activeProject.deadline}

                        Dear ${activeProject.clientName} Team,

                        Here is our proposed milestone schedule for *${activeProject.name}* to ensure seamless and high-quality delivery:

                        *   **Milestone 1 (UX Design Review):** Target completion within 7 days.
                        *   **Milestone 2 (Core Feature Build):** Target date: ${activeProject.startDate}.
                        *   **Milestone 3 (Final Deployment & Launch):** Final delivery by ${activeProject.deadline}.

                        Total Contract Value: ₹${String.format("%,.0f", activeProject.budget)}
                        Our lead engineer, ${activeProject.managerName}, is ready to schedule our first kickoff meeting today.
                        """.trimIndent()
                    } else {
                        "🦁 No active projects found in Room DB. Try adding a project structure first!"
                    }
                }
            ),
            ProposalTemplate(
                title = "Client Onboarding Checklist",
                description = "Professional welcome checklist structure populated with active employee assignments.",
                iconEmoji = "📋",
                generateContent = { vm ->
                    val latestLead = leads.firstOrNull()
                    val assignedTo = latestLead?.assignedTo ?: "Arjun Mehta (Engineering Manager)"
                    """
                    🦁 **Client Onboarding & Kickoff Template**
                    -------------------------------------------
                    **Partner Organization:** ${latestLead?.company ?: "Making Brands Client"}
                    **Assigned Executive Partner:** $assignedTo
                    **Onboarding Status:** In Progress

                    Dear Client Partner,

                    Welcome to the Making Brands Family! To facilitate a seamless integration, we have initiated your executive onboarding checklist:

                    1.  **Lead Executive Assigned:** $assignedTo
                    2.  **Telemetry Workspace Configuration:** Activated
                    3.  **Kickoff Call Scheduling:** Pending Follow-Up
                    4.  **Resource Allocation:** Completed

                    We look forward to building a high-value, long-term alliance together.
                    """.trimIndent()
                }
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "Milo Drafting Templates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Select a pre-configured business proposal structure. Milo will instantly populate it with context-aware lead and project records from your local database.",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(templates) { template ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val content = template.generateContent(viewModel)
                                    onSelectTemplate(template.title, content)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(template.iconEmoji, fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = template.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
