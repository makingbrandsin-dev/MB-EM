package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.screens.MainViewModel
import com.example.ui.screens.formatLegibleTime
import com.example.util.AudioRecorderHelper
import com.example.util.MiloHaptics
import kotlinx.coroutines.launch

// WhatsApp Theme Colors
val WhatsAppTeal = Color(0xFF075E54)
val WhatsAppTealDark = Color(0xFF128C7E)
val WhatsAppLightGreen = Color(0xFF25D366)
val WhatsAppChatBg = Color(0xFFEFEAE2)
val WhatsAppBubbleSent = Color(0xFFD9FDD3)
val WhatsAppBubbleReceived = Color(0xFFFFFFFF)
val WhatsAppCheckBlue = Color(0xFF34B7F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppQuickChatDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.chatMessages.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var inputText by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf("company_chat") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val channels = listOf(
        Pair("company_chat", "Making Brands HQ"),
        Pair("sales_team", "Sales & Leads"),
        Pair("dev_team", "Dev & Sprint"),
        Pair("dm_1", "Arjun Mehta (Lead)"),
        Pair("dm_2", "Priya Singh (Design)")
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = if (isTablet) Alignment.Center else Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(if (isTablet) 0.88f else 1f)
                    .fillMaxHeight(if (isTablet) 0.92f else 0.97f),
                shape = if (isTablet) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = WhatsAppChatBg
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                // 🟢 WhatsApp Executive Header
                Surface(
                    color = WhatsAppTeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(onClick = onDismiss) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }

                                Box {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = channels.firstOrNull { it.first == selectedChannel }?.second?.take(1) ?: "M",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    // Live Green Dot
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(WhatsAppLightGreen)
                                            .border(1.5.dp, WhatsAppTeal, CircleShape)
                                            .align(Alignment.BottomEnd)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = channels.firstOrNull { it.first == selectedChannel }?.second ?: "Making Brands Team",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "online · typing...",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    viewModel.sendChatMessage("📞 Voice call initiated...")
                                }) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel.sendChatMessage("📹 Video meeting invite shared: https://meet.google.com/mb-traker")
                                }) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "Video Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // WhatsApp Chat Channels Horizontal Selector
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WhatsAppTealDark)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(channels) { (cId, cName) ->
                                val isSel = selectedChannel == cId
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSel) Color.White else Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.clickable {
                                        MiloHaptics.performReactionTick(context, hapticFeedback)
                                        selectedChannel = cId
                                        viewModel.selectChatChannel(cId)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSel) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(WhatsAppTeal)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = cName,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) WhatsAppTeal else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // WhatsApp Security Notice
                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Messages are end-to-end encrypted with MB CRM Cloud.",
                            fontSize = 10.sp,
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 💬 Chat Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.isMe
                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val maxBubbleWidth = (configuration.screenWidthDp * 0.78f).dp

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 14.dp,
                                    topEnd = 14.dp,
                                    bottomStart = if (isMe) 14.dp else 2.dp,
                                    bottomEnd = if (isMe) 2.dp else 14.dp
                                ),
                                color = if (isMe) WhatsAppBubbleSent else WhatsAppBubbleReceived,
                                shadowElevation = 1.dp,
                                modifier = Modifier.widthIn(max = maxBubbleWidth)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Text(
                                        text = msg.senderName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isMe) Color(0xFF0F766E) else Color(0xFF1E88E5)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))

                                    if (msg.attachmentFileName != null) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.InsertDriveFile,
                                                    contentDescription = null,
                                                    tint = WhatsAppTeal,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        msg.attachmentFileName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Text(
                                                        msg.attachmentFileSize ?: "Document",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (msg.isVoiceMessage) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isMe) Color(0xFFC7F8C0) else Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp)
                                        ) {
                                            var isPlaying by remember { mutableStateOf(false) }
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        MiloHaptics.performButtonClick(context, hapticFeedback)
                                                        if (isPlaying) {
                                                            AudioRecorderHelper.stopPlaying()
                                                            isPlaying = false
                                                        } else {
                                                            val path = msg.audioPath
                                                            if (!path.isNullOrBlank()) {
                                                                AudioRecorderHelper.playAudio(
                                                                    filePathOrUrl = path,
                                                                    onPrepared = { isPlaying = true },
                                                                    onCompletion = { isPlaying = false }
                                                                )
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                        contentDescription = "Play voice note",
                                                        tint = WhatsAppTeal,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (msg.audioPath?.startsWith("http") == true) {
                                                            Icon(
                                                                Icons.Default.CloudDone,
                                                                contentDescription = "Firebase Storage",
                                                                tint = WhatsAppTeal,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                        }
                                                        Text(
                                                            "Voice Note • ${msg.audioDurationSeconds}s",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    }
                                                    Text(
                                                        if (isPlaying) "Playing audio..." else "Tap to play",
                                                        fontSize = 9.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = msg.messageText,
                                        fontSize = 14.sp,
                                        color = Color(0xFF111B21),
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.align(Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatLegibleTime(msg.timestampText),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF667781)
                                        )
                                        if (isMe) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.DoneAll,
                                                contentDescription = "Read",
                                                tint = WhatsAppCheckBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Response Suggestions
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickReplies = listOf(
                        "👍 Working on it now!",
                        "🚀 Task completed and tested",
                        "📞 Calling client in 5 mins",
                        "📄 Quotation PDF sent",
                        "☕ Taking a 15m tea break"
                    )
                    items(quickReplies) { reply ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable {
                                MiloHaptics.performMessageSent(context, hapticFeedback)
                                viewModel.sendChatMessage(reply)
                            }
                        ) {
                            Text(
                                text = reply,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Attachment Popup Options
                AnimatedVisibility(
                    visible = showAttachmentMenu,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            AttachmentOptionItem(
                                icon = Icons.Default.InsertDriveFile,
                                label = "Document",
                                bgColor = Color(0xFF5F66CD),
                                onClick = {
                                    viewModel.sendChatMessage("Shared Document: Company_Profile_MB.pdf", "Company_Profile_MB.pdf", "1.8 MB")
                                    showAttachmentMenu = false
                                }
                            )
                            AttachmentOptionItem(
                                icon = Icons.Default.CameraAlt,
                                label = "Camera",
                                bgColor = Color(0xFFE91E63),
                                onClick = {
                                    viewModel.sendChatMessage("Live Office Photo attached", "Office_Live_Snap.jpg", "2.1 MB")
                                    showAttachmentMenu = false
                                }
                            )
                            AttachmentOptionItem(
                                icon = Icons.Default.Image,
                                label = "Gallery",
                                bgColor = Color(0xFF9C27B0),
                                onClick = {
                                    viewModel.sendChatMessage("UI Mockup screenshot shared", "Dashboard_Mockup.png", "3.4 MB")
                                    showAttachmentMenu = false
                                }
                            )
                            AttachmentOptionItem(
                                icon = Icons.Default.ReceiptLong,
                                label = "Invoice/Quote",
                                bgColor = Color(0xFF009688),
                                onClick = {
                                    viewModel.sendChatMessage("Invoice #INV-2026-004 sent for review", "Invoice_MB_2026.pdf", "840 KB")
                                    showAttachmentMenu = false
                                }
                            )
                        }
                    }
                }

                // 🟢 WhatsApp Bottom Message Composer Bar
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            if (isRecordingAudio) {
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
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.Red)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Recording Voice Note: ${recordingSeconds}s",
                                            color = Color.Red,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Text(
                                        "Cancel",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE11D48),
                                        modifier = Modifier
                                            .clickable {
                                                MiloHaptics.performActionWarning(context, hapticFeedback)
                                                AudioRecorderHelper.cancelRecording()
                                                isRecordingAudio = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { inputText += " 😊" },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.SentimentSatisfiedAlt,
                                            contentDescription = "Emoji",
                                            tint = Color(0xFF667781)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = inputText,
                                        onValueChange = { inputText = it },
                                        placeholder = { Text("Message...", fontSize = 14.sp, color = Color(0xFF8696A0)) },
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        maxLines = 4
                                    )

                                    IconButton(
                                        onClick = { showAttachmentMenu = !showAttachmentMenu },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = "Attach",
                                            tint = Color(0xFF667781)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Voice Note / Send Button in WhatsApp Emerald Green
                        FloatingActionButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    val duration = recordingSeconds.coerceAtLeast(1)
                                    val audioFile = AudioRecorderHelper.stopRecording()
                                    isRecordingAudio = false
                                    if (audioFile != null && audioFile.exists()) {
                                        MiloHaptics.performMessageSent(context, hapticFeedback)
                                        viewModel.sendVoiceChatMessage(audioFile.absolutePath, duration)
                                    } else {
                                        viewModel.sendChatMessage("🎙️ Voice Note (${duration}s)", "voice_note_${System.currentTimeMillis()}.m4a", "140 KB")
                                    }
                                } else if (inputText.isNotBlank()) {
                                    MiloHaptics.performMessageSent(context, hapticFeedback)
                                    viewModel.sendChatMessage(inputText.trim())
                                    inputText = ""
                                } else {
                                    // Start voice note with AudioRecorderHelper
                                    val recorded = AudioRecorderHelper.startRecording(context)
                                    if (recorded != null) {
                                        MiloHaptics.performReactionTick(context, hapticFeedback)
                                        isRecordingAudio = true
                                        recordingSeconds = 0
                                    }
                                }
                            },
                            containerColor = WhatsAppLightGreen,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                        ) {
                            if (inputText.isNotBlank() || isRecordingAudio) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Note",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun AttachmentOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
    }
}
