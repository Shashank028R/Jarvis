# 08 — Agent & Tool System

## The Agent Loop

```
UNDERSTAND → PLAN → ACT → OBSERVE → VERIFY → ADAPT → ACT AGAIN → COMPLETE
```

Implemented by the Orchestrator; each arrow is a well-defined state transition with a bounded
timeout and a defined maximum number of ADAPT retries (default 2) before the task is reported to
the user as incomplete rather than looping indefinitely.

## Tool Registry

Every tool is a declarative entry, not ad-hoc code the model can invoke freely:

```kotlin
data class ToolDefinition(
    val name: String,
    val description: String,          // shown to the model, not the user
    val inputSchema: JsonSchema,
    val outputSchema: JsonSchema,
    val riskTier: RiskTier,           // LOW, MEDIUM, HIGH, CRITICAL
    val requiredPermissions: List<AndroidPermission>,
    val requiresForegroundUi: Boolean, // some tools cannot run while app is backgrounded
    val executionTimeoutMs: Long = 5000L, // execution timeout before fail/abort
    val isIdempotent: Boolean = false,    // whether retrying this tool is safe without side effects
    val cancellationHandler: (suspend () -> Unit)? = null // rollback/cleanup hook on task abort
)
```

The model is only ever offered the subset of `ToolDefinition`s whose `requiredPermissions` are
currently granted — this means an ungranted permission doesn't just block execution, it prevents
the model from even planning around a capability that doesn't exist yet, avoiding a confusing
"I tried but it failed" experience where "I don't have that capability yet" is more honest.

## Initial Tool Catalog (introduced across versions, not all at once)

| Tool | Risk Tier | Introduced | Notes |
|---|---|---|---|
| `searchWeb(query)` | LOW | V10 | Grounded search API |
| `getWeather(location)` | LOW | V10 | Weather API |
| `openApp(packageOrName)` | LOW | V6 | Standard launch intent via PackageManager |
| `setTimer(minutes)` | LOW | V6 | `AlarmClock.ACTION_SET_TIMER` |
| `setAlarm(time)` | LOW | V6 | `AlarmClock.ACTION_SET_ALARM` |
| `controlMedia(action)` | LOW | V6 | MediaSession transport controls |
| `playMediaIntent(query, app)` | LOW | V6 | Direct Intent-First media playback (e.g. YouTube search/watch URI) |
| `changeVolume(stream, level)` | LOW | V6 | AudioManager streams |
| `openSettings(screen)` | LOW | V6 | Settings panel deep-links |
| `readNotifications(filter)` | MEDIUM | V6 | Requires Notification Access special grant |
| `readScreen()` | MEDIUM | V8 | Accessibility node tree semantic summary |
| `takeScreenshot()` | MEDIUM | V8 | MediaProjection fallback (per-session consent dialog) |
| `click(target)` / `typeText(target, text)` / `swipe(direction)` | HIGH | V7 | Accessibility-gated, confirmed before action |
| `dismissAppToHome()` | HIGH | V7 | Accessibility `GLOBAL_ACTION_HOME` to exit another app |
| `pressBack()` | LOW | V7 | Accessibility `GLOBAL_ACTION_BACK` |
| `makeCall(contact)` | HIGH | Deferred | `ACTION_DIAL` is LOW tier; `ACTION_CALL` is HIGH tier |
| `sendMessage(app, contact, text)` | HIGH | V9 | Delivers as "compose and stop before send" by default |

Risk tier determines the default confirmation policy (`13_SECURITY.md`): LOW executes
immediately, MEDIUM surfaces a brief on-screen/spoken notice, HIGH always requires explicit
confirmation regardless of user settings. CRITICAL targets (passwords, 2FA, payments) are blocked at the schema level.

## Policy / Permission Engine

Every tool call, regardless of origin, passes through:

1. **Schema validation** — reject malformed calls outright.
2. **Permission check** — is the required Android permission currently granted?
3. **Risk-tier confirmation check** — does this call's tier require user confirmation, and if so
   has it been obtained for *this specific invocation* (confirmations are not cached/reused across
   different parameter values for HIGH-risk tools)?
4. **Rate/abuse check** — bounded number of tool calls per conversation turn to prevent runaway
   loops (protects against both AI misbehavior and prompt-injection-driven loops).

Only after all four checks pass does the Tool Executor run.

## Extensibility Rule

Adding tool #N+1 means: add a `ToolDefinition`, implement its executor against the Android
Integration Layer, write its tests (happy path + failure paths), and register it — no existing
tool's code is touched. This is verified structurally by keeping each tool's executor in its own
file/class with no shared mutable state between tools beyond what's explicitly passed through the
Android Integration Layer's own interfaces.

## What the Agent Must Never Do

- Never invent a tool call outside the registry.
- Never chain HIGH-risk tool calls without a confirmation between each one, even within a single
  multi-step plan.
- Never treat a MEDIUM/HIGH tool's previous confirmation as blanket approval for future, different
  invocations.
- Never retry a failed HIGH-risk action automatically — retries for HIGH tier require the user to
  be told a retry is being attempted, not attempted silently.
