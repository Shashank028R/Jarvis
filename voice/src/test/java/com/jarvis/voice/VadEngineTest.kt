package com.jarvis.voice

import com.jarvis.voice.vad.EnergyVadEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VadEngineTest {

    private lateinit var vadEngine: EnergyVadEngine

    @Before
    fun setUp() {
        vadEngine = EnergyVadEngine(
            baseThreshold = 1000.0,
            ttsSuppressionOffset = 2000.0,
            speechTriggerFrames = 2,
            silenceHangoverMs = 50L,
            sampleRateHz = 16000
        )
    }

    @Test
    fun initialState_isNotSpeechActive() {
        assertFalse(vadEngine.isSpeechActive)
    }

    @Test
    fun silenceFrames_doNotTriggerSpeech() {
        val silentBuffer = ShortArray(320) { 0 } // 20ms of silence
        repeat(5) {
            val active = vadEngine.processFrame(silentBuffer, silentBuffer.size)
            assertFalse(active)
        }
        assertFalse(vadEngine.isSpeechActive)
    }

    @Test
    fun speechFrames_triggerSpeechAfterConsecutiveFrames() {
        val speechBuffer = ShortArray(320) { 2500 } // high amplitude speech
        // Frame 1: consecutive = 1 < 2
        assertFalse(vadEngine.processFrame(speechBuffer, speechBuffer.size))
        // Frame 2: consecutive = 2 >= 2 -> speech becomes active
        assertTrue(vadEngine.processFrame(speechBuffer, speechBuffer.size))
        assertTrue(vadEngine.isSpeechActive)
    }

    @Test
    fun ttsSuppression_elevatesThreshold() {
        assertEquals(1000.0, vadEngine.currentThreshold, 0.01)

        vadEngine.setTtsActive(true)
        assertEquals(3000.0, vadEngine.currentThreshold, 0.01)

        // Moderate amplitude: 1800 RMS (above 1000 base, but below 3000 TTS threshold)
        val moderateBuffer = ShortArray(320) { 1800 }
        repeat(5) {
            assertFalse(vadEngine.processFrame(moderateBuffer, moderateBuffer.size))
        }

        vadEngine.setTtsActive(false)
        assertEquals(1000.0, vadEngine.currentThreshold, 0.01)
    }

    @Test
    fun silenceAfterSpeech_resetsSpeechActiveAfterHangover() {
        val speechBuffer = ShortArray(320) { 3000 }
        repeat(2) { vadEngine.processFrame(speechBuffer, speechBuffer.size) }
        assertTrue(vadEngine.isSpeechActive)

        // Silence buffer (320 samples @ 16kHz = 20ms per frame)
        val silentBuffer = ShortArray(320) { 0 }
        // 20ms silence
        vadEngine.processFrame(silentBuffer, silentBuffer.size)
        assertTrue(vadEngine.isSpeechActive)

        // 40ms silence
        vadEngine.processFrame(silentBuffer, silentBuffer.size)
        assertTrue(vadEngine.isSpeechActive)

        // 60ms silence >= 50ms hangover -> becomes inactive
        vadEngine.processFrame(silentBuffer, silentBuffer.size)
        assertFalse(vadEngine.isSpeechActive)
    }

    @Test
    fun reset_clearsState() {
        val speechBuffer = ShortArray(320) { 3000 }
        repeat(2) { vadEngine.processFrame(speechBuffer, speechBuffer.size) }
        assertTrue(vadEngine.isSpeechActive)

        vadEngine.reset()
        assertFalse(vadEngine.isSpeechActive)
    }
}
