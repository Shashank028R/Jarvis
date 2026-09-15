package com.jarvis.voice

import com.jarvis.core.error.JarvisError
import com.jarvis.core.result.Result
import com.jarvis.voice.aec.AecManager
import com.jarvis.voice.controller.ConversationalIntentDispatcher
import com.jarvis.voice.controller.VoiceInteractionController
import com.jarvis.voice.stt.SpeechToTextEngine
import com.jarvis.voice.tts.TextToSpeechEngine
import com.jarvis.voice.vad.EnergyVadEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSpeechToTextEngine : SpeechToTextEngine {
    override var isAvailable: Boolean = true
    override var isListening: Boolean = false
    var startListeningCount = 0
    var stopCount = 0
    var cancelCount = 0
    var destroyCount = 0

    private var currentOnResult: ((Result<String>) -> Unit)? = null
    private var currentOnPartial: ((String) -> Unit)? = null

    override fun startListening(
        onResult: (Result<String>) -> Unit,
        onPartialResult: ((String) -> Unit)?,
        onRmsChanged: ((Float) -> Unit)?
    ) {
        isListening = true
        startListeningCount++
        currentOnResult = onResult
        currentOnPartial = onPartialResult
    }

    override fun stopListening() {
        isListening = false
        stopCount++
    }

    override fun cancel() {
        isListening = false
        cancelCount++
    }

    override fun destroy() {
        isListening = false
        destroyCount++
    }

    fun emitResult(text: String) {
        isListening = false
        currentOnResult?.invoke(Result.success(text))
    }

    fun emitPartial(text: String) {
        currentOnPartial?.invoke(text)
    }

    fun emitError(error: JarvisError) {
        isListening = false
        currentOnResult?.invoke(Result.failure(error))
    }
}

class FakeTextToSpeechEngine : TextToSpeechEngine {
    override var isInitialized: Boolean = true
    override var isSpeaking: Boolean = false
    var stopCount = 0
    var spokenTexts = mutableListOf<String>()

    private var activeOnStart: (() -> Unit)? = null
    private var activeOnDone: (() -> Unit)? = null
    private var activeOnError: ((JarvisError) -> Unit)? = null

    override fun initialize(): Result<Unit> {
        isInitialized = true
        return Result.success(Unit)
    }

    override fun speak(
        text: String,
        onStart: (() -> Unit)?,
        onDone: (() -> Unit)?,
        onError: ((JarvisError) -> Unit)?
    ): Result<Unit> {
        if (!isInitialized) return Result.failure(JarvisError.Configuration("Not initialized"))
        if (text.isBlank()) return Result.failure(JarvisError.InvalidState("Blank text"))

        isSpeaking = true
        spokenTexts.add(text)
        activeOnStart = onStart
        activeOnDone = onDone
        activeOnError = onError
        onStart?.invoke()
        return Result.success(Unit)
    }

    override fun stop() {
        isSpeaking = false
        stopCount++
    }

    override fun shutdown() {
        isSpeaking = false
        isInitialized = false
    }

    fun finishSpeaking() {
        isSpeaking = false
        activeOnDone?.invoke()
    }

    fun failSpeaking(error: JarvisError) {
        isSpeaking = false
        activeOnError?.invoke(error)
    }
}

class FakeAecManager : AecManager {
    override var isHardwareAecAvailable: Boolean = true
    override var isAttached: Boolean = false
    var attachedSession: Int? = null

    override fun attachToAudioSession(audioSessionId: Int): Result<Unit> {
        isAttached = true
        attachedSession = audioSessionId
        return Result.success(Unit)
    }

