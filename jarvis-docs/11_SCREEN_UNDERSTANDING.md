# 11 — Screen Understanding

## Goal

Let JARVIS answer "what's on my screen" / "explain this page" / "find the Wi-Fi setting" by
building a structured understanding of the current screen, preferring non-visual (accessibility
tree) data over pixels, and falling back to vision only when necessary.

## Pipeline

```mermaid
flowchart TD
    Trigger[User asks about screen] --> Tree[Accessibility tree snapshot]
    Tree --> Rich{Tree has enough<br/>semantic info?}
    Rich -->|yes| Parse[Screen Parser:<br/>build structured node summary]
    Rich -->|no, e.g. custom canvas| Shot[takeScreenshot via MediaProjection,<br/>per-session consent]
    Shot --> Vision[Vision model pass]
    Parse --> Struct[Structured screen representation]
    Vision --> Struct
    Struct --> Filter[Privacy filter: redact sensitive fields]
    Filter --> Planner[AI Planner]
    Planner --> Action[Action Planner<br/>e.g. click(target)]
    Planner --> Answer[Natural-language answer to user]
```

## Preferring the Accessibility Tree Over Screenshots

Reading the node tree is cheaper, faster, and does not require the `MediaProjection` per-session
consent dialog (which the user would otherwise see every single time). Screenshots/vision are used
only when the tree is too sparse to answer the question (custom-rendered UI) — this is both a
privacy and a UX decision (fewer consent interruptions).

## Token-Efficient Tree Pruning (Latency & Context Optimization)

Raw Android accessibility trees frequently contain 200+ nodes, which would exhaust LLM token limits and exceed the 2.5s latency budget. Before serialization into AI context, `ScreenParser` applies deterministic pruning:
1. **Container Pruning**: Strip all non-interactive, transparent layout wrappers (`FrameLayout`, `LinearLayout`, `ViewGroup`, `Box`) that contain no direct text or content descriptions.
2. **Viewport Clipping**: Filter out any node whose bounding coordinates lie completely outside the active display viewport.
3. **Semantic Attribute Condensation**: Retain only actionable attributes: `text`, `contentDescription`, `resourceId` (short stem), `isClickable`, `isEditable`, `isScrollable`, `boundsInScreen`.
4. **Node Budget Capping**: Enforce an upper bound of 40 highest-prominence nodes per screen turn, sorted by visual hierarchy and interactivity.

## Privacy Filtering (mandatory, not optional)

Before any screen-derived structured representation reaches the cloud AI model:

- Fields that look like credentials, OTP/2FA codes, card numbers, or are inside password-type
  input nodes are redacted at the Screen Parser stage, never forwarded.
- If the user asks about a screen containing such fields, JARVIS can describe *that* a sensitive
  field is present without transmitting its value.
- Screenshots taken for the vision fallback are processed and discarded — not persisted to disk
  or included in memory/history — unless the user has an explicit "save this" action for a given
  response, which is a distinct, deliberate user action, not a default.

## What "Explain This Page" Produces

The Screen Parser's structured output (headings, buttons, key text blocks, current focus) is
handed to the AI Planner as `converse()` context, producing a natural-language summary — not a
literal read-out of every node, which would be both unhelpful and closer to reproducing another
app's UI content wholesale than a genuine explanation.

## Interplay with the Accessibility Policy Constraint

Screen understanding (V8) is read-only by default — it does not, by itself, trigger any action.
Combining "what's on screen" with "now click X" is where V8 (understanding) hands off to V7's
Accessibility Bridge (action), and the confirmation rules in `10_ACCESSIBILITY_ARCHITECTURE.md`
apply to the action half exactly as if the target had been named directly by the user.

## Limitations

- Vision-based understanding is bound by whatever the underlying multimodal model can reliably
  extract from a screenshot — small text, low contrast, or unusual UI conventions reduce accuracy,
  and the AI Planner is expected to express uncertainty rather than guess confidently.
- `MediaProjection` consent must be re-obtained per capture session on current Android versions;
  this is a real, unavoidable UX friction point that must be explained to the user rather than
  hidden or worked around.
