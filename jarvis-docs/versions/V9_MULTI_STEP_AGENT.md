# V9 — Multi-Step Agent

## Purpose
Implement the full agent loop (`UNDERSTAND → PLAN → ACT → OBSERVE → VERIFY → ADAPT → ACT AGAIN →
COMPLETE`) for compound requests spanning multiple tool calls, per `08_AGENT_AND_TOOL_SYSTEM.md`.

## Goals
- `AiClient.interpret()` extended to produce `MultiStepPlan` decisions.
- Orchestrator drives the full loop with bounded ADAPT retries.
- `sendMessage` tool introduced as "compose and stop before send" per the capability table.

## Features
- Compound requests like "play Believer on YouTube and set a 15-minute timer" work end-to-end,
  including partial-failure honest reporting.
- Per-step confirmation gating still applies exactly as in single-step tool use — multi-step
  status does not relax any HIGH-risk confirmation requirement.

## Dependencies
V6 (Tool Registry/Policy/Executor), V7 (Accessibility Bridge), V8 (screen observations for
verification).

## Architectural Changes
`:orchestrator` gains real plan-execution state management: step sequencing, dependency
resolution (a step's target may depend on the previous step's observation, resolved at execution
time, not planned as a hardcoded value), and the ADAPT retry-bound logic.

## New Components
- `PlanExecutor` (sequences steps, tracks partial completion state).
- `VerificationLoop` (calls `AiClient.verify()` after each step, decides continue/adapt/fail).
- `sendMessage` tool (compose-and-stop-before-send behavior).

## User Experience
Compound spoken requests work naturally; JARVIS reports precisely what succeeded and what didn't
on partial failure (Constitution Rule 12 / `16_ERROR_HANDLING.md`).

## Permissions
No new permissions beyond V6–V8; this version is about orchestration logic, not new API surface.

## Security Considerations
Rate/abuse bound on tool calls per turn (`08_AGENT_AND_TOOL_SYSTEM.md`) is implemented and tested
here for the first time, since this is the first version where chaining could otherwise loop.

## Testing
- All scenario tests defined in `15_TESTING_STRATEGY.md`'s agentic scenario section, including
  every listed failure variant.
- Unit: `PlanExecutor` step-dependency resolution and ADAPT retry-bound logic.
- Integration: full loop against fake tool executors covering every branch (success, failure,
  ambiguous, declined confirmation).

## Acceptance Criteria
- [ ] The canonical "play song + set timer" scenario passes across both Intent-First primary execution and Accessibility UI fallback paths, including all documented failure variants.
- [ ] ADAPT retries are bounded (max 2) and this is verified with a test that would otherwise loop
      indefinitely.
- [ ] Partial-failure reporting is accurate and specific in every failure-variant test.
- [ ] Per-step confirmation gating is verified not to be weakened by multi-step context (a test
      asserting two consecutive HIGH-risk steps each require their own confirmation).

## Known Limitations
Plans are bounded in length (a sane maximum, e.g. 8 steps) to keep the abuse/rate bound
meaningful; genuinely long/complex tasks are reported as needing to be broken into smaller
requests rather than attempted as one unbounded plan.

## Exit Criteria
Acceptance criteria met; V1–V8 regression suite passes.

## Next Version Dependencies
V10 integrates web-search results into plans; V11 lets completed-task history feed memory.

---
## Phases
1. **`PlanExecutor` scaffold** — step sequencing, dependency resolution model.
2. **`VerificationLoop`** — `verify()` integration, continue/adapt/fail decision logic.
3. **ADAPT retry bounding & abuse-rate limiting** — with the deliberately-looping test case.
4. **`sendMessage` tool** — compose-and-stop-before-send implementation.
5. **Canonical scenario implementation** — the "song + timer" test end-to-end (Intent-First primary path + Accessibility UI navigation fallback).
6. **Full failure-variant test suite** — every documented failure path.
7. **Stabilization** — regression pass, lock version.
