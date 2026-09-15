package com.jarvis.voice.controller

import com.jarvis.core.logger.JarvisLogger
import com.jarvis.core.result.Result
import com.jarvis.voice.VoiceState
import com.jarvis.voice.VoiceStateManager
import com.jarvis.voice.aec.AecManager
import com.jarvis.voice.stt.SpeechToTextEngine
import com.jarvis.voice.tts.TextToSpeechEngine
import com.jarvis.voice.vad.VadEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Functional interface decoupling the voice pipeline from specific orchestrator or AI layers.
 */
fun interface ConversationalIntentDispatcher {
    suspend fun dispatchIntent(intentText: String): Result<String>
}

/**
 * Central controller coordinating Voice State, VAD, STT, Intent Dispatch, TTS, and Barge-in Interruption.
 */
class VoiceInteractionController(
    val voiceStateManager: VoiceStateManager,
    val sttEngine: SpeechToTextEngine,
    val ttsEngine: TextToSpeechEngine,
    val aecManager: AecManager,
    val vadEngine: VadEngine,
    private val intentDispatcher: ConversationalIntentDispatcher,
    private val coroutineScope: CoroutineScope,
    private val logger: JarvisLogger? = null
) {
    val voiceState: StateFlow<VoiceState> = voiceStateManager.currentState

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _lastAiResponse = MutableStateFlow<String?>(null)
    val lastAiResponse: StateFlow<String?> = _lastAiResponse.asStateFlow()

    private val _lastVoiceError = MutableStateFlow<String?>(null)
    val lastVoiceError: StateFlow<String?> = _lastVoiceError.asStateFlow()

    /**
     * Toggles voice interaction:
     * - If SPEAKING: interrupts TTS and stops.
     * - If LISTENING: cancels listening.
     * - If IDLE or ERROR: starts listening for speech.
     */
    fun onMicTapped() {
        when (voiceState.value) {
            VoiceState.SPEAKING -> {
                interruptTts()
            }
            VoiceState.LISTENING -> {
                stopListening()
            }
            VoiceState.IDLE, VoiceState.ERROR, VoiceState.INTERRUPTED -> {
                startListening()
            }
            VoiceState.THINKING -> {
                // Ignore tap during active AI reasoning
            }
        }
    }

    /**
     * Starts listening for user speech via STT.
     */
    fun startListening() {
        _partialTranscript.value = ""
        _lastVoiceError.value = null

        val transition = voiceStateManager.transitionTo(VoiceState.LISTENING)
        if (transition.isFailure) {
            logger?.w("VoiceController", { "Cannot transition to LISTENING from ${voiceState.value}" })
            return
        }

        vadEngine.setTtsActive(false)

        sttEngine.startListening(
            onResult = { result ->
                result.fold(
                    onSuccess = { recognizedText ->
                        logger?.d("VoiceController") { "Recognized speech utterance: $recognizedText" }
                        _partialTranscript.value = recognizedText
                        processRecognizedText(recognizedText)
                    },
                    onFailure = { error ->
                        logger?.w("VoiceController", { "STT failure: ${error.message}" })
                        if (error is com.jarvis.core.error.JarvisError.ExecutionFailure && error.details == "NO_SPEECH") {
                            voiceStateManager.transitionTo(VoiceState.IDLE)
                        } else {
                            _lastVoiceError.value = error.message
                            voiceStateManager.transitionTo(VoiceState.ERROR)
                        }
                    }
                )
            },
            onPartialResult = { partial ->
                _partialTranscript.value = partial
            }
        )
    }

    private fun processRecognizedText(text: String) {
        val transition = voiceStateManager.transitionTo(VoiceState.THINKING)
        if (transition.isFailure) return

        coroutineScope.launch {
            val result = intentDispatcher.dispatchIntent(text)
            result.fold(
                onSuccess = { responseText ->
                    _lastAiResponse.value = responseText
                    speakResponse(responseText)
                },
                onFailure = { error ->
                    _lastVoiceError.value = error.message
                    voiceStateManager.transitionTo(VoiceState.ERROR)
                }
            )
        }
    }

    private fun speakResponse(text: String) {
        val transition = voiceStateManager.transitionTo(VoiceState.SPEAKING)
        if (transition.isFailure) return

        vadEngine.setTtsActive(true)

        ttsEngine.speak(
            text = text,
            onStart = {
                logger?.d("VoiceController") { "TTS playback started" }
            },
            onDone = {
                logger?.d("VoiceController") { "TTS playback completed" }
                vadEngine.setTtsActive(false)
                voiceStateManager.transitionTo(VoiceState.IDLE)
            },
            onError = { error ->
                logger?.w("VoiceController", { "TTS error: ${error.message}" })
                vadEngine.setTtsActive(false)
                _lastVoiceError.value = error.message
                voiceStateManager.transitionTo(VoiceState.ERROR)
            }
        )
    }

    /**
     * Barge-in interruption: immediately halts TTS speech output.
     */
    fun interruptTts() {
        if (voiceState.value == VoiceState.SPEAKING) {
            logger?.d("VoiceController") { "Barge-in interruption triggered" }
            ttsEngine.stop()
            vadEngine.setTtsActive(false)
            voiceStateManager.transitionTo(VoiceState.INTERRUPTED)
            voiceStateManager.transitionTo(VoiceState.IDLE)
        }
    }

    /**
     * Cancels active recognition and resets to IDLE.
     */
    fun stopListening() {
        sttEngine.stopListening()
        voiceStateManager.transitionTo(VoiceState.IDLE)
    }

    /**
     * Complete lifecycle cancellation and audio release.
     */
    fun cancel() {
        sttEngine.cancel()
        ttsEngine.stop()
        vadEngine.setTtsActive(false)
        voiceStateManager.reset()
        _partialTranscript.value = ""
    }

    fun clearError() {
        _lastVoiceError.value = null
        if (voiceState.value == VoiceState.ERROR) {
            voiceStateManager.reset()
        }
    }
}
