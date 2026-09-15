# 17 — Performance

## Budgets

| Metric | Target | Notes |
|---|---|---|
| Cold app start → ready to listen | < 2s on a mid-range device | Excludes first-run permission flows |
| End of user speech → start of JARVIS speech (network reachable) | < 2.5s p50 | Includes STT finalization + planner round trip + TTS start |
| Wake-word detection latency | < 500ms from utterance end | Local KWS model, no network round trip |
| Wake-word listening battery draw (Tier 1 foreground-service mode) | Measured and documented per device class; must not trigger OS battery-usage warnings in a full day of typical use | Benchmarked in V5 acceptance tests |
| Memory footprint, idle | < 150MB resident | Prevents OS from aggressively killing the foreground service |
| Multi-step task (3–5 tool calls) total wall time | < 8s typical, excluding steps that inherently take longer (e.g., waiting for another app's UI to render) | |

## Battery Strategy

- No component polls continuously when an event-driven API exists instead (e.g., use
  `NotificationListenerService` callbacks, not polling notifications).
- Foreground services are stopped immediately when their purpose ends (listening session closed,
  task complete), not kept alive "just in case."
- Wake-word Tier 1 mode is user-toggleable and clearly shown as active (persistent notification is
  both a policy requirement and a battery-transparency feature).

## Latency Strategy

- Streaming STT (partial results) begins planner-relevant work (e.g., pre-fetching likely tool
  schemas) before the user finishes speaking where safe to do so, without triggering any action
  prematurely.
- TTS begins speaking the first sentence of a response while the rest of a longer response is
  still being generated, where the model API supports streaming, to reduce perceived latency.

## Performance Testing

- Benchmarked on a defined device tier set (documented in `testing/DEVICE_TEST_MATRIX.md`), not
  only on high-end development hardware.
- Regression-tested: a version cannot be locked if it regresses a previous version's benchmark
  beyond an agreed tolerance (e.g., +10%) without an explicit, documented justification.
