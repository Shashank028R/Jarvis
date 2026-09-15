package com.jarvis.ai.client

import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.PlannerContext
import com.jarvis.ai.model.PlannerDecision
import com.jarvis.ai.model.TextResponse
import com.jarvis.ai.model.ToolObservation
import com.jarvis.ai.model.VerificationResult
import com.jarvis.core.result.Result

/**
 * Core AI abstraction decoupling provider details (Gemini, local models) from the orchestrator.
 * Specified in 07_AI_ARCHITECTURE.md and ADR-002.
 */
interface AiClient {
    /**
     * Primary conversational reasoning loop for V2.
     * Takes the current user turn and bounded conversation context, returning an AI text response.
     */
    suspend fun converse(input: ConversationTurn, context: PlannerContext): Result<TextResponse>

    /**
     * Reasoning and tool-selection planner.
     * In V2, returns [PlannerDecision.ConversationalReply]. Advanced planning arrives in V9.
     */
    suspend fun interpret(input: ConversationTurn, context: PlannerContext): Result<PlannerDecision>

    /**
     * Post-tool observation verification.
     * Stubbed for V2; active verification arrives in V9.
     */
    suspend fun verify(observation: ToolObservation, context: PlannerContext): Result<VerificationResult>
}
