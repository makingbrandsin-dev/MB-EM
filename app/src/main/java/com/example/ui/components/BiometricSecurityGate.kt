package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.*
import com.example.util.BiometricHelper
import com.example.util.MiloHaptics
import kotlinx.coroutines.delay

/**
 * Modern, enterprise-grade Biometric Security Gate wrapping sensitive screens
 * (Employee Dashboard and Team Chat) using the androidx.biometric library.
 * Requires biometric verification (fingerprint or face scan) before sensitive
 * attendance trends, performance metrics, and confidential chats are revealed.
 */
@Composable
fun BiometricSecurityGate(
    isUnlocked: Boolean,
    featureTitle: String,
    featureSubtitle: String,
    securityDescription: String = "Biometric authentication (fingerprint or face scan) is required to access sensitive enterprise records and confidential communications.",
    icon: ImageVector = Icons.Default.Fingerprint,
    onUnlockSuccess: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptic = LocalHapticFeedback.current
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val triggerBiometricPrompt: () -> Unit = {
        if (activity != null) {
            isAuthenticating = true
            authError = null
            MiloHaptics.performReactionTick(context, haptic)
            BiometricHelper.promptBiometricAuth(
                activity = activity,
                title = "Unlock $featureTitle",
                subtitle = "Verify fingerprint or face scan to access sensitive data",
                description = securityDescription,
                negativeButtonText = "Cancel",
                onSuccess = {
                    isAuthenticating = false
                    authError = null
                    onUnlockSuccess()
                },
                onError = { err ->
                    isAuthenticating = false
                    authError = err
                },
                onCancel = {
                    isAuthenticating = false
                },
                onFailed = {
                    isAuthenticating = false
                    authError = "Biometric scan unrecognized. Please try again."
                }
            )
        }
    }

    // Auto-trigger biometric prompt on first entry if locked
    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            delay(250)
            triggerBiometricPrompt()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isUnlocked) {
            content()
        } else {
            // Biometric Locked State Screen
            BiometricLockedScreenContent(
                featureTitle = featureTitle,
                featureSubtitle = featureSubtitle,
                securityDescription = securityDescription,
                icon = icon,
                isAuthenticating = isAuthenticating,
                authError = authError,
                onScanClick = triggerBiometricPrompt,
                onBack = onBack
            )
        }
    }
}

@Composable
private fun BiometricLockedScreenContent(
    featureTitle: String,
    featureSubtitle: String,
    securityDescription: String,
    icon: ImageVector,
    isAuthenticating: Boolean,
    authError: String?,
    onScanClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandDarkBlue,
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Shield Tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = BrandAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "androidx.biometric · 256-Bit Secured",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Glowing Pulsing Biometric Scanner Icon Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clickable { onScanClick() }
            ) {
                // Outer glowing pulse ring
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.22f))
                )

                // Mid ring
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = 0.45f))
                        .border(2.dp, BrandAccent.copy(alpha = 0.7f), CircleShape)
                )

                // Core icon button
                Surface(
                    shape = CircleShape,
                    color = BrandBlue,
                    shadowElevation = 10.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Scan Biometrics",
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = featureTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = featureSubtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = BrandAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Information Card
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.07f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sensitive App Data Protected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = securityDescription,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            if (authError != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = authError,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Unlock Button
            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Scanning Sensor...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Scan Fingerprint / Face ID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            if (onBack != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Go Back",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
