package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.milo.MiloState
import com.example.domain.milo.MiloViewModel
import com.example.presentation.components.milo.MiloCharacter
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Data model for the 4 Milo Explainer Onboarding Slides.
 */
data class MiloOnboardingSlide(
    val stepIndex: Int,
    val miloState: MiloState,
    val title: String,
    val subtitle: String,
    val description: String,
    val speechBubble: String,
    val badgeText: String,
    val badgeColor: Color,
    val primaryColor: Color,
    val gradientColors: List<Color>,
    val highlightTags: List<String>
)

/**
 * 4 Full-Screen Interactive Milo Splash / Explainer Screens with Skip button,
 * smooth pager transitions, animated Milo character expressions, and rich feature breakdowns.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiloOnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val slides = remember { getMiloOnboardingSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })

    val currentSlide = slides[pagerState.currentPage]

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step Indicator Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentSlide.badgeColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = currentSlide.badgeColor,
                            modifier = Modifier.size(7.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STEP ${pagerState.currentPage + 1} OF 4 · ${currentSlide.badgeText}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentSlide.badgeColor
                        )
                    }
                }

                // Skip / Enter Button (Top right)
                TextButton(
                    onClick = onFinishOnboarding,
                    modifier = Modifier.testTag("milo_onboarding_skip_button"),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF64748B))
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Skip",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        containerColor = SurfaceBg,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("milo_onboarding_pager")
        ) { page ->
            val slide = slides[page]
            val isLastPage = page == slides.size - 1

            MiloOnboardingSlideContent(
                slide = slide,
                pageIndex = page,
                totalPages = slides.size,
                isLastPage = isLastPage,
                onPageSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                onFinishOnboarding = onFinishOnboarding
            )
        }
    }
}

/**
 * Centered Slide Content Layout displaying real Milo video/character (with green background removed),
 * speech bubble, title, description, highlight tags, centered page dots, and launch CTA.
 */
@Composable
private fun MiloOnboardingSlideContent(
    slide: MiloOnboardingSlide,
    pageIndex: Int,
    totalPages: Int,
    isLastPage: Boolean,
    onPageSelected: (Int) -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Page Indicator Dots (Centered in middle content)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 20.dp)
        ) {
            repeat(totalPages) { index ->
                val isSelected = pageIndex == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (isSelected) 28.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) slide.primaryColor else Color(0xFFCBD5E1)
                        )
                        .clickable { onPageSelected(index) }
                )
            }
        }

        // Milo Real Character / Video Container (Clean green-background removal with circular clip)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = slide.gradientColors,
                            radius = 450f
                        )
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Real Milo MP4 or Mascot View (No Green Background)
                    com.example.milo.MiloRealStatusView(
                        state = slide.miloState,
                        size = 140.dp,
                        showStateBadge = true,
                        modifier = Modifier.testTag("onboarding_milo_character_${slide.stepIndex}")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Milo Speech Bubble
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = slide.primaryColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🦁", fontSize = 15.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = slide.speechBubble,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Title & Description (Centered)
        Text(
            text = slide.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = slide.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = slide.primaryColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = slide.description,
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Highlight Tags / Chips
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            slide.highlightTags.forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Central Action CTA Button on last slide or tap to launch
        if (isLastPage) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onFinishOnboarding,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp)
                    .testTag("milo_onboarding_launch_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = slide.primaryColor),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "Launch MB Workspace 🚀",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 4 Comprehensive Milo Explainer Slides.
 */
fun getMiloOnboardingSlides(): List<MiloOnboardingSlide> {
    return listOf(
        // Slide 1: Welcome & Milo Mascot
        MiloOnboardingSlide(
            stepIndex = 1,
            miloState = MiloState.WELCOME,
            title = "Meet Milo, Your Smart AI Co-Pilot",
            subtitle = "Always by your side to boost productivity",
            description = "Milo is Making Brands' energetic lion companion. He tracks your live tasks, guides daily priorities, and celebrates your business milestones!",
            speechBubble = "“Roar! Welcome to MB Traker! I'm here to help you crush today's goals!”",
            badgeText = "MEET MILO",
            badgeColor = ElectricBlue,
            primaryColor = ElectricBlue,
            gradientColors = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color.White),
            highlightTags = listOf("🦁 Smart Mascot", "⚡ Real-time AI", "🎯 Daily Coach")
        ),
        // Slide 2: Real-Time Lead Distribution & SLA
        MiloOnboardingSlide(
            stepIndex = 2,
            miloState = MiloState.LEAD_IMPORTED,
            title = "Instant Lead Settlement & Tracking",
            subtitle = "Never miss a 15-Minute SLA Follow-up",
            description = "Auto-sync leads from Justdial, Meta Ads, and your website in real-time. Milo alerts you with 1-tap WhatsApp and direct dialing options.",
            speechBubble = "“Incoming Justdial leads! Let's reach out under 15 minutes!”",
            badgeText = "LEAD MANAGEMENT",
            badgeColor = Color(0xFF0D9488),
            primaryColor = Color(0xFF0D9488),
            gradientColors = listOf(Color(0xFFF0FDF4), Color(0xFFCCFBF1), Color.White),
            highlightTags = listOf("📥 Auto-Import", "⚡ 15-Min SLA", "💬 1-Tap WhatsApp")
        ),
        // Slide 3: Smart Attendance & Shift Check-In
        MiloOnboardingSlide(
            stepIndex = 3,
            miloState = MiloState.WORKING,
            title = "Effortless Attendance & Timesheets",
            subtitle = "Punch In, log breaks & track hours automatically",
            description = "Seamless biometric authentication, live shift timers, lunch break tracking, and automated task workload visualizers built for the whole team.",
            speechBubble = "“Checked In! Your active shift is running smoothly.”",
            badgeText = "WORK & SHIFTS",
            badgeColor = Color(0xFFD97706),
            primaryColor = Color(0xFFD97706),
            gradientColors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color.White),
            highlightTags = listOf("⏰ 1-Tap Punch", "☕ Break Log", "📊 Live Timesheet")
        ),
        // Slide 4: Win Deals & Celebrate Success
        MiloOnboardingSlide(
            stepIndex = 4,
            miloState = MiloState.CELEBRATION,
            title = "Close Deals & Win Big Rewards",
            subtitle = "Celebrate sales conversions & earn trophies",
            description = "Generate instant PDF quotations, track won revenue pipelines, and trigger confetti celebrations with Milo whenever deals are closed!",
            speechBubble = "“High five! Deal WON! Let's celebrate your team victory!”",
            badgeText = "REWARDS & DEALS",
            badgeColor = Color(0xFF7C3AED),
            primaryColor = Color(0xFF7C3AED),
            gradientColors = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color.White),
            highlightTags = listOf("🏆 Deal Won", "📄 PDF Invoices", "🎉 Confetti Party")
        )
    )
}
