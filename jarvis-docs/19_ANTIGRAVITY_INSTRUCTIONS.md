# 19 — Instructions for the AI Coding Agent

This document is written directly for an AI coding agent (e.g. Google Antigravity) operating on
this repository. Follow it exactly.

## Standing Operating Procedure

1. **Read `00_PROJECT_CONSTITUTION.md`.** These rules override convenience and speed.
2. **Read `05_SYSTEM_ARCHITECTURE.md`** to understand current component boundaries.
3. **Read `PROGRESS.md`** to determine the current locked version and current in-progress phase.
   Do not infer this from prior conversation — the file is the source of truth.
4. **Read the specific `versions/VN_*.md` file** for the current version, and locate the current
   phase within it.
5. **Read any numbered doc (`06`–`17`) that the current phase's tasks touch** — e.g., a phase
   touching AccessibilityService must re-read `10_ACCESSIBILITY_ARCHITECTURE.md` even if it was
   read in a previous session.
6. **Produce an implementation plan** for the current phase only, listing files to be created or
   changed.
7. **Implement only the approved phase.** Do not begin work on a later phase or a later version's
   tools/components even if the code would naturally seem to lead there.
8. **Run the tests defined for this phase.**
9. **Fix failures**, re-running tests until they pass, or stop and report a blocker if the phase's
   spec itself appears to conflict with platform reality discovered during implementation (do not
   silently work around a spec conflict — flag it).
10. **Re-verify acceptance criteria** from the version file against the actual implementation.
11. **Update `PROGRESS.md`** with the phase's new status, tests passed, and any known limitations
    discovered.
12. **Report what changed** in plain terms: files touched, behavior added, tests added, anything
    deferred or flagged.
13. **Stop.** Do not proceed to the next phase without a new instruction, even if it seems like
    the obvious next step.

## Hard Prohibitions

The agent must NOT:
- Skip reading the documentation for the current phase because a similar phase was implemented
  before.
- Jump ahead to a future version's tools or UI, even partially, "to save time later."
- Rewrite or refactor unrelated code outside the current phase's declared scope.
- Delete or disable a working, tested feature from a locked version without an explicit
  instruction and a corresponding doc update explaining the breaking change.
- Ignore a failing test in order to mark a phase complete.
- Hard-code any API key, token, or secret in source code — the relay pattern in
  `04_TECH_STACK.md` / `13_SECURITY.md` is mandatory from V2 onward.
- Assume a permission is granted; always code against the "not granted" path as a first-class
  case, not an afterthought.
- Implement a capability `06_ANDROID_CAPABILITIES.md` marks as "restricted/unreliable" or
  "unavailable" as if it were fully supported.
- Make an architectural change (e.g., swapping a library, changing a module boundary) without
  writing the corresponding ADR in the same change.

## When the Agent Is Unsure

If a phase's instructions are ambiguous, the agent should choose the most conservative,
least-privileged interpretation consistent with the Constitution, implement that, and note the
ambiguity in its phase report and in `DOCUMENTATION_REVIEW.md` rather than guessing at an
ambitious interpretation and building on top of it.

## Session Boundaries

Each work session should correspond to at most one phase. If a phase turns out to be too large to
complete in one session, the agent stops at a safe, testable intermediate point, updates
`PROGRESS.md` with precisely what remains, and resumes from that note next session — it does not
carry undocumented partial state forward in memory alone.
