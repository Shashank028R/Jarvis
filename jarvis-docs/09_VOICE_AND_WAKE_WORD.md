# 09 — Voice System & Wake Word

## Voice State Machine

```
Idle → Listening → Thinking → Speaking → (Idle | Listening on interruption)
```

Every UI surface (activation animation, idle indicator) is a pure function of this state, owned by
the `:voice` module and observed via `StateFlow` by the `:app` module.

## Speech-to-Text

- **Baseline (V3)**: Android `SpeechRecognizer` with on-device recognition where the OEM provides
  it (`RecognizerIntent.EXTRA_PREFER_OFFLINE`), cloud recognition as fallback. This keeps V3
  shippable without a dependency on a specific cloud STT vendor.
  - Requires `RECORD_AUDIO` runtime permission.
  - Must run inside a foreground service with the `microphone` foreground-service type when
    listening continues while the app is not the foreground activity (Android 14+ requirement).
- **Quality upgrade (later, ADR-gated)**: evaluate a higher-accuracy cloud STT if the baseline
  proves insufficient for conversational (not just command) input; tracked as a decision, not
  assumed.

## Text-to-Speech

- **Baseline (V3)**: Android `TextToSpeech` system engine, voice/pitch/rate tuned toward the "deep,
  calm, slightly futuristic" direction in `03_USER_EXPERIENCE.md` within what the system engine's
  parameters allow.
- **Upgrade path**: a neural TTS (cloud-based) can be evaluated once V3 is stable, as an ADR,
  because it changes latency and offline characteristics materially.
- TTS playback must request appropriate audio focus (`USAGE_ASSISTANT` / `CONTENT_TYPE_SPEECH`)
  so it behaves correctly alongside music/media the user may have playing.

## Voice Activity Detection & Interruption (AEC Mandate)

VAD determines end-of-utterance for STT segmentation and detects when the user starts speaking
while JARVIS is mid-response (barge-in), which immediately halts TTS playback and transitions
to `Listening`. 

**Critical Acoustic Echo Cancellation (AEC) Requirement**:
When JARVIS speaks through the device speaker, acoustic feedback into the open microphone will trigger naive energy-based VAD, causing continuous self-interruption. To prevent this:
1. **Hardware AEC**: Enable Android's `android.media.audiofx.AcousticEchoCanceler` on the `AudioRecord` session whenever `AcousticEchoCanceler.isAvailable()` is true.
2. **Audio-Focus Duck-Back & Loopback Suppression**: Implement software reference subtraction and a dynamic threshold offset while TTS is actively streaming to the speaker.
3. **Model-Assisted VAD**: Transition from simple amplitude thresholds to an efficient spectral/energy classifier (or lightweight ONNX Silero VAD) to differentiate human speech from speaker leakage.

## Wake Word — Realistic Architecture

Per `06_ANDROID_CAPABILITIES.md`, there is no free lunch here. Two tiers are designed:

**Tier 1 (V5 baseline) — Explicit foreground listening session.**
The user taps to start a listening session (or it is started via a widget/notification action),
after which a foreground service with `microphone` type keeps the mic open and a *local*
keyword-spotting model listens for "Jarvis" to trigger full activation, with a persistent
notification indicating the mic is active (required — cannot be hidden). This is honest about
battery/privacy trade-offs and does not claim silent, indefinite background listening.

**Tier 2 (opt-in enhancement, gated behind holding the Assistant role) — System-level hotword.**
If JARVIS holds `android.app.role.ASSISTANT` and the device exposes `AlwaysOnHotwordDetector`
hardware DSP support, true low-power always-on detection becomes possible through the
`VoiceInteractionService` path. This is documented as **best-effort and device-dependent**, not a
baseline guarantee, given: (a) hardware DSP hotword support varies by OEM/chipset, (b) holding the
Assistant role means competing with the OS default assistant, and (c) reports of the OS clearing
assistant role Secure Settings on app reinstall, requiring a recovery flow.

## Local Keyword Spotting (Tier 1) — Technology Choice

Tier 1 utilizes **ONNX Runtime Mobile** executing a quantized, single-keyword "Jarvis" model (derived from openWakeWord or custom TFLite micro-speech architecture). This provides deterministic inference latency (<100ms per audio chunk) with minimal CPU footprint (<5% battery draw over an 8-hour session) without cloud audio streaming (Rule 7). Only audio captured after a verified wake-word trigger is passed to the STT pipeline. Raw pre-wake-word audio is continuously overwritten in a volatile 3-second circular PCM ring buffer and never persisted to disk or transmitted off-device.

## Permissions & Privacy Summary

| Concern | Handling |
|---|---|
| `RECORD_AUDIO` | Runtime permission, requested at first voice-feature use, not at install |
| Background/continuous mic | Only inside a foreground service with a persistent, honest notification; never silent |
| Raw audio retention | Not persisted beyond the active recognition buffer unless the user has explicitly opted into "save recordings for debugging" |
| Wake-word audio | Processed locally; only the post-wake-word utterance may leave the device (and only for cloud STT, if that path is enabled) |

## Battery Considerations

Continuous foreground-service mic capture is measured against a defined budget in
`17_PERFORMANCE.md`; V5's acceptance criteria include a battery-drain benchmark, not just "wake
word works."
