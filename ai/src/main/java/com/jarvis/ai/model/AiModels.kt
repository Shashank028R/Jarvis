package com.jarvis.ai.model

import java.util.UUID

/**
 * Role taxonomy for conversation turns.
 */
enum class TurnRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * Atomic turn in an ongoing conversation session.
 */
data class ConversationTurn(
    val id: String = UUID.randomUUID().toString(),
    val role: TurnRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Context payload provided to the AI planner and conversational client.
 */
data class PlannerContext(
    val recentTurns: List<ConversationTurn> = emptyList(),
    val systemInstruction: String? = null
)

/**
 * Plain conversational text response from the AI client.
 */
data class TextResponse(
    val text: String
)

/**
 * Sealed taxonomy of decisions the planner can make.
 * In V2, conversational reply is the primary operative branch.
 */
sealed class PlannerDecision {
    data class ConversationalReply(val replyText: String) : PlannerDecision()
    data class SingleToolCall(val toolName: String, val argumentsJson: String) : PlannerDecision()
    data class MultiStepPlan(val steps: List<String>) : PlannerDecision()
    data class NeedsClarification(val question: String) : PlannerDecision()
}

/**
 * Structured observation from tool execution (stubbed for future version compatibility).
 */
data class ToolObservation(
    val toolName: String,
    val resultJson: String
)

/**
 * Outcome of post-execution verification (stubbed for future version compatibility).
 */
data class VerificationResult(
    val isSuccess: Boolean,
    val nextAction: String? = null
)
