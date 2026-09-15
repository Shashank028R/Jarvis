package com.jarvis.ai.transport

import com.jarvis.core.result.Result

/**
 * Transport-level request representation for AI provider endpoints.
 */
data class TransportRequest(
    val endpointUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val bodyJson: String,
    val connectTimeoutMs: Long = 15_000L,
    val readTimeoutMs: Long = 30_000L
)

/**
 * Transport-level response representation from AI provider endpoints.
 */
data class TransportResponse(
    val statusCode: Int,
    val bodyJson: String,
    val headers: Map<String, List<String>> = emptyMap()
)

/**
 * Pluggable transport interface decoupling AI clients from network details
 * (Direct Gemini HTTP in development vs Relay proxy in production).
 * Specified in versions/V2_AI_CONVERSATION.md.
 */
interface AiTransport {
    val transportName: String

    /**
     * Executes the HTTP transport request asynchronously.
     */
    suspend fun execute(request: TransportRequest): Result<TransportResponse>
}
