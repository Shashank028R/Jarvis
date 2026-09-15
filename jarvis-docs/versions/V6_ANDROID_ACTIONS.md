# V6 — Basic Android Actions

## Purpose
Deliver the first real device-control tools using the well-supported, low-risk half of Android's
API surface (per `06_ANDROID_CAPABILITIES.md`), establishing the Tool Registry / Policy Engine /
Tool Executor pattern before touching anything Accessibility-based.

## Goals
Implement `08_AGENT_AND_TOOL_SYSTEM.md`'s architecture for real, with the LOW/MEDIUM-risk tools
listed as "Introduced: V6."

## Features
- `openApp`, `setTimer`, `setAlarm`, `controlMedia`, `playMediaIntent`, `changeVolume`, `openSettings`,
  `readNotifications`.
- The AI Planner (extending `AiClient` beyond pure `converse()` to `interpret()` for
  `SingleToolCall` decisions).
- Real Policy Engine with risk-tier confirmation gating.
- Real Security/Audit Log.

## Dependencies
V2's `AiClient`, V3's voice loop for spoken confirmation prompts.

## Architectural Changes
`:tools`, `:android-integration`, and `:security` modules become real. Orchestrator's agent loop
now supports the single-tool-call subset of `UNDERSTAND → PLAN → ACT → OBSERVE → VERIFY →
COMPLETE` (no multi-step chaining yet — that's V9).

## New Components
- `ToolRegistry`, `PolicyEngine`, `ToolExecutor`, `AuditLog` (real implementations).
- Concrete tool executors for each listed tool against Intents/AlarmManager/MediaSessionManager/
  NotificationListenerService.

## User Experience
"Jarvis, set a 15-minute timer" works end-to-end, spoken, with correct confirmation behavior for
`readNotifications` (MEDIUM tier).

## Permissions
`POST_NOTIFICATIONS` (if targeting a version requiring it for JARVIS's own notifications),
Notification access (special access, for `readNotifications`), no new dangerous permissions
beyond what V3 already requested.

## Security Considerations
This version is where the Policy Engine's confirmation gating must be proven correct before any
higher-risk tool exists — see the security test list below.

## Testing
- Unit: Policy Engine decision matrix (every risk tier × permission-state combination).
- Integration: each tool executor against a fake Android Integration Layer.
- Instrumentation: each tool executor against real Android APIs (e.g., verify `setTimer` actually
  produces the system dialog/clock app timer).
- Security: attempt to invoke a tool the model shouldn't have been offered (permission not
  granted) — assert it's structurally unavailable, not just blocked at execution.

## Acceptance Criteria
- [ ] Each listed tool works correctly end-to-end via spoken command.
- [ ] MEDIUM-tier tool (`readNotifications`) produces the correct notice-before-execute behavior.
- [ ] Denying `readNotifications`'s special-access grant results in the tool being unavailable to
      the planner, not a runtime crash.
- [ ] Audit log correctly records every tool invocation attempt and outcome.

## Known Limitations
No screen reading, no cross-app UI automation, no multi-step chaining yet.

## Exit Criteria
Acceptance criteria met; V1–V5 regression suite passes.

## Next Version Dependencies
V7 extends the Tool Registry/Policy Engine pattern to Accessibility-based tools; V9 extends the
Orchestrator's single-tool-call loop into true multi-step chaining.

---
## Phases
1. **Tool Registry & schemas** — declarative `ToolDefinition`s for the V6 tool set.
2. **Policy Engine** — risk-tier confirmation logic, permission-gated tool offering to the planner.
3. **Tool Executor + Android Integration Layer** — real implementations per tool.
4. **Audit Log** — append-only local record, viewable in a debug/settings screen.
5. **Planner extension** — `AiClient.interpret()` producing `SingleToolCall` decisions.
6. **End-to-end voice-to-action wiring** — full loop from spoken command to executed tool to
   spoken confirmation.
7. **Stabilization** — security + regression pass, lock version.
