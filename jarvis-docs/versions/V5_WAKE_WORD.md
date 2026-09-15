# V5 — Wake Word

## Purpose
Add hands-free activation via a spoken wake phrase, using the realistic Tier 1 (foreground
service + local keyword spotting) architecture from `09_VOICE_AND_WAKE_WORD.md`, with Tier 2
(Assistant role) as an explicitly optional, separately gated enhancement.

## Goals
- User can enable a "Listen for 'Jarvis'" mode; while enabled, saying the wake word triggers the
  V4 activation animation and transitions the voice state machine to Listening.
- Battery and privacy behavior are measured and honest (persistent notification while active,
  documented drain).

## Features
- Toggle to enable/disable wake-word listening (off by default — explicit opt-in, per Rule 7).
- Local keyword-spotting model detecting "Jarvis" (or "Hey Jarvis").
- Persistent, honest foreground-service notification while listening is active.
- (Optional, separately gated, best-effort) Assistant-role registration path for Tier 2 hardware
  hotword detection, with a documented recovery flow for the reinstall Secure-Settings-reset issue
  noted in `06_ANDROID_CAPABILITIES.md`.

## Dependencies
V3 voice state machine, V4 activation animation.

## Architectural Changes
Introduces a foreground service (`WakeWordListeningService`) with `microphone` FGS type.

## New Components
- ONNX Runtime Mobile engine integrated in `:voice` executing a quantized, local "Jarvis" KWS model.
- `WakeWordListeningService` (foreground service, persistent notification, start/stop lifecycle
  tied explicitly to the user toggle).
- Circular volatile PCM audio buffer (3s max window, non-persisted).
- (Optional path) `JarvisVoiceInteractionService` + assist activity for Assistant-role
  registration, plus an in-app "Re-enable JARVIS as your assistant" recovery screen.

## User Experience
User enables wake word once; thereafter, saying "Jarvis" from within the app's allowed
background/foreground context triggers activation exactly as tap-to-talk does in V3/V4.

## Permissions
`RECORD_AUDIO` (already granted from V3), `FOREGROUND_SERVICE_MICROPHONE` type declaration.
Optional: `ROLE_ASSISTANT` request flow for Tier 2.

## Security Considerations
- Persistent notification cannot be suppressed — this is by design (transparency requirement),
  not a bug to "fix."
- Pre-wake-word audio never leaves the local KWS buffer; only post-wake-word utterance is passed
  to STT (`09_VOICE_AND_WAKE_WORD.md`).

## Testing
- Unit: KWS trigger logic against a labeled audio test set (measuring false-accept/false-reject
  rate).
- Instrumentation: foreground service lifecycle (survives backgrounding, stops correctly on
  toggle-off, stops correctly on app force-stop).
- Manual acceptance: real-room wake-word test at varied distances/noise levels.
- Performance: battery-drain benchmark against the budget in `17_PERFORMANCE.md`.

## Acceptance Criteria
- [ ] Wake word reliably triggers activation in a quiet room at conversational distance.
- [ ] False-accept rate is low enough not to be annoying in normal ambient conditions (documented
      benchmark, not just "felt fine").
- [ ] Toggle off immediately and fully stops the foreground service and mic access.
- [ ] Battery drain benchmark is measured and documented, within budget or explicitly flagged as
      a known trade-off disclosed to the user in-app.
- [ ] If Tier 2 is attempted: Assistant-role reinstall-reset issue has a working in-app recovery
      flow.

## Known Limitations
Wake word is not "always on with zero battery cost" — this is a foreground-service trade-off, not
a hardware-DSP-level always-on detector, unless Tier 2 is achieved and the device supports it.

## Exit Criteria
Acceptance criteria met; V1–V4 regression suite passes.

## Next Version Dependencies
V12 (Offline) treats wake word as the primary offline-relevant surface and must not regress its
behavior when network is unavailable.

---
## Phases
1. **KWS model selection & benchmarking** — evaluate candidate on-device keyword-spotting
   approaches (ONNX Runtime Mobile quantized openWakeWord vs TFLite micro-speech) against accuracy/footprint/latency; document the choice as an ADR.
2. **Foreground service scaffold** — `WakeWordListeningService`, notification, lifecycle tied to
   toggle.
3. **Wake detection → state machine wiring** — trigger flows into `VoiceStateMachine.Listening`.
4. **Toggle & settings UI** — enable/disable, explanation of what's happening while active.
5. **Battery/latency benchmarking** — measure against `17_PERFORMANCE.md` budgets.
6. **(Optional) Assistant-role exploration** — spike Tier 2, document findings, implement only if
   findings are favorable; otherwise document as a known limitation and move on.
7. **Stabilization** — regression pass, lock version.
