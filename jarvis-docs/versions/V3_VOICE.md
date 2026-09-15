# V3 — Voice Interaction

## Purpose
Add spoken input/output on top of the V2 conversation loop: STT, TTS, VAD, interruption.

## Goals
Implement the `Idle → Listening → Thinking → Speaking` state machine in `:voice`
(`09_VOICE_AND_WAKE_WORD.md`) and wire it to the existing `AiClient.converse()` path.

## Features
- Tap-to-talk voice input (no wake word yet — that's V5).
- STT via Android `SpeechRecognizer`.
- TTS via Android `TextToSpeech`, tuned per `03_USER_EXPERIENCE.md` voice direction.
- Barge-in interruption: user speaking during JARVIS's TTS stops playback and starts listening.

## Dependencies
V2's `AiClient`/orchestrator.

## Architectural Changes
`:voice` module becomes real; `:orchestrator` now receives input from either voice or the V2 text
UI, unified through the same `ConversationTurn` model.

## New Components
- `VoiceStateMachine` (StateFlow-based).
- `SpeechToTextEngine` wrapper around `SpeechRecognizer`.
- `TextToSpeechEngine` wrapper around `TextToSpeech` with audio-focus handling
  (`USAGE_ASSISTANT`/`CONTENT_TYPE_SPEECH`).
- `AudioRecordAecWrapper`: manages hardware `AcousticEchoCanceler` and dynamic threshold duck-back suppression.
- VAD engine for end-of-utterance and barge-in detection.

## User Experience
User taps a mic button, speaks, sees/hears JARVIS respond aloud; can interrupt mid-response.

## Permissions
`RECORD_AUDIO` (runtime, requested at first use with a clear rationale shown first).

## Security Considerations
Audio is not persisted beyond the active recognition buffer (`09_VOICE_AND_WAKE_WORD.md`).

## Testing
- Unit: state machine transition correctness (including interruption edge cases).
- Integration: STT/TTS wrapped engines against Robolectric-simulated Android voice APIs.
- Instrumentation: real-device test of a fixed utterance set for STT accuracy baseline.
- Acoustic Loopback Test: TTS plays a full paragraph at maximum speaker volume in a quiet room while mic is open; assert zero self-interruptions occur.
- Manual acceptance: interruption feels responsive (< defined latency budget).

## Acceptance Criteria
- [ ] Tap-to-talk round trip works reliably on at least two real devices.
- [ ] Barge-in interrupts TTS within the latency budget in `17_PERFORMANCE.md`.
- [ ] TTS playback through device speaker does not falsely trigger barge-in self-interruption.
- [ ] `RECORD_AUDIO` denial is handled gracefully (falls back to text UI, no crash).
- [ ] No raw audio persisted to disk without explicit opt-in.

## Known Limitations
No wake word (manual trigger only); STT accuracy bound by the on-device/system recognizer quality.

## Exit Criteria
Acceptance criteria met; V1–V2 regression suite passes.

## Next Version Dependencies
V4 layers the activation animation onto this same state machine; V5 adds wake-word as an
additional trigger into `Listening`.

---
## Phases
1. **Voice state machine** — StateFlow-based, unit tested transitions.
2. **STT integration** — `SpeechRecognizer` wrapper, permission flow, failure handling.
3. **TTS integration** — `TextToSpeech` wrapper, audio focus, voice/pitch/rate tuning.
4. **Barge-in / VAD & Acoustic Echo Cancellation (AEC)** — `AcousticEchoCanceler` initialization, speakerphone loopback suppression, dynamic threshold offset during active playback.
5. **Wire to Orchestrator** — unify voice and text input paths into one `ConversationTurn` model.
6. **Device/accuracy benchmarking & loopback validation** — instrumentation tests across device tier matrix.
7. **Stabilization** — regression pass, lock version.
