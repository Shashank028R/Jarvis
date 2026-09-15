# V13 — Security Hardening

## Purpose
A deepening security pass across everything built so far (V6 device actions, V7 accessibility, V9
agent loop, V11 memory), rather than introducing new user-facing features.

## Goals
- Formal threat-model review against `13_SECURITY.md`, updated with anything learned during
  V6–V12 implementation.
- Penetration-style testing of the confirmation gate against adversarial inputs.
- Certificate pinning for the API relay (evaluated, implemented if it doesn't create unacceptable
  maintenance burden for key rotation).
- Full audit-log review UI polish (this was functional but minimal since V6).

## Features
No new user-facing tools. Hardening of existing ones.

## Dependencies
V6, V7, V9, V11.

## Architectural Changes
Potential: certificate pinning in the network layer; potential: stricter schema validation
tightening based on any fuzzing findings.

## New Components
- Security test harness formalizing the prompt-injection and confirmation-bypass scenarios as a
  permanent regression suite (`testing/REGRESSION_TESTS.md`).
- Any remediation components arising from the threat-model review (documented as they arise, not
  pre-specified here since this version's exact content depends on what V6–V12 actually reveal).

## User Experience
No visible change ideally, beyond a more complete audit-log/security settings view.

## Permissions
None new expected.

## Security Considerations
This entire version *is* the security consideration. Deliverable: an updated `13_SECURITY.md`
reflecting any findings, plus a formal sign-off checklist before V14.

## Testing
- Full prompt-injection scenario suite (screen content, web content, notification content vectors).
- Fuzzing of every tool's schema validation with malformed/adversarial inputs.
- Confirmation-bypass attempts across all HIGH-risk tools.
- Permission-revocation-mid-session tests across all tools introduced since V6.

## Acceptance Criteria
- [ ] Threat-model review complete and documented, with any findings remediated or explicitly
      accepted-and-documented as a residual risk.
- [ ] Full security regression suite passes and is wired into CI permanently.
- [ ] No HIGH-risk tool can be triggered without a fresh, specific confirmation in any tested
      adversarial scenario.

## Known Limitations
Security hardening is never "complete" — this version establishes a baseline and a permanent
regression suite, not a final state.

## Exit Criteria
Acceptance criteria met; full V1–V12 regression suite passes.

## Next Version Dependencies
V14's performance work must not silently reintroduce a security shortcut in the name of speed
(e.g., caching a confirmation to reduce latency) — this is called out explicitly as a prohibited
optimization.

---
## Phases
1. **Threat-model review** — formal pass across all versions, documented findings.
2. **Prompt-injection & confirmation-bypass test suite** — formalized and added to CI.
3. **Schema fuzzing** — across every tool.
4. **Certificate pinning evaluation & implementation** — if net-positive.
5. **Remediation** — fix any findings from Phases 1–4.
6. **Sign-off & documentation update** — `13_SECURITY.md` updated to reflect final state.
7. **Stabilization** — full regression pass, lock version.
