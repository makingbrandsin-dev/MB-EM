package com.example.presentation.components.milo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import com.example.R
import com.example.domain.milo.MiloState
import com.example.domain.milo.MiloViewModel
import com.example.milo.MiloVideoHelper
import com.example.milo.MiloVideoSurface
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Presentation Composable for Milo the Smart Lion Assistant.
 * Uses AnimatedContent to transition between the 13 states from the MiloViewModel.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiloCharacter(
    viewModel: MiloViewModel,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showStateBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()

    MiloCharacter(
        state = state,
        modifier = modifier,
        size = size,
        showStateBadge = showStateBadge,
        onClick = onClick
    )
}

/**
 * Direct MiloCharacter Composable utilizing AnimatedContent for seamless state transitions.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun MiloCharacter(
    state: MiloState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    showStateBadge: Boolean = false,
    showTooltipOnTap: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            delay(3500)
            showTooltip = false
        }
    }

    // Continuous dynamic animation cycles
    val infiniteTransition = rememberInfiniteTransition(label = "MiloCharacterContinuous")

    // Breathing pulse
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathing"
    )

    // Eye blinking
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3500
                0f at 0
                0f at 3000
                1f at 3120
                0f at 3240
                0f at 3500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "Blink"
    )

    // Hand wave & motion oscillation
    val motionCycle by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "MotionCycle"
    )

    // Float jump for celebration
    val jumpOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Jump"
    )

    // Lightbulb / Badge Glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(
        modifier = modifier
            .size(size)
            .combinedClickable(
                onClick = {
                    if (showTooltipOnTap) {
                        showTooltip = !showTooltip
                    } else if (onClick != null) {
                        onClick()
                    } else {
                        showTooltip = !showTooltip
                    }
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTooltip = true
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // AnimatedContent wrapper with smooth Crossfade & easing for transitioning between Milo states
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 1.03f, animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing))
                    )
            },
            label = "MiloStateAnimatedContent"
        ) { targetState ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = if (targetState == MiloState.CONVERTED || targetState == MiloState.CELEBRATION) jumpOffset.dp else 0.dp)
                    .scale(breathingScale),
                contentAlignment = Alignment.Center
            ) {
                // Milo Real Character with ExoPlayer MP4 Video Playback Support
                val context = androidx.compose.ui.platform.LocalContext.current
                val videoUri = remember(targetState) { MiloVideoHelper.getMiloVideoUri(context, targetState) }

                Box(
                    modifier = Modifier
                        .fillMaxSize(if (targetState == MiloState.THINKING) 0.98f else 0.92f),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoUri != null) {
                        MiloVideoSurface(
                            videoUri = videoUri,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val imageRes = if (targetState == MiloState.THINKING) R.drawable.milo_thinking else R.drawable.milo_final
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Milo Lion Assistant - ${targetState.title}: ${targetState.description}",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Clean character without status overlays
            }
        }

        // State Description Tooltip Popup
        if (showTooltip) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, -140),
                onDismissRequest = { showTooltip = false },
                properties = PopupProperties(focusable = false, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, state.primaryColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = state.emoji,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Milo • ${state.title}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = state.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Procedural Vector Drawing for Milo the Lion.
 * Features rounded mane, muzzle, dark ears, expressive eyes, and MB polo uniform.
 */
