# V12 — Offline Capabilities

## Purpose
Define and implement exactly what works without network connectivity, honestly bounded per
`06_ANDROID_CAPABILITIES.md` §11 (no claim of a full offline Gemini-equivalent).

## Goals
- Wake-word detection (V5's Tier 1) continues to function with no network (it already runs
  locally — this version verifies and hardens that, and defines what happens after detection with
  no network: e.g., "I heard you, but I don't have a connection right now" rather than hanging).
- A small, explicitly-scoped set of local commands function without network: local device actions
  that don't require the AI planner at all could be special-cased (e.g., a hardcoded "stop"/
  "cancel" command), evaluated and documented rather than assumed.
- Every network-dependent tool fails gracefully and honestly when offline, per
  `16_ERROR_HANDLING.md`.

## Dependencies
V5 (wake word), V10 (defines what must degrade), V9 (agent loop must handle mid-task network loss).

## Architectural Changes
Introduces a `ConnectivityMonitor` in `:core` that the Orchestrator and Policy Engine consult
before offering network-dependent tools to the planner, mirroring the permission-gating pattern
already used for Android permissions.

## New Components
- `ConnectivityMonitor`.
- Offline-specific response templates (clear, non-repetitive "no connection" messaging).
- (If justified after review) a minimal local-only command path bypassing the cloud AI planner
  entirely for a small, fixed set of safety/utility commands (e.g., "stop", "cancel", "stop
  listening").

## User Experience
Losing connectivity mid-conversation or mid-task produces a clear, non-confusing explanation
rather than a silent hang or a confidently wrong answer.

## Permissions
None new.

## Security Considerations
None new; ensure the local-only command path (if implemented) cannot be tricked into bypassing the
Policy Engine's confirmation requirements just because it's a "local" path.

## Testing
- Integration: simulate connectivity loss at every stage of the V9 agent loop (before plan,
  mid-plan, during verification) and assert honest, non-hanging behavior in each case.
- Manual acceptance: airplane-mode test of wake word + local commands.

## Acceptance Criteria
- [ ] Wake word functions correctly with no network.
- [ ] Every network-dependent tool fails with a clear, specific message when offline, in every
      agent-loop stage tested.
- [ ] No agent-loop stage hangs indefinitely waiting on an unreachable network call (bounded
      timeouts everywhere).
- [ ] If a local-only command path is implemented, it correctly still goes through the Policy
      Engine's confirmation rules for anything above LOW risk.

## Known Limitations
No offline equivalent of the cloud reasoning model; offline capability is intentionally narrow
and explicitly documented as such to the user (a visible "offline mode" indicator), not implied to
be a full experience.

## Exit Criteria
Acceptance criteria met; V1–V11 regression suite passes.

## Next Version Dependencies
V13 reviews the offline path for any security shortcuts taken under connectivity-loss handling.

---
## Phases
1. **`ConnectivityMonitor`** — real-time connectivity state exposed to Orchestrator/Policy Engine.
2. **Network-dependent tool gating** — tools unavailable to the planner when offline, mirroring
   the permission-gating pattern.
3. **Timeout auditing** — bounded timeouts added anywhere previously unbounded.
4. **Offline messaging** — clear, honest user-facing copy for every offline scenario.
5. **(If justified) local-only command path** — implementation with Policy Engine compliance
   verified.
6. **Airplane-mode manual acceptance & stabilization** — lock version.
