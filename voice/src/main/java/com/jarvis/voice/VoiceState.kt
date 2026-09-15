package com.jarvis.voice

import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State machine representing the voice pipeline state matching V3 Voice specifications.
 */
enum class VoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    INTERRUPTED,
    ERROR
}

/**
 * Interface controlling voice pipeline state transitions with explicit validation.
 */
interface VoiceStateManager {
    val currentState: StateFlow<VoiceState>
    fun transitionTo(newState: VoiceState): Result<Unit>
    fun reset()
}

/**
 * Thread-safe implementation of [VoiceStateManager] enforcing valid lifecycle transitions.
 */
class DefaultVoiceStateManager : VoiceStateManager {
    private val _currentState = MutableStateFlow(VoiceState.IDLE)
    override val currentState: StateFlow<VoiceState> = _currentState.asStateFlow()

    private val lock = Any()

    override fun transitionTo(newState: VoiceState): Result<Unit> = synchronized(lock) {
        val current = _currentState.value
        if (current == newState) {
            return Result.success(Unit)
        }

        val isValid = when (current) {
            VoiceState.IDLE -> newState in setOf(VoiceState.LISTENING, VoiceState.ERROR)
            VoiceState.LISTENING -> newState in setOf(VoiceState.THINKING, VoiceState.IDLE, VoiceState.ERROR)
            VoiceState.THINKING -> newState in setOf(VoiceState.SPEAKING, VoiceState.IDLE, VoiceState.ERROR)
            VoiceState.SPEAKING -> newState in setOf(VoiceState.IDLE, VoiceState.INTERRUPTED, VoiceState.ERROR)
            VoiceState.INTERRUPTED -> newState in setOf(VoiceState.LISTENING, VoiceState.IDLE, VoiceState.ERROR)
            VoiceState.ERROR -> newState in setOf(VoiceState.IDLE, VoiceState.LISTENING)
        }

        return if (isValid) {
            _currentState.value = newState
            Result.success(Unit)
        } else {
            Result.failure(
                JarvisError.InvalidState("Illegal voice transition from $current to $newState")
            )
        }
    }

    override fun reset() = synchronized(lock) {
        _currentState.value = VoiceState.IDLE
    }
}

