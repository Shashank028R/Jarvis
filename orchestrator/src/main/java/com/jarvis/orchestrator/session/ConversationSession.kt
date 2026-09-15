package com.jarvis.orchestrator.session

import com.jarvis.ai.model.ConversationTurn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages conversational context window per session.
 * Enforces bounded turn history (default 10 turns) to control context size and token consumption.
 * Specified in 07_AI_ARCHITECTURE.md and versions/V2_AI_CONVERSATION.md.
 */
interface ConversationSession {
    val turns: StateFlow<List<ConversationTurn>>
    val maxTurns: Int

    fun addTurn(turn: ConversationTurn)
    fun getRecentTurns(): List<ConversationTurn>
    fun clear()
}

/**
 * Thread-safe default implementation of [ConversationSession] with FIFO windowing.
 */
class DefaultConversationSession(
    override val maxTurns: Int = DEFAULT_MAX_TURNS
) : ConversationSession {

    companion object {
        const val DEFAULT_MAX_TURNS = 10
    }

    private val lock = Any()
    private val _turns = MutableStateFlow<List<ConversationTurn>>(emptyList())
    override val turns: StateFlow<List<ConversationTurn>> = _turns.asStateFlow()

    override fun addTurn(turn: ConversationTurn) {
        synchronized(lock) {
            val currentList = _turns.value.toMutableList()
            currentList.add(turn)
            if (currentList.size > maxTurns) {
                _turns.value = currentList.takeLast(maxTurns)
            } else {
                _turns.value = currentList
            }
        }
    }

    override fun getRecentTurns(): List<ConversationTurn> {
        return _turns.value
    }

    override fun clear() {
        synchronized(lock) {
            _turns.value = emptyList()
        }
    }
}
