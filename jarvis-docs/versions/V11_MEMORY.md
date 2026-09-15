# V11 — Memory

## Purpose
Implement persistent memory per `12_MEMORY_ARCHITECTURE.md`, with full user controls, opt-in by
default off for anything beyond explicit preferences.

## Goals
- Encrypted Room-backed long-term store for task history and explicit user-stated facts.
- DataStore-backed preferences (already partially used since V4 for UI toggles; now formalized).
- A "Memory" settings screen: view, per-item delete, delete-all, global off switch.
- Bounded retrieval feeding the AI Planner (recency + simple relevance), not full-history dumping.

## Dependencies
V9 (task history has something to record), V2 (`AiClient` context construction gains a memory-
retrieval input).

## Architectural Changes
`:memory` module becomes real: Room database (Keystore-encrypted), retrieval logic, and the
settings UI surface.

## New Components
- `MemoryStore` (Room + encryption).
- `MemoryRetriever` (bounded, simple relevance + recency selection).
- Memory settings screen (list, delete, delete-all, global toggle).

## User Experience
"Remember that my mother's name is Sarah" is stored only on explicit request; "what did I ask you
to remind me about" retrieves accurately; the user can see and delete everything stored at any
time.

## Permissions
None new (local storage only, no new runtime permission).

## Security Considerations
Encryption-at-rest via Android Keystore-backed key is tested explicitly (not just "we added
encryption" — verify the raw DB file is not plaintext-readable).

## Testing
- Unit: `MemoryRetriever` relevance/recency selection logic.
- Integration: `MemoryStore` CRUD + encryption verification (attempt to read the raw file,
  confirm it's not plaintext).
- Manual acceptance: global off switch verified to prevent any write, tested by attempting to
  trigger a memory-worthy interaction with the switch off and confirming nothing is stored.

## Acceptance Criteria
- [ ] Explicit "remember X" requests are stored and later retrievable.
- [ ] Nothing is stored automatically without an explicit user-facing trigger (per
      `12_MEMORY_ARCHITECTURE.md`'s "never remembered automatically" list).
- [ ] Global memory-off switch verified to block all writes.
- [ ] Per-item and delete-all controls work and are immediate/irreversible.
- [ ] Raw database file is confirmed encrypted, not plaintext.

## Known Limitations
No cross-device sync; relevance retrieval is simple (recency + keyword-ish matching), not
embedding-based, unless a later ADR upgrades it.

## Exit Criteria
Acceptance criteria met; V1–V10 regression suite passes.

## Next Version Dependencies
V13's security hardening pass includes a dedicated review of the memory store's encryption and
retrieval-scoping correctness.

---
## Phases
1. **`MemoryStore` schema & encryption** — Room + Keystore-backed key, encryption verification
   test.
2. **Explicit-write pathways** — "remember X" intent handling, task-history auto-logging (still
   opt-in per category).
3. **`MemoryRetriever`** — bounded retrieval logic wired into `AiClient` context construction.
4. **Memory settings screen** — view/delete/delete-all/global-off.
5. **Global-off enforcement testing** — the explicit "verify nothing is written" test.
6. **Stabilization** — regression pass, lock version.
