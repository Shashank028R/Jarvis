package com.jarvis.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.orchestrator.AssistantState
import com.jarvis.orchestrator.JarvisOrchestrator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the V2 AI conversational surface.
 */
data class ConversationUiState(
    val turns: List<ConversationTurn> = emptyList(),
    val assistantState: AssistantState = AssistantState.READY,
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedInput: String? = null
)

/**
 * ViewModel managing the text-based conversational loop for V2.
 * Connects Compose UI to [JarvisOrchestrator] and manages conversation session state.
 */
class JarvisViewModel(
    private val orchestrator: JarvisOrchestrator
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lastFailedInput = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ConversationUiState> = combine(
        orchestrator.session.turns,
        orchestrator.state,
        _inputText,
        _errorMessage,
        _lastFailedInput
    ) { turns, state, input, error, failedInput ->
        ConversationUiState(
            turns = turns,
            assistantState = state,
            inputText = input,
            isProcessing = state == AssistantState.PROCESSING,
            errorMessage = error,
            lastFailedInput = failedInput
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ConversationUiState()
    )


    init {
        orchestrator.initialize()
    }

    fun onInputChanged(newText: String) {
        _inputText.value = newText
        if (_errorMessage.value != null && newText.isNotBlank()) {
            _errorMessage.value = null
        }
    }

    fun sendMessage() {
        val textToSend = _inputText.value.trim()
        if (textToSend.isBlank() || uiState.value.isProcessing) return

        _inputText.value = ""
        _errorMessage.value = null

        viewModelScope.launch {
            val result = orchestrator.handleUserIntent(textToSend)
            result.onFailure { error ->
                _errorMessage.value = error.message
                _lastFailedInput.value = textToSend
            }.onSuccess {
                _lastFailedInput.value = null
            }
        }
    }

    fun retryLast() {
        val failed = _lastFailedInput.value ?: return
        _errorMessage.value = null

        viewModelScope.launch {
            val result = orchestrator.handleUserIntent(failed)
            result.onFailure { error ->
                _errorMessage.value = error.message
            }.onSuccess {
                _lastFailedInput.value = null
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearConversation() {
        orchestrator.resetSession()
        _errorMessage.value = null
        _lastFailedInput.value = null
    }

    companion object {
        fun provideFactory(orchestrator: JarvisOrchestrator): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JarvisViewModel(orchestrator) as T
                }
            }
        }
    }
}
