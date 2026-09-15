package com.jarvis.ai.transport

import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result

/**
 * Production transport specification stub for V15.
 * In production releases, AI requests route through a lightweight serverless relay proxy
 * to prevent client-side secret exposure (13_SECURITY.md).
 * In V2, this is a placeholder stub.
 */
class RelayGeminiTransport(
    val relayBaseUrl: String = "https://relay.jarvis-assistant.internal/v1"
) : AiTransport {

    override val transportName: String = "RelayGeminiTransport"

    override suspend fun execute(request: TransportRequest): Result<TransportResponse> {
        return Result.failure(
            JarvisError.Configuration(
                "RelayGeminiTransport is reserved for V15 production serverless proxy deployment."
            )
        )
    }
}
