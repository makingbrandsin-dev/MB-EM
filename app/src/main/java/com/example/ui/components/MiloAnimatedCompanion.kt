package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.util.MiloHaptics

enum class MiloMood {
    SLEEPING,      // Inactive / No work -> Sleeping with Zzz
    LISTENING,     // User speaking / typing -> Ears up, radar pulse
    THINKING,      // Processing intent -> Thinking pulse
    SPEAKING,      // Delivering response -> Soundwave / roar bounce
    AWAKE_IDLE     // Ready & waiting
}

@Composable
fun MiloMiniAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 32
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(sizeDp.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.milo_final),
            contentDescription = "Milo",
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}

@Composable
fun FloatingAskMiloButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mood: MiloMood = MiloMood.AWAKE_IDLE,
    expanded: Boolean = true
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "floating_milo")

    // Subtle gentle float / breathing
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    // Pulsing aura for idle state
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_scale"
    )

    Surface(
        onClick = {
            MiloHaptics.performButtonTap(context, hapticFeedback)
            onClick()
        },
        shape = RoundedCornerShape(28.dp),
        color = BrandDarkBlue,
        shadowElevation = 10.dp,
        border = androidx.compose.foundation.BorderStroke(2.dp, BrandAccent),
        modifier = modifier
            .offset(y = floatOffset.dp)
            .scale(auraScale)
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mascot Icon with mood ring
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.milo_final),
                    contentDescription = "Milo Lion",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                )

                // Active dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (mood == MiloMood.SLEEPING) Color(0xFF94A3B8) else Color(0xFF10B981))
                        .border(1.5.dp, BrandDarkBlue, CircleShape)
                )
            }

            if (expanded) {
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ASK MILO",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BrandAccent
                        ) {
                            Text(
                                text = "AI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandDarkBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "20X Assistant ⚡",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFDE68A)
                    )
                }
            }
        }
    }
}

@Composable
fun MiloAnimatedAvatar(
    mood: MiloMood,
    modifier: Modifier = Modifier,
    sizeDp: Int = 64,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Trigger state change haptics
    LaunchedEffect(mood) {
        when (mood) {
            MiloMood.SLEEPING -> MiloHaptics.performSleep(context, hapticFeedback)
            MiloMood.LISTENING -> MiloHaptics.performListening(context, hapticFeedback)
            MiloMood.THINKING -> MiloHaptics.performThinking(context, hapticFeedback)
            MiloMood.SPEAKING -> MiloHaptics.performRoarResponse(context, hapticFeedback)
            MiloMood.AWAKE_IDLE -> { /* ready / awake state */ }
        }
    }

    // 1. Breathing / Sleep Animation
    val infiniteTransition = rememberInfiniteTransition(label = "milo_anim")

    val scale by infiniteTransition.animateFloat(
        initialValue = if (mood == MiloMood.SLEEPING) 0.94f else 0.98f,
        targetValue = if (mood == MiloMood.SLEEPING) 1.04f else 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (mood == MiloMood.SLEEPING) 2200 else 650,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "milo_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = if (mood == MiloMood.SPEAKING) -5f else if (mood == MiloMood.THINKING) -3f else 0f,
        targetValue = if (mood == MiloMood.SPEAKING) 5f else if (mood == MiloMood.THINKING) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (mood == MiloMood.SPEAKING) 320 else 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "milo_rotate"
    )

    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (mood == MiloMood.LISTENING || mood == MiloMood.SPEAKING) 36f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (mood == MiloMood.LISTENING || mood == MiloMood.SPEAKING) 0.75f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .size((sizeDp + 24).dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        if (mood == MiloMood.SLEEPING) {
                            MiloHaptics.performWakeUp(context, hapticFeedback)
                        } else {
                            MiloHaptics.performMiloMascotTap(context, hapticFeedback)
                        }
                        onClick()
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Radar / Soundwave Ring Canvas
        if (mood == MiloMood.LISTENING || mood == MiloMood.SPEAKING || mood == MiloMood.THINKING) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerOffset = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    color = if (mood == MiloMood.LISTENING) Color(0xFF10B981).copy(alpha = pulseAlpha)
                    else BrandAccent.copy(alpha = pulseAlpha),
                    radius = (size.width / 2.6f) + pulseRadius,
                    center = centerOffset,
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Sleeping Zzz overlay particles
        if (mood == MiloMood.SLEEPING) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandDarkBlue.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "💤 Zzz...",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Avatar Core Container - Clean transparent, NO circle shape, NO dark background
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .scale(scale)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = if (mood == MiloMood.THINKING) R.drawable.milo_thinking else R.drawable.milo_final,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                label = "MiloAvatarCrossfade"
            ) { imageRes ->
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Milo Lion Mascot",
                    modifier = Modifier.fillMaxSize()
                )
            }

                // Small mood badge overlay in bottom center
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BrandDarkBlue.copy(alpha = 0.85f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = when (mood) {
                            MiloMood.SLEEPING -> "SLEEP"
                            MiloMood.LISTENING -> "HEARING"
                            MiloMood.THINKING -> "THINK"
                            MiloMood.SPEAKING -> "ROAR!"
                            MiloMood.AWAKE_IDLE -> "MILO"
                        },
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black,
                        color = when (mood) {
                            MiloMood.SLEEPING -> Color(0xFF94A3B8)
                            MiloMood.LISTENING -> Color(0xFF34D399)
                            else -> BrandAccent
                        }
                    )
                }
            }
        }
    }