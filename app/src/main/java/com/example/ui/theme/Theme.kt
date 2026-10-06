package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandLightBlue.copy(alpha = 0.12f),
    onPrimaryContainer = BrandDarkBlue,
    secondary = BrandAccent,
    onSecondary = Color.Black,
    background = SurfaceBg,
    onBackground = TextPrimary,
    surface = CardBg,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandLightBlue,
    onPrimary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.WHITE
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * High-contrast, creative text field color provider to ensure letters are ALWAYS
 * 100% visible, legible, and styled with creative blue accents when typing across
 * both light and dark system themes.
 */
@Composable
fun appTextFieldColors(
    containerColor: Color = Color.White,
    focusedBorderColor: Color = ElectricBlue,
    unfocusedBorderColor: Color = Color(0xFFCBD5E1),
    textColor: Color = Color(0xFF0F172A)
): androidx.compose.material3.TextFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor.copy(alpha = 0.6f),
    focusedBorderColor = focusedBorderColor,
    unfocusedBorderColor = unfocusedBorderColor,
    focusedLabelColor = focusedBorderColor,
    unfocusedLabelColor = Color(0xFF64748B),
    cursorColor = focusedBorderColor,
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    focusedLeadingIconColor = focusedBorderColor,
    unfocusedLeadingIconColor = Color(0xFF64748B),
    focusedTrailingIconColor = focusedBorderColor,
    unfocusedTrailingIconColor = Color(0xFF64748B),
    selectionColors = androidx.compose.foundation.text.selection.TextSelectionColors(
        handleColor = ElectricBlue,
        backgroundColor = ElectricBlue.copy(alpha = 0.3f)
    )
)

