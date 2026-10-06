package com.example.milo

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.ui.theme.*
import com.example.util.WhatsAppHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MiloDailyTip(
    val id: Int,
    val category: String,
    val emoji: String,
    val title: String,
    val adviceText: String,
    val actionableStep: String
)

object MiloDailyInsightRepository {
    val tips = listOf(
        MiloDailyTip(
            id = 1,
            category = "⚡ Rapid Follow-up",
            emoji = "📞",
            title = "The 15-Minute Response Rule",
            adviceText = "Responding to inbound leads within 15 minutes increases your closing probability by 300%. First touch establishes trust!",
            actionableStep = "Tip: Call new leads immediately upon arrival."
        ),
        MiloDailyTip(
            id = 2,
            category = "🎯 Lead Scoring",
            emoji = "🔥",
            title = "Identify High-Urgency Signals",
            adviceText = "When a client mentions 'budget approved', 'quote today', or 'asap', auto-escalate the lead priority to URGENT.",
            actionableStep = "Tip: Use Milo's AI Lead Scanner to auto-detect urgency."
        ),
        MiloDailyTip(
            id = 3,
            category = "💼 Closing Strategy",
            emoji = "🏆",
            title = "Send Structured Proposals",
            adviceText = "Proposals sent with 2 clear pricing options convert 40% faster than single-item quotes. Offer a standard vs premium package.",
            actionableStep = "Tip: Use MB Tracker Auto-Quotation to generate PDFs."
        ),
        MiloDailyTip(
            id = 4,
            category = "💬 WhatsApp CRM",
            emoji = "📲",
            title = "Attach Visual Company Portfolios",
            adviceText = "Clients review WhatsApp PDF brochures 5x more often than long text messages. Always attach a PDF portfolio on first contact.",
            actionableStep = "Tip: Tap 'Send Brochure' directly in MB Tracker."
        ),
        MiloDailyTip(
            id = 5,
            category = "👥 Team Synergy",
            emoji = "📊",
            title = "Log Every Call Outcome",
            adviceText = "Logging call notes keeps your team aligned and ensures no client details are lost when field reps hand over accounts.",
            actionableStep = "Tip: Save quick voice or text call notes after every call."
        ),
        MiloDailyTip(
            id = 6,
            category = "⏰ Pipeline Hygiene",
            emoji = "⚠️",
            title = "Clear Stale High-Value Leads",
            adviceText = "Leads over ₹10,000 uncontacted for 2+ hours stall momentum. Re-engage them with a quick WhatsApp check-in.",
            actionableStep = "Tip: Check Milo's Proactive Pipeline Pulse daily."
        )
    )

    fun getDailyTipForToday(context: Context): MiloDailyTip {
        val todayDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val dateHash = todayDateStr.hashCode()
        val index = kotlin.math.abs(dateHash % tips.size)
        return tips[index]
    }
}

/**
 * Small, non-intrusive 'Milo Daily Insight' Card for the Home / Dashboard Screen.
 */
@Composable
fun MiloDailyInsightCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var tipIndex by remember {
        mutableIntStateOf(
            MiloDailyInsightRepository.getDailyTipForToday(context).id - 1
        )
    }

    val currentTip = MiloDailyInsightRepository.tips[tipIndex % MiloDailyInsightRepository.tips.size]
    var isExpanded by remember { mutableStateOf(false) }
    var hasClaimedXp by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            BrandBlue.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            // Header Row: Milo Avatar, Category Badge, Next Tip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    MiloCharacter(
                        state = MiloState.THINKING,
                        size = 38.dp,
                        showStateBadge = false
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MILO DAILY INSIGHT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandBlue,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = BrandBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "${currentTip.emoji} ${currentTip.category}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = currentTip.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tipIndex = (tipIndex + 1) % MiloDailyInsightRepository.tips.size
                        hasClaimedXp = false
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Random Tip",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Advice Body text in Milo's voice
            Text(
                text = "🦁 \"${currentTip.adviceText}\"",
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium
            )

            // Expanded Actionable Details & Buttons
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Surface(
                        color = SurfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentTip.actionableStep,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandDarkBlue,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(currentTip.adviceText))
                                Toast.makeText(context, "Tip copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Tip", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (!hasClaimedXp) {
                                    hasClaimedXp = true
                                    MiloXpManager.addXp(context, 10, "Read Milo Daily Insight")
                                    Toast.makeText(context, "+10 Milo XP Claimed! ⚡", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Already claimed for this tip!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !hasClaimedXp,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (hasClaimedXp) "XP Claimed" else "+10 XP", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
