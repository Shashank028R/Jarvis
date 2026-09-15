# ADR-004: Confirmed, Bounded Accessibility Automation

**Status**: Accepted, with an open compliance follow-up tracked for resolution no later than V15
**Context**: Google Play's Accessibility API policy prohibits autonomous AI-driven UI automation
by apps not declared/qualifying as accessibility tools for users with disabilities
(`06_ANDROID_CAPABILITIES.md`, `10_ACCESSIBILITY_ARCHITECTURE.md`). JARVIS's core value
proposition depends on AI-planned UI interaction.
**Decision**: Design the Accessibility Bridge around per-action user confirmation grounded in the
actual current screen state, narrow single-app scoping, and exclusion of security-sensitive
targets — moving the design as far toward the "deterministic, user-directed" end of the policy
spectrum as the product goal allows, rather than building freeform autonomous automation and
hoping it's tolerated.
**Consequences**: Some fluidity/autonomy the original vision described (fully hands-off multi-step
automation) is deliberately traded for compliance defensibility and, arguably, for user trust and
safety regardless of policy. The open compliance question (does this design actually satisfy Play
Console review) is not resolved by this ADR alone and is explicitly gated before V7 begins
(Phase 1 of `versions/V7_ACCESSIBILITY_AGENT.md`) and finally closed out in V15.
**Alternatives Considered**: Fully autonomous automation as originally envisioned (rejected — high
risk of policy violation and account suspension); dropping Accessibility-based automation entirely
(rejected — would remove a core piece of the product vision without first attempting a compliant
design); declaring JARVIS as an accessibility tool regardless of true primary purpose (rejected —
would be a misrepresentation to Google Play, a compliance and ethical problem, not a solution).
