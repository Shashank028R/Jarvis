# 02 — Product Requirements

## Functional Requirements (eventual, full-system scope — not all in V1)

| ID | Requirement | Delivered in |
|----|-------------|---------------|
| FR-1 | User can type or speak a natural-language request and receive a coherent AI response | V2 |
| FR-2 | User can hold a spoken conversation (STT in, TTS out) with interruption support | V3 |
| FR-3 | App presents a distinctive activation animation and idle visual identity | V4 |
| FR-4 | User can invoke JARVIS via a wake phrase without manually opening the app (where Android permits) | V5 |
| FR-5 | JARVIS can launch apps, control media, set timers/alarms, adjust settings via supported APIs | V6 |
| FR-6 | JARVIS can read the current screen's semantic content and describe it to the user | V8 |
| FR-7 | JARVIS can operate another app's UI via accessibility actions to complete a stated goal | V7/V9 |
| FR-8 | JARVIS can chain 2+ tool calls to satisfy one compound request, verifying each step | V9 |
| FR-9 | JARVIS can answer questions requiring current information via web search | V10 |
| FR-10 | JARVIS remembers user-approved preferences and recent task context across sessions | V11 |
| FR-11 | Core wake-word detection and a defined set of actions work with no network connection | V12 |
| FR-12 | Every consequential action requires and honors explicit user confirmation | V6 onward, hardened V13 |

## Non-Functional Requirements

- **Reliability**: A tool call either succeeds, fails with a clear reason, or is retried with a
  bounded number of attempts. It never leaves the UI or device state ambiguous without telling
  the user.
- **Latency**: Conversational round-trip (end of user speech → start of JARVIS speech) targets
  under ~2.5s on a mid-range device with network reachable; degraded-but-honest behavior when not.
- **Battery**: Any always-on component (wake-word listening) is budgeted and measured; see
  `17_PERFORMANCE.md`. No background component may cause the OS to flag the app for excessive
  battery use.
- **Privacy**: No sensitive data (screen content, notifications, audio, contacts) is transmitted
  off-device except as required to fulfill the specific active request, and only after the user
  has enabled the relevant capability.
- **Security**: See `13_SECURITY.md` — least privilege, confirmation gating, injection resistance.
- **Compatibility**: Target minimum SDK is decided in `04_TECH_STACK.md`; capability tables in
  `06_ANDROID_CAPABILITIES.md` call out version-gated behavior explicitly.
- **Maintainability**: Modules are separated so that, e.g., swapping the STT engine does not
  require touching the planner, and swapping the AI model provider does not require touching the
  tool registry.

## Explicit Non-Requirements (for now)

- Root-level device control.
- Guaranteed control of arbitrary third-party apps that expose no accessible UI semantics.
- Fully offline equivalent of the cloud reasoning model.
- Multi-user / family account support.
- iOS or cross-platform support.

## Constraints

- Must be distributable through normal channels (Google Play) as a primary goal; sideload-only
  distribution is treated as a fallback documented per-capability, not the default plan, because
  it drastically limits reach and update mechanics.
- Must use a modern Gemini model via API as the primary cloud reasoning layer (per project
  vision), with the AI architecture designed so the provider is swappable behind an interface
  (see `07_AI_ARCHITECTURE.md`, `ADR-002`).
