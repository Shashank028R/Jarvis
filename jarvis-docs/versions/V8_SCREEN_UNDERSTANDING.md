# V8 — Screen Understanding

## Purpose
Let JARVIS answer questions about the current screen, using the pipeline in
`11_SCREEN_UNDERSTANDING.md`: accessibility tree first, vision fallback only when necessary.

## Goals
- `readScreen()` tool (MEDIUM tier) producing a structured, privacy-filtered screen summary.
- `takeScreenshot()` tool (MEDIUM tier) as the vision-fallback path, with correct per-session
  `MediaProjection` consent handling.
- Privacy filter redacting sensitive fields before anything reaches the AI model.

## Dependencies
V7's Accessibility Bridge (for tree access), V6's Tool Registry pattern.

## Architectural Changes
Adds a `ScreenParser` component (in `:accessibility` or a new `:screen-understanding` submodule)
and a `PrivacyFilter` (in `:security`) sitting between raw screen data and the AI Planner.

## New Components
- `ScreenParser` (tree → structured summary).
- `PrivacyFilter` (redaction of credential/OTP/payment fields).
- `MediaProjection`-based screenshot capture path with per-session consent handling.
- Vision-pass integration for the screenshot fallback.

## User Experience
"What's on my screen?" / "Explain this page" / "Find the Wi-Fi setting" produce accurate, natural
-language answers without requiring a consent dialog in the common case (tree-based path).

## Permissions
`MediaProjection` per-session consent (system dialog, not a persisted grant) for the screenshot
fallback path only.

## Security Considerations
Privacy filter is mandatory and tested explicitly — this version cannot ship if any credential-
like field reaches the AI request payload in any test scenario.

## Testing
- Unit: `PrivacyFilter` against a labeled test set of node trees containing planted sensitive
  fields — assert 100% redaction.
- Integration: `ScreenParser` producing correct structured summaries against varied real screens.
- Instrumentation: screenshot fallback triggers correctly only when the tree is genuinely sparse.
- Manual acceptance: "explain this page" tested against 5+ varied real app screens for answer
  quality and honesty about uncertainty.

## Acceptance Criteria
- [ ] Tree-based path used whenever sufficient, avoiding unnecessary `MediaProjection` prompts.
- [ ] Privacy filter redaction is 100% effective against the planted-field test set.
- [ ] Screenshots are not persisted beyond the fallback processing step without explicit opt-in.
- [ ] Answers correctly express uncertainty on genuinely ambiguous/low-quality screen data rather
      than fabricating detail.

## Known Limitations
Vision-fallback accuracy is bound by the underlying model's general screenshot-understanding
capability; `MediaProjection` consent friction is a real, disclosed UX cost per capture session.

## Exit Criteria
Acceptance criteria met; V1–V7 regression suite passes.

## Next Version Dependencies
V9's multi-step agent uses `readScreen`/`takeScreenshot` observations as part of its VERIFY step.

---
## Phases
1. **`ScreenParser`** — tree-to-structured-summary logic.
2. **`PrivacyFilter`** — redaction logic + the planted-sensitive-field test set.
3. **`readScreen()` tool** — wiring into Tool Registry/Executor.
4. **`MediaProjection` screenshot path** — per-session consent, vision-pass integration.
5. **Vision fallback trigger logic** — "tree too sparse" detection.
6. **Manual acceptance across real apps** — quality and honesty review.
7. **Stabilization** — regression pass, lock version.
