# 16 — Error Handling

## Failure Taxonomy

| Category | Example | Handling Principle |
|---|---|---|
| Permission missing | Tool needs `RECORD_AUDIO`, not granted | Tool not offered to planner at all; UI explains what enabling it would unlock |
| Network unavailable | `searchWeb` call times out | Reported plainly; degrade to local/offline capability if one exists for this request (V12+), otherwise say so |
| Target not found | Accessibility bridge can't find "the blue button" | Report "I couldn't find that on screen" rather than guessing via coordinates |
| Ambiguous result | Multiple plausible search results with no clear best match | Ask the user to choose rather than picking arbitrarily |
| Partial multi-step failure | Step 1 succeeds, step 2 fails | Report exactly which parts succeeded and which didn't; never collapse to a single "done" or "failed" |
| Model produced invalid tool call | Schema validation rejects it | Retried once with corrective context if within the same turn's stated intent; otherwise surfaced as "I'm not able to do that safely" |
| User declines confirmation | HIGH-risk action confirmation declined | Task step aborted immediately, no retry without a fresh user request |
| Unexpected exception | Any unhandled runtime error in a tool executor | Caught at the Tool Executor boundary, converted to a `Failure(reason)` result — never crashes the Orchestrator or leaves the voice state machine stuck |

## Golden Rule (Constitution Rule 12)

JARVIS never asserts an action succeeded unless the Observation Collector actually confirmed it.
"I've started that" and "That's done" are different claims and must not be used interchangeably.

## Voice-Specific Error Handling

- STT failure (couldn't transcribe) → JARVIS asks the user to repeat, does not silently drop the
  turn.
- TTS failure (engine unavailable) → falls back to on-screen text display of the response rather
  than failing silently with no feedback at all.
- Wake-word false accept → activation animation plays, JARVIS listens briefly, and if no
  meaningful utterance follows within a short window, returns to idle without treating silence as
  a failed request needing an apology (avoid being annoying on false triggers).

## Retry Policy

- LOW-risk tool calls: up to 1 automatic retry on transient failure (e.g., a flaky Intent resolve),
  silently to the user if it succeeds on retry.
- MEDIUM/HIGH-risk tool calls: no silent automatic retry; any retry is a new, visible attempt the
  user is aware of.
- Agent loop overall: bounded to 2 ADAPT cycles per multi-step task before reporting incomplete
  status, to prevent infinite loops from a persistently wrong plan.

## User Communication Standards

- Error messages are specific enough to be actionable ("YouTube doesn't appear to be installed" not
  "Something went wrong").
- Errors never expose raw stack traces, internal exception messages, or API error payloads to the
  user in release builds; a debug-only verbose mode exists behind a developer setting for
  engineering use.
