package com.jarvis.voice.vad

import kotlin.math.sqrt

/**
 * Interface for Voice Activity Detection (VAD).
 */
interface VadEngine {
    val isSpeechActive: Boolean
    fun processFrame(audioData: ShortArray, length: Int): Boolean
    fun setTtsActive(isActive: Boolean)
    fun reset()
}

/**
 * Energy-based Voice Activity Detection engine with dynamic TTS speakerphone loopback suppression.
 *
 * @param baseThreshold Base RMS energy threshold required to classify audio frame as speech.
 * @param ttsSuppressionOffset Extra RMS offset applied while JARVIS is actively speaking aloud to avoid acoustic self-interruption.
 * @param speechTriggerFrames Consecutive frames of energy exceeding threshold before marking speech started.
 * @param silenceHangoverMs Milliseconds of silence after speech before declaring utterance ended.
 */
class EnergyVadEngine(
    private val baseThreshold: Double = 1200.0,
    private val ttsSuppressionOffset: Double = 2500.0,
    private val speechTriggerFrames: Int = 3,
    private val silenceHangoverMs: Long = 1200L,
    private val sampleRateHz: Int = 16000
) : VadEngine {

    private var _isTtsActive = false
    private var _isSpeechActive = false
    override val isSpeechActive: Boolean
        get() = _isSpeechActive

    private var consecutiveSpeechFrames = 0
    private var silenceFrames = 0

    val currentThreshold: Double
        get() = if (_isTtsActive) baseThreshold + ttsSuppressionOffset else baseThreshold

    override fun setTtsActive(isActive: Boolean) {
        _isTtsActive = isActive
    }

    override fun processFrame(audioData: ShortArray, length: Int): Boolean {
        if (length <= 0) return _isSpeechActive

        var sumSquare = 0.0
        for (i in 0 until length) {
            val sample = audioData[i].toDouble()
            sumSquare += sample * sample
        }
        val rms = sqrt(sumSquare / length)

        val threshold = currentThreshold
        val frameHasVoice = rms >= threshold

        if (frameHasVoice) {
            consecutiveSpeechFrames++
            silenceFrames = 0
            if (consecutiveSpeechFrames >= speechTriggerFrames) {
                _isSpeechActive = true
            }
        } else {
            consecutiveSpeechFrames = 0
            if (_isSpeechActive) {
                silenceFrames++
                // Calculate duration of silence from frame size
                val msPerFrame = (length.toDouble() / sampleRateHz.toDouble()) * 1000.0
                val totalSilenceMs = silenceFrames * msPerFrame
                if (totalSilenceMs >= silenceHangoverMs) {
                    _isSpeechActive = false
                }
            }
        }

        return _isSpeechActive
    }

    override fun reset() {
        _isSpeechActive = false
        consecutiveSpeechFrames = 0
        silenceFrames = 0
    }
}
