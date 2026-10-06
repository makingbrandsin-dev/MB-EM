package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.domain.milo.MiloViewModel
import com.example.presentation.screens.DashboardScreen as PresentationDashboardScreen

/**
 * Public Composable for the DashboardScreen in the ui.screens package,
 * delegating to the presentation architecture layer.
 */
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    miloViewModel: MiloViewModel = androidx.compose.runtime.remember { MiloViewModel() },
    onNavigateToLeads: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {},
    onNavigateToLeadDetail: (Long) -> Unit = {},
    onNavigateToChat: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    PresentationDashboardScreen(
        viewModel = viewModel,
        miloViewModel = miloViewModel,
        onNavigateToLeads = onNavigateToLeads,
        onNavigateToTasks = onNavigateToTasks,
        onNavigateToAttendance = onNavigateToAttendance,
        onNavigateToProjects = onNavigateToProjects,
        onNavigateToLeadDetail = onNavigateToLeadDetail,
        onNavigateToChat = onNavigateToChat,
        onNavigateToNotifications = onNavigateToNotifications
    )
}
