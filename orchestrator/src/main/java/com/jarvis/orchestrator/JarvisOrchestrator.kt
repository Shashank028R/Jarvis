package com.jarvis.orchestrator

import com.jarvis.accessibility.AccessibilityBridge
import com.jarvis.ai.client.AiClient
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.PlannerContext
import com.jarvis.ai.model.TurnRole
import com.jarvis.ai.prompt.JarvisPersonality
import com.jarvis.androidintegration.SystemAdapter
import com.jarvis.core.error.JarvisError
import com.jarvis.core.logger.JarvisLogger
import com.jarvis.core.result.Result
import com.jarvis.memory.MemoryStore
import com.jarvis.orchestrator.session.ConversationSession
import com.jarvis.orchestrator.session.DefaultConversationSession
import com.jarvis.security.SecurityPolicyEngine
import com.jarvis.tools.ToolRegistry
import com.jarvis.voice.VoiceStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level assistant lifecycle state.
 */
enum class AssistantState {
    INITIALIZING,
    READY,
    LISTENING,
    PROCESSING,
    EXECUTING_ACTION,
    SPEAKING,
    ERROR
}

/**
 * Central orchestrator interface coordinating AI, tools, voice, memory, and OS integration.
 */
interface JarvisOrchestrator {
    val state: StateFlow<AssistantState>
    val session: ConversationSession
    fun initialize(): Result<Unit>
    suspend fun handleUserIntent(intentText: String): Result<String>
    fun resetSession()
}

/**
 * Production orchestrator engine for V2 AI Conversation.
 * Orchestrates conversation flow between user input, conversational session state, and [AiClient].
 */
class DefaultJarvisOrchestrator(
    private val securityPolicyEngine: SecurityPolicyEngine,
    private val toolRegistry: ToolRegistry,
    private val voiceStateManager: VoiceStateManager,
    private val systemAdapter: SystemAdapter,
    private val accessibilityBridge: AccessibilityBridge,
    private val memoryStore: MemoryStore,
    private val aiClient: AiClient? = null,
    override val session: ConversationSession = DefaultConversationSession(),
    private val logger: JarvisLogger? = null
) : JarvisOrchestrator {

    private val _state = MutableStateFlow(AssistantState.INITIALIZING)
    override val state: StateFlow<AssistantState> = _state.asStateFlow()

    override fun initialize(): Result<Unit> {
        _state.value = AssistantState.READY
        return Result.success(Unit)
    }

    override suspend fun handleUserIntent(intentText: String): Result<String> {
        val trimmed = intentText.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(JarvisError.InvalidState("Intent text cannot be empty"))
        }

        if (aiClient == null) {
            _state.value = AssistantState.ERROR
            return Result.failure(
                JarvisError.Configuration("No AI client configured in orchestrator")
            )
        }

        _state.value = AssistantState.PROCESSING

        val userTurn = ConversationTurn(
            role = TurnRole.USER,
            text = trimmed
        )
        session.addTurn(userTurn)

        // Provide recent context excluding current turn
        val recentHistory = session.getRecentTurns().filter { it.id != userTurn.id }
        val context = PlannerContext(
            recentTurns = recentHistory,
            systemInstruction = JarvisPersonality.DEFAULT_SYSTEM_INSTRUCTION
        )

        logger?.d("JarvisOrchestrator") { "Dispatching conversation turn to AI client" }
        val result = aiClient.converse(userTurn, context)

        return result.fold(
            onSuccess = { textResponse ->
                val assistantTurn = ConversationTurn(
                    role = TurnRole.ASSISTANT,
                    text = textResponse.text
                )
                session.addTurn(assistantTurn)
                _state.value = AssistantState.READY
                Result.success(textResponse.text)
            },
            onFailure = { error ->
                _state.value = AssistantState.ERROR
                logger?.e("JarvisOrchestrator", message = { "AI client converse failed: ${error.message}" })
                Result.failure(error)

            }
        )

    }

    override fun resetSession() {
        session.clear()
        _state.value = AssistantState.READY
    }
}
