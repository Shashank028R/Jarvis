package com.jarvis.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine representing the voice pipeline state matching ADR-002 and V3 Voice specs.
 */
enum class VoiceState {
    DISCONNECTED,
    CONNECTING,
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

/**
 * Interface controlling voice pipeline state transitions.
 */
interface VoiceStateManager {
    val currentState: StateFlow<VoiceState>
    fun transitionTo(newState: VoiceState)
}

/**
 * In-memory implementation of [VoiceStateManager] for V1 foundation.
 */
class DefaultVoiceStateManager : VoiceStateManager {
    private val _currentState = MutableStateFlow(VoiceState.IDLE)
    override val currentState: StateFlow<VoiceState> = _currentState.asStateFlow()

    override fun transitionTo(newState: VoiceState) {
        _currentState.value = newState
    }
}
