# 18 — Development Workflow

## Branching & Versioning

- `main` always reflects the latest **locked** version — it is always in a shippable, stable state.
- Work for the current version happens on a version branch (`version/vN`), merged to `main` only
  when that version's exit criteria (defined in its `versions/VN_*.md` file) are met.
- Within a version branch, each phase is a small, reviewable commit or PR, in phase order.

## Review Gates

Before a phase is considered complete:
1. Its defined tests pass (unit/integration/instrumentation as applicable).
2. Its acceptance criteria (from `versions/VN_*.md`) are checked off.
3. `PROGRESS.md` is updated.
4. Any architectural decision made during the phase that wasn't anticipated in the docs has a
   corresponding ADR and doc update in the same change (Constitution Rule 10).

Before a version is locked:
1. All its phases are complete.
2. Its version-level exit criteria are met.
3. Regression tests for all previously-locked versions still pass.
4. `20_VERSION_ROADMAP.md` and `PROGRESS.md` are updated to reflect the lock.

## Working with an AI Coding Agent

See `19_ANTIGRAVITY_INSTRUCTIONS.md` for the exact operating procedure. In summary: the agent
always re-reads `PROGRESS.md` at the start of a session to determine the current phase, never
assumes it from conversation memory alone, since the ground truth lives in the repository, not in
any one conversation.

## Handling Scope Creep

If, mid-phase, a clearly-better approach for a *future* version becomes apparent, it is recorded
as a note in `DOCUMENTATION_REVIEW.md` or a new ADR — not implemented early. The current phase's
scope, as written, is what gets built.

## Handling Platform Changes

Android/Play policy changes (a new OS version, a Play policy update) that affect an already-locked
version's assumptions trigger: an update to `06_ANDROID_CAPABILITIES.md` first, then a review of
every version file that depended on the changed assumption, tracked as a maintenance task, not
silently patched in isolation.
