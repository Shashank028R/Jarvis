# 04 — Technology Stack

Each choice states *why*, not just *what*, per the constitution's ban on cargo-culting popular
tech.

## Platform & SDK Baseline

- **compileSdk = 35** (Android 15) — access to the latest platform APIs, foreground service type definitions, and edge-to-edge system bars.
- **targetSdk = 35** (Android 15) — full compliance with modern Google Play target API requirements, foreground service contracts, and permission policies.
- **minSdk = 29** (Android 10 Q) — provides `RoleManager` (API 29+), Scoped Storage baseline, modern Coroutines/Compose compatibility, while wrapping Android 14+ FGS types (`FOREGROUND_SERVICE_MICROPHONE`) in runtime SDK guards. Balances reach (supporting ~90% of active Android devices) with modern API capabilities.

## Build Flavors & Distribution Architecture

From V1 onward, the project defines two Gradle product flavors in `:app`:
- **`playStore`**: Geared for Google Play Store distribution. Complies strictly with Google Play Accessibility and Privacy policies (confirmed, bounded, user-in-the-loop actions only; no unprompted background cross-app automation).
- **`fullAssistant` (Power User / Sideload)**: Geared for direct APK and GitHub distribution. Unconstrained AccessibilityBridge capabilities for users who want fully autonomous multi-step execution across other applications.

## Language & UI

- **Kotlin** — required for modern Android; first-class coroutine support is essential for a
  system with many concurrent async operations (voice streaming, tool execution, network calls).
- **Jetpack Compose** — declarative UI matches the state-machine-driven nature of the JARVIS UI
  (listening/thinking/speaking/idle states drive UI directly from state, not imperative view
  mutation); needed for the custom animated activation sequence.

## Architecture Components

- **Coroutines + Flow** — the entire pipeline (audio → STT → planner → tool executor → TTS) is a
  chain of asynchronous, cancellable, backpressure-aware streams. Flow models this cleanly;
  callbacks would not.
- **Dependency Injection (Hilt)** — the tool registry, AI client, and Android integration layers
  need to be swappable/mockable for testing (e.g., inject a fake AI client in unit tests, a fake
  AccessibilityService bridge in agent-loop tests).
- **WorkManager** — for deferred, guaranteed-eventually work that does not need to run instantly
  (e.g., scheduled reminders that must survive reboot), not for anything requiring low latency.
- **Foreground Services** — required for anything that must keep running while the user is in
  another app: an active voice session, an active wake-word listener (where the Assistant role
  makes that legitimate), an in-progress multi-step task. Each foreground service declares the
  correct Android 14+ foreground service type (`microphone`, `mediaPlayback`, etc.) per
  `06_ANDROID_CAPABILITIES.md`.
- **DataStore (Proto)** — structured, typed local settings and preference storage, replacing
  SharedPreferences; used for user-controlled settings (address-as-"Sir" toggle, memory
  on/off, confirmation policy).
- **Room** — local database for anything relational and queryable: task history, memory records,
  conversation summaries. Not used until a version actually needs persisted structured data
  (V11 Memory), per Rule 5 (least privilege / least footprint by default).

## AI & Voice

- **Gemini API** (cloud) — primary reasoning layer: NLU, planning, tool selection, response
  generation. Accessed via a thin `AiClient` interface so the concrete provider is swappable
  (`ADR-002`), never called directly from UI or tool-execution code.
- **Android SpeechRecognizer / on-device recognition** where available for STT; cloud STT as a
  fallback/quality option, gated by a user setting given the privacy implications of streaming
  raw audio off-device.
- **Android TextToSpeech (system engine)** as the default TTS; evaluate a higher-quality
  neural TTS (cloud or on-device) once the baseline pipeline is proven in V3, tracked as an ADR
  if adopted, because it changes the offline story documented in `11_MEMORY... / OFFLINE`.
- **Wake-word engine**: On-device keyword spotting (KWS) powered by **ONNX Runtime Mobile** executing
  a quantized "Jarvis" model (or custom TFLite micro-speech model), running locally inside
  `WakeWordListeningService` (Tier 1). Evaluated against Android's `AlwaysOnHotwordDetector` /
  Assistant-role path (Tier 2) where hardware DSP support exists.

## Android Integration

- **AccessibilityService** — for UI observation/automation, gated heavily by Play policy (see
  `10_ACCESSIBILITY_ARCHITECTURE.md`); this is the highest-risk dependency in the stack and is
  isolated behind a narrow interface so it can be constrained, audited, or replaced without
  touching the planner.
- **RoleManager / VoiceInteractionService** — for the Assistant role, evaluated but not assumed
  achievable at launch (holding the Assistant role competes with Google Assistant/Gemini as the
  device default and has historically had reliability issues surviving app reinstalls).
- **MediaSessionManager / NotificationListenerService / AlarmManager / Intents** — the
  well-supported, low-risk half of device integration; prioritized in V6 before any Accessibility
  work begins.
- **Intent-First Doctrine**: Standard Android Intents (`ACTION_VIEW`, `ACTION_SEND`, `ACTION_SET_TIMER`)
  are always prioritized as the primary execution path; AccessibilityService automation is strictly
  a secondary fallback when no platform Intent contract exists.

## Testing

- **JUnit5 + MockK** for unit tests (Kotlin-idiomatic mocking).
- **Turbine** for testing Flow emissions from the async pipeline.
- **Espresso + Compose UI Testing** for UI tests.
- **UI Automator** for instrumentation tests that need to interact with other apps or system UI
  (necessary for testing V6+ device-action tools against real launched apps).
- **Robolectric** for fast JVM-level Android framework tests where a full emulator is unnecessary.

## Backend & API Secret Management Strategy

To ensure seamless local development without blocking on remote infrastructure, API secret management follows a two-stage strategy:

1. **Development Mode (V2–V14)**:
   - The app communicates directly with Google's Gemini API endpoints using an API key stored in git-ignored `local.properties` (`GEMINI_API_KEY=your_key_here`).
   - Gradle injects this key into `BuildConfig.GEMINI_API_KEY` at compile time for debug builds.
   - The key is never committed to source control and is excluded from release APKs.
2. **Production Mode (V15)**:
   - For production distribution, all external Gemini calls route through a lightweight serverless relay proxy (`/relay`, e.g., Cloudflare Worker or GCP Cloud Function).
   - The relay validates client authenticity using Firebase App Check / Play Integrity API before attaching the master Gemini key and forwarding the request.
   - Swapping between Direct and Relay transport is handled transparently behind the `AiRelayTransport` interface in `:ai`.
