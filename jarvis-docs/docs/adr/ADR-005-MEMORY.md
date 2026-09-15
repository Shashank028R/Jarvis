# ADR-005: Local-Only, Opt-In-by-Default Memory

**Status**: Accepted
**Context**: Long-term memory is highly sensitive personal data; the constitution requires privacy
to be architectural, not a bolted-on feature (`12_MEMORY_ARCHITECTURE.md`).
**Decision**: Memory beyond explicit user preferences is off by default, stored locally only
(Keystore-encrypted Room database), with no cloud sync in the current roadmap, and with granular
user controls (per-item delete, delete-all, global off) shipped in the same version that
introduces persistence (V11), not deferred to a later hardening pass.
**Consequences**: No cross-device memory continuity for now; this is an accepted trade-off in
favor of a materially simpler and more defensible privacy posture at launch.
**Alternatives Considered**: Cloud-synced memory from the start (rejected — the privacy/security
review burden and user trust cost outweigh the convenience at this stage; revisit only via a new
ADR if genuinely needed later); always-on automatic memory of everything discussed (rejected —
directly conflicts with Constitution Rule 7 and the "never remembered automatically" list).
