package com.example.presentation.components.banner

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Data model for promotional & offer banners with Image / MP4 video support.
 */
data class AppOfferBanner(
    val id: String,
    val headline: String,
    val subtext: String? = null,
    val ctaText: String,
    val disclaimer: String? = null,
    val badge: String? = null,
    val bgGradientColors: List<Color>,
    val ctaButtonColor: Color,
    val ctaTextColor: Color = Color.White,
    val accentIcon: String = "⚡",
    val routeAction: String,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val isVideo: Boolean = false,
    val videoDuration: String? = null,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)

/**
 * Modern Banner Slider component matching the clean curved-card aesthetic with floating accent dots
 * and CTA buttons.
 */
@Composable
fun OfferBannerSlider(
    banners: List<AppOfferBanner> = defaultOfferBanners,
    modifier: Modifier = Modifier,
    onBannerClick: (AppOfferBanner) -> Unit = {}
) {
    if (banners.isEmpty()) return

    val context = androidx.compose.ui.platform.LocalContext.current
    val batterySaverManager = remember { com.example.util.BatterySaverManager.getInstance(context) }
    val isBatterySaverActive by batterySaverManager.isBatterySaverActive.collectAsState()

    val pagerState = rememberPagerState(pageCount = { banners.size })

    // Auto-scroll loop with power-aware delay
    LaunchedEffect(pagerState, banners.size, isBatterySaverActive) {
        if (banners.size > 1) {
            val scrollDelay = if (isBatterySaverActive) 15_000L else 5_000L
            while (true) {
                delay(scrollDelay)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % banners.size
                    pagerState.animateScrollToPage(nextPage, animationSpec = tween(600))
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offer_banner_pager"),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp
        ) { page ->
            val banner = banners[page]
            OfferBannerCard(
                banner = banner,
                onClick = { onBannerClick(banner) }
            )
        }

        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))

            // Pager Indicator Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(banners.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(if (isSelected) 20.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF0F766E) else Color(0xFFCBD5E1)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Individual Offer Banner Card with clean curved geometry, decorative accent shapes, and high-contrast CTA.
 */
@Composable
fun OfferBannerCard(
    banner: AppOfferBanner,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("banner_card_${banner.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        val imageModel = remember(banner.imageUri) {
            val uriStr = banner.imageUri
            if (uriStr.isNullOrBlank()) null
            else if (uriStr.startsWith("/")) java.io.File(uriStr)
            else if (uriStr.startsWith("file://")) java.io.File(uriStr.removePrefix("file://"))
            else uriStr
        }
        val hasImage = imageModel != null

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 145.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = banner.bgGradientColors
                    )
                )
        ) {
            if (hasImage) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = banner.headline,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // High-contrast gradient overlay so text remains readable over any image
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.78f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.20f)
                                )
                            )
                        )
                )
            } else {
                // Decorative background curves & yellow speech bubble accents (like in the reference AD)
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    val w = size.width
                    val h = size.height

                    // Large soft circle on right
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f),
                        radius = h * 0.95f,
                        center = Offset(w * 0.85f, h * 0.5f)
                    )

                    // Secondary soft curved ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        radius = h * 0.65f,
                        center = Offset(w * 0.85f, h * 0.5f)
                    )

                    // Decorative Yellow Speech Bubble Accents
                    // Top bubble
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = 14.dp.toPx(),
                        center = Offset(w * 0.87f, h * 0.22f)
                    )
                    val topTail = Path().apply {
                        moveTo(w * 0.87f - 10.dp.toPx(), h * 0.22f + 8.dp.toPx())
                        lineTo(w * 0.87f - 18.dp.toPx(), h * 0.22f + 14.dp.toPx())
                        lineTo(w * 0.87f - 5.dp.toPx(), h * 0.22f + 14.dp.toPx())
                        close()
                    }
                    drawPath(topTail, color = Color(0xFFFFEB3B), style = Fill)

                    // Left small bubble
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = 16.dp.toPx(),
                        center = Offset(w * 0.61f, h * 0.44f)
                    )
                    val leftTail = Path().apply {
                        moveTo(w * 0.61f + 6.dp.toPx(), h * 0.44f + 10.dp.toPx())
                        lineTo(w * 0.61f + 14.dp.toPx(), h * 0.44f + 18.dp.toPx())
                        lineTo(w * 0.61f + 1.dp.toPx(), h * 0.44f + 16.dp.toPx())
                        close()
                    }
                    drawPath(leftTail, color = Color(0xFFFFEB3B), style = Fill)

                    // Right bottom bubble
                    drawCircle(
                        color = Color(0xFFFFEB3B),
                        radius = 19.dp.toPx(),
                        center = Offset(w * 0.95f, h * 0.48f)
                    )
                    val rightTail = Path().apply {
                        moveTo(w * 0.95f - 8.dp.toPx(), h * 0.48f + 12.dp.toPx())
                        lineTo(w * 0.95f - 16.dp.toPx(), h * 0.48f + 20.dp.toPx())
                        lineTo(w * 0.95f - 2.dp.toPx(), h * 0.48f + 18.dp.toPx())
                        close()
                    }
                    drawPath(rightTail, color = Color(0xFFFFEB3B), style = Fill)
                }
            }

            // Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                if (banner.badge != null || banner.isVideo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (banner.badge != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hasImage) banner.ctaButtonColor else banner.ctaButtonColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = banner.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasImage) Color.White else banner.ctaButtonColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (banner.isVideo) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Video Preview",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = banner.videoDuration ?: "MP4 VIDEO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Headline
                Text(
                    text = banner.headline,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (hasImage) Color.White else Color(0xFF1E293B),
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(0.72f)
                )

                if (banner.subtext != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = banner.subtext,
                        fontSize = 12.sp,
                        color = if (hasImage) Color(0xFFE2E8F0) else Color(0xFF475569),
                        lineHeight = 16.sp,
                        modifier = Modifier.fillMaxWidth(0.72f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // CTA Button (Pill shaped with forward arrow)
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = banner.ctaButtonColor,
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = banner.ctaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = banner.ctaTextColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = banner.ctaTextColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (banner.disclaimer != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = banner.disclaimer,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * Default promotional offers tailored to Making Brands CRM & Business Tools.
 */
val defaultOfferBanners = listOf(
    AppOfferBanner(
        id = "auto_call_recording_active",
        headline = "Auto Call Recording & Smart Lead Notes Active",
        subtext = "Every incoming & outgoing client call is auto-recorded and indexed to CRM leads.",
        ctaText = "Open Call Tracker",
        disclaimer = "*High-fidelity 16kHz audio capture with instant Room database sync",
        badge = "AUTO RECORDING",
        bgGradientColors = listOf(Color(0xFFFEF2F2), Color(0xFFFEE2E2), Color(0xFFFECACA)),
        ctaButtonColor = Color(0xFFDC2626),
        routeAction = "calls",
        displayOrder = 0
    ),
    AppOfferBanner(
        id = "sales_training_video_mp4",
        headline = "Watch Video: Closing Deals 3X Faster with Making Brands",
        subtext = "Quick MP4 walkthrough on WhatsApp auto-quotes and CRM pipeline conversions.",
        ctaText = "Play Video Guide",
        disclaimer = "*HD MP4 Video walkthrough for sales executives",
        badge = "TRAINING",
        bgGradientColors = listOf(Color(0xFFF5F3FF), Color(0xFFEDE9FE), Color(0xFFDDD6FE)),
        ctaButtonColor = Color(0xFF7C3AED),
        routeAction = "milo_ai",
        isVideo = true,
        videoDuration = "02:15 MP4",
        displayOrder = 1
    ),
    AppOfferBanner(
        id = "instant_lead_import",
        headline = "Enjoy Instant Lead Settlement & Auto Sync",
        subtext = "Get real-time Justdial & Meta Ads leads distributed in 15 seconds.",
        ctaText = "Sync Live Leads",
        disclaimer = "*SLA guarantee active for all registered employees",
        badge = "CRM OFFER",
        bgGradientColors = listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFBBF7D0)),
        ctaButtonColor = Color(0xFF0D9488),
        routeAction = "leads",
        displayOrder = 2
    ),
    AppOfferBanner(
        id = "milo_ai_copilot",
        headline = "Supercharge Pipeline with Milo 2.5 AI Assistant",
        subtext = "Smart follow-up drafts, deal probability & voice coaching.",
        ctaText = "Chat with Milo AI",
        disclaimer = "*Powered by FirebaseAI Gemini Intelligence",
        badge = "SMART AI",
        bgGradientColors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFBFDBFE)),
        ctaButtonColor = Color(0xFF2563EB),
        routeAction = "milo_ai",
        displayOrder = 3
    ),
    AppOfferBanner(
        id = "whatsapp_automation",
        headline = "1-Tap WhatsApp Quotations & Client Follow-ups",
        subtext = "Send branded brochures and invoices in under 5 seconds.",
        ctaText = "Send WhatsApp Now",
        disclaimer = "*Official WhatsApp Business API integration",
        badge = "AUTOMATION",
        bgGradientColors = listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFED7AA)),
        ctaButtonColor = Color(0xFFEA580C),
        routeAction = "invoices",
        displayOrder = 4
    )
)
