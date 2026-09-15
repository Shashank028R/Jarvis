# 06 — Android Platform Capabilities (Ground Truth)

This document is the single source of truth for what Android and Google Play actually permit. No
other document may claim a capability that contradicts this one. Where the platform changes, this
file is updated first, and every downstream doc is checked for now-invalid assumptions.

## Capability Tiers

1. **Fully supported** — public API, no special permission beyond a normal manifest declaration.
2. **Requires runtime permission / user approval** — public API gated by a permission the user
   can grant or deny at any time.
3. **Possible via AccessibilityService / UI automation** — not a purpose-built API for the task;
   works by reading/acting on the accessibility node tree. Subject to Play Store policy
   restrictions on *autonomous* use (see the Accessibility section below) and to per-app UI
   reliability risk.
4. **Possible only through a specific role/privileged API** — e.g., the Assistant role, Default
   Dialer/SMS app role, Device Admin/Device Owner (enterprise-only in practice).
5. **Restricted or unreliable** — technically reachable on some devices/OS versions but not a
   dependable foundation for a shipped feature.
6. **Unavailable to a normal third-party app** — no legitimate path; excluded from the roadmap
   entirely, or replaced with the closest legitimate alternative.

## Capability Table

| Capability | Method | Permission / Role | Android Version Notes | Reliability | Limitations | Security Notes |
|---|---|---|---|---|---|---|
| Launch another app | Explicit/implicit `Intent` | None (normal) | All supported versions | High | Target app must expose a launchable activity or handle the intent | Low risk |
| Close another foreground app | Not supported via standard API; `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)` | None (standard) / Accessibility | All supported versions | **Unavailable via standard APIs**; Medium via Accessibility | Android does not permit 3rd-party apps to kill another app (`ActivityManager.killBackgroundProcesses` only kills background cached processes; `FORCE_STOP_PACKAGES` is system-only). Dismissal to Home screen requires Accessibility | Reclassified as `dismissAppToHome` in V7 (HIGH risk), removed from V6 |
| Open a specific Settings screen | `Intent` (e.g. `ACTION_BLUETOOTH_SETTINGS`) | None | Varies; some specific toggle intents removed over time (e.g. cannot silently toggle Wi-Fi/Bluetooth state directly since Android 10, only open the settings screen) | High for "open screen", **not possible** for silently flipping the toggle | Cannot programmatically enable/disable Wi-Fi/Bluetooth directly on modern Android — only launch the settings panel for the user to tap | Prevents apps from silently changing radios | 
| Set an alarm | `AlarmClock.ACTION_SET_ALARM` intent to default Clock app | None | All supported versions | High if a compatible clock app is installed | Delegates to whatever clock app is default; no in-app custom UI | Low risk |
| Set a timer | `AlarmClock.ACTION_SET_TIMER` intent | None | All supported versions | High if a compatible clock app is installed | Same as above | Low risk |
| Control currently playing media | `MediaSessionManager` / media button intents | `MEDIA_CONTENT_CONTROL`-adjacent APIs / notification listener for session discovery | All supported versions | Medium–High | Requires the target app to expose an active `MediaSession`; not all apps do | Exposes some now-playing metadata |
| Read active notifications | `NotificationListenerService` | User must enable in Settings > Notification access (special access, not a runtime permission dialog) | All supported versions | High once granted | One-time manual grant, cannot be requested via a normal permission dialog | High privacy sensitivity — notification bodies can contain messages, codes, etc. |
| Record audio (STT / wake word) | `RECORD_AUDIO` | Runtime permission | Background mic use restricted since Android 9+ unless in foreground service w/ `microphone` FGS type (Android 14+ requires declared FGS type) | High while foreground/FGS active | Cannot silently record in background without an active, user-visible foreground service notification | Must never be silent from the user's perspective |
| Always-on wake-word / hotword detection | `AlwaysOnHotwordDetector` (via `VoiceInteractionService`) OR continuous mic capture in a foreground service | Requires holding the **Assistant role**, hardware DSP hotword support (device-dependent) for the low-power path; otherwise a persistent foreground service with visible notification and real battery cost | Assistant role via `RoleManager`; historically unstable across app reinstalls (Secure Settings for assistant/voice_interaction_service can be cleared on reinstall, requiring re-selection) | Low–Medium without the Assistant role; Medium with it | True low-power always-listening is not available to an app that isn't the selected system Assistant with hardware DSP support; a foreground-service fallback works but costs battery and shows a persistent notification | Continuous audio capture is a major privacy commitment — must be opt-in, clearly indicated, and locally processed |
| Full "assistant" invocation (long-press power / gesture) | `VoiceInteractionService` + `ROLE_ASSISTANT` | Assistant role, replacing the current default (often Gemini/Google Assistant) | API 21+ for the service classes; user must actively select JARVIS as default assistant | Medium — technically works but user must fight OS defaults and role can silently reset | Only one app can hold this role; competing with Gemini for the slot; OS-level friction | Elevated trust — treat with the same caution as accessibility |
| Take a screenshot of current screen | `MediaProjection` API | Runtime user consent **per session** (re-prompted), `FOREGROUND_SERVICE_MEDIA_PROJECTION` type | Android 14+ requires FGS type; user must approve each capture session via system dialog | Medium | Cannot be done silently — system shows an ongoing capture indicator; consent is not "remember forever" in later OS versions | High privacy sensitivity |
| Read on-screen UI content (non-visual) | `AccessibilityService` node tree traversal | User must manually enable the service in Settings > Accessibility (cannot be auto-enabled) | All supported versions | Medium — depends on target app exposing content descriptions/text nodes | Some apps under-label their UI or use custom rendering (e.g. Compose/Flutter/Unity canvases) that expose little semantic info | See dedicated policy discussion below — this is the highest-risk capability in the project |
| Click / type / swipe inside another app | `AccessibilityService.dispatchGesture` / `performAction` | Same as above | All supported versions | Medium | Same reliability caveats as above; some apps have `FLAG_SECURE` or explicitly resist automation | Same policy exposure as above |
| Read/parse arbitrary file content on shared storage | Storage Access Framework / scoped storage APIs | Runtime permission / picker-based, scoped storage since Android 10+ | All supported versions | High for user-picked files, low for broad unprompted access | Broad `MANAGE_EXTERNAL_STORAGE` is heavily restricted by Play policy to a narrow set of app categories JARVIS does not qualify for | Must use SAF pickers, not is broad storage grabs |
| Make a phone call directly (no dialer UI) | `CALL_PHONE` + `Intent.ACTION_CALL` | Dangerous runtime permission | All supported versions | High | User must grant a sensitive permission; Play policy scrutinizes CALL_PHONE usage closely for "core use case" justification | Must be clearly justified in the Play Console permissions declaration |
| Send an SMS directly | `SEND_SMS` | Dangerous runtime permission, and full SMS capability effectively requires being the **default SMS app** for broad use per Play policy | All supported versions | Low–Medium unless default SMS role held | Play policy restricts SMS permission group to apps whose core function is messaging; JARVIS is not primarily an SMS app | Prefer intent-based hand-off to the user's messaging app (e.g., WhatsApp intent) over direct SEND_SMS |
| Send a message inside a third-party messaging app (e.g. WhatsApp) | `Intent` deep link to compose screen (opens app, prefills text) OR AccessibilityService automation to actually tap send | Deep link: none. Full auto-send: accessibility, policy-restricted | All supported versions | Deep link: High (opens composer, user taps send). Full auto-send: Medium, policy-restricted | Most third-party chat apps do not expose a public "send programmatically" API to other apps | Auto-send without user's final tap is a high-risk, high-friction feature; default behavior should stop at "composer prefilled" |
| Play media in third-party apps (e.g. YouTube) | Deep-link URI `Intent(ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=..."))` OR Accessibility UI navigation | None (Intent) / Accessibility | All supported versions | Intent: High. UI Automation: Fragile due to ads and layout changes | YouTube pre-roll ads and obfuscated node trees break UI clicks. Intent search/view URI is rock-solid and immediate | Intent-First Doctrine: always dispatch Intent first; Accessibility only if in-app navigation required |
| Web search / current information | Standard HTTPS calls from app (own logic) or Gemini's built-in web-grounding tool | Internet permission (`INTERNET`, granted by default at install, not runtime) | All supported versions | High | Bound by API rate limits/cost, not Android itself | Standard network security practices apply |
| Root-level device control (any app, any permission, arbitrary system config) | N/A | Not available to a normal (non-rooted, non-Device-Owner) app | N/A | N/A | Fundamentally unavailable | Out of scope entirely |

