# V10 — Web / Internet Intelligence

## Purpose
Route "current information" requests to real web search rather than the model's static training
data, per the routing design in `07_AI_ARCHITECTURE.md`.

## Goals
- `searchWeb(query)` and `getWeather(location)` tools (LOW tier).
- Correct routing classification: static knowledge vs. current information vs. device action vs.
  user-specific information.
- Answers cite/ground responses in actually-retrieved content, not fabricated "current" facts.

## Dependencies
V2 (`AiClient`), V9 (planner can incorporate a web-search step into a multi-step plan, e.g. "search
the internet and tell me why gold prices increased today").

## Architectural Changes
`:tools` gains web-search/weather executors calling out to real external APIs (via the same
server-side relay pattern as the AI calls, to avoid embedding third-party API keys client-side).

## New Components
- `SearchWebTool`, `GetWeatherTool` executors.
- Routing classifier logic (implemented as part of the `interpret()` prompt/response contract,
  tested via the fixed-utterance routing test set from `15_TESTING_STRATEGY.md`).

## User Experience
"What's the weather tomorrow?" and "search the internet for the latest Android news" produce
accurate, current, clearly-sourced-feeling answers rather than stale or fabricated ones.

## Permissions
None new beyond `INTERNET` (already declared).

## Security Considerations
Treat all fetched web content as untrusted input per `13_SECURITY.md`'s prompt-injection handling
— a malicious page's embedded instructions must not be treated as user instructions.

## Testing
- Unit: routing classifier against the fixed utterance test set (static/current/device/
  user-specific).
- Integration: `SearchWebTool`/`GetWeatherTool` against fake HTTP responses.
- Security: prompt-injection scenario using crafted search-result content attempting to trigger an
  unconfirmed tool call — assert the confirmation gate still fires.
- Manual acceptance: spot-check answer accuracy/currency against known current events.

## Acceptance Criteria
- [ ] Routing classifier correctly categorizes the full fixed test set.
- [ ] Current-information answers are grounded in actually-retrieved content, verified by manual
      spot-check.
- [ ] Prompt-injection scenario test passes (confirmation gate not bypassed).

## Known Limitations
Bound by underlying search API coverage/rate limits/cost; not a general web-browsing agent.

## Exit Criteria
Acceptance criteria met; V1–V9 regression suite passes.

## Next Version Dependencies
V12 (Offline) must define what happens to these tools with no network — graceful, honest failure.

---
## Phases
1. **Routing classifier implementation & test set** — the four-category fixed utterance set.
2. **`SearchWebTool`** — relay-backed search integration.
3. **`GetWeatherTool`** — relay-backed weather integration.
4. **Prompt-injection hardening pass** — untrusted-content handling for search results.
5. **Manual accuracy review** — spot-check against known current events.
6. **Stabilization** — regression pass, lock version.
