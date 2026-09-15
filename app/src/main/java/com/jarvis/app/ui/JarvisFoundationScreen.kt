package com.jarvis.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import com.jarvis.voice.VoiceState


import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.TurnRole
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
    SubsystemStatus(":ai", "Provider Abstractions & Gemini"),
    SubsystemStatus(":tools", "Registry & Tool Schemas"),
    SubsystemStatus(":voice", "State Machine & Audio"),
    SubsystemStatus(":android-integration", "System Adapters"),
    SubsystemStatus(":accessibility", "Privileged Bridge"),
    SubsystemStatus(":memory", "Store & Persistence"),
    SubsystemStatus(":orchestrator", "Session & Central Engine"),
    SubsystemStatus(":app", "Compose AMOLED UI")
)

/**
 * Main UI surface for JARVIS V2 AI Conversation.
 * Preserves the pure AMOLED visual identity, header, and static V1 visual anchor,
 * while adding interactive multi-turn text conversation, loading, and error states.
 */
@Composable
fun JarvisFoundationScreen(
    uiState: ConversationUiState,
    onInputChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onRetry: () -> Unit,
    onClearConversation: () -> Unit,
    onMicTapped: () -> Unit = {},
    onInterruptVoice: () -> Unit = {},
    onCancelVoice: () -> Unit = {},
    onClearVoiceError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    var subsystemsExpanded by remember { mutableStateOf(false) }
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionDeniedNotice = false
            onMicTapped()
        } else {
            permissionDeniedNotice = true
        }
    }

    val handleMicClick = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            permissionDeniedNotice = false
            onMicTapped()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Auto-scroll to bottom when new turns arrive
    LaunchedEffect(uiState.turns.size, uiState.isProcessing) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Status Bar & Action Header
            TopStatusBar(
                hasTurns = uiState.turns.isNotEmpty(),
                onClear = onClearConversation
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Visual Brand Anchor & Branding Typography (compact for conversation layout)
            CenterVisualSection()

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Conversation Message Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.turns.isEmpty() && !uiState.isProcessing && uiState.errorMessage == null) {
                    EmptyConversationState(onSuggestionClick = { onInputChanged(it) })
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.turns, key = { it.id }) { turn ->
                            ConversationTurnBubble(turn = turn)
                        }

                        // Processing Indicator
                        if (uiState.isProcessing) {
                            item {
                                ProcessingIndicatorBubble()
                            }
                        }

                        // Error Banner / Card
                        if (uiState.errorMessage != null) {
                            item {
                                ErrorMessageCard(
                                    errorMessage = uiState.errorMessage,
                                    onRetry = onRetry
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Voice Interaction HUD / Feedback Banner
            VoiceInteractionHud(
                voiceState = uiState.voiceState,
                partialTranscript = uiState.partialTranscript,
                voiceError = uiState.voiceError,
                permissionDenied = permissionDeniedNotice,
                onDismissPermission = { permissionDeniedNotice = false },
                onDismissError = onClearVoiceError,
                onInterruptVoice = onInterruptVoice,
                onCancelVoice = onCancelVoice
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Expandable Subsystem Architecture Health Status
            SubsystemsExpandableSection(
                expanded = subsystemsExpanded,
                onToggle = { subsystemsExpanded = !subsystemsExpanded }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 6. Text & Voice Input Bar
            ConversationInputBar(
                inputText = uiState.inputText,
                isProcessing = uiState.isProcessing,
                voiceState = uiState.voiceState,
                onInputChanged = onInputChanged,
                onSend = {
                    keyboardController?.hide()
                    onSendMessage()
                },
                onMicClick = handleMicClick,
                onInterruptVoice = onInterruptVoice
            )
        }
    }
}

@Composable
private fun TopStatusBar(
    hasTurns: Boolean,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasTurns) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Clear conversation",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun CenterVisualSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // V1 Visual Geometry Placeholder (Static brand anchor)
        DrlEyeAnchorVisual(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Branding Typography
        Text(
            text = "JARVIS",
            color = JarvisTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = "YOUR PERSONAL AI ASSISTANT",
            color = JarvisRedPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DrlEyeAnchorVisual(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val centerX = width / 2f

        val waveWidth = 100f
        val strokeWidth = 2.5f

        val redBrush = Brush.linearGradient(
            colors = listOf(JarvisRedDark, JarvisRedBright, JarvisRedDark),
            start = Offset(centerX - waveWidth, centerY),
            end = Offset(centerX + waveWidth, centerY)
        )

        // Center baseline
        drawLine(
            brush = redBrush,
            start = Offset(centerX - waveWidth, centerY),
            end = Offset(centerX + waveWidth, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Left stylized DRL bracket
        val leftEyeX = centerX - 90f
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(leftEyeX - 30f, centerY - 10f),
            end = Offset(leftEyeX, centerY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(leftEyeX, centerY),
            end = Offset(leftEyeX - 22f, centerY + 8f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )

        // Right stylized DRL bracket (symmetric)
        val rightEyeX = centerX + 90f
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(rightEyeX + 30f, centerY - 10f),
            end = Offset(rightEyeX, centerY),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = JarvisRedPrimary,
            start = Offset(rightEyeX, centerY),
            end = Offset(rightEyeX + 22f, centerY + 8f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun EmptyConversationState(
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hello Sir. How can I help you today?",
            color = JarvisTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Type a message below to converse with the AI reasoning layer.",
            color = JarvisTextMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Suggestion prompts
        val suggestions = listOf(
            "Status report on active subsystems",
            "Explain quantum computing concisely",
            "What are the core laws of robotics?"
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            suggestions.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(16.dp))
                        .background(JarvisSurfaceDark, RoundedCornerShape(16.dp))
                        .clickable { onSuggestionClick(prompt) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = prompt,
                        color = JarvisTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationTurnBubble(turn: ConversationTurn) {
    val isUser = turn.role == TurnRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    width = 1.dp,
                    color = if (isUser) JarvisRedDark.copy(alpha = 0.6f) else JarvisSurfaceBorder,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(
                    color = if (isUser) Color(0xFF141416) else JarvisSurfaceDark,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (isUser) JarvisTextSecondary else JarvisRedPrimary,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (isUser) "USER" else "JARVIS",
                        color = if (isUser) JarvisTextSecondary else JarvisRedBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = turn.text,
                    color = JarvisTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun ProcessingIndicatorBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(12.dp))
                .background(JarvisSurfaceDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = JarvisRedPrimary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "JARVIS // PROCESSING...",
                    color = JarvisRedBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ErrorMessageCard(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisRedDark, RoundedCornerShape(8.dp))
            .background(Color(0xFF1F0608), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Error",
                    tint = JarvisRedBright,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "COMMUNICATION EXCEPTION",
                    color = JarvisRedBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Text(
                text = errorMessage,
                color = JarvisTextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, JarvisRedPrimary, RoundedCornerShape(4.dp))
                        .background(JarvisRedDark.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable { onRetry() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = JarvisRedBright,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "RETRY",
                            color = JarvisRedBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsystemsExpandableSection(
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "chevron_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
            .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp))
            .background(JarvisSurfaceDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(JarvisSuccess, CircleShape)
                )
                Text(
                    text = "SUBSYSTEM HEALTH (10/10 READY)",
                    color = JarvisTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse subsystem health" else "Expand subsystem health",
                tint = JarvisTextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronRotation)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(durationMillis = 250, delayMillis = 50, easing = FastOutSlowInEasing)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SubsystemList.forEach { sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sub.name,
                            color = JarvisTextPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = sub.role,
                            color = JarvisTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceInteractionHud(
    voiceState: VoiceState,
    partialTranscript: String,
    voiceError: String?,
    permissionDenied: Boolean,
    onDismissPermission: () -> Unit,
    onDismissError: () -> Unit,
    onInterruptVoice: () -> Unit,
    onCancelVoice: () -> Unit
) {
    if (permissionDenied) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisRedPrimary, RoundedCornerShape(8.dp))
                .background(JarvisRedDark.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = null,
                        tint = JarvisRedBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Microphone permission required for voice. Text conversation remains active.",
                        color = JarvisTextPrimary,
                        fontSize = 11.sp
                    )
                }
                IconButton(
                    onClick = onDismissPermission,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notice",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    } else if (voiceError != null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisRedPrimary, RoundedCornerShape(8.dp))
                .background(JarvisRedDark.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = JarvisRedBright,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = voiceError,
                        color = JarvisRedBright,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(
                    onClick = onDismissError,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss error",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    } else if (voiceState == VoiceState.LISTENING) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisRedBright, RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(JarvisRedBright, CircleShape)
                    )
                    Column {
                        Text(
                            text = "VOICE INPUT ACTIVE — LISTENING...",
                            color = JarvisRedBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (partialTranscript.isNotBlank()) "\"$partialTranscript\"" else "Speak naturally to JARVIS",
                            color = if (partialTranscript.isNotBlank()) JarvisTextPrimary else JarvisTextMuted,
                            fontSize = 12.sp,
                            fontFamily = if (partialTranscript.isNotBlank()) FontFamily.SansSerif else FontFamily.Monospace
                        )
                    }
                }
                IconButton(
                    onClick = onCancelVoice,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel listening",
                        tint = JarvisTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else if (voiceState == VoiceState.SPEAKING) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisRedPrimary, RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = JarvisRedBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = "JARVIS SPEAKING — BARGE-IN ACTIVE",
                            color = JarvisRedBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Tap stop or speak to interrupt",
                            color = JarvisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, JarvisRedPrimary, RoundedCornerShape(4.dp))
                        .background(JarvisRedDark.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { onInterruptVoice() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Interrupt speech",
                            tint = JarvisRedBright,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "STOP",
                            color = JarvisRedBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    } else if (voiceState == VoiceState.THINKING) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, JarvisSurfaceBorder, RoundedCornerShape(8.dp))
                .background(JarvisSurfaceDark, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = JarvisRedBright
                )
                Text(
                    text = "PROCESSING SPEECH WITH AI...",
                    color = JarvisTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ConversationInputBar(
    inputText: String,
    isProcessing: Boolean,
    voiceState: VoiceState,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onInterruptVoice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = if (voiceState == VoiceState.LISTENING) "Listening..." else "Ask JARVIS or tap Mic...",
                    color = JarvisTextMuted,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisSurfaceDark,
                unfocusedContainerColor = JarvisSurfaceDark,
                focusedBorderColor = JarvisRedPrimary,
                unfocusedBorderColor = JarvisSurfaceBorder,
                focusedTextColor = JarvisTextPrimary,
                unfocusedTextColor = JarvisTextPrimary,
                cursorColor = JarvisRedBright
            ),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        val canSend = inputText.isNotBlank() && !isProcessing
        if (inputText.isNotBlank()) {
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = if (canSend) JarvisRedPrimary else JarvisSurfaceDark,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = if (canSend) JarvisRedBright else JarvisSurfaceBorder,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = if (canSend) Color.White else JarvisTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Voice Interaction Action Button
        when (voiceState) {
            VoiceState.SPEAKING -> {
                IconButton(
                    onClick = onInterruptVoice,
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = JarvisRedPrimary, shape = CircleShape)
                        .border(width = 1.dp, color = JarvisRedBright, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Interrupt speech",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            VoiceState.LISTENING -> {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = JarvisRedDark, shape = CircleShape)
                        .border(width = 1.5.dp, color = JarvisRedBright, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Stop listening",
                        tint = JarvisRedBright,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            VoiceState.THINKING -> {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = JarvisSurfaceDark, shape = CircleShape)
                        .border(width = 1.dp, color = JarvisSurfaceBorder, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = JarvisRedBright
                    )
                }
            }
            else -> {
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(color = JarvisSurfaceDark, shape = CircleShape)
                        .border(width = 1.dp, color = JarvisSurfaceBorder, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start voice input",
                        tint = JarvisTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
