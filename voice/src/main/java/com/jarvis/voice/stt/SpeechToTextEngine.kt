package com.jarvis.voice.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.jarvis.core.error.JarvisError
import com.jarvis.core.logger.JarvisLogger
import com.jarvis.core.result.Result
import java.util.Locale

/**
 * Interface abstracting Speech-to-Text capabilities.
 */
interface SpeechToTextEngine {
    val isAvailable: Boolean
    val isListening: Boolean
    fun startListening(
        onResult: (Result<String>) -> Unit,
        onPartialResult: ((String) -> Unit)? = null,
        onRmsChanged: ((Float) -> Unit)? = null
    )
    fun stopListening()
    fun cancel()
    fun destroy()
}

/**
 * Android implementation of [SpeechToTextEngine] wrapping [SpeechRecognizer].
 * Guarantees all recognizer lifecycle calls execute on the Main looper.
 */
class AndroidSpeechToTextEngine(
    private val context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    private val logger: JarvisLogger? = null
) : SpeechToTextEngine {

    private var speechRecognizer: SpeechRecognizer? = null
    private var _isListening = false
    override val isListening: Boolean
        get() = _isListening

    override val isAvailable: Boolean
        get() = try {
            SpeechRecognizer.isRecognitionAvailable(context)
        } catch (e: Throwable) {
            logger?.w("SpeechToText", { "Failed to check isRecognitionAvailable: ${e.message}" })
            false
        }

    private fun ensureRecognizerOnMainThread(): SpeechRecognizer {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
        return speechRecognizer!!
    }

    override fun startListening(
        onResult: (Result<String>) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onRmsChanged: ((Float) -> Unit)?
    ) {
        mainHandler.post {
            try {
                if (!isAvailable) {
                    onResult(Result.failure(JarvisError.Configuration("Speech recognition service is unavailable on this device")))
                    return@post
                }

                val recognizer = ensureRecognizerOnMainThread()
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        logger?.d("SpeechToText") { "Ready for speech" }
                    }

                    override fun onBeginningOfSpeech() {
                        logger?.d("SpeechToText") { "User began speaking" }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsChanged?.invoke(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        logger?.d("SpeechToText") { "End of speech detected" }
                        _isListening = false
                    }

                    override fun onError(error: Int) {
                        _isListening = false
                        val mappedError = when (error) {
                            SpeechRecognizer.ERROR_AUDIO ->
                                JarvisError.ExecutionFailure("Audio recording error ($error)")
                            SpeechRecognizer.ERROR_CLIENT ->
                                JarvisError.ExecutionFailure("Speech recognition client error ($error)")
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                JarvisError.Permission(
                                    permission = "android.permission.RECORD_AUDIO",
                                    message = "Microphone permission denied"
                                )
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                JarvisError.Network("Network failure during speech recognition ($error)")
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                JarvisError.ExecutionFailure("No speech recognized", details = "NO_SPEECH")
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                JarvisError.ExecutionFailure("Speech recognizer busy ($error)")
                            SpeechRecognizer.ERROR_SERVER ->
                                JarvisError.Network("Speech recognition server error ($error)")
                            else ->
                                JarvisError.ExecutionFailure("Speech recognition error ($error)")
                        }
                        logger?.w("SpeechToText", { "Speech recognition error: ${mappedError.message}" })
                        onResult(Result.failure(mappedError))
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            logger?.d("SpeechToText") { "Speech recognized successfully" }
                            onResult(Result.success(text))
                        } else {
                            onResult(Result.failure(JarvisError.ExecutionFailure("No speech detected", details = "NO_SPEECH")))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            onPartialResult?.invoke(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }

                recognizer.startListening(intent)
                _isListening = true
                logger?.d("SpeechToText") { "SpeechRecognizer listening started" }
            } catch (e: Throwable) {
                _isListening = false
                logger?.e("SpeechToText", { "Failed to start speech recognition: ${e.message}" }, e)
                onResult(Result.failure(JarvisError.Unknown("Failed to start speech recognition: ${e.message}", cause = e)))
            }
        }
    }

    override fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening = false
            } catch (e: Throwable) {
                logger?.w("SpeechToText", { "Error stopping SpeechRecognizer: ${e.message}" })
            }
        }
    }

    override fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _isListening = false
            } catch (e: Throwable) {
                logger?.w("SpeechToText", { "Error cancelling SpeechRecognizer: ${e.message}" })
            }
        }
    }

    override fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (e: Throwable) {
                logger?.w("SpeechToText", { "Error destroying SpeechRecognizer: ${e.message}" })
            } finally {
                speechRecognizer = null
                _isListening = false
            }
        }
    }
}
