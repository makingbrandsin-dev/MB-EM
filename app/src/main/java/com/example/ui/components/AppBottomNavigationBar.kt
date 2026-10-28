package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.*
import com.example.R
import com.example.Screen
import com.example.domain.milo.MiloState
import com.example.milo.MiloRealStatusView
import com.example.milo.MiloViewModel
import com.example.util.MiloHaptics
import com.example.util.NotificationHelper

/**
 * Immutable navigation item specification for the persistent bottom bar.
 */
@Immutable
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
 * Production-grade, ultra-responsive persistent Bottom Navigation Bar.
 * - Zero UI thread lag, completely eliminated unnecessary recompositions.
 * - Free of grey box or layout shift artifacts on tap.
 * - Enriched, prominent ASK MILO elevated mascot (76dp) with immediate response.
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
    modifier: Modifier = Modifier,
    miloViewModel: MiloViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentMiloState by miloViewModel.state.collectAsState()

    // Cache static lists keyed on badge counts to eliminate layout thrashing
    val leftItems = remember(crmBadgeCount, taskBadgeCount) {
        listOf(
            BottomNavItem(
                route = Screen.Crm.route,
                title = "CRM",
                drawableRes = R.drawable.ic_nav_crm,
                activeColor = ButtonPrimary,
                activeBgColor = ButtonPrimary.copy(alpha = 0.12f),
                testTag = "bottom_nav_crm",
                badgeCount = crmBadgeCount,
                isRouteMatching = { it == Screen.Crm.route || it?.startsWith("lead_detail/") == true || it == Screen.FollowUps.route }
            ),
            BottomNavItem(
                route = Screen.Tasks.route,
                title = "Tasks",
                drawableRes = R.drawable.ic_nav_tasks,
                activeColor = Color(0xFFD97706),
                activeBgColor = Color(0xFFFFFBEB),
                testTag = "bottom_nav_tasks",
                badgeCount = taskBadgeCount,
                isRouteMatching = { it == Screen.Tasks.route }
            )
        )
    }

    val rightItems = remember(attendanceBadgeCount, chatBadgeCount) {
        listOf(
            BottomNavItem(
                route = Screen.Attendance.route,
                title = "Attendance",
                drawableRes = R.drawable.ic_nav_attendance,
                activeColor = Color(0xFF0D9488),
                activeBgColor = Color(0xFFF0FDFA),
                testTag = "bottom_nav_attendance",
                badgeCount = attendanceBadgeCount,
                isRouteMatching = { it == Screen.Attendance.route }
            ),
            BottomNavItem(
                route = Screen.Chat.route,
                title = "Team Chat",
                drawableRes = R.drawable.ic_nav_chat,
                activeColor = Color(0xFF059669),
                activeBgColor = Color(0xFFECFDF5),
                testTag = "bottom_nav_chat",
                badgeCount = chatBadgeCount,
                isRouteMatching = { it == Screen.Chat.route || it?.startsWith("chat_room/") == true }
            )
        )
    }

    val isAskMiloActive = currentRoute == Screen.AskMilo.route
    val askButtonVideoUri = remember { com.example.milo.MiloVideoHelper.getAskMiloButtonVideoUri(context) }

    // Instant click handler for center Milo button
    val centerInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 8.dp)
            .height(88.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Floating Navbar Dock Surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color(0xFF1E293B).copy(alpha = 0.18f)
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
                        key(item.route) {
                            AnimatedNavItem(
                                item = item,
                                currentRoute = currentRoute,
                                onNavigate = onNavigateToRoute
                            )
                        }
                    }
                }

                // Spacer gap for center prominent "ASK MILO" button
                Spacer(modifier = Modifier.width(88.dp))

                // Right 2 items: Attendance & Team Chat
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightItems.forEach { item ->
                        key(item.route) {
                            AnimatedNavItem(
                                item = item,
                                currentRoute = currentRoute,
                                onNavigate = onNavigateToRoute
                            )
                        }
                    }
                }
            }
        }

        // 🦁 Center Floating Prominent "ASK MILO" Elevated Dock
        // 76dp Mascot button, perfectly centered, immediate tap response with no box artifacts
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .width(100.dp)
                .height(102.dp)
                .testTag("bottom_nav_ask_milo")
                .clickable(
                    interactionSource = centerInteractionSource,
                    indication = null
                ) {
                    MiloHaptics.performButtonTap(context)
                    if (onAskMiloClick != null) {
                        onAskMiloClick()
                    } else {
                        onNavigateToRoute(Screen.AskMilo.route)
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Prominent Milo Mascot (76dp)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .testTag("nav_milo_mascot_button"),
                    contentAlignment = Alignment.Center
                ) {
                    MiloRealStatusView(
                        state = if (isAskMiloActive) MiloState.THINKING else currentMiloState,
                        size = 76.dp,
                        showStateBadge = false,
                        customVideoUri = askButtonVideoUri,
                        onClick = null
                    )
                }

                // 2. "ASK MILO" Pill Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAskMiloActive) ButtonPrimary else Color(0xFF1E293B),
                    shadowElevation = 2.dp,
                    modifier = Modifier.testTag("nav_ask_milo_text_button")
                ) {
                    Text(
                        text = "ASK MILO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Individual Navigation Item.
 * Lightweight, zero-lag, no box effects or grey squares on tap.
 */
@Composable
private fun AnimatedNavItem(
    item: BottomNavItem,
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val isSelected = remember(item, currentRoute) { item.isRouteMatching(currentRoute) }
    val itemInteractionSource = remember { MutableInteractionSource() }

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) item.activeBgColor else Color.Transparent,
        animationSpec = tween(90),
        label = "BgColor"
    )

    Box(
        modifier = Modifier
            .testTag(item.testTag)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = itemInteractionSource,
                indication = null
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
                    // When any bottom nav item with a notification badge is clicked, clear ALL notifications
                    NotificationHelper.clearCategoryNotifications(context, "all")
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
                                "CRM" -> Color(0xFFDC2626)
                                "Task", "Tasks" -> Color(0xFFD97706)
                                "Attendance" -> Color(0xFFDC2626)
                                "Team Chat" -> Color(0xFF059669)
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) item.activeColor else Color(0xFF475569)
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
                popUpTo(navController.graph.findStartDestination().id) {
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
