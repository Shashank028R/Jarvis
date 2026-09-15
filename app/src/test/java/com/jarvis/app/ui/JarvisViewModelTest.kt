package com.jarvis.app.ui

import com.jarvis.ai.model.ConversationTurn
import com.jarvis.ai.model.TurnRole
import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import com.jarvis.orchestrator.AssistantState
import com.jarvis.orchestrator.JarvisOrchestrator
import com.jarvis.orchestrator.session.ConversationSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JarvisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mockOrchestrator = mockk<JarvisOrchestrator>(relaxed = true)
    private val mockSession = mockk<ConversationSession>(relaxed = true)
    private val turnsFlow = MutableStateFlow<List<ConversationTurn>>(emptyList())
    private val stateFlow = MutableStateFlow(AssistantState.READY)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { mockOrchestrator.session } returns mockSession
        every { mockSession.turns } returns turnsFlow
        every { mockOrchestrator.state } returns stateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and initializes orchestrator`() = runTest {
        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()
        verify { mockOrchestrator.initialize() }

        val state = viewModel.uiState.value
        assertTrue(state.turns.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isProcessing)
        assertNull(state.errorMessage)
    }

    @Test
    fun `input changes update inputText and clear existing error`() = runTest {
        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()

        viewModel.onInputChanged("Hello JARVIS")
        advanceUntilIdle()

        assertEquals("Hello JARVIS", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage dispatches intent and clears input text`() = runTest {
        coEvery { mockOrchestrator.handleUserIntent(any()) } returns Result.success("Greetings, Sir.")

        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()

        viewModel.onInputChanged("Status report")
        advanceUntilIdle()

        viewModel.sendMessage()
        advanceUntilIdle()

        // Input should be cleared
        assertEquals("", viewModel.uiState.value.inputText)

        coVerify { mockOrchestrator.handleUserIntent("Status report") }
        assertNull(viewModel.uiState.value.errorMessage)

    }

    @Test
    fun `sendMessage error captures errorMessage and lastFailedInput`() = runTest {
        val configError = JarvisError.Configuration("Gemini API key is not configured.")
        coEvery { mockOrchestrator.handleUserIntent(any()) } returns Result.failure(configError)

        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()

        viewModel.onInputChanged("Test prompt")
        advanceUntilIdle()

        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals("Configuration error: Gemini API key is not configured.", viewModel.uiState.value.errorMessage)
        assertEquals("Test prompt", viewModel.uiState.value.lastFailedInput)
    }

    @Test
    fun `retryLast resubmits failed intent`() = runTest {
        val configError = JarvisError.Configuration("Temporary failure")
        coEvery { mockOrchestrator.handleUserIntent("Prompt A") } returnsMany listOf(
            Result.failure(configError),
            Result.success("Success on retry")
        )

        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()

        viewModel.onInputChanged("Prompt A")
        advanceUntilIdle()

        viewModel.sendMessage()
        advanceUntilIdle()

        assertEquals("Prompt A", viewModel.uiState.value.lastFailedInput)

        viewModel.retryLast()
        advanceUntilIdle()

        coVerify(exactly = 2) { mockOrchestrator.handleUserIntent("Prompt A") }
        assertNull(viewModel.uiState.value.lastFailedInput)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearConversation resets session in orchestrator`() = runTest {
        val viewModel = JarvisViewModel(mockOrchestrator)
        advanceUntilIdle()

        viewModel.clearConversation()
        advanceUntilIdle()

        verify { mockOrchestrator.resetSession() }
        assertNull(viewModel.uiState.value.errorMessage)
    }
}

