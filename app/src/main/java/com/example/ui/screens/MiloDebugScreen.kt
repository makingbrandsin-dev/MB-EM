package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.domain.milo.MiloViewModel
import com.example.presentation.screens.MiloDebugScreen as PresentationMiloDebugScreen

/**
 * Public wrapper for MiloDebugScreen in the ui.screens package.
 */
@Composable
fun MiloDebugScreen(
    viewModel: MainViewModel,
    miloViewModel: MiloViewModel = androidx.compose.runtime.remember { MiloViewModel() },
    onBack: () -> Unit = {}
) {
    PresentationMiloDebugScreen(
        viewModel = viewModel,
        miloViewModel = miloViewModel,
        onBack = onBack
    )
}
