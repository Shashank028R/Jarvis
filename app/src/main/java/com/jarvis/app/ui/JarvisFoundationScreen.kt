package com.jarvis.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.app.BuildConfig
import com.jarvis.app.ui.theme.JarvisBlack
import com.jarvis.app.ui.theme.JarvisRedBright
import com.jarvis.app.ui.theme.JarvisRedDark
import com.jarvis.app.ui.theme.JarvisRedPrimary
import com.jarvis.app.ui.theme.JarvisSuccess
import com.jarvis.app.ui.theme.JarvisSurfaceBorder
import com.jarvis.app.ui.theme.JarvisSurfaceDark
import com.jarvis.app.ui.theme.JarvisTextMuted
import com.jarvis.app.ui.theme.JarvisTextPrimary
import com.jarvis.app.ui.theme.JarvisTextSecondary

private data class SubsystemStatus(
    val name: String,
    val role: String,
    val active: Boolean = true
)

private val SubsystemList = listOf(
    SubsystemStatus(":core", "Contracts & Result<T>"),
    SubsystemStatus(":security", "Policy Engine & Tiers"),
    SubsystemStatus(":ai", "Provider Abstractions"),
    SubsystemStatus(":tools", "Registry & Tool Schemas"),
    SubsystemStatus(":voice", "State Machine & Audio"),
    SubsystemStatus(":android-integration", "System Adapters"),
    SubsystemStatus(":accessibility", "Privileged Bridge"),
    SubsystemStatus(":memory", "Store & Persistence"),
    SubsystemStatus(":orchestrator", "Central Engine"),
    SubsystemStatus(":app", "Compose AMOLED UI")
)

/**
 * Foundation screen for V1.
 * Provides the pure AMOLED black container, status bar, branding typography,
 * static V1 visual anchor, and the complete 10-subsystem readiness dashboard.
 */
@Composable
fun JarvisFoundationScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBlack)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Status Bar & Flavor Badge
            TopStatusBar()

            Spacer(modifier = Modifier.height(28.dp))

            // Center: Visual DRL Anchor Placeholder, Waveform, Typography
            CenterVisualSection()

            Spacer(modifier = Modifier.height(36.dp))

            // Bottom: Modular Subsystem Grid (all 10 fully accessible)
            SubsystemsSection()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // System status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(JarvisSuccess, shape = CircleShape)
            )
            Text(
                text = "SYSTEM // ONLINE",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Flavor badge
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (BuildConfig.IS_FULL_ASSISTANT) JarvisRedPrimary else JarvisSurfaceBorder,
                    shape = RoundedCornerShape(4.dp)
                )
                .background(
                    color = if (BuildConfig.IS_FULL_ASSISTANT) JarvisRedDark.copy(alpha = 0.3f) else JarvisSurfaceDark,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (BuildConfig.IS_FULL_ASSISTANT) "FULL ASSISTANT" else "PLAY STORE",
                color = if (BuildConfig.IS_FULL_ASSISTANT) JarvisRedBright else JarvisTextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CenterVisualSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // V1 Visual Geometry Placeholder (Static brand anchor; formal [0..1000] vector animation activates in V4)
        DrlEyeAnchorVisual(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Branding Typography
        Text(
            text = "JARVIS",
            color = JarvisTextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "YOUR PERSONAL AI ASSISTANT",
            color = JarvisRedPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting Prompt (from reference design)
        Text(
            text = "Hello Sir. How can I help you today?",
            color = JarvisTextSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Static V1 placeholder representing the brand visual anchor.
 * Explicitly designed as a lightweight, non-animated geometric representation
 * to reserve layout space without conflicting with the formal V4 eye engine.
 */
@Composable
private fun DrlEyeAnchorVisual(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val centerX = width / 2f

        // Center pulsating audio waveform line
        val waveWidth = 140f
        val strokeWidth = 3f

        // Glow brush for the central waveform
        val redBrush = Brush.linearGradient(
            colors = listOf(JarvisRedDark, JarvisRedBright, JarvisRedDark),
            start = Offset(centerX - waveWidth, centerY),
            end = Offset(centerX + waveWidth, centerY)
        )

        // Draw center baseline
        drawLine(
            brush = redBrush,
            start = Offset(centerX - waveWidth, centerY),
            end = Offset(centerX + waveWidth, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw left stylized DRL bracket
        val leftEyeX = centerX - 120f
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(leftEyeX - 40f, centerY - 14f),
            end = Offset(leftEyeX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(leftEyeX - 40f, centerY + 14f),
            end = Offset(leftEyeX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )

        // Draw right stylized DRL bracket
        val rightEyeX = centerX + 120f
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(rightEyeX + 40f, centerY - 14f),
            end = Offset(rightEyeX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(rightEyeX + 40f, centerY + 14f),
            end = Offset(rightEyeX, centerY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun SubsystemsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SUBSYSTEMS (V1 FOUNDATION)",
                color = JarvisTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "10/10 READY",
                color = JarvisSuccess,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Fully accessible paired cards (no nested clipping, fully scrollable with screen)
        val chunked = SubsystemList.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            chunked.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { sub ->
                        Box(modifier = Modifier.weight(1f)) {
                            SubsystemCard(sub)
                        }
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsystemCard(sub: SubsystemStatus) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = JarvisSurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = JarvisSurfaceBorder,
                shape = RoundedCornerShape(6.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sub.name,
                    color = JarvisTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(JarvisSuccess, shape = CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub.role,
                color = JarvisTextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}
