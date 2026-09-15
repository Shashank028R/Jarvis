# 00 — Project Constitution

These rules are binding on every contributor, human or AI. They outrank convenience, speed, and
any single feature request. If a later document appears to conflict with this one, this document
wins and the conflict must be logged in `DOCUMENTATION_REVIEW.md`.

## Rule 1 — Specification Before Code
No production code is written until the specification for the current version and current phase
exists and has been read. This documentation set fulfills that requirement for the pre-V1 stage.

## Rule 2 — One Locked Version at a Time
Development happens on exactly one version at a time, in the order defined in
`20_VERSION_ROADMAP.md`, unless a documented architectural dependency requires reordering (in
which case an ADR must be written first). A version is not "started" until the prior version has
passed its exit criteria and been tagged as locked in `PROGRESS.md`.

## Rule 3 — Phases Are Atomic and Sequential
Within a version, phases are implemented in order. A phase is not begun until the previous phase's
acceptance criteria are met and tests pass. An AI coding agent must not implement phase N+2 while
phase N+1 is incomplete, even if it seems efficient to do so.

## Rule 4 — No Capability Is Assumed
Every claim about what Android, Google Play, or a third-party app allows must be traceable to
`06_ANDROID_CAPABILITIES.md` (or a cited, dated source added to it). If a desired feature turns
out to be unavailable or unreliable, the documentation is updated to reflect reality — the
documentation does not get to be more optimistic than the platform.

## Rule 5 — Least Privilege by Default
No permission, role, or API is requested until the version that needs it. No tool in the agent's
tool registry executes an action more privileged than what the calling intent requires. See
`13_SECURITY.md`.

## Rule 6 — Confirm Before Consequential Action
Any action that sends data externally, spends money, deletes data, or is otherwise hard to undo
requires explicit user confirmation before execution, regardless of how confident the AI planner
is. This cannot be disabled by the AI itself, only by an explicit, auditable user setting.

## Rule 7 — Privacy Is Architecture, Not a Feature
Screen content, notification content, conversation memory, and audio are sensitive by default.
Nothing sensitive leaves the device to a cloud service unless it is necessary for the specific
request being served, and the user can see and delete what has been sent. See `14_PRIVACY.md`.

## Rule 8 — Backwards Compatibility Within a Major Line
A new version must not silently break a capability an earlier, locked version delivered. If a
capability must change behavior, it is a documented breaking change with a migration note in that
version's file, not a silent regression discovered later.

## Rule 9 — Tests Are Not Optional
No phase is marked complete without its defined tests passing, including explicit failure-path
tests. "It worked when I tried it once" is not an acceptance criterion.

## Rule 10 — Documentation Is Updated With the System
Any architectural decision made during implementation that was not anticipated in these docs
produces an ADR (`docs/adr/`) and an update to the relevant numbered document and `PROGRESS.md`
in the same change. Undocumented architecture drift is treated as a bug.

## Rule 11 — No Premature Flash
Visual polish, personality flourishes, and "wow" features are sequenced after the underlying
mechanism they decorate is proven to work. The activation animation (V4) exists once conversation
and voice already work (V2–V3), not before.

## Rule 12 — Honesty Over Ambition in User-Facing Claims
JARVIS never tells the user it did something it did not actually verify. If a tool call's result
is uncertain, the AI says so instead of asserting success. See `16_ERROR_HANDLING.md`.

## Rule 13 — Reality Check Trumps the Original Prompt
Where the product vision (this document set was originally derived from a founder's prompt)
asks for something Android/Play policy does not allow, the closest legitimate alternative is
documented and the gap is called out explicitly, not silently dropped or silently faked.
