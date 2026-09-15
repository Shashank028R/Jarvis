# JARVIS — Personal AI Assistant for Android

## What This Is

JARVIS is a long-term Android application project: a personal AI agent that understands natural
language, converses by voice, and — where Android legitimately allows it — takes action on the
user's device and inside other apps. This repository is **documentation-only**. It is the
blueprint an AI coding agent (e.g. Google Antigravity) or a human engineering team will use to
build the product **one locked version at a time**. No application source code is produced at
this stage, by design (see `00_PROJECT_CONSTITUTION.md`, Rule 1).

## Project Goals

1. Ship a genuinely useful, trustworthy voice-first assistant, not a tech demo.
2. Be honest about what Android's platform and Google Play's policies actually permit — never
   design around a fictional set of capabilities.
3. Grow capability incrementally: each version is small enough to build, test, and stabilize
   before the next one starts.
4. Treat security and privacy as first-class architecture, not an afterthought bolted on in v13.
5. Produce documentation precise enough that an AI coding agent needs no clarifying assumptions.

## Current Version

**Pre-V1 — Specification Complete.** No code has been written. The next step is `V1_FOUNDATION`.
See `PROGRESS.md` for the live status ledger.

## How This Documentation Is Organized

```
jarvis-docs/
├── 00_PROJECT_CONSTITUTION.md      Non-negotiable rules for every contributor (human or AI)
├── 01_PROJECT_VISION.md            Why JARVIS exists, what it is and is not
├── 02_PRODUCT_REQUIREMENTS.md      Functional & non-functional requirements
├── 03_USER_EXPERIENCE.md           Personality, voice, activation animation, UI direction
├── 04_TECH_STACK.md                Technology choices and rationale
├── 05_SYSTEM_ARCHITECTURE.md       Component architecture, data flow, Mermaid diagrams
├── 06_ANDROID_CAPABILITIES.md      Ground-truth research: what Android actually permits
├── 07_AI_ARCHITECTURE.md           Gemini integration, planning, reasoning pipeline
├── 08_AGENT_AND_TOOL_SYSTEM.md     Tool registry, policy engine, execution loop
├── 09_VOICE_AND_WAKE_WORD.md       STT/TTS/VAD/wake-word technical design
├── 10_ACCESSIBILITY_ARCHITECTURE.md AccessibilityService design & Play Store constraints
├── 11_SCREEN_UNDERSTANDING.md      Screen parsing, vision fallback, privacy filtering
├── 12_MEMORY_ARCHITECTURE.md       What is remembered, how, where, and for how long
├── 13_SECURITY.md                  Threat model, confirmation policy, prompt-injection defense
├── 14_PRIVACY.md                   Data classification, retention, user controls
├── 15_TESTING_STRATEGY.md          Test pyramid and agentic scenario testing
├── 16_ERROR_HANDLING.md            Failure taxonomy and recovery behavior
├── 17_PERFORMANCE.md               Latency, battery, memory budgets
├── 18_DEVELOPMENT_WORKFLOW.md      Branching, review, versioning process
├── 19_ANTIGRAVITY_INSTRUCTIONS.md  Exact operating procedure for the AI coding agent
├── 20_VERSION_ROADMAP.md           V1–V15 index and dependency graph
├── DOCUMENTATION_REVIEW.md         Consistency review, risks, open questions
├── PROGRESS.md                     Version/phase status ledger (kept up to date by the agent)
├── versions/V1_FOUNDATION.md … V15_PRODUCTION.md   One file per version, phase-by-phase
├── testing/                        Test plan, acceptance criteria, regression & device matrix
└── docs/adr/                       Architectural Decision Records
```

## How an AI Coding Agent Should Read This

Follow `19_ANTIGRAVITY_INSTRUCTIONS.md` exactly. In short: read the constitution, then the
architecture docs relevant to the current version, then `PROGRESS.md` to find the current
phase, then the specific `versions/VN_*.md` file — and implement **only** that phase.

## Version Progression

`V1 Foundation → V2 AI Conversation → V3 Voice → V4 JARVIS UI → V5 Wake Word → V6 Android
Actions → V7 Accessibility Agent → V8 Screen Understanding → V9 Multi-Step Agent → V10 Web
Intelligence → V11 Memory → V12 Offline → V13 Security Hardening → V14 Performance → V15
Production.`

Full rationale for this order — including why UI/activation comes before wake word, and why
accessibility automation is deliberately placed after the core agent loop is proven with safer
tools — is in `20_VERSION_ROADMAP.md`.

## Development Philosophy

**Design → Implement → Test → Verify → Stabilize → Lock Version → Move to Next Version.**
No version starts before the previous one is locked. No phase is skipped. No feature is
implemented "while we're in there" if it belongs to a later version.

## Testing Philosophy

Every capability that touches the device, another app, or the network gets both a happy-path
test and an explicit failure-path test (permission denied, app not installed, no network, wrong
UI state, ambiguous AI output). Agentic multi-step tasks are tested as scenarios with defined
expected state transitions, not just "did it not crash." See `15_TESTING_STRATEGY.md`.

## Known Limitations (read before assuming JARVIS can do something)

- Android does not allow a normal third-party app to run a low-power always-listening wake-word
  detector with the same privileges as the system Assistant without holding the **Assistant
  role**, and even then hardware hotword support varies by device.
- Google Play policy (Accessibility API policy, effective use restricts **autonomous** AI-driven
  UI automation unless the app is declared and qualifies as a genuine accessibility tool for
  users with disabilities — see `10_ACCESSIBILITY_ARCHITECTURE.md`). This materially affects the
  distribution strategy for V7–V9 and is the single biggest open risk in the project; it is
  tracked in `DOCUMENTATION_REVIEW.md`.
- Full "control anything on the phone" is not achievable on stock, non-rooted Android. The
  architecture targets the largest legitimate subset of that goal and documents the rest as
  unavailable.

Do not start writing code until you have read `00_PROJECT_CONSTITUTION.md` and
`06_ANDROID_CAPABILITIES.md`.
