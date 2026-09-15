# V7 — Accessibility Integration

## Purpose
Introduce the Accessibility Bridge for cross-app UI observation/automation, built specifically to
the confirmed-bounded-automation design in `10_ACCESSIBILITY_ARCHITECTURE.md`, given this is the
project's single highest compliance/reliability risk area.

## PRE-REQUISITE (blocking, not a formality)
Before implementation begins: review current Google Play Accessibility API policy against the
design in `10_ACCESSIBILITY_ARCHITECTURE.md`, and, ideally, seek explicit Play Console guidance on
the declaration/compliance approach. This is tracked as an open item in `DOCUMENTATION_REVIEW.md`
and this version's Phase 1 formally includes it as a gating task, not implementation work.

## Goals
Implement `click`, `typeText`, `swipe`, `dismissAppToHome` as HIGH-risk, and `pressBack` as LOW-risk,
confirmation-gated tools operating via `AccessibilityNodeInfo` matching and global accessibility actions,
scoped to the single app the user is actively directing JARVIS to control.

## Features
- `AccessibilityService` declared, with a clear onboarding flow explaining what it's for and
  directing the user to the special-access settings screen (cannot be auto-enabled).
- Node-matching strategy: content-description → text → resource-id, no coordinate-based primary
  strategy (`10_ACCESSIBILITY_ARCHITECTURE.md`).
- `dismissAppToHome()` tool implemented via `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)`.
- Confirmation UI: before a HIGH-risk action executes, JARVIS states the specific proposed action
  grounded in the actual current screen ("I'll tap the search icon — go ahead?").
- Explicit exclusion at the schema level of security-sensitive targets (password fields, payment
  confirmation UI).

## Dependencies
V6's Tool Registry/Policy Engine/Executor pattern.

## Architectural Changes
`:accessibility` module becomes real, isolated behind a narrow interface consumed only by the
Tool Executor — no other module depends on it directly, so it can be constrained or swapped
without touching the planner or UI.

## New Components
- `JarvisAccessibilityService`.
- `NodeMatcher` (priority-ordered matching strategy).
- `AccessibilityConfirmationPrompt` UI flow.
- Onboarding screen explaining the accessibility permission request.

## User Experience
"Jarvis, tap the search icon in this app" → JARVIS reads the current screen, proposes the specific
tap, asks for confirmation, executes only after "yes."

## Permissions
`AccessibilityService` (special access, manual grant only, cannot be requested via a normal
runtime dialog).

## Security Considerations
This is where `13_SECURITY.md`'s confirmation-gate design is proven against the actual highest-
risk tool category. Security testing here is not optional or abbreviated.

## Testing
- Unit: `NodeMatcher` against synthetic accessibility trees of varying label quality.
- Instrumentation: against a purpose-built test app with deliberately varied labeling (good
  content-descriptions, text-only, no labels at all) verifying correct match / graceful "not
  found" behavior in each case.
- Security: attempt automation against a password-field-containing test screen — assert it's
  excluded at the schema level.
- Manual acceptance: real-world test against 2–3 popular real apps (e.g., a browser, a settings
  screen) for practical reliability, documented honestly including failure cases found.

## Acceptance Criteria
- [ ] Compliance review of Phase 1 gating task is complete and documented (decision recorded even
      if the decision is "proceed with sideload-only distribution for now").
- [ ] Node matching correctly handles all three test-app labeling scenarios.
- [ ] Confirmation prompt correctly blocks execution until explicit user approval, every time,
      for every HIGH-risk invocation (no caching across distinct calls).
- [ ] Security-sensitive target exclusion is enforced and tested.
- [ ] Graceful "I couldn't find that" behavior when node matching fails, no coordinate-guessing.

## Known Limitations
Apps with custom-rendered, unlabeled UI remain out of reach; `FLAG_SECURE` apps are correctly
excluded entirely; UI changes in target apps can break matching over time (documented, not solved).

## Exit Criteria
Acceptance criteria met, including the compliance gating task; V1–V6 regression suite passes.

## Next Version Dependencies
V8 (Screen Understanding) and V9 (Multi-Step Agent) both depend on this bridge being reliable and
correctly scoped before building further capability on top of it.

---
## Phases
1. **Compliance review & flavor gating** — document Play policy analysis for the `playStore` flavor and verify the `fullAssistant` flavor allows unconstrained sideload distribution.
2. **`AccessibilityService` scaffold & onboarding** — service declaration, settings deep link,
   explanation UI.
3. **`NodeMatcher`** — priority-ordered matching against synthetic and real trees.
4. **Confirmation UI & Policy Engine extension** — HIGH-risk gating specific to accessibility
   targets, security-sensitive exclusion list.
5. **Tool executors** — `click`, `typeText`, `swipe`, `dismissAppToHome`, `pressBack`.
6. **Test-app-based reliability testing** — the purpose-built varied-labeling test app.
7. **Real-app manual acceptance & stabilization** — lock version.
