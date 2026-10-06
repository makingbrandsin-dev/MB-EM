package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.presentation.screens.MiloOnboardingScreen as PresentationMiloOnboardingScreen

/**
 * Public wrapper for MiloOnboardingScreen in ui.screens package.
 */
@Composable
fun MiloOnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    PresentationMiloOnboardingScreen(
        onFinishOnboarding = onFinishOnboarding,
        modifier = modifier
    )
}
