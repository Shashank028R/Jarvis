# V2 — Basic AI Conversation

## Purpose
Prove the core AI reasoning loop end-to-end via text input/output before adding voice or device
control complexity.

## Goals
- Text-in, text-out conversation via Gemini through a pluggable transport interface (`AiTransport`).
- `AiClient` interface implemented for real (`07_AI_ARCHITECTURE.md`).
- Basic conversation session management (bounded turn history) in `:orchestrator`.
- Local development configuration via `local.properties` without blocking on remote infrastructure.

## Features
- A minimal text input UI (temporary, superseded visually by V4 but functionally retained as the
  accessibility/text-fallback surface per `03_USER_EXPERIENCE.md`).
- Coherent multi-turn conversation with reasonable context retention within a session.

## Dependencies
V1 module skeleton and DI graph.

## Architectural Changes
- Introduces `AiTransport` interface with `DirectGeminiTransport` for development (reading `BuildConfig.GEMINI_API_KEY` from git-ignored `local.properties`) and `RelayGeminiTransport` specification for V15 production (`13_SECURITY.md`).
- `:ai` module gets its real `AiClient` implementation; `:orchestrator` gets real session state.

## New Components
- `GeminiAiClient` implementing `AiClient.converse()` only (planning/tool-calling comes later).
- `AiTransport` interface: abstracts the HTTP network transport layer, decoupling the client logic from whether requests go directly to Google's API or through a relay.
- `ConversationSession` state holder (bounded turn window, e.g. last 10 turns).

## User Experience
User types a message, sees JARVIS's text reply, personality-consistent tone per
`03_USER_EXPERIENCE.md` (calm, concise, "Sir" default) even though voice/animation don't exist yet.

## Permissions
`INTERNET` (default-granted, declared in manifest).

## Security Considerations
- Development API key lives exclusively in git-ignored `local.properties` on the developer's machine and is injected into `BuildConfig` at compile time for debug builds; it is never committed to source control.
- In production builds (V15), `RelayGeminiTransport` routes through the serverless proxy, preventing API key extraction from release APKs.

## Testing
- Unit: `ConversationSession` turn-window bounding logic.
- Integration: `GeminiAiClient` against a fake HTTP layer (no live API calls in CI).
- Manual acceptance: a real conversation session against Gemini using local key, checked for coherence and personality tone.

## Acceptance Criteria
- [ ] User can hold a multi-turn text conversation with contextually coherent replies.
- [ ] No API key committed to source control (`local.properties` is git-ignored).
- [ ] App launches and converses successfully with a valid `GEMINI_API_KEY` in `local.properties`.
- [ ] Missing API key produces a clear, friendly setup error prompt in debug builds rather than a crash.
- [ ] Personality tone matches `03_USER_EXPERIENCE.md` in manual review.

## Known Limitations
No voice, no tool use, no device action — pure conversation only.

## Exit Criteria
All acceptance criteria met; regression test suite for V1 still passes.

## Next Version Dependencies
V3 wraps this same `AiClient.converse()` path with voice I/O; V9's planning extends
`PlannerDecision` beyond plain conversation.

---
## Phases
1. **Gemini Transport Interface & Local Dev Configuration** — create `AiTransport` interface and `DirectGeminiTransport` reading `BuildConfig.GEMINI_API_KEY` from `local.properties`. Document error handling when key is missing. Tests: unit test asserting key injection and missing-key error propagation.
2. **`AiClient` interface + Gemini implementation** — `converse()` only. Tests: fake-HTTP
   integration tests.
3. **`ConversationSession`** — bounded turn history, session lifecycle (start/end/timeout).
   Tests: unit tests on windowing and timeout logic.
4. **Text UI surface** — minimal input/output screen wired to the orchestrator.
   Tests: Compose UI test for send/receive round trip against a fake client.
5. **Personality tuning pass** — system prompt calibration against `03_USER_EXPERIENCE.md`.
   Tests: manual acceptance review against a checklist of tone criteria.
6. **Error handling** — network failure, rate limit handling, malformed response.
   Tests: failure-path unit/integration tests per `16_ERROR_HANDLING.md`.
7. **Stabilization** — full regression pass, lock version.
