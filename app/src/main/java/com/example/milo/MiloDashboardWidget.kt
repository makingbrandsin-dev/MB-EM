package com.example.milo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.domain.milo.MiloEvent
import com.example.domain.milo.MiloState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Milo Dashboard Card Component.
 * Features:
 * - Animated Milo Character
 * - Dynamic AI speech bubble
 * - New Leads & Follow-ups mini counters
 * - One-tap action suggestions
 * - Interactive state switcher chip strip
 */
@Composable
fun MiloDashboardWidget(
    miloViewModel: MiloViewModel,
    modifier: Modifier = Modifier,
    newLeadsCount: Int = 12,
    followUpsCount: Int = 8,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {}
) {
    val state by miloViewModel.state.collectAsState()
    val speechText by miloViewModel.speechText.collectAsState()
    val subSpeechText by miloViewModel.subSpeechText.collectAsState()
    val suggestedAction by miloViewModel.suggestedAction.collectAsState()
    val isAiThinking by miloViewModel.isAiThinking.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    var showStatePicker by remember { mutableStateOf(false) }

    val cardInteractionSource = remember { MutableInteractionSource() }
    val isCardHovered by cardInteractionSource.collectIsHoveredAsState()
    var isPointerHovered by remember { mutableStateOf(false) }
    val isHovered = isCardHovered || isPointerHovered

    val infiniteTransition = rememberInfiniteTransition(label = "DashboardWidgetMiloPulse")
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
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, state.primaryColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            state.primaryColor.copy(alpha = 0.05f),
                            Color.White
                        )
                    )
                )
        ) {
            // Linear progress indicator when AI assistant is processing a query
            if (isAiThinking || state == MiloState.THINKING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp),
                    color = state.primaryColor,
                    trackColor = state.primaryColor.copy(alpha = 0.15f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
            // Header Row: Mascot title & State badge / Interactive mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = state.primaryColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🦁", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Milo — Smart Assistant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandDarkBlue
                    )
                }

                // Current State pill (Clickable to switch animations)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = state.primaryColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, state.primaryColor.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable { showStatePicker = !showStatePicker }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(state.emoji, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = state.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = state.primaryColor
                        )
                        Icon(
                            imageVector = if (showStatePicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle States",
                            tint = state.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Optional Interactive State Selector Strip
            AnimatedVisibility(
                visible = showStatePicker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        "Tap a state to test Milo's reaction:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MiloState.values().forEach { st ->
                            val isSelected = st == state
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) st.primaryColor else SurfaceBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) st.primaryColor else BorderLight
                                ),
                                modifier = Modifier.clickable {
                                    com.example.util.MiloHaptics.performReactionTick(context, hapticFeedback)
                                    miloViewModel.setState(st)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(st.emoji, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = st.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content: Milo on Left + Speech Bubble on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated Milo Mascot (Full body, bigger size with hover pulse/grow animation)
                Box(
                    modifier = Modifier.graphicsLayer {
                        val total = growScale * pulseScale
                        scaleX = total
                        scaleY = total
                    }
                ) {
                    MiloCharacter(
                        state = state,
                        size = 140.dp,
                        showStateBadge = false,
                        useFullBody = true,
                        onClick = {
                            com.example.util.MiloHaptics.performMiloMascotTap(context, hapticFeedback)
                            onOpenAiAssistant()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Speech Bubble Container with animated text crossfade
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceBg,
                    border = BorderStroke(1.dp, BorderLight),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            com.example.util.MiloHaptics.performMiloMascotTap(context, hapticFeedback)
                            onOpenAiAssistant()
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        AnimatedContent(
                            targetState = speechText,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)))
                                    .togetherWith(fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                            },
                            label = "SpeechTextCrossfade"
                        ) { targetText ->
                            Text(
                                text = targetText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandDarkBlue,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        AnimatedContent(
                            targetState = subSpeechText,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)))
                                    .togetherWith(fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                            },
                            label = "SubSpeechTextCrossfade"
                        ) { targetSubText ->
                            Text(
                                text = targetSubText,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mini Metric Chips: New Leads & Follow-ups
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // New Leads chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricBlueBg,
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToLeads() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("New Leads", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text("$newLeadsCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlue)
                        }
                        Surface(
                            shape = CircleShape,
                            color = BrandBlue.copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Follow-ups chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VibrantAmberBg,
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToFollowUps() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Follow-ups", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                            Text("$followUpsCount", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFD97706).copy(alpha = 0.15f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Suggested Action + AI Insights Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (suggestedAction != null) {
                    Button(
                        onClick = {
                            when (suggestedAction) {
                                "View Leads" -> onNavigateToLeads()
                                "Call Now", "Call Client", "Open Follow-ups" -> onNavigateToFollowUps()
                                else -> onOpenAiAssistant()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = state.primaryColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = suggestedAction ?: "Take Action",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onOpenAiAssistant,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandBlue
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ask Milo AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
}
