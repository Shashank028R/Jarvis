# ADR-001: Android Technology Stack

**Status**: Accepted
**Context**: Need a modern, testable, maintainable Android stack appropriate for a long-running,
multi-year agentic assistant project (see `04_TECH_STACK.md` for full rationale).
**Decision**: Kotlin + Jetpack Compose + Coroutines/Flow + Hilt + Room + DataStore + WorkManager +
Foreground Services, with a multi-module Gradle structure matching `05_SYSTEM_ARCHITECTURE.md`.
**Consequences**: Strong testability and separation of concerns; Compose's animation capabilities
support the V4 activation sequence directly; module boundaries prevent architectural drift
(`:accessibility` cannot silently become a dependency of unrelated modules).
**Alternatives Considered**: XML Views + LiveData (rejected — declarative UI better matches the
state-machine-driven design); a single-module monolith (rejected — would make the intentional
isolation of `:accessibility` and `:security` impossible to enforce structurally).
