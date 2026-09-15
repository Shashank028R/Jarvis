package com.jarvis.voice.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.jarvis.core.error.JarvisError
import com.jarvis.core.logger.JarvisLogger
import com.jarvis.core.result.Result
import java.util.Locale
import java.util.UUID

/**
 * Interface abstracting Text-to-Speech capabilities.
 */
interface TextToSpeechEngine {
    val isInitialized: Boolean
    val isSpeaking: Boolean
    fun initialize(): Result<Unit>
    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((JarvisError) -> Unit)? = null
    ): Result<Unit>
    fun stop()
    fun shutdown()
}

/**
 * Android implementation of [TextToSpeechEngine] wrapping [TextToSpeech].
 * Manages audio focus (USAGE_ASSISTANT) and custom voice rate/pitch parameters.
 */
class AndroidTextToSpeechEngine(
    private val context: Context,
    private val logger: JarvisLogger? = null,
    private val pitch: Float = 0.92f,
    private val speechRate: Float = 1.02f
) : TextToSpeechEngine, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var _isInitialized = false
    override val isInitialized: Boolean
        get() = _isInitialized

    private var _isSpeaking = false
    override val isSpeaking: Boolean
        get() = _isSpeaking

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var focusRequest: AudioFocusRequest? = null

    // Track active callbacks per utterance
    private val activeCallbacks = mutableMapOf<String, Pair<(() -> Unit)?, (() -> Unit)?>>()
    private val errorCallbacks = mutableMapOf<String, ((JarvisError) -> Unit)?>()

    override fun initialize(): Result<Unit> {
        return try {
            tts = TextToSpeech(context.applicationContext, this)
            Result.success(Unit)
        } catch (e: Throwable) {
            logger?.e("TextToSpeech", { "Failed to instantiate TextToSpeech: ${e.message}" }, e)
            Result.failure(JarvisError.Unknown("Failed to instantiate TextToSpeech: ${e.message}", cause = e))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                engine.language = Locale.getDefault()
                engine.setPitch(pitch)
                engine.setSpeechRate(speechRate)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking = true
                        utteranceId?.let { id ->
                            activeCallbacks[id]?.first?.invoke()
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking = false
                        abandonAudioFocus()
                        utteranceId?.let { id ->
                            activeCallbacks.remove(id)?.second?.invoke()
                            errorCallbacks.remove(id)
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking = false
                        abandonAudioFocus()
                        utteranceId?.let { id ->
                            activeCallbacks.remove(id)
                            errorCallbacks.remove(id)?.invoke(
                                JarvisError.ExecutionFailure("TextToSpeech synthesis error")
                            )
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking = false
                        abandonAudioFocus()
                        utteranceId?.let { id ->
                            activeCallbacks.remove(id)
                            errorCallbacks.remove(id)?.invoke(
                                JarvisError.ExecutionFailure("TextToSpeech synthesis error ($errorCode)")
                            )
                        }
                    }
                })
                _isInitialized = true
                logger?.d("TextToSpeech") { "TextToSpeech initialized successfully" }
            }
        } else {
            _isInitialized = false
            logger?.e("TextToSpeech", { "TextToSpeech initialization failed with status $status" })
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (audioManager == null) return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                            focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                        ) {
                            stop()
                        }
                    }
                    .build()
                focusRequest = request
                audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    { focusChange ->
                        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) stop()
                    },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Throwable) {
            logger?.w("TextToSpeech", { "Audio focus request failed: ${e.message}" })
            true
        }
    }

    private fun abandonAudioFocus() {
        if (audioManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Throwable) {
            logger?.w("TextToSpeech", { "Abandon audio focus failed: ${e.message}" })
        }
    }

    override fun speak(
        text: String,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?,
        onError: ((JarvisError) -> Unit)?
    ): Result<Unit> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(JarvisError.InvalidState("Text to speak cannot be empty"))
        }

        val engine = tts
        if (engine == null || !_isInitialized) {
            return Result.failure(JarvisError.Configuration("TextToSpeech engine is not initialized"))
        }

        requestAudioFocus()

        val utteranceId = UUID.randomUUID().toString()
        activeCallbacks[utteranceId] = Pair(onStart, onDone)
        if (onError != null) {
            errorCallbacks[utteranceId] = onError
        }

        val queueMode = TextToSpeech.QUEUE_FLUSH
        val result = engine.speak(trimmed, queueMode, null, utteranceId)
        return if (result == TextToSpeech.SUCCESS) {
            Result.success(Unit)
        } else {
            abandonAudioFocus()
            activeCallbacks.remove(utteranceId)
            errorCallbacks.remove(utteranceId)
            Result.failure(JarvisError.ExecutionFailure("TTS speak invocation failed ($result)"))
        }
    }

    override fun stop() {
        try {
            tts?.stop()
            _isSpeaking = false
            abandonAudioFocus()
            activeCallbacks.clear()
            errorCallbacks.clear()
        } catch (e: Throwable) {
            logger?.w("TextToSpeech", { "Error stopping TextToSpeech: ${e.message}" })
        }
    }

    override fun shutdown() {
        try {
            stop()
            tts?.shutdown()
            tts = null
            _isInitialized = false
        } catch (e: Throwable) {
            logger?.w("TextToSpeech", { "Error shutting down TextToSpeech: ${e.message}" })
        }
    }
}
