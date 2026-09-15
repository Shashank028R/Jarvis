# V4 — JARVIS UI / Activation Animation

## Purpose
Deliver the distinctive visual identity: the black-screen, red-eyes activation sequence and
minimal idle UI, synchronized to the voice state machine built in V3.

## Goals
Implement the activation animation as a pure rendering of `VoiceStateMachine` state, per
`03_USER_EXPERIENCE.md`.

## Features
- Pure black background across the app.
- Original angular red "eyes" motif rendering on activation, synchronized to the TTS greeting.
- Distinct visual treatment for Listening/Thinking/Speaking/Idle states.
- Minimal idle-state visual (reduced eye motif or subtle indicator).

## Dependencies
V3's voice state machine.

## Architectural Changes
None structural — this is a pure `:app` UI layer consuming existing `:voice` state.

## New Components
- Compose custom-drawn (Canvas/vector) eye graphics — original geometry, not a traced/rasterized
  copy of the provided reference image (see legal note in `03_USER_EXPERIENCE.md`).
- Animation timing coordinator synchronizing visual beats to TTS start/stop events.

## User Experience
Activation feels like "a futuristic AI system coming online," not a chat app opening.

## Permissions
None new.

## Security Considerations
None new; standard UI code review for performance (avoid excessive recomposition/battery cost
from animation).

## Testing
- Compose UI tests: each voice state renders its expected visual variant.
- Manual acceptance: side-by-side design review confirming originality versus the reference image
  and versus any real manufacturer's trademarked lamp design.
- Frame-rate test: animation maintains 60fps on minSdk target device without dropped frames or jank.
- Responsiveness test: eye geometry scales proportionally on 16:9, 19.5:9, foldable, and tablet aspect ratios.

## Acceptance Criteria
- [ ] Activation animation plays correctly synchronized to the greeting TTS.
- [ ] Design review confirms the eye motif adheres to the mathematical vector spec in `03_USER_EXPERIENCE.md` and is an original interpretation, not a traced copy of the reference image or any real vehicle's trademarked design.
- [ ] All cognitive voice states (Idle, Listening, Thinking, Speaking, Error) have distinct, correct visual representations.
- [ ] Idle state is genuinely minimal per `03_USER_EXPERIENCE.md` ("avoid clutter").
- [ ] JARVIS's own UI meets accessibility contrast/TalkBack requirements despite the dark theme.

## Known Limitations
Purely cosmetic layer; no new functional capability.

## Exit Criteria
Acceptance criteria met; V1–V3 regression suite passes.

## Next Version Dependencies
V5's wake-word trigger enters the same state machine this UI already renders correctly.

---
## Phases
1. **Design spec finalization & vector paths** — translate normalized `[0..1000, 0..1000]` vector paths from `03_USER_EXPERIENCE.md` into reusable Compose `Path` objects with responsive viewport scaling.
2. **Static rendering & color tokens** — render static dual-eye state with AMOLED black `#000000`, core `#FF1E27`, radial bloom glow `#FF3B30`, and filament highlight `#FFF2F2`.
3. **Activation sequence animation** — keyframed Compose animation: 0–400ms left eye reveal, 250–650ms right eye reveal, 650–900ms dual bloom pulse, synchronized to TTS audio start at 900ms.
4. **State-driven visual variants & audio reactivity** — wire `VoiceStateMachine` to visual states: audio amplitude modulation during Listening/Speaking, traveling shimmer during Thinking, and breathing pulse during Idle.
5. **Idle screen & energy optimization** — minimal persistent UI with frame-throttling when idling to conserve battery.
6. **Accessibility pass** — contrast, TalkBack labels, font scaling.
7. **Performance & stabilization** — frame-rate check on min-spec device, lock version.
