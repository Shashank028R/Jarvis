package com.jarvis.orchestrator

import com.jarvis.accessibility.AccessibilityBridge
import com.jarvis.ai.client.AiClient
import com.jarvis.ai.model.TextResponse
import com.jarvis.androidintegration.SystemAdapter
import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import com.jarvis.memory.MemoryStore
import com.jarvis.orchestrator.session.DefaultConversationSession
import com.jarvis.security.SecurityPolicyEngine
import com.jarvis.tools.ToolRegistry
import com.jarvis.voice.VoiceStateManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultJarvisOrchestratorTest {

    private val mockSecurity = mockk<SecurityPolicyEngine>(relaxed = true)
    private val mockTools = mockk<ToolRegistry>(relaxed = true)
    private val mockVoice = mockk<VoiceStateManager>(relaxed = true)
    private val mockSystem = mockk<SystemAdapter>(relaxed = true)
    private val mockAccessibility = mockk<AccessibilityBridge>(relaxed = true)
    private val mockMemory = mockk<MemoryStore>(relaxed = true)
    private val mockAiClient = mockk<AiClient>()

    private fun createOrchestrator(aiClient: AiClient? = mockAiClient): DefaultJarvisOrchestrator {
        return DefaultJarvisOrchestrator(
            securityPolicyEngine = mockSecurity,
            toolRegistry = mockTools,
            voiceStateManager = mockVoice,
            systemAdapter = mockSystem,
            accessibilityBridge = mockAccessibility,
            memoryStore = mockMemory,
            aiClient = aiClient,
            session = DefaultConversationSession(maxTurns = 10)
        )
    }

    @Test
    fun `initialize sets state to READY`() {
        val orchestrator = createOrchestrator()
        val result = orchestrator.initialize()

        assertTrue(result.isSuccess)
        assertEquals(AssistantState.READY, orchestrator.state.value)
    }

    @Test
    fun `blank user intent returns InvalidState error`() = runTest {
        val orchestrator = createOrchestrator()
        val result = orchestrator.handleUserIntent("   ")

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is JarvisError.InvalidState)
    }

    @Test
    fun `missing AI client returns Configuration error`() = runTest {
        val orchestrator = createOrchestrator(aiClient = null)
        val result = orchestrator.handleUserIntent("Hello")

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is JarvisError.Configuration)
        assertEquals(AssistantState.ERROR, orchestrator.state.value)
    }

    @Test
    fun `successful AI converse flow records turns and returns text`() = runTest {
        val orchestrator = createOrchestrator()
        orchestrator.initialize()

        coEvery { mockAiClient.converse(any(), any()) } returns Result.success(
            TextResponse("Good evening, Sir.")
        )

        val result = orchestrator.handleUserIntent("Hello JARVIS")

        assertTrue(result.isSuccess)
        assertEquals("Good evening, Sir.", result.getOrThrow())
        assertEquals(AssistantState.READY, orchestrator.state.value)

        val turns = orchestrator.session.getRecentTurns()
        assertEquals(2, turns.size)
        assertEquals("Hello JARVIS", turns[0].text)
        assertEquals("Good evening, Sir.", turns[1].text)
    }

    @Test
    fun `AI converse failure transitions to ERROR and propagates error`() = runTest {
        val orchestrator = createOrchestrator()
        orchestrator.initialize()

        val networkError = JarvisError.Network("Connection refused", isTransient = true)
        coEvery { mockAiClient.converse(any(), any()) } returns Result.failure(networkError)

        val result = orchestrator.handleUserIntent("Hello JARVIS")

        assertTrue(result.isFailure)
        assertEquals(networkError, result.errorOrNull())
        assertEquals(AssistantState.ERROR, orchestrator.state.value)
    }

    @Test
    fun `resetSession clears history and sets state to READY`() = runTest {
        val orchestrator = createOrchestrator()
        orchestrator.initialize()

        coEvery { mockAiClient.converse(any(), any()) } returns Result.success(
            TextResponse("Yes, Sir.")
        )
        orchestrator.handleUserIntent("Test")
        assertEquals(2, orchestrator.session.getRecentTurns().size)

        orchestrator.resetSession()

        assertTrue(orchestrator.session.getRecentTurns().isEmpty())
        assertEquals(AssistantState.READY, orchestrator.state.value)
    }
}
