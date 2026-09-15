package com.jarvis.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.model.ConversationTurn
import com.jarvis.orchestrator.AssistantState
import com.jarvis.orchestrator.JarvisOrchestrator
import com.jarvis.voice.VoiceState
import com.jarvis.voice.controller.VoiceInteractionController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the V2 AI conversational and V3 voice interaction surface.
 */
data class ConversationUiState(
    val turns: List<ConversationTurn> = emptyList(),
    val assistantState: AssistantState = AssistantState.READY,
    val inputText: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val lastFailedInput: String? = null,
    val voiceState: VoiceState = VoiceState.IDLE,
    val partialTranscript: String = "",
    val voiceError: String? = null
)

/**
 * ViewModel managing the text-based conversational loop for V2 and voice pipeline for V3.
 * Connects Compose UI to [JarvisOrchestrator] and [VoiceInteractionController].
 */
class JarvisViewModel(
    private val orchestrator: JarvisOrchestrator,
    private val voiceController: VoiceInteractionController? = null
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _lastFailedInput = MutableStateFlow<String?>(null)

    private val baseUiFlow = combine(
        orchestrator.session.turns,
        orchestrator.state,
        _inputText,
        _errorMessage,
        _lastFailedInput
    ) { turns, state, input, error, failedInput ->
        Tuple5(turns, state, input, error, failedInput)
    }

    val uiState: StateFlow<ConversationUiState> = if (voiceController != null) {
        combine(
            baseUiFlow,
            voiceController.voiceState,
            voiceController.partialTranscript,
            voiceController.lastVoiceError
        ) { base, vState, transcript, vError ->
            ConversationUiState(
                turns = base.t1,
                assistantState = base.t2,
                inputText = base.t3,
                isProcessing = base.t2 == AssistantState.PROCESSING || vState == VoiceState.THINKING,
                errorMessage = base.t4,
                lastFailedInput = base.t5,
                voiceState = vState,
                partialTranscript = transcript,
                voiceError = vError
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ConversationUiState()
        )
    } else {
        baseUiFlow.combine(MutableStateFlow(Unit)) { base, _ ->
            ConversationUiState(
                turns = base.t1,
                assistantState = base.t2,
                inputText = base.t3,
                isProcessing = base.t2 == AssistantState.PROCESSING,
                errorMessage = base.t4,
                lastFailedInput = base.t5
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ConversationUiState()
        )
    }

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

        // If voice is currently speaking, barge in and stop TTS
        voiceController?.interruptTts()

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

    fun onMicTapped() {
        voiceController?.onMicTapped()
    }

    fun interruptVoice() {
        voiceController?.interruptTts()
    }

    fun cancelVoice() {
        voiceController?.cancel()
    }

    fun clearVoiceError() {
        voiceController?.clearError()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearConversation() {
        voiceController?.cancel()
        orchestrator.resetSession()
        _errorMessage.value = null
        _lastFailedInput.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceController?.cancel()
    }

    private data class Tuple5<T1, T2, T3, T4, T5>(
        val t1: T1,
        val t2: T2,
        val t3: T3,
        val t4: T4,
        val t5: T5
    )

    companion object {
        fun provideFactory(
            orchestrator: JarvisOrchestrator,
            voiceController: VoiceInteractionController? = null
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return JarvisViewModel(orchestrator, voiceController) as T
                }
            }
        }
    }
}
