# 03 — User Experience

## Personality

JARVIS is calm, intelligent, respectful, concise by default but capable of depth on request.
Never sycophantic, never over-enthusiastic, never filler-heavy. Addresses the user as "Sir" by
default, configurable in settings (some users will want this off — treat it as a preference, not
a hardcoded identity trait, per `12_MEMORY_ARCHITECTURE.md` user-controlled settings).

Default greeting on activation: **"Hello Sir. How can I help you today?"**

Tone rules for generated responses:
- Prefer one clear sentence over three hedging ones.
- Offer detail when asked for it, not preemptively.
- State uncertainty and failure plainly ("I could not confirm the timer was set") rather than
  papering over it.
- Never fabricate confidence about an action's outcome (see `16_ERROR_HANDLING.md`).

## Voice Direction

Deep, calm, clear, natural, slightly futuristic male voice; not theatrical. This is a TTS voice
selection/tuning requirement, not a claim to imitate any specific copyrighted character's actual
voice — see `09_VOICE_AND_WAKE_WORD.md` for TTS engine options and how voice selection is
implemented without infringing on protected voice performances.

## Activation Animation (Original Design, Reference-Inspired)

Visual concept, delivered in V4:

1. Screen goes to pure black (`#000000`), no status bar chrome beyond the system minimum.
2. A left glowing red "eye" shape resolves in first — sharp, angular, automotive-inspired LED
   geometry (inspired by the aggressive daytime-running-light visual language of modern
   performance cars, **not a reproduction of any specific manufacturer's trademarked lamp
   design** — see the legal note below).
3. A right eye shape resolves in a beat later, symmetric to the left.
4. A subtle pulse/brighten plays once both eyes are visible, timed to the start of the TTS
   greeting audio.
5. "Hello Sir. How can I help you today?" plays; a minimal waveform or pulse indicator can
   accompany the speech.
6. On idle, the UI reduces to a minimal dark screen with a subtle version of the eye motif (or a
   smaller status indicator) rather than a conventional chat list.

### Mathematical Vector & Geometry Specification (Normalized 1000x1000 Canvas)

For programmatic rendering in Jetpack Compose Canvas, the eye geometry is mapped to a normalized `[0..1000, 0..1000]` viewport centered on screen:

- **Center Axis**: $X = 500$
- **Left Eye Outer Contour (Path)**:
  - Start at outer top brow: `(160, 420)`
  - Sweep downward-inward at 35° angle to inner sharp corner: `(440, 490)`
  - Turn sharply downward to lower inner point: `(420, 525)`
  - Follow aggressive lower contour outward: `(210, 475)`
  - Close back to outer top brow: `(160, 420)`
- **Left Eye Inner Projector Slit (Accent Core)**:
  - Path: `(220, 445) -> (400, 495) -> (390, 510) -> (245, 465) -> Close`
- **Right Eye Contour**: Exact symmetrical reflection of Left Eye across the $X = 500$ axis:
  - Outer top brow: `(840, 420)`, inner corner: `(560, 490)`, lower inner: `(580, 525)`, lower outer: `(790, 475)`.
- **Palette Tokens**:
  - `Background`: `#000000` (AMOLED black)
  - `EyeCorePrimary`: `#FF1E27` (intense automotive LED red)
  - `EyeBloomGlow`: `#FF3B30` (radial gradient bloom, alpha 0.4 to 0.8, radius 25dp to 50dp)
  - `EyeFilamentHighlight`: `#FFF2F2` (white-hot inner highlight line, 1.5dp stroke)
  - `EyeIdleDim`: `#550A0D` (alpha 0.35)

### Keyframe Animation Choreography (Activation Sequence)

- **`0ms`**: Screen transitions to `#000000`.
- **`0ms – 400ms`**: Left eye horizontal sweep reveal using `FastOutSlowInEasing` (clip rect expands from $X=160$ to $X=440$).
- **`250ms – 650ms`**: Right eye horizontal sweep reveal symmetrically ($X=840$ to $X=560$).
- **`650ms – 900ms`**: Dual-eye radial glow bloom expands from 20dp to 45dp radius; luminance pulses to 100%.
- **`900ms`**: TTS greeting begins. Pupil glow radius and core luminance modulate dynamically with audio output amplitude.

### Cognitive State Machine Visual Matrix

| Cognitive State | Visual Representation | Dynamics & Audio Reactivity |
|---|---|---|
| **Idle** | Dim red outline (`#550A0D`), glow alpha 0.25 | Subtle 0.5 Hz breathing sine-wave modulation |
| **Listening** | Bright red (`#FF1E27`), expanded bloom | Real-time pupil pulse driven by incoming microphone amplitude (0–100%) |
| **Thinking** | Full brightness core, traveling shimmer | Coordinated horizontal light sweep across eyes (left to right, 1.2s period) |
| **Speaking** | Vibrant crimson with filament highlights | Real-time luminance and glow bloom modulated by TTS audio waveform stream |
| **Error / Blocked** | Amber/crimson flicker (`#FF6F00` $\rightarrow$ `#FF1E27`) | 350ms double-flicker, transitioning back to Idle |
| **Interrupted** | Instant snap to `Listening` state | Transition completes within <50ms of detected barge-in speech |

**Legal note (binding):** The delivered UI is an original geometric interpretation — angular,
red, symmetric, automotive-*inspired* — and must not reproduce a specific manufacturer's
trademarked lamp shape or logo. `versions/V4_JARVIS_UI.md` phase acceptance criteria include an
explicit design-originality check before the animation is considered done.

## Interface Principles

- Predominantly black background, red as the sole accent color, minimal chrome.
- Avoid: bright multi-color gradients, cartoon styling, dense button grids, conventional chatbot
  bubble UI as the primary surface.
- A text-based fallback conversation view exists for accessibility and for environments where
  voice is impractical, but it is not the primary visual identity.
- Listening / thinking / speaking states are visually distinct (see `09_VOICE_AND_WAKE_WORD.md`
  for the state machine) so the user always knows what JARVIS is currently doing.

## Accessibility of the App Itself

Note the distinction: `10_ACCESSIBILITY_ARCHITECTURE.md` covers JARVIS *using* Android's
Accessibility API to observe/act on other apps. This section covers JARVIS's own UI being usable
by people who rely on Android accessibility services (TalkBack, switch access, font scaling,
high-contrast). The all-black/red aesthetic must still meet WCAG-equivalent contrast minimums for
its text elements, and every visual affordance needs a non-visual (voice or TalkBack-readable)
equivalent.
