# Documentation Review

A consistency and reality-check pass across the full specification, per Constitution requirements.

## Strengths

- Every version has explicit, testable acceptance criteria and a phase breakdown small enough for
  incremental, verifiable implementation.
- Android/Play platform claims are grounded in checked sources (`06_ANDROID_CAPABILITIES.md`),
  not assumed from the original product brief's optimistic framing.
- Security and privacy are threaded through every version (confirmation gating, risk tiers, data
  classification) rather than concentrated only in V13/V14 — those versions deepen and audit an
  already-present baseline rather than bolting one on late.
- The tool/policy/executor architecture (`08_AGENT_AND_TOOL_SYSTEM.md`) gives a single, consistent
  choke point for every device-affecting action, which makes both security review and future
  extensibility tractable.
- The roadmap order was checked against real dependencies (e.g., V4 UI before V5 wake word, so the
  voice state machine is proven under a simple trigger before a second, always-on trigger is added)
  rather than followed blindly from the original brief.

## Pre-V1 Documentation Hardening (Resolved Decisions)

The following critical P0/P1 gaps identified during the technical documentation audit were formally resolved and integrated into the specification:
1. **Explicit SDK Baseline**: Configured `compileSdk = 35`, `targetSdk = 35`, `minSdk = 29` across all modules.
2. **Dual-Flavor Build Architecture**: Established `playStore` (Play Store compliant) and `fullAssistant` (unrestricted power-user/sideload) Gradle flavors from V1 onward.
3. **Two-Stage Secret Strategy**: Direct Gemini API key in git-ignored `local.properties` (`BuildConfig.GEMINI_API_KEY`) for development (V2–V14); serverless relay proxy (`/relay`) for production release (V15).
4. **Acoustic Echo Cancellation (AEC)**: Mandated `AcousticEchoCanceler` and speaker loopback suppression on the V3 audio pipeline to prevent TTS self-interruption.
5. **Platform Realism for App Dismissal**: Removed impossible `closeApp()` from V6; formalized `dismissAppToHome()` via Accessibility `GLOBAL_ACTION_HOME` in V7.
6. **Deterministic V4 Eye Geometry**: Specified normalized vector coordinates `[0..1000]`, timing keyframes, and cognitive state color tokens in `03_USER_EXPERIENCE.md`.
7. **Intent-First Doctrine**: Standardized on Intent-First dispatch for YouTube playback and messaging, using Accessibility UI automation as a fallback.
8. **Token-Efficient Screen Pruning**: Added tree condensation rules in `11_SCREEN_UNDERSTANDING.md` to prevent LLM context exhaustion.

## Risks

1. **Accessibility API Play policy compliance (highest risk in the project).** Google Play policy
   prohibits autonomous AI-driven UI automation by apps not qualifying as accessibility tools.
   JARVIS's core multi-step-agent value proposition is structurally in tension with this. The
   mitigation (confirmed, bounded, single-app-scoped automation — ADR-004) narrows the gap but
   does **not** guarantee Play Console approval. **Recommendation**: seek explicit Play Console
   developer support / policy guidance before V7 begins any code (already gated as V7 Phase 1),
   and maintain a documented non-Play distribution fallback so the project isn't blocked on this
   external, non-engineering decision.
2. **Assistant role reliability.** Real-world reports show the OS clearing assistant/voice-
   interaction Secure Settings on app reinstall, and holding the role means directly competing
   with the OS default (often Gemini). Tier 2 wake word and any "long-press power button" invocation
   depend on this and should be treated as best-effort, not core-path, per ADR-003.
3. **STT/TTS quality ceiling with baseline Android APIs.** V3's baseline (system `SpeechRecognizer`
   / `TextToSpeech`) may not reach the "premium personal assistant" voice quality bar described in
   the vision without a later neural-TTS upgrade; this is flagged as an open ADR-worthy decision,
   not resolved in the current spec.
4. **AccessibilityService reliability against unlabeled/custom UI.** A meaningful fraction of
   popular apps (games, some Compose/Flutter/custom-canvas UIs) may expose too little semantic
   information for reliable automation; V7's acceptance criteria require honest "not found"
   behavior rather than guessing, but this does inherently limit real-world usefulness for some
   target apps, and that limitation should be communicated to users, not hidden.
5. **Cost/rate limits of the Gemini API and any web-search provider** are not modeled with actual
   numbers in this spec (no budget or usage projections given) — a business/ops concern to address
   before V2's relay is built for real, since the relay's rate-limiting design should be informed
   by actual budget constraints, not an arbitrary number.

## Technical Unknowns

- Actual on-device keyword-spotting model accuracy/footprint for the "Jarvis" wake phrase
  specifically — no benchmark exists yet; V5 Phase 1 is explicitly a benchmarking/evaluation
  phase for this reason.
- Real-world AccessibilityService reliability against a representative sample of popular apps
  (YouTube, WhatsApp, Settings) — the V7 manual acceptance phase is where this gets tested for
  real; results could meaningfully narrow or widen the practical scope of V9's compound tasks.
- Whether Play Console will accept the confirmed-bounded-automation framing for the Accessibility
  declaration — genuinely unknown until asked; see Risk #1.

## Decisions That Still Need Validation

- Final on-device vs. cloud STT choice for V3 (baseline decided; upgrade path open).
- TTS engine upgrade decision (system engine vs. neural TTS) — open ADR.
- Whether a Tier 2 (Assistant role) wake-word path is worth pursuing given its documented fragility
  — decided by the V5 Phase 6 spike, not assumed.
- Final Accessibility distribution strategy (Play-compliant declaration vs. adjusted scope vs.
  alternate distribution channel) — the single most consequential open decision in the project,
  formally closed out no later than V15.

## Recommended Proof-of-Concepts (before committing further engineering time)

1. A throwaway spike: local KWS "Jarvis" detection accuracy in a real room, before V5's real
   implementation phase, to validate the Tier 1 approach is viable at all.
2. A throwaway spike: AccessibilityService automation against 3 real popular apps (not a test app)
   to gauge real-world node-matching reliability, before committing to V7/V9's full scope.
3. An early, informal conversation with Google Play developer support about the Accessibility API
   declaration for an AI-planned automation use case, before V7 begins.

## Features That Should NOT Be Attempted Until Later (or at all, without a new decision)

- Direct SMS sending as a default behavior (Play policy restricts this to default-SMS-app-role
  apps; JARVIS should stick to deep-link hand-off).
- Silent/automatic message sending in third-party chat apps without a final user tap or explicit
  voice confirmation of the literal message content.
- Any attempt to programmatically toggle Wi-Fi/Bluetooth state directly (not possible on modern
  Android for a normal app — only opening the settings screen is available).
- Cloud sync of memory, multi-user support, cross-platform support — all explicitly out of the
  current roadmap's scope (see `02_PRODUCT_REQUIREMENTS.md`).

## Potential Project Blockers

- An unfavorable outcome on the Accessibility API compliance question could force a significant
  scope reduction of the project's headline "control other apps" capability. This is the one risk
  with the potential to reshape the product vision itself, not just delay a version, and is
  surfaced here deliberately rather than downplayed.
- Assistant-role instability could mean the "long-press power button to activate JARVIS" experience
  from the original vision never becomes reliable; the Tier 1 wake-word path is designed
  specifically so the product still works well even if this never resolves favorably.
