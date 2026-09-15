# 15 — Testing Strategy

## Test Pyramid

1. **Unit tests** (JUnit5 + MockK) — pure logic: schema validation, Policy Engine decisions,
   routing classification, risk-tier lookup. Fast, run on every change.
2. **Integration tests** — module boundaries with fakes: `:orchestrator` against a fake
   `AiClient` and fake Tool Executor, verifying the agent loop's state transitions and retry
   bounds without touching real Android APIs or the network.
3. **Android instrumentation tests** (Espresso / Compose UI Testing / UI Automator) — real device
   or emulator: app launching via Intents, AccessibilityService actions against a controlled test
   app, foreground service lifecycle.
4. **Manual acceptance tests** — scenarios that are impractical to fully automate (real wake-word
   detection in a noisy room, actual third-party app automation against apps that change their UI
   over time) — run before each version is locked, logged in `testing/ACCEPTANCE_CRITERIA.md`.

## Test Categories by Concern

- **Permission tests**: for every permission-gated tool, verify behavior in granted, denied, and
  revoked-mid-session states.
- **Voice tests**: STT accuracy on a fixed utterance set, TTS playback correctness, barge-in
  interruption latency, wake-word false-accept/false-reject rate on a benchmark audio set.
- **AI/tool routing tests**: fixed utterance → expected `PlannerDecision` type test set (using a
  fake or recorded model response) to catch routing regressions when prompts change.
- **Accessibility tests**: node-matching strategy against a purpose-built test app with varied
  labeling quality, plus the "no actionable elements found" graceful-failure path.
- **Regression tests**: see `testing/REGRESSION_TESTS.md` — every locked version's core behaviors
  get a regression test that must keep passing in all future versions.
- **Security tests**: prompt-injection scenarios, confirmation-gate bypass attempts, schema-fuzzing
  (`13_SECURITY.md`).
- **Performance/battery tests**: wake-word listening battery draw, cold-start-to-first-response
  latency, memory usage under a long-running multi-step task.
- **Device compatibility tests**: matrix in `testing/DEVICE_TEST_MATRIX.md` — different OEMs
  (especially for AccessibilityService and foreground-service behavior quirks), different Android
  versions from the declared minSdk upward.

## Agentic Scenario Testing (V9+)

Each multi-step capability gets a named scenario with an explicit expected state sequence, not
just a final "did it work" check:

```
SCENARIO: play_song_and_set_timer (Intent-First Primary Execution)
INPUT: "Play Believer by Imagine Dragons on YouTube and set a 15-minute timer."
EXPECTED PRIMARY SEQUENCE (Intent-First):
  1. playMediaIntent("Believer Imagine Dragons", YouTube) → observation: YouTube playback intent dispatched
  2. setTimer(15) → observation: timer confirmation from Clock app
  3. verify playback & timer states
  4. JARVIS reports success mentioning both sub-tasks explicitly

EXPECTED FALLBACK SEQUENCE (Accessibility-Driven UI Navigation):
  1. openApp(YouTube) → observation: YouTube foregrounded
  2. search("Believer Imagine Dragons") → observation: results list present
  3. select best match (confirmed if ambiguous) → observation: playback screen shown
  4. verify playback state == playing
  5. setTimer(15) → observation: timer confirmation from Clock app
  6. JARVIS reports success mentioning both sub-tasks explicitly

FAILURE VARIANTS (each is its own test):
  - YouTube not installed → JARVIS reports this specifically, does not silently fail
  - No network → playback fails cleanly, JARVIS explains, does not attempt timer step blindly
  - Intent dispatch fails → Planner adapts and attempts Accessibility UI fallback
  - No clear best match in UI results → JARVIS asks the user to choose rather than guessing
  - Accessibility permission not granted in fallback mode → UI navigation tool unavailable; JARVIS explains why
  - User manually switches apps mid-task → task is paused/aborted cleanly, not continued blindly against wrong app
  - Timer tool step fails after playback already started → JARVIS reports partial success accurately (playback yes, timer no)
```

## Definition of Done for a Phase

A phase is complete only when: its unit/integration/instrumentation tests all pass, its defined
failure-path tests all pass, its manual acceptance checklist item (if any) is checked off, and
`PROGRESS.md` is updated in the same change.
