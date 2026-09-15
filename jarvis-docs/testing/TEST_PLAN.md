# Test Plan

This document indexes the testing approach; the authoritative strategy lives in
`../15_TESTING_STRATEGY.md`. This file tracks the concrete, per-version test suite composition.

## Suite Composition by Version

| Version | Unit | Integration | Instrumentation | Manual Acceptance | Security |
|---|---|---|---|---|---|
| V1 | Module graph, Result type | — | App launch | Clean-checkout build | — |
| V2 | Session windowing | Fake-HTTP AiClient | — | Conversation coherence review | Relay auth |
| V3 | Voice state machine | STT/TTS Robolectric | Real-device STT accuracy | Interruption feel | Audio non-persistence |
| V4 | — | Compose state rendering | Frame-rate on min-spec | Design originality review | — |
| V5 | KWS trigger logic | FGS lifecycle | Real-room wake test | Battery benchmark | Pre-wake audio non-leak |
| V6 | Policy Engine matrix | Fake tool executors | Real API tool execution | — | Ungrantable-tool test |
| V7 | NodeMatcher | Synthetic tree matching | Varied-labeling test app | Real-app reliability | Sensitive-field exclusion |
| V8 | PrivacyFilter | ScreenParser summaries | Fallback trigger logic | Answer quality/honesty | Redaction completeness |
| V9 | PlanExecutor, retry bound | Full-loop fake executors | — | Canonical scenario + variants | Rate/abuse bound |
| V10 | Routing classifier | Fake search/weather | — | Accuracy spot-check | Injection via search results |
| V11 | MemoryRetriever | Store CRUD + encryption | — | Global-off enforcement | Encryption verification |
| V12 | — | Connectivity-loss simulation | — | Airplane-mode test | Local-path confirmation compliance |
| V13 | — | — | — | — | Full threat-model + fuzzing + bypass suite |
| V14 | — | — | Device-tier benchmarks | Long-session stability | Non-regression of V13 suite |
| V15 | — | — | Pre-launch report | Store listing accuracy | Final sign-off checklist |

## Continuous Regression

Every locked version contributes tests to `REGRESSION_TESTS.md`'s permanent suite, run on every
subsequent version's CI pipeline without exception.
