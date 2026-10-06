package com.example.ui.screens

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.data.model.ChatMessageEntity
import com.example.data.model.EmployeeEntity
import com.example.data.model.PresenceStatus
import com.example.ui.components.AppHeader
import com.example.ui.components.StandardScreenHeader
import com.example.ui.components.StatusIndicatorBadge
import com.example.ui.components.BiometricSecurityGate
import com.example.util.BiometricHelper
import com.example.ui.theme.*
import com.example.util.AudioRecorderHelper
import com.example.util.MiloHaptics
import com.example.util.ReactionUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeamChatListScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenChannel: (String, String) -> Unit,
    onNavigateToProfile: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val employees by viewModel.employees.collectAsState(initial = emptyList())
    val unreadCounts by viewModel.channelUnreadCounts.collectAsState()
    val unsyncedChatCount by viewModel.unsyncedChatCount.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val context = LocalContext.current

    val isChatUnlocked by viewModel.isChatBiometricUnlocked.collectAsState()
    val isBiometricChatEnabled = remember { BiometricHelper.isBiometricForChatEnabled(context) }
    val effectivelyUnlocked = !isBiometricChatEnabled || isChatUnlocked

    val channels = listOf(
        Triple("company_chat", "Company Chat", "Arjun: Great work everyone!"),
        Triple("dev_team", "Development Team", "Looks good. Deploying today."),
        Triple("sales_team", "Sales Team", "Rohit: New lead received 🔥"),
        Triple("marketing", "Marketing", "Priya: Campaign updated"),
        Triple("project_alpha", "Project Alpha", "Suresh: Files shared")
    )

    BiometricSecurityGate(
        isUnlocked = effectivelyUnlocked,
        featureTitle = "Team Chat Hub",
        featureSubtitle = "Confidential Enterprise Communications",
        securityDescription = "Internal company messaging, lead conversations, and confidential team chat rooms are protected under biometric security. Please verify your fingerprint or face scan.",
        icon = Icons.Default.Chat,
        onUnlockSuccess = {
            viewModel.unlockChatBiometric()
        },
        onBack = onBack
    ) {
    Scaffold(
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = "Team Chat",
                subMenuSubtitle = "Secure Real-time Messaging",
                onBack = onBack,
                onNavigateToProfile = onNavigateToProfile,
                actions = {
                    IconButton(onClick = { viewModel.lockChatBiometric() }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock Chat with Biometrics",
                            tint = BrandDarkBlue
                        )
                    }
                }
            )
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search chats or team...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }

            // 💾 Offline Room Database Local Cache & Sync Status Banner
            if (!isDeviceOnline || unsyncedChatCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isDeviceOnline) Color(0xFFFFFBEB) else Color(0xFFF0FDF4)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isDeviceOnline) Color(0xFFFCD34D) else Color(0xFF86EFAC)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (!isDeviceOnline) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (!isDeviceOnline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                        contentDescription = "Room Local Cache",
                                        tint = if (!isDeviceOnline) Color(0xFFD97706) else Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (!isDeviceOnline) "Offline Mode · Room Cache Active" else "Room Cache: $unsyncedChatCount Messages Pending Sync",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (!isDeviceOnline) Color(0xFF92400E) else Color(0xFF166534)
                                    )
                                    Text(
                                        text = if (!isDeviceOnline) 
                                            "You can read and compose messages offline. Messages are cached in Room DB and sync when back online."
                                        else 
                                            "Messages safely stored in local Room DB. Ready to sync with Firestore.",
                                        fontSize = 10.5.sp,
                                        color = if (!isDeviceOnline) Color(0xFFB45309) else Color(0xFF15803D),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                            if (isDeviceOnline && unsyncedChatCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { viewModel.syncCachedRoomDataNow() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Tab Switcher between Channels & Direct Messages
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandBlue,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Channels",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Team Members",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = BrandBlue.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        "${employees.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                val filteredChannels = channels.filter {
                    it.second.contains(searchQuery, ignoreCase = true) || it.third.contains(searchQuery, ignoreCase = true)
                }
                items(filteredChannels) { (id, name, lastMsg) ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectChatChannel(id)
                                onOpenChannel(id, name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Forum, contentDescription = null, tint = BrandBlue)
                                        }
                                    }
                                    StatusIndicatorBadge(
                                        status = PresenceStatus.ONLINE,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(lastMsg, fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val channelUnread = unreadCounts[id.replace("-", "_")] ?: 0
                                if (channelUnread > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "$channelUnread",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("2m", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                    }
                }
            } else {
                val filteredEmployees = employees.filter {
                    it.name.contains(searchQuery, ignoreCase = true) || it.designation.contains(searchQuery, ignoreCase = true)
                }
                items(filteredEmployees) { employee ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val dmChannelId = viewModel.getDirectMessageChannelId(employee.name)
                                viewModel.selectChatChannel(dmChannelId)
                                onOpenChannel(dmChannelId, employee.name)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = BrandBlue.copy(alpha = 0.12f),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                employee.name.take(1),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = BrandBlue
                                            )
                                        }
                                    }
                                    StatusIndicatorBadge(
                                        status = employee.presenceStatus,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(employee.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        StatusIndicatorBadge(
                                            status = employee.presenceStatus,
                                            showLabel = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${employee.designation} · ${employee.department.name.lowercase().capitalize()}", fontSize = 12.sp, color = TextSecondary)
                                }
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = "Chat", tint = TextMuted)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun ChatRoomScreen(
    channelId: String,
    channelTitle: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val activeTypingUsers by viewModel.activeTypingUsers.collectAsState()
    val isDeviceOnline by viewModel.isDeviceOnline.collectAsState()
    val unsyncedChatCount by viewModel.unsyncedChatCount.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var inputText by remember { mutableStateOf("") }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val recordedFile = AudioRecorderHelper.startRecording(context)
            if (recordedFile != null) {
                isRecordingVoice = true
                recordingDurationSeconds = 0
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val requestVoiceRecording = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val recordedFile = AudioRecorderHelper.startRecording(context)
            if (recordedFile != null) {
                isRecordingVoice = true
                recordingDurationSeconds = 0
            }
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDurationSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDurationSeconds += 1
            }
        }
    }

    DisposableEffect(channelId) {
        viewModel.selectChatChannel(channelId)
        viewModel.markChannelAsRead(channelId)
        val chatRegistration = viewModel.attachChatSnapshotListener(channelId)
        val typingRegistration = viewModel.attachTypingStatusListener(channelId)
        onDispose {
            viewModel.setTypingStatus(channelId, false)
            chatRegistration?.remove()
            typingRegistration?.remove()
        }
    }

    LaunchedEffect(messages.size, activeTypingUsers.size) {
        val totalCount = messages.size + if (activeTypingUsers.isNotEmpty()) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount)
        }
        if (messages.isNotEmpty()) {
            viewModel.markChannelAsRead(channelId)
        }
    }

    val currentUserName by viewModel.currentEmployeeName.collectAsState()
    var selectedMessageForDetails by remember { mutableStateOf<ChatMessageEntity?>(null) }

    val isChatUnlocked by viewModel.isChatBiometricUnlocked.collectAsState()
    val isBiometricChatEnabled = remember { BiometricHelper.isBiometricForChatEnabled(context) }
    val effectivelyUnlocked = !isBiometricChatEnabled || isChatUnlocked

    BiometricSecurityGate(
        isUnlocked = effectivelyUnlocked,
        featureTitle = channelTitle,
        featureSubtitle = "Protected Discussion Channel",
        securityDescription = "Messages and attachments in '$channelTitle' are secured with biometric authentication.",
        icon = Icons.Default.Lock,
        onUnlockSuccess = {
            viewModel.unlockChatBiometric()
        },
        onBack = onBack
    ) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StandardScreenHeader(
                viewModel = viewModel,
                subMenuTitle = channelTitle,
                subMenuSubtitle = if (activeTypingUsers.isNotEmpty()) {
                    val names = activeTypingUsers.joinToString(", ")
                    "$names typing..."
                } else {
                    "Active Chat Channel · Live Realtime Synced"
                },
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.lockChatBiometric() }) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Lock Channel with Biometrics",
                            tint = BrandDarkBlue
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                if (isRecordingVoice) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rec")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(500, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_alpha"
                    )

                    // Voice Recording Active Bar with Live Timer & Animated Waves
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE11D48).copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val minutes = recordingDurationSeconds / 60
                            val seconds = recordingDurationSeconds % 60
                            val timeStr = "%02d:%02d".format(minutes, seconds)
                            Text(
                                "Recording... $timeStr",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFE11D48)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            // Animated wave bars while recording
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bars = listOf(6, 12, 18, 10, 16, 8, 14)
                                bars.forEachIndexed { i, h ->
                                    val waveH by infiniteTransition.animateValue(
                                        initialValue = (h * 0.4f).dp,
                                        targetValue = h.dp,
                                        typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(250 + i * 35, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "rec_bar_$i"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(waveH)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color(0xFFE11D48).copy(alpha = 0.85f))
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    MiloHaptics.performActionWarning(context, hapticFeedback)
                                    AudioRecorderHelper.cancelRecording()
                                    isRecordingVoice = false
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Cancel Recording", tint = TextMuted)
                            }

                            IconButton(
                                onClick = {
                                    val duration = recordingDurationSeconds.coerceAtLeast(1)
                                    val audioFile = AudioRecorderHelper.stopRecording()
                                    isRecordingVoice = false
                                    if (audioFile != null && audioFile.exists()) {
                                        MiloHaptics.performMessageSent(context, hapticFeedback)
                                        viewModel.sendVoiceChatMessage(audioFile.absolutePath, duration)
                                        Toast.makeText(context, "Voice note sent to channel", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Send Voice Note", tint = Color.White)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            MiloHaptics.performMessageSent(context, hapticFeedback)
                            // Quick send sample release APK file attachment
                            viewModel.sendChatMessage(
                                text = "Sharing the latest release build",
                                fileName = "app-release-v1.3.0.apk",
                                fileSize = "2.6 MB"
                            )
                        }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = TextSecondary)
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = {
                                inputText = it
                                if (it.isNotBlank()) {
                                    viewModel.onUserTyping(channelId)
                                } else {
                                    viewModel.setTypingStatus(channelId, false)
                                }
                            },
                            placeholder = { Text("Type a message...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = appTextFieldColors()
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Mic Button for Recording Voice Note (with runtime permission flow)
                        IconButton(
                            onClick = {
                                MiloHaptics.performButtonClick(context, hapticFeedback)
                                requestVoiceRecording()
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record Voice Note", tint = BrandBlue, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    MiloHaptics.performMessageSent(context, hapticFeedback)
                                    viewModel.sendChatMessage(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        },
        containerColor = SurfaceBg
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "flow_date_header") {
                ChatDateDivider(dateText = "Today • Conversation History")
            }

            if (!isDeviceOnline || unsyncedChatCount > 0) {
                item(key = "room_cache_offline_banner") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isDeviceOnline) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!isDeviceOnline) Color(0xFFFCD34D) else Color(0xFF86EFAC)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (!isDeviceOnline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (!isDeviceOnline) Color(0xFFD97706) else Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!isDeviceOnline)
                                        "Offline: Storing messages in local Room DB. Will auto-sync when online."
                                    else
                                        "Room Cache: $unsyncedChatCount message(s) ready to sync to Firestore.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (!isDeviceOnline) Color(0xFF92400E) else Color(0xFF166534)
                                )
                            }
                            if (isDeviceOnline && unsyncedChatCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { viewModel.syncCachedRoomDataNow() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                ChatBubble(
                    message = message,
                    currentUserName = currentUserName,
                    onMessageClick = { selectedMessageForDetails = it },
                    onToggleReaction = { emoji ->
                        MiloHaptics.performReactionTick(context, hapticFeedback)
                        viewModel.toggleChatReaction(message.id, emoji, channelId)
                    }
                )
            }

            if (activeTypingUsers.isNotEmpty()) {
                item(key = "typing_indicator_bubble") {
                    TypingIndicatorBubble(typingUsers = activeTypingUsers)
                }
            }
        }

        selectedMessageForDetails?.let { selectedMsg ->
            ChatMessageDetailsDialog(
                message = selectedMsg,
                onDismiss = { selectedMessageForDetails = null }
            )
        }
    }
    }
}

