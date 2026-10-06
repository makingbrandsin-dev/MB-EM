package com.example.milo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.milo.MiloState
import com.example.ui.theme.*
import com.example.util.MiloHaptics
import kotlin.math.cos
import kotlin.math.sin

/**
 * Celebratory Dialog triggered whenever user earns enough XP to reach a new level.
 * Features Milo in MiloState.CELEBRATION with starburst confetti, scale pulse, and accessory unlock notice.
 */
@Composable
fun MiloLevelUpCelebrationDialog(
    newLevel: Int,
    onDismiss: () -> Unit,
    onOpenWardrobe: () -> Unit
) {
    val context = LocalContext.current

    // Trigger celebratory haptic feedback on launch
    LaunchedEffect(newLevel) {
        MiloHaptics.performSuccess(context)
    }

    val themeUnlocked = remember(newLevel) {
        MiloThemeAccessory.entries.find { it.requiredLevel == newLevel } ?: MiloThemeAccessory.CLASSIC
    }

    // Continuous celebration bounce animation
    val infiniteTransition = rememberInfiniteTransition(label = "LevelUpBounce")
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    val confettiRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing)
        ),
        label = "ConfettiRotation"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceBg,
                            Color.White,
                            BrandBlue.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(2.dp, StatusOrange, RoundedCornerShape(28.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Header Badge
                Surface(
                    color = StatusOrange,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🎉 LEVEL UP! LEVEL $newLevel",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Milo Character in CELEBRATION State with Particle Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    // Confetti Particles Background Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2.2f
                        val colors = listOf(
                            Color(0xFFFFD700), Color(0xFFFF5722), Color(0xFF00E5FF),
                            Color(0xFF4CAF50), Color(0xFFE91E63), Color(0xFF9C27B0)
                        )
                        for (i in 0 until 12) {
                            val angle = Math.toRadians((i * 30 + confettiRotation).toDouble())
                            val x = center.x + radius * cos(angle).toFloat()
                            val y = center.y + radius * sin(angle).toFloat()
                            drawCircle(
                                color = colors[i % colors.size],
                                radius = 6f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Milo in CELEBRATION State
                    MiloCharacter(
                        state = MiloState.CELEBRATION,
                        size = 120.dp,
                        showStateBadge = true,
                        modifier = Modifier.scale(celebrationScale)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Incredible Field Milestone!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Milo is celebrating your productivity boost!",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Unlocked Theme Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(18.dp),
                    border = borderBorder(themeUnlocked.primaryColorHex),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(themeUnlocked.primaryColorHex).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(themeUnlocked.iconEmoji, fontSize = 24.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unlocked: ${themeUnlocked.title}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = themeUnlocked.description,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = {
                        onDismiss()
                        onOpenWardrobe()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Equip in Wardrobe")
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue to CRM", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun borderBorder(colorHex: Long): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.5.dp, Color(colorHex))
}
