package com.example.milo

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

/**
 * Controller holding animation clocks, breathing loops, particle systems,
 * and state-machine transitions for Milo the Lion.
 */
class MiloAnimationController {
    // Breathing & Idle sway
    val breathingAnim = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Eye blinking
    val blinkAnim = infiniteRepeatable<Float>(
        animation = keyframes {
            durationMillis = 3500
            0.0f at 0
            0.0f at 3100
            1.0f at 3200
            0.0f at 3300
            0.0f at 3500
        },
        repeatMode = RepeatMode.Restart
    )

    // Arm waving / celebration jump
    val waveAnim = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 600, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Typing fingers / laptop glow
    val typingAnim = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 300, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
    )

    // Pulse for alerts / lightbulb
    val pulseAnim = infiniteRepeatable<Float>(
        animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
}

data class MiloConfettiParticle(
    val id: Int,
    val xOffset: Float,
    val yOffset: Float,
    val size: Float,
    val color: Color,
    val rotation: Float
)

fun generateConfettiParticles(count: Int = 24): List<MiloConfettiParticle> {
    val colors = listOf(
        Color(0xFFEF4444),
        Color(0xFFF59E0B),
        Color(0xFF10B981),
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899)
    )
    val random = Random(42)
    return List(count) { i ->
        MiloConfettiParticle(
            id = i,
            xOffset = (random.nextFloat() - 0.5f) * 160f,
            yOffset = -random.nextFloat() * 120f,
            size = 4f + random.nextFloat() * 8f,
            color = colors[i % colors.size],
            rotation = random.nextFloat() * 360f
        )
    }
}
