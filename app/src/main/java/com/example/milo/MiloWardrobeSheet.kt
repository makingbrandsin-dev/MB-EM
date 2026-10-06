package com.example.milo

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Milo Wardrobe & Rewards Sheet (MiloWardrobeSheet.kt)
 * Gamified shop/wardrobe where users view XP, unlock accessory themes, equip items,
 * and view recent XP earnings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloWardrobeSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val xpState by MiloXpManager.xpState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Wardrobe & Accessories, 1: XP History

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Clear level up popup on view
    DisposableEffect(Unit) {
        onDispose {
            MiloXpManager.dismissLevelUpNotice()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.50f),
        dragHandle = null,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SurfaceBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .imePadding()
                    .padding(16.dp)
            ) {
            // Level Up Celebration Banner if active
            xpState.recentLevelUp?.let { newLevel ->
                Surface(
                    color = StatusOrange,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎉", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "LEVEL UP! REACHED LEVEL $newLevel",
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                "New Milo Accessory Themes Unlocked in Wardrobe!",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = { MiloXpManager.dismissLevelUpNotice() }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            // Top Header & Level Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MiloCharacter(
                                state = MiloState.CONVERTED,
                                size = 52.dp,
                                showStateBadge = true
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LVL ${xpState.currentLevel}: ${xpState.levelTitle}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Equipped: ${xpState.equippedTheme.iconEmoji} ${xpState.equippedTheme.title}",
                                    fontSize = 12.sp,
                                    color = BrandBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            color = BrandBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ ${xpState.totalXp} XP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlue,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress Bar to next level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level ${xpState.currentLevel} Progress",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${xpState.totalXp} / ${xpState.xpForNextLevel} XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { xpState.progressInLevel },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = BrandBlue,
                        trackColor = BrandBlue.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = BrandBlue,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("👑 Wardrobe & Accessories", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("📜 XP Activity Log", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab View Body
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTab == 0) {
                    WardrobeAccessoriesList(
                        currentState = xpState,
                        onEquip = { theme ->
                            MiloXpManager.equipTheme(context, theme)
                            Toast.makeText(context, "Equipped ${theme.title}! ${theme.iconEmoji}", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    XpHistoryLogList(logs = xpState.logs)
                }
            }
        }
    }
}
}

@Composable
private fun WardrobeAccessoriesList(
    currentState: MiloXpState,
    onEquip: (MiloThemeAccessory) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Unlock unique Milo themes & accessory overlays by completing CRM tasks and closing high-value leads!",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        items(MiloThemeAccessory.entries) { theme ->
            val isUnlocked = currentState.unlockedThemes.contains(theme)
            val isEquipped = currentState.equippedTheme == theme

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEquipped) BrandBlue.copy(alpha = 0.08f) else CardBg
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isEquipped) 2.dp else 1.dp,
                    color = if (isEquipped) BrandBlue else if (isUnlocked) TextSecondary.copy(alpha = 0.25f) else TextSecondary.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(theme.primaryColorHex).copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = theme.iconEmoji, fontSize = 24.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = theme.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            if (isEquipped) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = BrandBlue,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "EQUIPPED",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = theme.description,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        if (!isUnlocked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔒 Requires Level ${theme.requiredLevel} (${theme.requiredXp} XP)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isUnlocked) {
                        Button(
                            onClick = { onEquip(theme) },
                            enabled = !isEquipped,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                disabledContainerColor = TextSecondary.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEquipped) "Active" else "Equip",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XpHistoryLogList(
    logs: List<MiloXpLog>
) {
    if (logs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No XP earned yet! Complete a task or create a lead to earn XP.", fontSize = 13.sp, color = TextSecondary)
            }
        }
    } else {
        val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = StatusGreen.copy(alpha = 0.15f),
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = log.reason,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = dateFormat.format(Date(log.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Text(
                            text = "+${log.xpEarned} XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusGreen
                        )
                    }
                }
            }
        }
    }
}
