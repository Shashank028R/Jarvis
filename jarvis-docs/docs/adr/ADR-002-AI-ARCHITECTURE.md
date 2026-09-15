# ADR-002: AI Provider Abstraction

**Status**: Accepted
**Context**: The project vision specifies Gemini as the primary reasoning layer, but provider
lock-in at the architecture level would be a long-term liability (`07_AI_ARCHITECTURE.md`).
**Decision**: All AI interaction goes through an `AiClient` interface; only one concrete
implementation (`GeminiAiClient`) exists initially, but no other module is permitted to reference
Gemini-specific types or endpoints directly.
**Consequences**: Provider swap or multi-provider support later is a `:ai`-module-only change.
Slightly more indirection than calling the SDK directly from the orchestrator.
**Alternatives Considered**: Direct SDK calls from `:orchestrator` (rejected — violates module
boundary and creates provider lock-in against the constitution's maintainability principle).
