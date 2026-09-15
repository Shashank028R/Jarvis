# 12 — Memory Architecture

## Categories

| Category | Examples | Default | Storage |
|---|---|---|---|
| Short-term conversation memory | Last N turns of the active session | On (session-scoped, cleared on session end unless summarized) | In-memory, not persisted |
| Long-term user preferences | "Address me as Sir" toggle, preferred units, default confirmation policy | On (these are explicit settings, not inferred) | DataStore |
| Task history | "Set a timer at 3pm for laundry", outcome | Off by default; opt-in | Room (encrypted at rest via platform keystore-backed encryption) |
| Contextual facts about the user | "My mother's name is X", inferred preferences | Off by default; opt-in, and even then only explicit statements, never inferred without confirmation | Room, encrypted |

## What Should Never Be Remembered Automatically

- Screen content, notification content, or accessibility-tree data encountered while performing a
  task — these are used transiently for the task and discarded, not folded into long-term memory,
  unless the user explicitly says "remember that."
- Anything read from a third-party app's UI during an automation step.
- Sensitive fields identified by the privacy filter in `11_SCREEN_UNDERSTANDING.md`.
- Precise location history (JARVIS may use current location for a specific request like weather,
  but does not build a location history log).

## User Controls (required, not aspirational)

- A visible "Memory" settings screen listing everything stored, with per-item delete and a
  "delete all" action.
- A global "Memory: Off" switch that, when set, prevents any write to the long-term stores
  regardless of what the AI infers is worth remembering.
- Memory writes always originate from an explicit code path the user can audit conceptually (e.g.
  "I asked JARVIS to remember X" or "JARVIS logged that this task completed"), never a hidden
  side-channel of a normal conversational answer.

## Storage & Security

- Long-term memory (Room database) is encrypted at rest using a key backed by the Android
  Keystore, not a hardcoded or app-derived key.
- No cloud sync of memory in the current roadmap; if added later, it is opt-in, end-to-end
  consideration documented in a dedicated ADR before implementation, given the sensitivity.

## Retrieval

The AI Planner never receives the full memory store as context. A bounded retrieval step (recency
+ simple relevance matching to the current request, expanded to embedding-based retrieval only if
the simple approach proves insufficient) selects a small number of relevant memory items per turn,
keeping both cost and unnecessary exposure of unrelated personal data to a minimum per request.

## Data Lifecycle

- Short-term memory: cleared when a conversation session ends (app backgrounded past a timeout, or
  explicit "new conversation").
- Task history / long-term facts: retained until the user deletes them or a configurable
  auto-expiry (e.g., 90 days) the user can adjust or disable.
- All deletions are immediate and irreversible — no soft-delete/trash state that could confuse the
  user about whether data is really gone.
