package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Google Play Policy-compliant Prominent Disclosure & Affirmative Consent Dialog.
 *
 * Requirements:
 * - Rendered in-app immediately preceding the sensitive system permission dialog.
 * - Explains what specific data is collected (e.g. GPS coordinates, Camera frames, Audio).
 * - Explains the explicit feature requiring it (e.g. Geofence clock-in, Attendance selfie, Voice note).
 * - Clarifies transmission/retention rules (e.g. sent encrypted to company database, never sold).
 * - Discloses background vs foreground behavior (e.g. "Location is ONLY collected at punch-in, NEVER in the background").
 * - Requires explicit affirmative user tap on "I Understand & Consent".
 */
@Composable
fun PermissionDisclosureDialog(
    icon: ImageVector = Icons.Default.Security,
    iconTint: Color = BrandBlue,
    title: String,
    featureName: String,
    dataAccessed: String,
    purposeExplanation: String,
    backgroundDisclosure: String = "This permission is used ONLY while this feature is actively in the foreground. No data is collected in the background.",
    transmissionDisclosure: String = "Data is encrypted in transit and stored securely in company enterprise records. It is never shared with third parties or advertisers.",
    confirmButtonText: String = "I Understand & Agree",
    denyButtonText: String = "Not Now",
    onDismiss: () -> Unit,
    onAffirmativeConsent: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Feature: $featureName",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = iconTint
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Structured Disclosures Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DisclosureItem(
                            label = "Data Accessed",
                            detail = dataAccessed
                        )
                        DisclosureItem(
                            label = "Purpose & Functionality",
                            detail = purposeExplanation
                        )
                        DisclosureItem(
                            label = "Background Policy",
                            detail = backgroundDisclosure
                        )
                        DisclosureItem(
                            label = "Storage & Transmission",
                            detail = transmissionDisclosure
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = denyButtonText,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = onAffirmativeConsent,
                        modifier = Modifier.weight(1.3f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iconTint)
                    ) {
                        Text(
                            text = confirmButtonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureItem(label: String, detail: String) {
    Column {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = detail,
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 16.sp
        )
    }
}
