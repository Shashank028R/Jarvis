# Device Test Matrix

## Tiers (fill in specific real device models once hardware is allocated for testing)

| Tier | Definition | Representative Use |
|---|---|---|
| Low-end | Minimum supported Android version, budget SoC, ≤4GB RAM | Battery/latency worst-case, AccessibilityService reliability on constrained hardware |
| Mid-range | 2–3 year old flagship or current mid-tier, most common real-world tier | Primary target for the day-to-day performance budgets in `17_PERFORMANCE.md` |
| High-end | Latest flagship | Upper-bound performance validation, latest-OS-version feature checks |

## OEM Coverage Notes

AccessibilityService behavior, foreground-service battery management, and notification-access
grants have historically varied across OEM Android skins (e.g., aggressive battery optimization
killing foreground services on some manufacturers' devices). At minimum, testing should include
one device from a manufacturer known for aggressive background-process management, in addition to
a close-to-stock-Android device, specifically to validate V5 (wake word) and V7 (accessibility)
survive real-world OEM behavior, not just emulator/stock-AOSP behavior.

## Android Version Coverage

Test against the declared minimum SDK (set in `04_TECH_STACK.md` / V1) and the latest stable
release available at each version's testing time, since capability tables in
`06_ANDROID_CAPABILITIES.md` are explicitly version-gated in several places (e.g., foreground
service type declarations, `MediaProjection` consent behavior).

## Matrix Status

To be populated with actual allocated test devices before V5 (first version with meaningful
device-dependent risk) begins implementation.
