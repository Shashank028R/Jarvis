# ADR-003: Two-Tier Wake Word Architecture

**Status**: Accepted
**Context**: Android does not give a normal third-party app free, low-power, always-on hotword
detection equivalent to the system Assistant's; achieving that requires the Assistant role and
hardware DSP support, both device/OS-dependent and, per real-world reports, subject to reliability
issues (Secure Settings reset on reinstall) — see `06_ANDROID_CAPABILITIES.md`.
**Decision**: Ship a Tier 1 baseline (explicit-toggle, foreground-service, local keyword-spotting
model, honest persistent notification and battery cost) as the guaranteed experience, with Tier 2
(Assistant-role, hardware hotword) as an optional, separately evaluated enhancement, not a
baseline promise.
**Consequences**: The product does not overclaim "always listening, no battery cost" hands-free
behavior; users get a working, honest hands-free mode from V5 regardless of Tier 2's outcome.
**Alternatives Considered**: Assistant-role-only approach (rejected — too fragile/uncertain to be
the sole path to a core feature); no wake word at all (rejected — conflicts with the product
vision's core hands-free requirement).
