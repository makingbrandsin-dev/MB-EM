package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import com.example.milo.MiloSmartAssistantSheet
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.util.NotificationHelper
import com.example.util.WhatsAppHelper

class MainActivity : androidx.fragment.app.FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        com.example.util.NotificationHelper.createNotificationChannels(applicationContext)
        setContent {
            MyApplicationTheme {
                MainAppNavHost(viewModel = viewModel)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Otp : Screen("otp")
    object Home : Screen("home")
    object Crm : Screen("crm")
    object Tasks : Screen("tasks")
    object ProductivityInsights : Screen("productivity_insights")
    object Attendance : Screen("attendance")
    object Work : Screen("work")
    object Leads : Screen("leads")
    object Chat : Screen("chat")
    object Profile : Screen("profile")

    object Projects : Screen("projects")
    object ProjectDetail : Screen("project_detail/{projectId}") {
        fun createRoute(id: Long) = "project_detail/$id"
    }
    object LeadDetail : Screen("lead_detail/{leadId}") {
        fun createRoute(id: Long) = "lead_detail/$id"
    }
    object FollowUps : Screen("follow_ups")
    object CallTracker : Screen("call_tracker")
    object ChatRoom : Screen("chat_room/{channelId}/{channelTitle}") {
        fun createRoute(id: String, title: String) = "chat_room/$id/$title"
    }
    object Notifications : Screen("notifications")
    object Holidays : Screen("holidays")
    object Invoices : Screen("invoices")
    object InvoicePdfPreview : Screen("invoice_pdf_preview")
    object DocumentScanner : Screen("document_scanner")
    object WorkScheduleCalendar : Screen("work_schedule_calendar")
    object ShiftSchedule : Screen("shift_schedule")
    object Settings : Screen("settings")
    object HelpSupport : Screen("help_support")
    object ClientMeetings : Screen("client_meetings")
    object ExpenseClaims : Screen("expense_claims")
    object Timesheets : Screen("timesheets")
    object Vault : Screen("vault")
    object LiveTeamTracking : Screen("live_team_tracking")
    object Employees : Screen("employees")
    object MiloDebug : Screen("milo_debug")
    object MiloOnboarding : Screen("milo_onboarding")
    object AskMilo : Screen("ask_milo")
    object PendingApproval : Screen("pending_approval")
}

private fun getScreenOrder(route: String?): Int {
    return when {
        route == Screen.Home.route -> 0
        route == Screen.Crm.route || route == Screen.Leads.route -> 1
        route == Screen.Tasks.route -> 2
        route == Screen.Attendance.route -> 3
        route == Screen.Profile.route -> 4
        route == Screen.Work.route || route == Screen.Projects.route -> 5
        route == Screen.Chat.route -> 6
        else -> 10
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition {
    val initialOrder = getScreenOrder(initialState.destination.route)
    val targetOrder = getScreenOrder(targetState.destination.route)
    val direction = if (targetOrder >= initialOrder) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideIntoContainer(
        towards = direction,
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(120))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition {
    val initialOrder = getScreenOrder(initialState.destination.route)
    val targetOrder = getScreenOrder(targetState.destination.route)
    val direction = if (targetOrder >= initialOrder) {
        AnimatedContentTransitionScope.SlideDirection.Start
    } else {
        AnimatedContentTransitionScope.SlideDirection.End
    }
    return slideOutOfContainer(
        towards = direction,
        animationSpec = tween(140, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(120))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailEnterTransition(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(180, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(150))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailExitTransition(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(140))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopEnterTransition(): EnterTransition {
    return slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(180, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(150))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.detailPopExitTransition(): ExitTransition {
    return slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(150, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(140))
}

@Composable
fun MainAppNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    val activity = context as? androidx.fragment.app.FragmentActivity

    // All runtime permissions required for Calling, Call Recording, Location Geofencing, Camera & Notifications
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CALL_LOG)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions acknowledged
    }

    // Prompt for required app permissions only after user enters the main app (not during splash or login)
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route && currentRoute != Screen.Otp.route) {
            val ungranted = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (ungranted.isNotEmpty()) {
                permissionsLauncher.launch(ungranted.toTypedArray())
            }
        }
    }

    // Handle deep links when user taps a background notification alert (authenticated users only)
    LaunchedEffect(activity?.intent) {
        if (viewModel.isLoggedIn.value) {
            activity?.intent?.let { intent ->
                val destination = intent.getStringExtra("destination")
                if (destination == "chat") {
                    val channelId = intent.getStringExtra("channelId") ?: "company_chat"
                    val channelTitle = intent.getStringExtra("channelTitle") ?: "Team Chat"
                    viewModel.selectChatChannel(channelId)
                    navController.navigate(Screen.ChatRoom.createRoute(channelId, channelTitle))
                } else if (destination == "tasks") {
                    navController.navigate(Screen.Tasks.route)
                } else if (destination == "call_tracker") {
                    navController.navigate(Screen.CallTracker.route)
                } else if (destination == "notifications") {
                    navController.navigate(Screen.Notifications.route)
                }
            }
        }
    }

    // Auto-dispatch Company Profile PDF & Brochure to WhatsApp on lead capture
    LaunchedEffect(Unit) {
        viewModel.whatsAppDispatchEvents.collect { event ->
            val brochureCfg = viewModel.autoBrochureConfig.value
            WhatsAppHelper.sendCompanyProfileToLead(
                context = context,
                leadName = event.leadName,
                leadPhone = event.phone,
                companyName = event.company,
                brochureConfig = brochureCfg
            )
        }
    }

    val nonFooterRoutes = listOf(
        Screen.Splash.route,
        Screen.Login.route,
        Screen.Otp.route,
        Screen.MiloOnboarding.route,
        Screen.PendingApproval.route
    )

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            when {
                route == Screen.Crm.route || route == Screen.Leads.route -> {
                    viewModel.markNotificationsAsReadByCategory("followup")
                    NotificationHelper.clearCategoryNotifications(context, "crm")
                    NotificationHelper.clearCategoryNotifications(context, "lead")
                }
                route == Screen.Tasks.route -> {
                    viewModel.markNotificationsAsReadByCategory("task")
                    NotificationHelper.clearCategoryNotifications(context, "task")
                }
                route == Screen.Attendance.route -> {
                    viewModel.markNotificationsAsReadByCategory("attendance")
                    NotificationHelper.clearCategoryNotifications(context, "attendance")
                }
                route == Screen.Chat.route || route.startsWith("chat_room/") -> {
                    viewModel.markNotificationsAsReadByCategory("message")
                    NotificationHelper.clearCategoryNotifications(context, "chat")
                }
            }
        }
    }

    val showBottomBar = currentRoute != null && currentRoute !in nonFooterRoutes
    var showGlobalMiloAssistant by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = SurfaceBg,
        topBar = {
            val isOnline by viewModel.isOnline.collectAsState()
            androidx.compose.animation.AnimatedVisibility(
                visible = !isOnline,
                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
            ) {
                Surface(
                    color = com.example.ui.theme.StatusRed,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Offline Mode - Limited Functionality",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                val crmCount by viewModel.pendingCrmCount.collectAsState()
                val taskCount by viewModel.pendingTaskCount.collectAsState()
                val attendanceCount by viewModel.pendingAttendanceCount.collectAsState()
                val chatCount by viewModel.unreadChatCount.collectAsState()

                AppBottomNavigationBar(
                    navController = navController,
                    crmBadgeCount = crmCount,
                    taskBadgeCount = taskCount,
                    attendanceBadgeCount = attendanceCount,
                    chatBadgeCount = chatCount,
                    onAskMiloClick = {
                        if (currentRoute != Screen.AskMilo.route) {
                            navController.navigate(Screen.AskMilo.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.fillMaxSize()
            ) {
            // Splash Screen: Shows only App Logo, then routes strictly to Login Screen
            composable(
                route = Screen.Splash.route,
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(350)) + scaleOut(targetScale = 1.05f, animationSpec = tween(350)) }
            ) {
                SplashScreen(
                    onTimeout = {
                        // User requirement: Always show only Logo then Splash or Login screen and no other screens
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            // Login Screen
            composable(
                route = Screen.Login.route,
                enterTransition = {
                    fadeIn(tween(350)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        tween(350, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    fadeOut(tween(280)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        tween(280, easing = FastOutSlowInEasing)
                    )
                }
            ) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { _ ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToOtp = {
                        navController.navigate(Screen.Otp.route)
                    }
                )
            }

            // OTP Screen
            composable(
                route = Screen.Otp.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                OtpVerificationScreen(
                    viewModel = viewModel,
                    onVerifySuccess = { _ ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            // 1. Employee Dashboard (Home)
            composable(
                route = Screen.Home.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                EmployeeDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToLeads = { navController.navigate(Screen.Crm.route) },
                    onNavigateToCalls = { navController.navigate(Screen.CallTracker.route) },
                    onNavigateToHolidays = { navController.navigate(Screen.Holidays.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToMeetings = { navController.navigate(Screen.ClientMeetings.route) },
                    onNavigateToExpenses = { navController.navigate(Screen.ExpenseClaims.route) },
                    onNavigateToTimesheets = { navController.navigate(Screen.Timesheets.route) },
                    onNavigateToVault = { navController.navigate(Screen.Vault.route) },
                    onNavigateToLiveTracking = { navController.navigate(Screen.LiveTeamTracking.route) },
                    onNavigateToEmployees = { navController.navigate(Screen.Employees.route) },
                    onNavigateToDocumentScanner = { navController.navigate(Screen.DocumentScanner.route) },
                    onNavigateToCalendar = { navController.navigate(Screen.WorkScheduleCalendar.route) }
                )
            }

            // 2. CRM Views (Screen.Crm & Screen.Leads)
            composable(
                route = Screen.Crm.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                LeadsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLeadClick = { leadId ->
                        navController.navigate(Screen.LeadDetail.createRoute(leadId))
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(
                route = Screen.Leads.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                LeadsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLeadClick = { leadId ->
                        navController.navigate(Screen.LeadDetail.createRoute(leadId))
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 3. Tasks View
            composable(
                route = Screen.Tasks.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                TasksScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCrm = {
                        navController.navigate(Screen.Crm.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAttendance = {
                        navController.navigate(Screen.Attendance.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 3b. Productivity Insights
            composable(
                route = Screen.ProductivityInsights.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                ProductivityInsightsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }


            // 4. Attendance View
            composable(
                route = Screen.Attendance.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToCrm = {
                        navController.navigate(Screen.Crm.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTasks = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }

            // 5. Work (Projects Hub)
            composable(
                route = Screen.Work.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }

            // 6. Team Chat Hub
            composable(
                route = Screen.Chat.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                TeamChatListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onOpenChannel = { id, title ->
                        viewModel.markChannelAsRead(id)
                        navController.navigate(Screen.ChatRoom.createRoute(id, title))
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            // 7. Profile & Settings
            composable(
                route = Screen.Profile.route,
                enterTransition = { tabEnterTransition() },
                exitTransition = { tabExitTransition() },
                popEnterTransition = { tabEnterTransition() },
                popExitTransition = { tabExitTransition() }
            ) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToHelp = { navController.navigate(Screen.HelpSupport.route) }
                )
            }

            composable(
                route = Screen.Settings.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToMiloDebug = { navController.navigate(Screen.MiloDebug.route) },
                    onNavigateToMiloOnboarding = { navController.navigate(Screen.MiloOnboarding.route) }
                )
            }

            composable(
                route = Screen.MiloOnboarding.route,
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(350)) },
                popEnterTransition = { fadeIn(tween(350)) },
                popExitTransition = { fadeOut(tween(350)) }
            ) {
                MiloOnboardingScreen(
                    onFinishOnboarding = {
                        com.example.util.AppPreferences.setOnboardingCompleted(context, true)
                        val isLogged = viewModel.isUserLoggedIn()
                        val destination = if (isLogged) Screen.Home.route else Screen.Login.route
                        navController.navigate(destination) {
                            popUpTo(Screen.MiloOnboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.MiloDebug.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                MiloDebugScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.HelpSupport.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                HelpSupportScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screens with detail slide & fade animations
            composable(
                route = Screen.Projects.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                ProjectsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }

            composable(
                route = Screen.ProjectDetail.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("projectId") ?: "1"
                val id = idStr.toLongOrNull() ?: 1L
                ProjectDetailScreen(
                    projectId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onViewTasks = { navController.navigate(Screen.Tasks.route) }
                )
            }

            composable(
                route = Screen.LeadDetail.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val idStr = backStackEntry.arguments?.getString("leadId") ?: "1"
                val id = idStr.toLongOrNull() ?: 1L
                LeadDetailScreen(
                    leadId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.FollowUps.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                FollowUpsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CallTracker.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                CallTrackerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ChatRoom.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId") ?: "dev_team"
                val channelTitle = backStackEntry.arguments?.getString("channelTitle") ?: "Development Team"
                ChatRoomScreen(
                    channelId = channelId,
                    channelTitle = channelTitle,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Notifications.route,
                enterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))
                },
                exitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(260))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(260))
                }
            ) {
                NotificationsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Holidays.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                HolidaysLeaveScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Invoices.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                InvoiceQuotationScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) },
                    onNavigateToDocumentScanner = { navController.navigate(Screen.DocumentScanner.route) }
                )
            }

            composable(
                route = Screen.InvoicePdfPreview.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                MiloInvoicePdfPreviewScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) }
                )
            }

            composable(
                route = Screen.DocumentScanner.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                com.example.ui.screens.DocumentScannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) }
                )
            }

            composable(
                route = Screen.WorkScheduleCalendar.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                com.example.ui.screens.WorkScheduleCalendarScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToShiftRotations = { navController.navigate(Screen.ShiftSchedule.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) }
                )
            }

            composable(
                route = Screen.ShiftSchedule.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                com.example.ui.screens.ShiftScheduleScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ClientMeetings.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                ClientMeetingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ExpenseClaims.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                ExpenseClaimsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Timesheets.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                TimesheetsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Vault.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                VaultScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.LiveTeamTracking.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                LiveTeamTrackingScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Employees.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                EmployeeListScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AskMilo.route,
                enterTransition = { detailEnterTransition() },
                exitTransition = { detailExitTransition() },
                popEnterTransition = { detailPopEnterTransition() },
                popExitTransition = { detailPopExitTransition() }
            ) {
                AskMiloScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToLeads = { navController.navigate(Screen.Crm.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToCalls = { navController.navigate(Screen.CallTracker.route) },
                    onNavigateToInvoices = { navController.navigate(Screen.Invoices.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToAttendance = { navController.navigate(Screen.Attendance.route) },
                    onNavigateToInvoicePdfPreview = { navController.navigate(Screen.InvoicePdfPreview.route) },
                    onNavigateToDocumentScanner = { navController.navigate(Screen.DocumentScanner.route) }
                )
            }

            composable(
                route = Screen.PendingApproval.route,
                enterTransition = { fadeIn(tween(400)) },
                exitTransition = { fadeOut(tween(350)) },
                popEnterTransition = { fadeIn(tween(350)) },
                popExitTransition = { fadeOut(tween(350)) }
            ) {
                PendingApprovalScreen(
                    viewModel = viewModel,
                    onApproved = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PendingApproval.route) { inclusive = true }
                        }
                    },
                    onSignOut = {
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    // 🦁 Global Milo AI Smart Assistant Sheet (Triggers from bottom bar or any screen)
    if (showGlobalMiloAssistant && currentRoute != null && currentRoute != Screen.Splash.route) {
        MiloSmartAssistantSheet(
            viewModel = viewModel,
            onDismiss = { showGlobalMiloAssistant = false },
            onNavigateToLeads = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Crm.route)
            },
            onNavigateToTasks = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Tasks.route)
            },
            onNavigateToCalls = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.CallTracker.route)
            },
            onNavigateToInvoices = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Invoices.route)
            },
            onNavigateToNotifications = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Notifications.route)
            },
            onNavigateToProjects = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Projects.route)
            },
            onNavigateToAttendance = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.Attendance.route)
            },
            onNavigateToInvoicePdfPreview = {
                showGlobalMiloAssistant = false
                navController.navigate(Screen.InvoicePdfPreview.route)
            }
        )
    }
}
}