## AccessibilityService — the Central Policy Risk

Google Play's Accessibility API policy (current as reviewed for this document) draws a sharp
line: **"Any use of the Accessibility API that enables an app to autonomously initiate, plan, and
execute actions or decisions is strictly prohibited."** Deterministic, human-authored
"if X then do Y" automation is allowed; an AI planner that decides at runtime what to click next
based on model reasoning is squarely on the wrong side of that line **unless** the app is declared
and genuinely qualifies as an accessibility tool (`isAccessibilityTool = true`), whose primary
purpose is supporting users with disabilities — which is not JARVIS's primary purpose.

**This is the single largest open risk in the entire project** and is treated as such throughout:
see `10_ACCESSIBILITY_ARCHITECTURE.md` for the mitigation strategy (constrained, user-confirmed,
narrowly-scoped automation rather than freeform autonomous control; and a clearly documented
fallback distribution path), and `DOCUMENTATION_REVIEW.md` for the recommendation to seek explicit
Play Console guidance / a compliance review before V7 is built, not after.

## The Assistant Role — Practical Notes

Registering as `android.app.role.ASSISTANT` requires shipping a `VoiceInteractionService` (which,
per AOSP guidance, is also expected to double as a `RecognitionService`) and an assist-handling
activity. It competes directly with the OS default (commonly Gemini). Real-world reports (e.g.
third-party assistant developers) show the Secure Settings pointing at the assistant/voice
interaction service can be **cleared on APK reinstall**, requiring the user to manually reselect
JARVIS as the assistant (or a developer to restore it via ADB during testing) — this is a genuine
platform rough edge, not a bug in JARVIS, and must be surfaced to users as an in-app
"Re-enable JARVIS as your assistant" recovery flow rather than assumed to persist.

## What This Means for the Roadmap

- V5 (Wake Word) ships a **foreground-service-based, explicitly-toggled listening mode** as the
  baseline, not a silent always-on background listener, and documents the Assistant-role path as
  an optional, separately-gated enhancement with the reliability caveats above.
- V7/V9 (Accessibility Agent / Multi-Step Agent) are designed around **narrow, user-confirmed,
  bounded automations** (e.g., "tap the item I already identified for you and confirmed") rather
  than fully autonomous freeform UI control, specifically to stay on the compliant side of the
  Accessibility API policy line, and the docs flag where a stricter interpretation might require
  further scoping down before Play Store submission.
