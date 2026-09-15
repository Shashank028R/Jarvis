# 07 — AI Architecture

## Role of the AI Layer

The AI layer (Gemini-backed) is responsible for language understanding, planning, tool selection,
response generation, and interpreting execution results — never for directly executing anything.
It communicates exclusively through structured tool-call requests validated against JSON schemas
declared in the Tool Registry (`08_AGENT_AND_TOOL_SYSTEM.md`). This separation exists so a
malformed or malicious model output can be rejected by schema validation before it ever reaches
device APIs.

## `AiClient` Interface

```kotlin
interface AiClient {
    suspend fun interpret(input: ConversationTurn, context: PlannerContext): PlannerDecision
    suspend fun verify(observation: ToolObservation, context: PlannerContext): VerificationResult
    suspend fun converse(input: ConversationTurn, context: PlannerContext): TextResponse
}
```

`PlannerDecision` is a sealed type: `ConversationalReply`, `SingleToolCall`, `MultiStepPlan`, or
`NeedsClarification`. This interface is the only thing the Orchestrator depends on; the concrete
Gemini implementation lives behind it (`ADR-002`), so a different model provider could be swapped
in without touching the Orchestrator, Tool Registry, or Policy Engine.

## Routing: Static Knowledge vs. Current Information vs. Device Action vs. User-Specific

The planner's first job on any input is classification:

- **Static knowledge** ("explain recursion") → answered directly by the model, no tool call.
- **Current information** ("why did gold prices rise today") → routed to a web-search tool
  (`searchWeb`), result summarized by the model with the actual retrieved content, not from the
  model's training data alone.
- **Device action** ("turn on Bluetooth", "play a song") → routed to the relevant tool(s) in the
  registry.
- **User-specific information** ("what did I ask you to remind me about") → routed to the Memory
  Store query interface, not re-derived from the model's general knowledge.

This routing itself is a model decision (few-shot / system-prompt guided), validated by the fact
that each branch produces a structurally different `PlannerDecision` type the Orchestrator can
type-check.

## Planning for Multi-Step Requests (V9+)

For a compound request, the planner produces an ordered list of tool calls with declared
dependencies (step 3 depends on the output of step 2, e.g., "click the search result" needs the
actual result list from step 2's observation — so steps that depend on runtime UI state are
planned as *intent* ("select the best matching result") rather than a hardcoded index, resolved
against the real observation at execution time). See the `VERIFY`/`ADAPT` responsibilities below.

## Verification and Adaptation

After each tool execution, the Observation Collector's structured result is sent back to
`AiClient.verify()`. The model determines: did this step succeed as intended, and if not, is there
a reasonable next action (retry, alternate approach, ask the user) or should the task be reported
as failed. This is what prevents JARVIS from confidently claiming "done" when a step silently
failed (Rule 12, Constitution).

## Prompt/Context Construction

Context sent to the model per turn includes: the current user utterance, a bounded window of
recent conversation turns, relevant retrieved memory (if V11+ is active and the user has opted
in), the declared tool schemas currently available given granted permissions (a tool the user has
not granted permission for is not even offered to the model as an option), and — for
verification calls — the specific tool observation, not the full conversation history, to keep
that call's context minimal and unambiguous.

## Failure Modes to Design For

- Model returns a tool call referencing a tool that does not exist or has invalid parameters →
  rejected by schema validation, surfaced to the user as "I couldn't figure out how to do that
  safely" rather than retried blindly.
- Model is confidently wrong about what a screenshot/accessibility tree shows → mitigated by
  requiring the Observation Collector to pass back the actual structured data alongside any
  AI-generated interpretation, so downstream verification isn't purely trusting the first
  interpretation.
- Network unavailable → planner calls fail gracefully; Orchestrator falls back to whatever local
  capability exists (V12) or tells the user plainly that it needs a connection.
- Prompt injection via untrusted content (a malicious web page's text, a manipulated screen
  element) attempting to make the model emit a dangerous tool call → this is a `13_SECURITY.md`
  concern; the Policy Engine treats *all* tool-call requests identically regardless of what
  prompted them, so injected instructions cannot escalate privilege beyond what the Policy Engine
  independently allows.
