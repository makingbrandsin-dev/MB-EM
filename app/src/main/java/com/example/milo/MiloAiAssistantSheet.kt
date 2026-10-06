package com.example.milo

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.ui.screens.MainViewModel

/**
 * Interactive Milo AI Assistant Bottom Sheet / Dialogue.
 * Wraps the Comprehensive Milo Smart Assistant Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAiAssistantSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToCalls: () -> Unit = {}
) {
    MiloSmartAssistantSheet(
        viewModel = viewModel,
        onDismiss = onDismiss,
        onNavigateToLeads = onNavigateToLeads,
        onNavigateToTasks = onNavigateToTasks,
        onNavigateToCalls = onNavigateToCalls
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloAiAssistantSheet(
    miloViewModel: MiloViewModel,
    onDismiss: () -> Unit,
    onNavigateToLeads: () -> Unit = {},
    onNavigateToFollowUps: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    MiloAssistantAdvancedDialog(
        miloViewModel = miloViewModel,
        onDismiss = onDismiss,
        onNavigateToLeads = onNavigateToLeads,
        onNavigateToTasks = onNavigateToTasks
    )
}

