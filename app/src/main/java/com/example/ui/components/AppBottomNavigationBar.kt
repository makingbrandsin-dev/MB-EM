package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.*
import com.example.R
import com.example.Screen
import com.example.domain.milo.MiloState
import com.example.milo.MiloAiAssistantSheet
import com.example.milo.MiloCharacter
import com.example.milo.MiloRealStatusView
import com.example.milo.MiloViewModel
import com.example.util.MiloHaptics

/**
 * Navigation item specification for the persistent bottom bar.
 */
data class BottomNavItem(
    val route: String,
    val title: String,
    val drawableRes: Int,
    val activeColor: Color,
    val activeBgColor: Color,
    val testTag: String,
    val badgeCount: Int = 0,
    val isRouteMatching: (String?) -> Boolean = { it == route }
)

/**
 * Modern Elevated Dock Bottom Navigation Bar featuring:
 * - Completely transparent outer box background (clean floating dock)
 * - Center elevated cradle button: "ASK MILO" with instant reliable tap action & spring lift
 * - Interactive spring scale bounce & tilt animations on click
 * - Real-time badge indicators for pending CRM, Tasks, Attendance & Team Chat
 */
@Composable
fun AppBottomNavigationBar(
    currentRoute: String?,
    onNavigateToRoute: (String) -> Unit,
    onAskMiloClick: (() -> Unit)? = null,
    crmBadgeCount: Int = 0,
    taskBadgeCount: Int = 0,
    attendanceBadgeCount: Int = 0,
    chatBadgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var localShowMiloSheet by remember { mutableStateOf(false) }
    val miloViewModel: MiloViewModel = viewModel()
    val currentMiloState by miloViewModel.state.collectAsState()

    // Left navigation items (CRM, Task) with live pending issue badges
    val leftItems = listOf(
        BottomNavItem(
            route = Screen.Crm.route,
            title = "CRM",
            drawableRes = R.drawable.ic_nav_crm,
            activeColor = ButtonPrimary,
            activeBgColor = ImportantCardBg.copy(alpha = 0.6f),
            testTag = "bottom_nav_crm",
            badgeCount = crmBadgeCount,
            isRouteMatching = { it == Screen.Crm.route || it == Screen.Leads.route }
        ),
        BottomNavItem(
            route = Screen.Tasks.route,
            title = "Task",
            drawableRes = R.drawable.ic_nav_tasks,
            activeColor = ButtonPrimary,
            activeBgColor = ImportantCardBg.copy(alpha = 0.6f),
            testTag = "bottom_nav_tasks",
            badgeCount = taskBadgeCount,
            isRouteMatching = { it == Screen.Tasks.route }
        )
    )

    // Right navigation items (Attendance, Team Chat) with live pending action badges
    val rightItems = listOf(
        BottomNavItem(
            route = Screen.Attendance.route,
            title = "Attendance",
            drawableRes = R.drawable.ic_nav_attendance,
            activeColor = ButtonSecondary,
            activeBgColor = AccentSage.copy(alpha = 0.45f),
            testTag = "bottom_nav_attendance",
            badgeCount = attendanceBadgeCount,
            isRouteMatching = { it == Screen.Attendance.route }
        ),
        BottomNavItem(
            route = Screen.Chat.route,
            title = "Team Chat",
            drawableRes = R.drawable.ic_nav_chat,
            activeColor = ButtonSecondary,
            activeBgColor = AccentSage.copy(alpha = 0.45f),
            testTag = "bottom_nav_chat",
            badgeCount = chatBadgeCount,
            isRouteMatching = { it == Screen.Chat.route || it?.startsWith("chat_room/") == true }
        )
    )

    val isAskMiloActive = currentRoute == Screen.AskMilo.route
    val centerInteractionSource = remember { MutableInteractionSource() }
    val askButtonVideoUri = remember { com.example.milo.MiloVideoHelper.getAskMiloButtonVideoUri(context) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 10.dp)
            .height(84.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Floating Navbar Dock Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0xFF1E293B).copy(alpha = 0.22f)
                ),
            color = Color.White,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            // Navbar Items Row Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left 2 items: CRM & Tasks
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftItems.forEach { item ->
                        AnimatedNavItem(
                            item = item,
                            currentRoute = currentRoute,
                            onNavigate = onNavigateToRoute
                        )
                    }
                }

                // Spacer gap for center floating "ASK MILO" button
                Spacer(modifier = Modifier.width(76.dp))

                // Right 2 items: Attendance & Team Chat
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightItems.forEach { item ->
                        AnimatedNavItem(
                            item = item,
                            currentRoute = currentRoute,
                            onNavigate = onNavigateToRoute
                        )
                    }
                }
            }
        }

        val openAskMilo = {
            MiloHaptics.performButtonTap(context)
            if (onAskMiloClick != null) {
                onAskMiloClick()
            } else {
                onNavigateToRoute(Screen.AskMilo.route)
            }
        }

        val miloButtonInteractionSource = remember { MutableInteractionSource() }
        val textButtonInteractionSource = remember { MutableInteractionSource() }

        // 🦁 Center Floating "ASK MILO" Elevated Cradle Dock
        // Dual-action: Clicking on Milo Mascot OR on "ASK MILO" text button both open "ASK MILO"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-6).dp)
                .width(84.dp)
                .height(90.dp)
                .testTag("bottom_nav_ask_milo")
                .clickable(
                    interactionSource = centerInteractionSource,
                    indication = null
                ) {
                    openAskMilo()
                }
                .liftOnPress(elevationLift = 0.dp, translateY = (-5).dp, scaleLift = 1.05f, interactionSource = centerInteractionSource),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                // 1. Milo Mascot Icon (20% bigger: 60.dp, NO circle shape, NO border, transparent background, live status visible)
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .testTag("nav_milo_mascot_button")
                        .clickable(
                            interactionSource = miloButtonInteractionSource,
                            indication = null
                        ) { openAskMilo() },
                    contentAlignment = Alignment.Center
                ) {
                    MiloRealStatusView(
                        state = if (isAskMiloActive) MiloState.THINKING else currentMiloState,
                        size = 60.dp,
                        showStateBadge = false,
                        customVideoUri = askButtonVideoUri,
                        onClick = null
                    )
                }

                // 2. "ASK MILO" Text Pill Button (Click to Open Ask Milo)
                Surface(
                    onClick = { openAskMilo() },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAskMiloActive) ButtonPrimary else ButtonSecondary,
                    shadowElevation = 2.dp,
                    interactionSource = textButtonInteractionSource,
                    modifier = Modifier
                        .testTag("nav_ask_milo_text_button")
                ) {
                    Text(
                        text = "ASK MILO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    // Fallback Local Milo Assistant Bottom Sheet if requested
    if (localShowMiloSheet) {
        MiloAiAssistantSheet(
            miloViewModel = miloViewModel,
            onDismiss = { localShowMiloSheet = false },
            onNavigateToLeads = {
                localShowMiloSheet = false
                onNavigateToRoute(Screen.Crm.route)
            },
            onNavigateToTasks = {
                localShowMiloSheet = false
                onNavigateToRoute(Screen.Tasks.route)
            }
        )
    }
}

/**
 * Individual Navigation Item with 10X interactive spring scale bounce, tilt, and lift-up hover animations.
 */
@Composable
private fun AnimatedNavItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val isSelected = item.isRouteMatching(currentRoute)
    val itemInteractionSource = remember { MutableInteractionSource() }

    // Spring scale bounce animation
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ItemScale"
    )

    // Slight rotation tilt animation on selection
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) -4f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ItemRotation"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) item.activeBgColor else Color.Transparent,
        animationSpec = tween(200),
        label = "BgColor"
    )

    Box(
        modifier = Modifier
            .testTag(item.testTag)
            .liftOnPress(elevationLift = 6.dp, translateY = (-4).dp, scaleLift = 1.04f, interactionSource = itemInteractionSource)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
            }
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = itemInteractionSource,
                indication = ripple(bounded = true, color = item.activeColor.copy(alpha = 0.25f))
            ) {
                MiloHaptics.performButtonClick(context)
                val cat = when (item.route) {
                    Screen.Crm.route -> "crm"
                    Screen.Tasks.route -> "task"
                    Screen.Attendance.route -> "attendance"
                    Screen.Chat.route -> "chat"
                    else -> null
                }
                if (cat != null) {
                    com.example.util.NotificationHelper.clearCategoryNotifications(context, cat)
                    if (cat == "crm") {
                        com.example.util.NotificationHelper.clearCategoryNotifications(context, "lead")
                        com.example.util.NotificationHelper.clearCategoryNotifications(context, "followup")
                    }
                }
                if (!isSelected) {
                    onNavigate(item.route)
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (item.badgeCount > 0) {
                        Badge(
                            containerColor = when (item.title) {
                                "CRM" -> Color(0xFFDC2626) // Vivid Red for pending CRM issues/leads
                                "Task", "Tasks" -> Color(0xFFD97706) // Amber for pending tasks
                                "Attendance" -> Color(0xFFDC2626) // Red for pending attendance action
                                "Team Chat" -> Color(0xFF059669) // Emerald green for unread chats
                                else -> ButtonPrimary
                            },
                            contentColor = Color.White
                        ) {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else "${item.badgeCount}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            ) {
                Image(
                    painter = painterResource(id = item.drawableRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (isSelected) item.activeColor else Color(0xFF334155)
            )
        }
    }
}

/**
 * Overloaded convenience composable taking [NavHostController] directly with live badge counts.
 */
@Composable
fun AppBottomNavigationBar(
    navController: NavHostController,
    onAskMiloClick: (() -> Unit)? = null,
    crmBadgeCount: Int = 0,
    taskBadgeCount: Int = 0,
    attendanceBadgeCount: Int = 0,
    chatBadgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AppBottomNavigationBar(
        currentRoute = currentRoute,
        onNavigateToRoute = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onAskMiloClick = onAskMiloClick,
        crmBadgeCount = crmBadgeCount,
        taskBadgeCount = taskBadgeCount,
        attendanceBadgeCount = attendanceBadgeCount,
        chatBadgeCount = chatBadgeCount,
        modifier = modifier
    )
}
