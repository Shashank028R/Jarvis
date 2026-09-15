package com.jarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceStateTest {

    private lateinit var stateManager: DefaultVoiceStateManager

    @Before
    fun setUp() {
        stateManager = DefaultVoiceStateManager()
    }

    @Test
    fun initialState_isIdle() {
        assertEquals(VoiceState.IDLE, stateManager.currentState.value)
    }

    @Test
    fun validTransitions_succeed() {
        // IDLE -> LISTENING
        assertTrue(stateManager.transitionTo(VoiceState.LISTENING).isSuccess)
        assertEquals(VoiceState.LISTENING, stateManager.currentState.value)

        // LISTENING -> THINKING
        assertTrue(stateManager.transitionTo(VoiceState.THINKING).isSuccess)
        assertEquals(VoiceState.THINKING, stateManager.currentState.value)

        // THINKING -> SPEAKING
        assertTrue(stateManager.transitionTo(VoiceState.SPEAKING).isSuccess)
        assertEquals(VoiceState.SPEAKING, stateManager.currentState.value)

        // SPEAKING -> INTERRUPTED
        assertTrue(stateManager.transitionTo(VoiceState.INTERRUPTED).isSuccess)
        assertEquals(VoiceState.INTERRUPTED, stateManager.currentState.value)

        // INTERRUPTED -> IDLE
        assertTrue(stateManager.transitionTo(VoiceState.IDLE).isSuccess)
        assertEquals(VoiceState.IDLE, stateManager.currentState.value)
    }

    @Test
    fun invalidTransition_failsAndPreservesState() {
        // IDLE -> SPEAKING is invalid
        val result = stateManager.transitionTo(VoiceState.SPEAKING)
        assertTrue(result.isFailure)
        assertEquals(VoiceState.IDLE, stateManager.currentState.value)
    }

    @Test
    fun transitionToError_isAlwaysPermitted() {
        stateManager.transitionTo(VoiceState.LISTENING)
        val result = stateManager.transitionTo(VoiceState.ERROR)
        assertTrue(result.isSuccess)
        assertEquals(VoiceState.ERROR, stateManager.currentState.value)

        // From ERROR back to IDLE
        assertTrue(stateManager.transitionTo(VoiceState.IDLE).isSuccess)
        assertEquals(VoiceState.IDLE, stateManager.currentState.value)
    }

    @Test
    fun reset_returnsToIdle() {
        stateManager.transitionTo(VoiceState.LISTENING)
        stateManager.transitionTo(VoiceState.THINKING)
        assertEquals(VoiceState.THINKING, stateManager.currentState.value)

        stateManager.reset()
        assertEquals(VoiceState.IDLE, stateManager.currentState.value)
    }
}
