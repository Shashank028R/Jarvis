package com.jarvis.ai

import com.jarvis.core.result.Result

/**
 * Role taxonomy for LLM conversation context.
 */
enum class AiRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * Atomic message in an AI conversation stream.
 */
data class AiMessage(
    val role: AiRole,
    val content: String,
    val name: String? = null
)

/**
 * AI completion or streaming outcome.
 */
data class AiResponse(
    val content: String,
    val toolCalls: List<AiToolCall> = emptyList(),
    val finishReason: String = "stop"
)

data class AiToolCall(
    val callId: String,
    val functionName: String,
    val argumentsJson: String
)

/**
 * Core AI abstraction decoupling provider details (Gemini, local models) from the orchestrator.
 */
interface AiProvider {
    val providerName: String
    suspend fun generateResponse(messages: List<AiMessage>): Result<AiResponse>
}
