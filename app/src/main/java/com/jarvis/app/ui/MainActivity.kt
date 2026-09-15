package com.jarvis.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jarvis.app.JarvisApplication
import com.jarvis.app.ui.theme.JarvisBlack
import com.jarvis.app.ui.theme.JarvisTheme

/**
 * Main activity hosting the JARVIS edge-to-edge Compose UI for V2 AI Conversation.
 * Configured for true AMOLED edge-to-edge without system bar scrims.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: JarvisViewModel by viewModels {
        val container = (application as JarvisApplication).container
        JarvisViewModel.provideFactory(
            orchestrator = container.orchestrator,
            voiceController = container.voiceInteractionController
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce true edge-to-edge without default navigation/status bar scrims
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        setContent {
            JarvisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = JarvisBlack
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    JarvisFoundationScreen(
                        uiState = uiState,
                        onInputChanged = viewModel::onInputChanged,
                        onSendMessage = viewModel::sendMessage,
                        onRetry = viewModel::retryLast,
                        onClearConversation = viewModel::clearConversation,
                        onMicTapped = viewModel::onMicTapped,
                        onInterruptVoice = viewModel::interruptVoice,
                        onCancelVoice = viewModel::cancelVoice,
                        onClearVoiceError = viewModel::clearVoiceError
                    )
                }
            }
        }
    }
}