fun formatLegibleTime(raw: String): String {
    if (raw.isBlank()) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
    // If raw is a numeric timestamp
    raw.toLongOrNull()?.let { epochMillis ->
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }
    val trimmed = raw.trim()
    if (trimmed.contains(",")) {
        val parts = trimmed.split(",")
        if (parts.size > 1 && parts[1].isNotBlank()) {
            return parts[1].trim()
        }
    }
    return trimmed
}

@Composable
fun ChatDateDivider(dateText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE2E8F0),
            shadowElevation = 0.dp
        ) {
            Text(
                text = dateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(typingUsers: List<String>) {
    if (typingUsers.isEmpty()) return

    val typingText = remember(typingUsers) {
        when {
            typingUsers.size == 1 -> "${typingUsers.first()} is typing..."
            typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
            else -> "${typingUsers[0]} and ${typingUsers.size - 1} others are typing..."
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = dot1Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = dot2Alpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BrandBlue.copy(alpha = dot3Alpha))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = typingText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessageEntity,
    currentUserName: String = "",
    onMessageClick: (ChatMessageEntity) -> Unit = {},
    onToggleReaction: (String) -> Unit = {}
) {
    val isMe = if (currentUserName.isNotBlank()) {
        message.senderName.trim().equals(currentUserName.trim(), ignoreCase = true)
    } else {
        message.isMe
    }
    val formattedTime = remember(message.timestampText) { formatLegibleTime(message.timestampText) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.78f).dp
    var showEmojiPicker by remember { mutableStateOf(false) }
    val reactionsMap = remember(message.reactionsJson) {
        ReactionUtils.parseReactions(message.reactionsJson)
    }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            val senderStatus = when {
                message.senderName.contains("Priya", ignoreCase = true) -> PresenceStatus.IN_MEETING
                message.senderName.contains("Suresh", ignoreCase = true) -> PresenceStatus.IN_MEETING
                message.senderName.contains("Neha", ignoreCase = true) -> PresenceStatus.OFFLINE
                else -> PresenceStatus.ONLINE
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            ) {
                StatusIndicatorBadge(
                    status = senderStatus,
                    dotSize = 7.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
                val rolePart = if (message.senderRole.isNotBlank()) " (${message.senderRole})" else ""
                Text(
                    "${message.senderName}$rolePart",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    message.senderName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Quick Emoji Picker Bar (appears above/below bubble when requested)
        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReactionUtils.POPULAR_CHAT_EMOJIS.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onToggleReaction(emoji)
                                    showEmojiPicker = false
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) BrandBlue else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = maxBubbleWidth)
                .clickable { onMessageClick(message) }
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!message.messageText.isNullOrBlank()) {
                    Text(
                        message.messageText,
                        fontSize = 14.5.sp,
                        color = if (isMe) Color.White else TextPrimary,
                        lineHeight = 20.sp
                    )
                }

                // APK / Document Attachment Preview Card
                if (message.attachmentFileName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isMe) BrandDarkBlue else SurfaceBg,
                        modifier = Modifier.widthIn(min = 200.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandLightBlue.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Android, contentDescription = null, tint = BrandAccent)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    message.attachmentFileName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else TextPrimary
                                )
                                Text(
                                    message.attachmentFileSize ?: "",
                                    fontSize = 10.sp,
                                    color = if (isMe) Color.White.copy(alpha = 0.7f) else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Voice Note Audio Player Bubble (Firebase Storage & Local Cached)
                if (message.isVoiceMessage) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMe) BrandDarkBlue else Color(0xFFF1F5F9),
                        modifier = Modifier.widthIn(min = 220.dp)
                    ) {
                        var isPlaying by remember { mutableStateOf(false) }

                        LaunchedEffect(Unit) {
                            while (true) {
                                delay(250)
                                val active = AudioRecorderHelper.isAudioPlaying(message.audioPath)
                                if (isPlaying != active) {
                                    isPlaying = active
                                }
                            }
                        }

                        val playWaveAnim = rememberInfiniteTransition(label = "bubble_play_wave")

                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    MiloHaptics.performButtonClick(context, hapticFeedback)
                                    if (isPlaying) {
                                        AudioRecorderHelper.stopPlaying()
                                        isPlaying = false
                                    } else {
                                        val path = message.audioPath
                                        if (!path.isNullOrBlank()) {
                                            AudioRecorderHelper.playAudio(
                                                filePathOrUrl = path,
                                                onPrepared = { isPlaying = true },
                                                onCompletion = { isPlaying = false }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Play voice note",
                                    tint = if (isMe) Color.White else BrandBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (message.audioPath?.startsWith("http") == true) {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = "Stored in Firebase Storage",
                                            tint = if (isMe) Color(0xFF38BDF8) else BrandBlue,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        "Voice Note • ${message.audioDurationSeconds}s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color.White else TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                // Audio wave bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val wavePattern = listOf(6, 12, 18, 10, 15, 8, 14, 19, 11, 7)
                                    wavePattern.forEachIndexed { i, baseH ->
                                        val barHeight = if (isPlaying) {
                                            val animatedH by playWaveAnim.animateValue(
                                                initialValue = (baseH * 0.4f).dp,
                                                targetValue = (baseH * 1.1f).dp,
                                                typeConverter = androidx.compose.ui.unit.Dp.VectorConverter,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(200 + i * 40, easing = FastOutSlowInEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "bwave_$i"
                                            )
                                            animatedH
                                        } else {
                                            baseH.dp
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(2.5.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(1.dp))
                                                .background(
                                                    if (isMe) Color.White.copy(alpha = if (isPlaying) 0.95f else 0.5f)
                                                    else BrandBlue.copy(alpha = if (isPlaying) 0.9f else 0.4f)
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isPlaying) "Playing..." else "Tap to play",
                                        fontSize = 10.sp,
                                        color = if (isMe) Color.White.copy(alpha = 0.75f) else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Legible Timestamp Row (Clean & High Contrast for both sender and receiver)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMe) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        if (!message.isSynced) {
                            // Offline Room Cache status
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Cached in Room (Offline)",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Cached",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        } else if (message.isRead) {
                            // Read Status: Cyan double-check with Read badge
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read by recipient",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Read",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        } else {
                            // Sent to Firestore / Delivered
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Sent",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    } else if (message.isRead) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Emoji Reactions Pill Bar & Quick Reaction Trigger
        Row(
            modifier = Modifier
                .padding(
                    top = 4.dp,
                    start = if (isMe) 0.dp else 4.dp,
                    end = if (isMe) 4.dp else 0.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            reactionsMap.forEach { (emoji, users) ->
                val hasUserReacted = users.any { it.equals(currentUserName.trim(), ignoreCase = true) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasUserReacted) BrandBlue.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasUserReacted) BrandBlue.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.clickable {
                        onToggleReaction(emoji)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = emoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${users.size}",
                            fontSize = 11.sp,
                            fontWeight = if (hasUserReacted) FontWeight.Bold else FontWeight.Medium,
                            color = if (hasUserReacted) BrandBlue else Color(0xFF475569)
                        )
                    }
                }
            }

            // Quick emoji reaction trigger button
            Surface(
                shape = CircleShape,
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showEmojiPicker = !showEmojiPicker }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "😊+",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        // Dedicated Viewed-by-Recipient badge below sender's message bubble
        if (isMe && message.isRead && message.readBy.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clickable { onMessageClick(message) }
            ) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Viewed by recipient",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Read by ${message.readBy}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0369A1)
                )
            }
        }
    }
}

@Composable
fun ChatMessageDetailsDialog(
    message: ChatMessageEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = BrandBlue)
            }
        },
        icon = {
            Surface(
                shape = CircleShape,
                color = if (message.isRead) Color(0xFFE0F2FE) else Color(0xFFF1F5F9),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (message.isRead) Icons.Default.DoneAll else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (message.isRead) Color(0xFF0284C7) else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                "Message Read Status",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${message.messageText}\"",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delivery Status:", fontSize = 12.sp, color = TextSecondary)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (message.isRead) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = if (message.isRead) "✓✓ Read by Recipient" else "✓ Delivered (Sent)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isRead) Color(0xFF15803D) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sent Time:", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        formatLegibleTime(message.timestampText),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                if (message.isRead && message.readBy.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Viewed By:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            message.readBy,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    }
                }

                if (message.readAt != null && message.readAt > 0L) {
                    val readTimeStr = remember(message.readAt) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        sdf.format(Date(message.readAt))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Viewed At:", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            readTimeStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    )
}
