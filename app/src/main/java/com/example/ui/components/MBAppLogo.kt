package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.ButtonPrimary
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.ImportantCardBg
import com.example.util.AppPreferences

/**
 * Official Milo Brand Emblem & Logo Container.
 * Supports dynamic Admin custom app logo uploads, custom background tints,
 * and seamless fallback to the official brand mascot emblem.
 */
@Composable
fun MBAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    inCircle: Boolean = false,
    showText: Boolean = false,
    textColor: Color = Color(0xFF2C2416)
) {
    val context = LocalContext.current
    val customLogoUri = remember(context) { AppPreferences.getCustomAppLogoUri(context) }
    val customBgHex = remember(context) { AppPreferences.getAppIconBgColor(context) }
    val bgContainerColor = remember(customBgHex) {
        try {
            Color(android.graphics.Color.parseColor(customBgHex))
        } catch (_: Exception) {
            Color(0xFF0F172A)
        }
    }

    val containerShape = if (inCircle) CircleShape else RoundedCornerShape(16.dp)
    val paddingAmount = (size * 0.12f).coerceAtLeast(4.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.size(size),
            shape = containerShape,
            color = bgContainerColor,
            border = BorderStroke(1.5.dp, Color(0xFF334155)),
            shadowElevation = 6.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingAmount)
            ) {
                if (!customLogoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = customLogoUri,
                        contentDescription = "Custom App Logo & Icon",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.milo_final),
                        contentDescription = "Milo Official Brand Mascot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showText) {
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "MB Taker",
                    fontFamily = CinzelFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp,
                    color = textColor,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Track · Manage · Grow",
                    fontSize = (size.value * 0.18f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF705C30),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
