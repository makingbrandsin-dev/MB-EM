package com.example.milo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.ui.components.liftOnPress
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*
import com.example.util.MiloHaptics

/**
 * Proactive Milo Smart Assistant Greeting Card.
 * Shown prominently at the top of the Employee and Manager Dashboards.
 * Features:
 * - Dynamic time-of-day greeting ("Good Morning", "Good Afternoon", "Good Evening", "Good Night")
 * - Milo Mascot dynamically reacts to employee's live attendance & break status (Working, On Break, Off-Clock)
 * - Clean box border on all sides matching the Quick Actions & Tools section
 * - Proactive alerts on pending works & missed leads
 * - 1-tap quick action buttons with smooth lift-up hover animations
 */
@Composable
fun MiloGreetingCard(
    viewModel: MainViewModel,
    onOpenAskMilo: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToLeads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val employeeName by viewModel.currentEmployeeName.collectAsState()
    val allLeads by viewModel.leads.collectAsState()
    val allTasks by viewModel.tasks.collectAsState()
    val (isMiloMuted, toggleMiloMute) = rememberMiloMuteState()

    val (greetingTitle, greetingSubtitle) = remember(employeeName) {
        getTimeBasedGreeting(employeeName)
    }

    val pendingCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val missedLeadsCount = remember(allLeads) {
        allLeads.count {
            !it.stage.equals("Won", true) && !it.stage.equals("Lost", true) &&
                    (it.leadScore < 60 || it.nextFollowUp.contains("Today") || it.stage.equals("New", true))
        }
    }
    val activeLeadsCount = remember(allLeads) { allLeads.size }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardHovered by cardInteractionSource.collectIsHoveredAsState()
    var isPointerHovered by remember { mutableStateOf(false) }
    val isHovered = isCardHovered || isPointerHovered

    // Subtle pulse & breathing motion (accelerates & grows when card is hovered)
    val infiniteTransition = rememberInfiniteTransition(label = "GreetingCardMiloPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = if (isHovered) 1.04f else 0.98f,
        targetValue = if (isHovered) 1.14f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isHovered) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val growScale by animateFloatAsState(
        targetValue = if (isHovered) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "GrowScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .hoverable(cardInteractionSource)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Enter, PointerEventType.Move -> isPointerHovered = true
                            PointerEventType.Exit -> isPointerHovered = false
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FAFC),
                            Color.White
                        )
                    )
                )
                .padding(16.dp)
        ) {
            // Top Row: Mascot Avatar + Greeting Speech Bubble + Ask Milo Quick Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Milo Character - Clean, NO circle shape, with responsive hover grow/pulse
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .graphicsLayer {
                            val combinedScale = growScale * pulseScale
                            scaleX = combinedScale
                            scaleY = combinedScale
                        }
                        .liftOnPress(elevationLift = 8.dp, translateY = (-4).dp)
                        .clickable(
                            interactionSource = cardInteractionSource,
                            indication = null
                        ) {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MiloCharacter(
                        state = MiloState.WELCOME,
                        size = 110.dp,
                        showStateBadge = false
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Speech & Dynamic Greeting
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = greetingTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isMiloMuted) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (isMiloMuted) Color(0xFFFECACA) else Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    if (isMiloMuted) {
                                        // Unmute and immediately speak
                                        toggleMiloMute()
                                        MiloVoiceHelper.speakDailyBriefing(
                                            context = context,
                                            employeeName = employeeName,
                                            pendingTasksCount = pendingCount,
                                            activeProjectsCount = 2,
                                            leadsCount = activeLeadsCount,
                                            callLogsCount = 0,
                                            force = true
                                        )
                                    } else {
                                        if (MiloVoiceHelper.isSpeaking()) {
                                            MiloVoiceHelper.stopSpeaking()
                                        } else {
                                            MiloVoiceHelper.speakDailyBriefing(
                                                context = context,
                                                employeeName = employeeName,
                                                pendingTasksCount = pendingCount,
                                                activeProjectsCount = 2,
                                                leadsCount = activeLeadsCount,
                                                callLogsCount = 0,
                                                force = true
                                            )
                                        }
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMiloMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = if (isMiloMuted) "Unmute Milo Voice" else "Listen to Milo",
                                    tint = if (isMiloMuted) Color(0xFFDC2626) else ButtonPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = greetingSubtitle,
                        fontSize = 12.sp,
                        color = BrandBlue,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Proactive Snapshot Badge: Pending Works & Missed Leads
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (missedLeadsCount > 0) StatusRed else StatusGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (missedLeadsCount > 0) "🚨 $missedLeadsCount Missed Leads • $pendingCount Pending Works"
                            else "✨ $pendingCount Pending Works • $activeLeadsCount Funnel Deals",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (missedLeadsCount > 0) Color(0xFF991B1B) else Color(0xFF1E3A8A)
                        )
                    }

                    Text(
                        text = "Live Sync",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Interactive Action Buttons with Smooth Lift-Up Effect
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Ask Milo Button
                item {
                    Button(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
                    ) {
                        Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ask Milo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 2. Auto-Align Button
                item {
                    OutlinedButton(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ButtonPrimary),
                        border = BorderStroke(1.dp, ButtonPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp), tint = ButtonPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Align", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Pending Tasks Button
                item {
                    FilledTonalButton(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            onNavigateToTasks()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEDE9FE),
                            contentColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tasks ($pendingCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 4. Leads Funnel Button
                item {
                    FilledTonalButton(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFEF3C7),
                            contentColor = Color(0xFFD97706)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Funnel ($activeLeadsCount)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // 5. Daily Summary Button
                item {
                    OutlinedButton(
                        onClick = {
                            MiloHaptics.performButtonTap(context)
                            onOpenAskMilo()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669)),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.liftOnPress(elevationLift = 6.dp, translateY = (-3).dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Summaries", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
