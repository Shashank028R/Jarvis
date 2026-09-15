package com.jarvis.app.di

import com.jarvis.accessibility.AccessibilityBridge
import com.jarvis.accessibility.DefaultAccessibilityBridge
import com.jarvis.ai.client.AiClient
import com.jarvis.ai.client.GeminiAiClient
import com.jarvis.ai.transport.AiTransport
import com.jarvis.ai.transport.DirectGeminiTransport
import com.jarvis.androidintegration.DefaultSystemAdapter
import com.jarvis.androidintegration.SystemAdapter
import com.jarvis.app.BuildConfig
import com.jarvis.core.coroutine.DefaultDispatcherProvider
import com.jarvis.core.coroutine.DispatcherProvider
import com.jarvis.core.logger.AndroidJarvisLogger
import com.jarvis.core.logger.JarvisLogger
import com.jarvis.memory.InMemoryMemoryStore
import com.jarvis.memory.MemoryStore
import com.jarvis.orchestrator.DefaultJarvisOrchestrator
import com.jarvis.orchestrator.JarvisOrchestrator
import com.jarvis.orchestrator.session.ConversationSession
import com.jarvis.orchestrator.session.DefaultConversationSession
import com.jarvis.security.DefaultSecurityPolicyEngine
import com.jarvis.security.SecurityPolicyEngine
import com.jarvis.tools.InMemoryToolRegistry
import com.jarvis.tools.ToolRegistry
import com.jarvis.voice.DefaultVoiceStateManager
import com.jarvis.voice.VoiceStateManager
import com.jarvis.voice.aec.AecManager
import com.jarvis.voice.aec.DefaultAecManager
import com.jarvis.voice.controller.VoiceInteractionController
import com.jarvis.voice.stt.AndroidSpeechToTextEngine
import com.jarvis.voice.stt.SpeechToTextEngine
import com.jarvis.voice.tts.AndroidTextToSpeechEngine
import com.jarvis.voice.tts.TextToSpeechEngine
import com.jarvis.voice.vad.EnergyVadEngine
import com.jarvis.voice.vad.VadEngine

/**
 * Dependency container providing modular, decoupled instances of all architectural subsystems for V2 AI Conversation.
 */
interface AppContainer {
    val logger: JarvisLogger
    val dispatchers: DispatcherProvider
    val securityPolicyEngine: SecurityPolicyEngine
    val toolRegistry: ToolRegistry
    val voiceStateManager: VoiceStateManager
    val systemAdapter: SystemAdapter
    val accessibilityBridge: AccessibilityBridge
    val memoryStore: MemoryStore
    val aiTransport: AiTransport
    val aiClient: AiClient
    val conversationSession: ConversationSession
    val orchestrator: JarvisOrchestrator
    val aecManager: AecManager
    val vadEngine: VadEngine
    val sttEngine: SpeechToTextEngine
    val ttsEngine: TextToSpeechEngine
    val voiceInteractionController: VoiceInteractionController
}

class DefaultAppContainer(private val context: android.content.Context) : AppContainer {
    override val logger: JarvisLogger by lazy {
        AndroidJarvisLogger(isDebug = BuildConfig.DEBUG)
    }

    override val dispatchers: DispatcherProvider by lazy {
        DefaultDispatcherProvider()
    }

    override val securityPolicyEngine: SecurityPolicyEngine by lazy {
        DefaultSecurityPolicyEngine()
    }

    override val toolRegistry: ToolRegistry by lazy {
        InMemoryToolRegistry()
    }

    override val voiceStateManager: VoiceStateManager by lazy {
        DefaultVoiceStateManager()
    }

    override val systemAdapter: SystemAdapter by lazy {
        DefaultSystemAdapter()
    }

    override val accessibilityBridge: AccessibilityBridge by lazy {
        DefaultAccessibilityBridge()
    }

    override val memoryStore: MemoryStore by lazy {
        InMemoryMemoryStore()
    }

    override val aiTransport: AiTransport by lazy {
        DirectGeminiTransport(
            apiKey = BuildConfig.GEMINI_API_KEY,
            dispatchers = dispatchers
        )
    }

    override val aiClient: AiClient by lazy {
        GeminiAiClient(transport = aiTransport)
    }

    override val conversationSession: ConversationSession by lazy {
        DefaultConversationSession()
    }

    override val orchestrator: JarvisOrchestrator by lazy {
        DefaultJarvisOrchestrator(
            securityPolicyEngine = securityPolicyEngine,
            toolRegistry = toolRegistry,
            voiceStateManager = voiceStateManager,
            systemAdapter = systemAdapter,
            accessibilityBridge = accessibilityBridge,
            memoryStore = memoryStore,
            aiClient = aiClient,
            session = conversationSession,
            logger = logger
        )
    }

    override val aecManager: AecManager by lazy {
        DefaultAecManager(logger = logger)
    }

    override val vadEngine: VadEngine by lazy {
        EnergyVadEngine()
    }

    override val sttEngine: SpeechToTextEngine by lazy {
        AndroidSpeechToTextEngine(
            context = context,
            logger = logger
        )
    }

    override val ttsEngine: TextToSpeechEngine by lazy {
        AndroidTextToSpeechEngine(
            context = context,
            logger = logger
        ).apply {
            initialize()
        }
    }

    override val voiceInteractionController: VoiceInteractionController by lazy {
        VoiceInteractionController(
            voiceStateManager = voiceStateManager,
            sttEngine = sttEngine,
            ttsEngine = ttsEngine,
            aecManager = aecManager,
            vadEngine = vadEngine,
            intentDispatcher = { intentText -> orchestrator.handleUserIntent(intentText) },
            coroutineScope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + dispatchers.main
            ),
            logger = logger
        )
    }
}
