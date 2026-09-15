package com.jarvis.ai.client

import com.jarvis.ai.AiMessage
import com.jarvis.ai.AiRole
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.PlannerContext
import com.jarvis.ai.model.PlannerDecision
import com.jarvis.ai.model.TurnRole
import com.jarvis.ai.transport.AiTransport
import com.jarvis.ai.transport.TransportRequest
import com.jarvis.ai.transport.TransportResponse
import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiAiClientTest {

    private val mockTransport = mockk<AiTransport>()
    private val client = GeminiAiClient(transport = mockTransport, model = "gemini-1.5-flash")

    @Test
    fun `converse returns clean text response on valid JSON`() = runTest {
        val validJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "Hello Sir. All systems are operational." }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ]
            }
        """.trimIndent()

        coEvery { mockTransport.execute(any()) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = validJson)
        )

        val input = ConversationTurn(role = TurnRole.USER, text = "Status report")
        val context = PlannerContext(recentTurns = emptyList())

        val result = client.converse(input, context)

        assertTrue(result.isSuccess)
        assertEquals("Hello Sir. All systems are operational.", result.getOrThrow().text)
    }

    @Test
    fun `converse properly serializes system instruction and multi-turn context`() = runTest {
        val capturedRequest = slot<TransportRequest>()
        val dummyResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "Acknowledged." }],
                    "role": "model"
                  }
                }
              ]
            }
        """.trimIndent()

        coEvery { mockTransport.execute(capture(capturedRequest)) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = dummyResponse)
        )

        val turn1 = ConversationTurn(role = TurnRole.USER, text = "Turn 1")
        val turn2 = ConversationTurn(role = TurnRole.ASSISTANT, text = "Turn 2")
        val current = ConversationTurn(role = TurnRole.USER, text = "Turn 3")
        val context = PlannerContext(
            recentTurns = listOf(turn1, turn2),
            systemInstruction = "Custom system directive"
        )

        val result = client.converse(current, context)

        assertTrue(result.isSuccess)

        // Verify JSON payload structure
        val requestJson = JSONObject(capturedRequest.captured.bodyJson)
        val systemInstruction = requestJson.getJSONObject("systemInstruction")
        val systemText = systemInstruction.getJSONArray("parts").getJSONObject(0).getString("text")
        assertEquals("Custom system directive", systemText)

        val contents = requestJson.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("Turn 1", contents.getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals("Turn 2", contents.getJSONObject(1).getJSONArray("parts").getJSONObject(0).getString("text"))
        assertEquals("user", contents.getJSONObject(2).getString("role"))
        assertEquals("Turn 3", contents.getJSONObject(2).getJSONArray("parts").getJSONObject(0).getString("text"))
    }

    @Test
    fun `transport failure is propagated unmodified`() = runTest {
        val networkError = JarvisError.Network("DNS resolution failure", isTransient = true)
        coEvery { mockTransport.execute(any()) } returns Result.failure(networkError)

        val input = ConversationTurn(role = TurnRole.USER, text = "Hello")
        val result = client.converse(input, PlannerContext())

        assertTrue(result.isFailure)
        assertEquals(networkError, result.errorOrNull())
    }

    @Test
    fun `empty candidates list returns Serialization error`() = runTest {
        val emptyCandidatesJson = "{\"candidates\":[]}"
        coEvery { mockTransport.execute(any()) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = emptyCandidatesJson)
        )

        val result = client.converse(ConversationTurn(role = TurnRole.USER, text = "Hello"), PlannerContext())

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is JarvisError.Serialization)
    }

    @Test
    fun `error field in JSON response returns Network error`() = runTest {
        val errorJson = """
            {
              "error": {
                "code": 403,
                "message": "The caller does not have permission"
              }
            }
        """.trimIndent()

        coEvery { mockTransport.execute(any()) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = errorJson)
        )

        val result = client.converse(ConversationTurn(role = TurnRole.USER, text = "Hello"), PlannerContext())

        assertTrue(result.isFailure)
        val error = result.errorOrNull() as JarvisError.Network
        assertEquals(403, error.code)
        assertTrue(error.message.contains("permission"))
    }

    @Test
    fun `interpret maps converse output to ConversationalReply`() = runTest {
        val replyJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "Certainly, Sir." }],
                    "role": "model"
                  }
                }
              ]
            }
        """.trimIndent()

        coEvery { mockTransport.execute(any()) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = replyJson)
        )

        val result = client.interpret(ConversationTurn(role = TurnRole.USER, text = "Hello"), PlannerContext())

        assertTrue(result.isSuccess)
        val decision = result.getOrThrow()
        assertTrue(decision is PlannerDecision.ConversationalReply)
        assertEquals("Certainly, Sir.", (decision as PlannerDecision.ConversationalReply).replyText)
    }

    @Test
    fun `generateResponse adapts AiMessage to ConversationTurn successfully`() = runTest {
        val replyJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "At your service." }],
                    "role": "model"
                  }
                }
              ]
            }
        """.trimIndent()

        coEvery { mockTransport.execute(any()) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = replyJson)
        )

        val messages = listOf(
            AiMessage(role = AiRole.SYSTEM, content = "Act calm."),
            AiMessage(role = AiRole.USER, content = "Hello JARVIS")
        )

        val result = client.generateResponse(messages)

        assertTrue(result.isSuccess)
        assertEquals("At your service.", result.getOrThrow().content)
    }

    @Test
    fun `isDeepThinkingRequested identifies trigger phrases correctly`() {
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Please use deep think for this"))
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Think deeply about this problem"))
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Give me the answer accurately"))
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Answer accurately please"))
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Explain accurately with deep reasoning"))
        assertTrue(GeminiAiClient.isDeepThinkingRequested("Switch to a higher model"))

        org.junit.Assert.assertFalse(GeminiAiClient.isDeepThinkingRequested("Hello Jarvis"))
        org.junit.Assert.assertFalse(GeminiAiClient.isDeepThinkingRequested("What time is it?"))
    }

    @Test
    fun `converse sets thinkingConfig budget and lower temperature when deep thinking requested`() = runTest {
        val capturedRequest = slot<TransportRequest>()
        val dummyResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [{ "text": "Deep thinking analysis complete, Sir." }],
                    "role": "model"
                  }
                }
              ]
            }
        """.trimIndent()

        coEvery { mockTransport.execute(capture(capturedRequest)) } returns Result.success(
            TransportResponse(statusCode = 200, bodyJson = dummyResponse)
        )

        val input = ConversationTurn(role = TurnRole.USER, text = "Deep think: what is the meaning of life?")
        val result = client.converse(input, PlannerContext())

        assertTrue(result.isSuccess)
        assertEquals("Deep thinking analysis complete, Sir.", result.getOrThrow().text)

        val payload = JSONObject(capturedRequest.captured.bodyJson)
        val genConfig = payload.getJSONObject("generationConfig")
        assertEquals(0.4, genConfig.getDouble("temperature"), 0.001)
        assertEquals(2048, genConfig.getInt("maxOutputTokens"))

        val thinkingConfig = genConfig.getJSONObject("thinkingConfig")
        assertEquals(2048, thinkingConfig.getInt("thinkingBudget"))
    }
}
