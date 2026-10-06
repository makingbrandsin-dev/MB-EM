package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.data.firebase.NetworkSyncStatus
import com.example.data.firebase.SyncState
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*

@Composable
fun SubMenuHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    badgeText: String? = null,
    badgeColor: Color = ElectricBlueBg,
    badgeTextColor: Color = BrandBlue,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (badgeText != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeTextColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actions()
                }
            }
            HorizontalDivider(thickness = 0.8.dp, color = BorderLight.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun StandardScreenHeader(
    viewModel: MainViewModel,
    subMenuTitle: String? = null,
    subMenuSubtitle: String? = null,
    onBack: (() -> Unit)? = null,
    badgeText: String? = null,
    badgeColor: Color = ElectricBlueBg,
    badgeTextColor: Color = BrandBlue,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val unreadChat by viewModel.unreadChatCount.collectAsState()
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Unified Primary Header (The same header from Home screen for all screens)
        AppHeader(
            title = "MB Traker",
            showBrandLogo = true,
            showSyncIndicator = true,
            onOpenChat = onNavigateToChat,
            unreadChatCount = unreadChat,
            onNavigateToNotifications = onNavigateToNotifications,
            unreadNotificationCount = unreadNotifications,
            onNavigateToProfile = onNavigateToProfile,
            showBottomDivider = subMenuTitle == null
        )

        // Sub Menu Header below main header
        if (subMenuTitle != null) {
            SubMenuHeader(
                title = subMenuTitle,
                subtitle = subMenuSubtitle,
                onBack = onBack,
                badgeText = badgeText,
                badgeColor = badgeColor,
                badgeTextColor = badgeTextColor,
                actions = actions
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    title: String,
    subtitle: String? = null,
    showBrandLogo: Boolean = false,
    showSyncIndicator: Boolean = true,
    onBack: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null,
    unreadChatCount: Int = 0,
    onNavigateToNotifications: (() -> Unit)? = null,
    unreadNotificationCount: Int = 0,
    onOpenMilo: (() -> Unit)? = null,
    showBottomDivider: Boolean = true,
    isAiProcessing: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BorderLight),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        if (showBrandLogo) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A), // bg-slate-900 container
                                    border = BorderStroke(1.dp, Color(0xFF334155)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp) // clean padding
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.milo_final),
                                            contentDescription = "Milo Brand Logo Emblem",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit, // object-contain
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = BrandDarkBlue
                                    )
                                    if (subtitle != null) {
                                        Text(
                                            text = subtitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    text = title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }
                        }
                    },
                    actions = {
                        actions()

                        // 🔄 Realtime Cloud Sync Status Indicator
                        if (showSyncIndicator) {
                            SyncStatusIndicator(modifier = Modifier.padding(end = 4.dp))
                        }

                        // 💬 Team Chat / Incoming Chat Alert Icon (Full Chat Icon)
                        if (onOpenChat != null) {
                            IconButton(
                                onClick = onOpenChat,
                                modifier = Modifier.size(42.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadChatCount > 0) {
                                            Badge(
                                                containerColor = Color(0xFF4A7C59),
                                                contentColor = Color.White
                                            ) {
                                                Text(if (unreadChatCount > 99) "99+" else "$unreadChatCount", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Team Chat",
                                        tint = Color(0xFF4A7C59),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 🔔 Notifications Icon (Full Notification Icon)
                        if (onNavigateToNotifications != null) {
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier.size(42.dp)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (unreadNotificationCount > 0) {
                                            Badge(
                                                containerColor = StatusRed,
                                                contentColor = Color.White
                                            ) {
                                                Text(if (unreadNotificationCount > 99) "99+" else "$unreadNotificationCount", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color(0xFF705C30),
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }
                        }

                        // 👤 Profile Avatar Action
                        if (onNavigateToProfile != null) {
                            IconButton(onClick = onNavigateToProfile) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_nav_profile),
                                            contentDescription = "Profile",
                                            tint = BrandBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
                // Visual Linear Progress Bar when AI Assistant is processing a query
                if (isAiProcessing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = BrandAccent,
                        trackColor = BrandBlue.copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

private data class SyncBadgeConfig(
    val bgColor: Color,
    val borderColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val label: String
)

@Composable
fun SyncStatusIndicator(
    modifier: Modifier = Modifier,
    syncState: SyncState = FirebaseRealtimeManager.syncState.collectAsState().value,
    onTriggerSync: () -> Unit = { FirebaseRealtimeManager.syncNow() },
    onToggleOffline: (Boolean) -> Unit = { FirebaseRealtimeManager.toggleSimulatedOffline(it) }
) {
    var showDetailsDialog by remember { mutableStateOf(false) }

    // Infinite rotation for spinning sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val config = when (syncState.status) {
        NetworkSyncStatus.SYNCING -> {
            val total = syncState.pendingSummary.totalPending
            val pendingText = if (total > 0) " ($total)" else ""
            SyncBadgeConfig(
                bgColor = Color(0xFFC8E8D0),
                borderColor = Color(0xFF4A7C59).copy(alpha = 0.4f),
                contentColor = Color(0xFF4A7C59),
                icon = Icons.Default.Sync,
                label = "Syncing$pendingText"
            )
        }
        NetworkSyncStatus.OFFLINE -> {
            val pendingCount = syncState.pendingSummary.totalPending
            val label = if (pendingCount > 0) "$pendingCount Pending" else "Offline"
            SyncBadgeConfig(
                bgColor = Color(0xFFFEF3C7),
                borderColor = Color(0xFFFDE68A),
                contentColor = Color(0xFFB45309),
                icon = Icons.Default.CloudOff,
                label = label
            )
        }
        NetworkSyncStatus.ERROR -> {
            SyncBadgeConfig(
                bgColor = Color(0xFFFEF2F2),
                borderColor = Color(0xFFFECACA),
                contentColor = Color(0xFFDC2626),
                icon = Icons.Default.SyncProblem,
                label = "Sync Retry"
            )
        }
        NetworkSyncStatus.SYNCED -> {
            SyncBadgeConfig(
                bgColor = Color(0xFFF0FDF4),
                borderColor = Color(0xFFBBF7D0),
                contentColor = Color(0xFF15803D),
                icon = Icons.Default.CloudDone,
                label = "Live"
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = config.bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, config.borderColor),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { showDetailsDialog = true }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = "Sync Status: ${config.label}",
                tint = config.contentColor,
                modifier = Modifier
                    .size(13.dp)
                    .then(
                        if (syncState.status == NetworkSyncStatus.SYNCING) Modifier.rotate(rotation)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (syncState.status == NetworkSyncStatus.SYNCED) {
                PulsingStatusDot(
                    color = config.contentColor,
                    dotSize = 6.dp,
                    haloExpansion = 4.dp
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = config.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = config.contentColor
            )
        }
    }

    if (showDetailsDialog) {
        SyncDetailsDialog(
            syncState = syncState,
            onDismiss = { showDetailsDialog = false },
            onSyncNow = onTriggerSync,
            onToggleOffline = onToggleOffline
        )
    }
}

@Composable
fun SyncDetailsDialog(
    syncState: SyncState,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onToggleOffline: (Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (syncState.isOnline) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (syncState.isOnline) Icons.Default.CloudSync else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (syncState.isOnline) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Synchronization",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandDarkBlue
                        )
                        Text(
                            text = "Firestore Realtime & Offline Queue",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Network & Cloud Status Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (syncState.isOnline) Color(0xFF16A34A) else Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (syncState.isOnline) "Connected to Firestore" else "Working Offline (Local Cache)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = syncState.statusMessage,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Last Synced: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Text(
                                text = syncState.formattedLastSyncTime,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pending Queue Section
                Text(
                    text = "PENDING UPDATES QUEUE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Leads queue item
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CRM Leads Updates", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                            val leadsCount = syncState.pendingSummary.pendingLeads
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (leadsCount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (leadsCount > 0) "$leadsCount Pending" else "Up to date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (leadsCount > 0) Color(0xFFB45309) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.6.dp, color = BorderLight)

                        // Tasks queue item
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = Color(0xFF9333EA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daily Tasks & Sprints", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                            val tasksCount = syncState.pendingSummary.pendingTasks
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (tasksCount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (tasksCount > 0) "$tasksCount Pending" else "Up to date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tasksCount > 0) Color(0xFFB45309) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.6.dp, color = BorderLight)

                        // Attendance queue item
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attendance & Punches", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                            val attCount = syncState.pendingSummary.pendingAttendance
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (attCount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = if (attCount > 0) "$attCount Pending" else "Up to date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attCount > 0) Color(0xFFB45309) else Color(0xFF64748B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Offline Mode Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulate Offline Mode",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Queue changes locally to test offline sync",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = syncState.isSimulatedOffline,
                            onCheckedChange = { onToggleOffline(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFD97706)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Close", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onSyncNow()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        enabled = !syncState.isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBadge(
    label: String,
    value: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val (bg, fg) = when (priority.lowercase()) {
        "high" -> StatusRedBg to StatusRed
        "medium" -> StatusOrangeBg to StatusOrange
        else -> StatusGreenBg to StatusGreen
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = priority,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun StatusTag(text: String, isGreen: Boolean = true) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isGreen) StatusGreenBg else StatusRedBg
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isGreen) StatusGreen else StatusRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isGreen) StatusGreen else StatusRed,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

fun formatLiveSeconds(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02dh %02dm %02ds", hrs, mins, secs)
}

@Composable
fun CrmTasksAttendanceSwitcher(
    selectedTab: String,
    onNavigateToCrm: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                Triple("crm", "CRM", Icons.Default.Groups),
                Triple("tasks", "Tasks", Icons.Default.Assignment),
                Triple("attendance", "Attendance", Icons.Default.AccessTime)
            )

            tabs.forEach { (id, title, icon) ->
                val isSelected = selectedTab == id
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!isSelected) {
                                when (id) {
                                    "crm" -> onNavigateToCrm()
                                    "tasks" -> onNavigateToTasks()
                                    "attendance" -> onNavigateToAttendance()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(15.dp),
                            tint = if (isSelected) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 💫 Reusable animated pulsing status dot
 * Uses Compose rememberInfiniteTransition to produce a subtle pulsing halo wave
 * to draw attention to real-time sync and active presence states.
 */
@Composable
fun PulsingStatusDot(
    color: Color = Color(0xFF22C55E),
    dotSize: androidx.compose.ui.unit.Dp = 8.dp,
    haloExpansion: androidx.compose.ui.unit.Dp = 5.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(dotSize + haloExpansion),
        contentAlignment = Alignment.Center
    ) {
        // Outer subtle pulsing wave
        Box(
            modifier = Modifier
                .size(dotSize)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .clip(CircleShape)
                .background(color)
        )

        // Core solid indicator dot
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatusIndicatorBadge(
    status: com.example.data.model.PresenceStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false,
    dotSize: androidx.compose.ui.unit.Dp = 8.dp
) {
    val isOnline = status == com.example.data.model.PresenceStatus.ONLINE

    val (dotColor, labelText, bgContainer) = when (status) {
        com.example.data.model.PresenceStatus.ONLINE -> Triple(Color(0xFF22C55E), "Active", Color(0xFFDCFCE7))
        com.example.data.model.PresenceStatus.IN_MEETING -> Triple(Color(0xFFF59E0B), "In Meeting", Color(0xFFFEF3C7))
        com.example.data.model.PresenceStatus.OFFLINE -> Triple(Color(0xFF94A3B8), "Offline", Color(0xFFF1F5F9))
    }

    if (showLabel) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgContainer,
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOnline) {
                    PulsingStatusDot(
                        color = dotColor,
                        dotSize = dotSize,
                        haloExpansion = 4.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = labelText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = dotColor
                )
            }
        }
    } else {
        if (isOnline) {
            PulsingStatusDot(
                color = dotColor,
                dotSize = dotSize,
                haloExpansion = 4.dp,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

