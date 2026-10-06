package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ActivityFeedItemEntity
import com.example.ui.screens.MainViewModel
import com.example.ui.theme.*

@Composable
fun DynamicActivityFeedSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hapticFeedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    val feedItems by viewModel.activityFeedItems.collectAsState()
    val currentUserName by viewModel.currentEmployeeName.collectAsState()
    val isConnected by viewModel.isFirebaseConnected.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreatePostDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "📢 Announcement", "🚀 Milestone", "🎉 Celebration", "💡 Idea", "☕ Social")

    val filteredItems = remember(feedItems, selectedCategory) {
        if (selectedCategory == "All") {
            feedItems
        } else {
            val cleanCat = selectedCategory.replace(Regex("[^a-zA-Z]"), "").trim()
            feedItems.filter { it.category.contains(cleanCat, ignoreCase = true) }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_activity_feed_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Live Sync badge and Share button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DynamicFeed,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Activity & Feeds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isConnected) "WebSocket & Cloud Synced" else "Local Persistent (Room)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isConnected) Color(0xFF059669) else Color(0xFFD97706)
                        )
                    }
                }

                Button(
                    onClick = { showCreatePostDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Post",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Share",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Category Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) BrandBlue else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .clickable {
                                com.example.util.MiloHaptics.performReactionTick(context, hapticFeedback)
                                selectedCategory = cat
                            }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Feed Items List
            if (filteredItems.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No updates in this category yet.",
                            fontSize = 12.5.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                com.example.util.MiloHaptics.performMessageSent(context, hapticFeedback)
                                viewModel.postActivityFeed(
                                    title = "🎉 Real-time Collaborative Engine Online",
                                    content = "Welcome to live team activities! Post updates, track project deliverables, and celebrate milestones in real-time.",
                                    category = "Announcement"
                                )
                            }
                        ) {
                            Text("Post Sample Team Announcement", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    filteredItems.take(5).forEach { item ->
                        ActivityFeedItemCard(
                            item = item,
                            currentUserName = currentUserName,
                            onToggleLike = {
                                com.example.util.MiloHaptics.performReactionTick(context, hapticFeedback)
                                viewModel.toggleFeedLike(item.id, item.likesCount, item.likedByUsers)
                            }
                        )
                    }
                }
            }
        }
    }

    // Create New Dynamic Post Dialog
    if (showCreatePostDialog) {
        CreateActivityPostDialog(
            onDismiss = { showCreatePostDialog = false },
            onPost = { title, content, category ->
                com.example.util.MiloHaptics.performMessageSent(context, hapticFeedback)
                viewModel.postActivityFeed(title, content, category)
                showCreatePostDialog = false
            }
        )
    }
}

@Composable
fun ActivityFeedItemCard(
    item: ActivityFeedItemEntity,
    currentUserName: String,
    onToggleLike: () -> Unit
) {
    val likedList = remember(item.likedByUsers) {
        if (item.likedByUsers.isBlank()) emptyList()
        else item.likedByUsers.split(", ").filter { it.isNotBlank() }
    }
    val hasUserLiked = remember(likedList, currentUserName) {
        likedList.any { it.equals(currentUserName.trim(), ignoreCase = true) }
    }

    val categoryColor = when {
        item.category.contains("Announcement", ignoreCase = true) -> Color(0xFFF59E0B)
        item.category.contains("Milestone", ignoreCase = true) -> Color(0xFF8B5CF6)
        item.category.contains("Celebration", ignoreCase = true) -> Color(0xFFEC4899)
        item.category.contains("Idea", ignoreCase = true) -> Color(0xFF10B981)
        else -> BrandBlue
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Author & Category Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = categoryColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.authorName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = categoryColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.authorName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${item.authorRole} • ${item.timestampText}",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = item.category,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Post Title & Content
            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = item.content,
                fontSize = 12.5.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Row: Like button + Likes Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasUserLiked) Color(0xFFFFE4E6) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (hasUserLiked) Color(0xFFFDA4AF) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.clickable { onToggleLike() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hasUserLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (hasUserLiked) Color(0xFFE11D48) else Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.likesCount > 0) "${item.likesCount}" else "Like",
                            fontSize = 11.sp,
                            fontWeight = if (hasUserLiked) FontWeight.Bold else FontWeight.Medium,
                            color = if (hasUserLiked) Color(0xFFE11D48) else Color(0xFF475569)
                        )
                    }
                }

                if (likedList.isNotEmpty()) {
                    Text(
                        text = if (likedList.size == 1) "Liked by ${likedList.first()}"
                        else "Liked by ${likedList.first()} +${likedList.size - 1}",
                        fontSize = 10.5.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CreateActivityPostDialog(
    onDismiss: () -> Unit,
    onPost: (title: String, content: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Announcement") }

    val categories = listOf("Announcement", "Milestone", "Celebration", "Idea", "Social")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Dynamic Update",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandBlue else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSelected) BrandBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    placeholder = { Text("e.g. Milestone Achieved: Q3 Release!") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("Write your update, shoutout, or announcement here...") },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            onPost(title.trim(), content.trim(), selectedCategory)
                        }
                    },
                    enabled = content.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Broadcast Update Live", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