private fun DrawScope.drawMiloLion(
    state: MiloState,
    blink: Float,
    motion: Float,
    glow: Float
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f

    // Color Palette based on MB Brand & Lion Character
    val maneColor = Color(0xFFC05621) // Rich Auburn Golden Mane
    val maneHighlight = Color(0xFFD97706)
    val furColor = Color(0xFFF59E0B) // Bright Golden Lion Fur
    val furShadow = Color(0xFFD97706)
    val muzzleColor = Color(0xFFFEF3C7) // Cream Muzzle
    val earInnerColor = Color(0xFFFCA5A5) // Soft Pink Inner Ear
    val eyeColor = Color(0xFF1E293B) // Dark expressive eyes
    val noseColor = Color(0xFF78350F) // Warm Brown Nose
    val mbShirtColor = Color(0xFF1E3A8A) // MB Navy Blue Polo
    val mbShirtCollar = Color(0xFF3B82F6)

    // 1. MANE (Voluminous rounded layered circles)
    val maneRadius = w * 0.44f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(maneHighlight, maneColor),
            center = Offset(cx, cy * 0.95f),
            radius = maneRadius * 1.15f
        ),
        radius = maneRadius,
        center = Offset(cx, cy * 0.92f)
    )

    // Mane tufts around perimeter
    val tuftCount = 12
    for (i in 0 until tuftCount) {
        val angle = (i * (360f / tuftCount)) * (Math.PI / 180.0)
        val tuftX = cx + (maneRadius * 0.86f * kotlin.math.cos(angle)).toFloat()
        val tuftY = (cy * 0.92f) + (maneRadius * 0.86f * kotlin.math.sin(angle)).toFloat()
        drawCircle(
            color = maneColor,
            radius = w * 0.14f,
            center = Offset(tuftX, tuftY)
        )
    }

    // 2. EARS
    val earOffsetY = cy * 0.52f
    val earSpacing = w * 0.28f
    // Left Ear
    drawCircle(color = maneColor, radius = w * 0.12f, center = Offset(cx - earSpacing, earOffsetY))
    drawCircle(color = earInnerColor, radius = w * 0.07f, center = Offset(cx - earSpacing, earOffsetY))
    // Right Ear
    drawCircle(color = maneColor, radius = w * 0.12f, center = Offset(cx + earSpacing, earOffsetY))
    drawCircle(color = earInnerColor, radius = w * 0.07f, center = Offset(cx + earSpacing, earOffsetY))

    // 3. MB POLO SHIRT & BODY (Bottom)
    val bodyPath = Path().apply {
        moveTo(cx - w * 0.32f, h)
        lineTo(cx - w * 0.22f, cy * 1.35f)
        quadraticBezierTo(cx, cy * 1.28f, cx + w * 0.22f, cy * 1.35f)
        lineTo(cx + w * 0.32f, h)
        close()
    }
    drawPath(bodyPath, color = mbShirtColor)

    // Polo Collar (V-neck)
    val collarPath = Path().apply {
        moveTo(cx - w * 0.16f, cy * 1.34f)
        lineTo(cx, cy * 1.58f)
        lineTo(cx + w * 0.16f, cy * 1.34f)
        lineTo(cx, cy * 1.44f)
        close()
    }
    drawPath(collarPath, color = mbShirtCollar)

    // MB Monogram Button / Badge
    drawCircle(
        color = Color.White,
        radius = w * 0.035f,
        center = Offset(cx + w * 0.12f, cy * 1.55f)
    )

    // 4. HEAD (Golden Fur Face)
    val headRadius = w * 0.31f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(furColor, furShadow),
            center = Offset(cx, cy * 0.88f),
            radius = headRadius * 1.1f
        ),
        radius = headRadius,
        center = Offset(cx, cy * 0.90f)
    )

    // 5. MUZZLE (Cream Colored Snout)
    val muzzleY = cy * 1.02f
    drawOval(
        color = muzzleColor,
        topLeft = Offset(cx - w * 0.20f, muzzleY - h * 0.12f),
        size = Size(w * 0.40f, h * 0.26f)
    )

    // Nose
    val nosePath = Path().apply {
        moveTo(cx - w * 0.075f, muzzleY - h * 0.04f)
        lineTo(cx + w * 0.075f, muzzleY - h * 0.04f)
        quadraticBezierTo(cx, muzzleY + h * 0.04f, cx - w * 0.075f, muzzleY - h * 0.04f)
        close()
    }
    drawPath(nosePath, color = noseColor)

    // Mouth Expression based on State
    val mouthPath = Path()
    when (state) {
        MiloState.ERROR, MiloState.WARNING -> {
            // Straight / concerned mouth
            mouthPath.moveTo(cx - w * 0.08f, muzzleY + h * 0.07f)
            mouthPath.lineTo(cx + w * 0.08f, muzzleY + h * 0.07f)
        }
        MiloState.CONVERTED, MiloState.CELEBRATION, MiloState.SUCCESS -> {
            // Big open smile
            mouthPath.moveTo(cx - w * 0.11f, muzzleY + h * 0.04f)
            mouthPath.quadraticBezierTo(cx, muzzleY + h * 0.13f, cx + w * 0.11f, muzzleY + h * 0.04f)
        }
        else -> {
            // Warm confident smile
            mouthPath.moveTo(cx - w * 0.09f, muzzleY + h * 0.05f)
            mouthPath.quadraticBezierTo(cx, muzzleY + h * 0.10f, cx + w * 0.09f, muzzleY + h * 0.05f)
        }
    }
    drawPath(
        path = mouthPath,
        color = noseColor,
        style = Stroke(width = w * 0.025f, cap = StrokeCap.Round)
    )

    // 6. EYES (Expressive, Animated Blinking)
    val eyeSpacing = w * 0.13f
    val eyeY = cy * 0.78f
    val eyeRadiusX = w * 0.055f
    val eyeRadiusY = w * 0.075f * (1f - blink)

    if (eyeRadiusY > 1f) {
        // Left Eye
        drawOval(
            color = eyeColor,
            topLeft = Offset(cx - eyeSpacing - eyeRadiusX, eyeY - eyeRadiusY),
            size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
        )
        // Left Catchlight
        drawCircle(
            color = Color.White,
            radius = eyeRadiusX * 0.35f,
            center = Offset(cx - eyeSpacing - eyeRadiusX * 0.25f, eyeY - eyeRadiusY * 0.25f)
        )

        // Right Eye
        drawOval(
            color = eyeColor,
            topLeft = Offset(cx + eyeSpacing - eyeRadiusX, eyeY - eyeRadiusY),
            size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
        )
        // Right Catchlight
        drawCircle(
            color = Color.White,
            radius = eyeRadiusX * 0.35f,
            center = Offset(cx + eyeSpacing - eyeRadiusX * 0.25f, eyeY - eyeRadiusY * 0.25f)
        )
    } else {
        // Closed / Blinking line
        drawLine(
            color = eyeColor,
            start = Offset(cx - eyeSpacing - eyeRadiusX, eyeY),
            end = Offset(cx - eyeSpacing + eyeRadiusX, eyeY),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = eyeColor,
            start = Offset(cx + eyeSpacing - eyeRadiusX, eyeY),
            end = Offset(cx + eyeSpacing + eyeRadiusX, eyeY),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )
    }

    // Eyebrows
    val browY = eyeY - h * 0.10f
    val browTilt = when (state) {
        MiloState.THINKING -> h * 0.02f
        MiloState.WARNING, MiloState.ERROR -> -h * 0.02f
        else -> 0f
    }
    drawLine(
        color = noseColor,
        start = Offset(cx - eyeSpacing - w * 0.06f, browY + browTilt),
        end = Offset(cx - eyeSpacing + w * 0.06f, browY - browTilt),
        strokeWidth = w * 0.022f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = noseColor,
        start = Offset(cx + eyeSpacing - w * 0.06f, browY - browTilt),
        end = Offset(cx + eyeSpacing + w * 0.06f, browY + browTilt),
        strokeWidth = w * 0.022f,
        cap = StrokeCap.Round
    )

    // 7. ARMS & ACCESSORIES BASED ON STATE
    when (state) {
        MiloState.WELCOME, MiloState.GOODBYE -> {
            // Waving Right Hand
            val handX = cx + w * 0.34f
            val handY = cy * 0.95f + (motion * 0.2f)
            drawCircle(color = furColor, radius = w * 0.09f, center = Offset(handX, handY))
            drawCircle(color = muzzleColor, radius = w * 0.05f, center = Offset(handX, handY))
        }
        MiloState.WORKING -> {
            // MB Laptop at bottom
            val lapTopTop = h * 0.80f
            drawRoundRect(
                color = Color(0xFF64748B),
                topLeft = Offset(cx - w * 0.28f, lapTopTop),
                size = Size(w * 0.56f, h * 0.18f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - w * 0.25f, lapTopTop + 2f),
                size = Size(w * 0.50f, h * 0.14f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
        MiloState.SUCCESS -> {
            // Thumbs up paw
            val thumbX = cx + w * 0.32f
            val thumbY = cy * 1.05f
            drawCircle(color = furColor, radius = w * 0.08f, center = Offset(thumbX, thumbY))
            drawOval(
                color = furColor,
                topLeft = Offset(thumbX - w * 0.03f, thumbY - h * 0.09f),
                size = Size(w * 0.06f, h * 0.09f)
            )
        }
        MiloState.CONVERTED, MiloState.CELEBRATION -> {
            // Raised double victory paws
            drawCircle(color = furColor, radius = w * 0.08f, center = Offset(cx - w * 0.32f, cy * 0.72f))
            drawCircle(color = furColor, radius = w * 0.08f, center = Offset(cx + w * 0.32f, cy * 0.72f))
        }
        MiloState.WARNING -> {
            // Raised Stop/Caution Palm
            val palmX = cx + w * 0.32f
            val palmY = cy * 0.98f
            drawCircle(color = furColor, radius = w * 0.09f, center = Offset(palmX, palmY))
            drawCircle(color = muzzleColor, radius = w * 0.055f, center = Offset(palmX, palmY))
        }
        MiloState.FOLLOW_UP -> {
            // Smartphone in paw
            val phoneX = cx + w * 0.30f
            val phoneY = cy * 0.98f
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(phoneX - w * 0.05f, phoneY - h * 0.08f),
                size = Size(w * 0.10f, h * 0.15f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
            drawCircle(color = furColor, radius = w * 0.065f, center = Offset(phoneX, phoneY))
        }
        else -> {}
    }
}