    override fun release() {
        isAttached = false
        attachedSession = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class VoicePipelineMocksTest {

    private lateinit var stateManager: DefaultVoiceStateManager
    private lateinit var fakeStt: FakeSpeechToTextEngine
    private lateinit var fakeTts: FakeTextToSpeechEngine
    private lateinit var fakeAec: FakeAecManager
    private lateinit var vadEngine: EnergyVadEngine
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private var lastDispatchedIntent: String? = null
    private var intentResponseToReturn: Result<String> = Result.success("Affirmative, sir.")

    private lateinit var controller: VoiceInteractionController

    @Before
    fun setUp() {
        stateManager = DefaultVoiceStateManager()
        fakeStt = FakeSpeechToTextEngine()
        fakeTts = FakeTextToSpeechEngine()
        fakeAec = FakeAecManager()
        vadEngine = EnergyVadEngine()

        val intentDispatcher = ConversationalIntentDispatcher { intentText ->
            lastDispatchedIntent = intentText
            intentResponseToReturn
        }

        controller = VoiceInteractionController(
            voiceStateManager = stateManager,
            sttEngine = fakeStt,
            ttsEngine = fakeTts,
            aecManager = fakeAec,
            vadEngine = vadEngine,
            intentDispatcher = intentDispatcher,
            coroutineScope = testScope,
            logger = null
        )
    }

    @Test
    fun completeVoiceConversationLoop_succeeds() = runTest(testDispatcher) {
        // Step 1: User taps mic
        controller.onMicTapped()
        assertEquals(VoiceState.LISTENING, controller.voiceState.value)
        assertTrue(fakeStt.isListening)

        // Step 2: Partial transcript arrives
        fakeStt.emitPartial("Hello")
        assertEquals("Hello", controller.partialTranscript.value)

        // Step 3: STT finishes recognition
        fakeStt.emitResult("Hello Jarvis")
        assertEquals("Hello Jarvis", controller.partialTranscript.value)
        assertEquals(VoiceState.THINKING, controller.voiceState.value)

        // Advance coroutines for intent dispatch
        advanceUntilIdle()

        // Step 4: Intent dispatched and AI response returned -> starts speaking
        assertEquals("Hello Jarvis", lastDispatchedIntent)
        assertEquals("Affirmative, sir.", controller.lastAiResponse.value)
        assertEquals(VoiceState.SPEAKING, controller.voiceState.value)
        assertTrue(fakeTts.isSpeaking)

        // Step 5: TTS playback completes
        fakeTts.finishSpeaking()
        assertEquals(VoiceState.IDLE, controller.voiceState.value)
        assertFalse(fakeTts.isSpeaking)
    }

    @Test
    fun bargeInInterruption_haltsTtsImmediately() = runTest(testDispatcher) {
        // Start conversation
        controller.startListening()
        fakeStt.emitResult("Status update")
        advanceUntilIdle()

        assertEquals(VoiceState.SPEAKING, controller.voiceState.value)
        assertTrue(fakeTts.isSpeaking)

        // User barges in / interrupts
        controller.interruptTts()

        assertEquals(1, fakeTts.stopCount)
        assertFalse(fakeTts.isSpeaking)
        assertEquals(VoiceState.IDLE, controller.voiceState.value)
    }

    @Test
    fun sttNoSpeech_resetsToIdleSilently() {
        controller.startListening()
        assertEquals(VoiceState.LISTENING, controller.voiceState.value)

        fakeStt.emitError(JarvisError.ExecutionFailure("No speech recognized", details = "NO_SPEECH"))
        assertEquals(VoiceState.IDLE, controller.voiceState.value)
        assertNull(controller.lastVoiceError.value)
    }

    @Test
    fun sttFatalError_transitionsToErrorState() {
        controller.startListening()
        assertEquals(VoiceState.LISTENING, controller.voiceState.value)

        fakeStt.emitError(JarvisError.Network("Network timeout during speech recognition"))
        assertEquals(VoiceState.ERROR, controller.voiceState.value)
        assertEquals("Network timeout during speech recognition", controller.lastVoiceError.value)
    }

    @Test
    fun ttsError_transitionsToErrorState() = runTest(testDispatcher) {
        controller.startListening()
        fakeStt.emitResult("Say something")
        advanceUntilIdle()

        assertEquals(VoiceState.SPEAKING, controller.voiceState.value)

        fakeTts.failSpeaking(JarvisError.ExecutionFailure("Audio track buffer underrun"))
        assertEquals(VoiceState.ERROR, controller.voiceState.value)
        assertEquals("Audio track buffer underrun", controller.lastVoiceError.value)
    }

    @Test
    fun cancel_resetsAllEnginesAndState() {
        controller.startListening()
        assertEquals(VoiceState.LISTENING, controller.voiceState.value)

        controller.cancel()
        assertEquals(VoiceState.IDLE, controller.voiceState.value)
        assertEquals(1, fakeStt.cancelCount)
        assertEquals(1, fakeTts.stopCount)
    }
}
