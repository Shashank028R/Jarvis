# V1 — Project Foundation

## Purpose
Establish a stable, testable, correctly-modularized Android project skeleton with nothing
functional yet — the scaffolding every later version builds on.

## Goals
- Multi-module Gradle project matching `05_SYSTEM_ARCHITECTURE.md` module boundaries.
- Explicit SDK baseline configured across all modules: `compileSdk = 35`, `targetSdk = 35`, `minSdk = 29`.
- Dual-flavor build setup configured in `:app`: `playStore` (Play Store compliant) and `fullAssistant` (unrestricted power-user/sideload).
- CI pipeline running unit tests and lint on every change.
- An app that launches to a placeholder black screen and does nothing else.
- Dependency injection wired (Hilt) with no real dependencies yet, just the graph skeleton.

## Features
None user-facing. This version is pure scaffolding.

## Dependencies
None (first version).

## Architectural Changes
- Establishes the module structure: `:app`, `:voice`, `:orchestrator`, `:ai`, `:tools`,
  `:android-integration`, `:accessibility`, `:memory`, `:security`, `:core`.
- Configures Gradle build flavors (`playStore`, `fullAssistant`).
- Pre-configures AndroidManifest `<queries>` declarations in `:app` and `:android-integration` for package visibility.

## New Components
- Gradle module skeletons with placeholder classes proving the dependency graph compiles.
- `:core` shared result types (`Result<Success, Failure>` sealed class) used by every later
  module.
- CI configuration (build + unit test + lint on push).
- Empty `MainActivity` with a black Compose surface.

## User Experience
App opens to a black screen. Nothing else. This is intentional.

## Permissions
None requested.

## Security Considerations
Establish the pattern: no secrets in source control from day one (`.gitignore` for local
properties, placeholder for future relay endpoint config).

## Testing
- Unit test: each module's placeholder compiles and its DI graph resolves.
- CI test: pipeline runs green on a trivial change.

## Acceptance Criteria
- [ ] Project builds from a clean checkout with one documented command.
- [ ] CI runs and passes on push.
- [ ] App installs and launches to a black screen with no crash.
- [ ] Module boundaries match `05_SYSTEM_ARCHITECTURE.md` exactly.
- [ ] SDK versions (`compileSdk = 35`, `targetSdk = 35`, `minSdk = 29`) are uniformly configured.
- [ ] Both `playStoreDebug` and `fullAssistantDebug` build variants compile successfully.

## Known Limitations
No functionality. This is expected and correct for V1.

## Exit Criteria
All acceptance criteria checked, `PROGRESS.md` updated, version tagged as locked.

## Next Version Dependencies
V2 depends on the `:ai` and `:core` module skeletons existing and the DI graph being extensible.

---

## Phases

### Phase 1 — Repository & Tooling Setup
**Objective**: Initialize the Git repo, Gradle config, Kotlin/Compose versions, SDK levels, product flavors, CI.
**Prerequisites**: None.
**Tasks**: Create root `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`,
`.github/workflows/ci.yml`. Configure `compileSdk = 35`, `targetSdk = 35`, `minSdk = 29`, and define `flavorDimensions += "distribution"` with flavors `playStore` and `fullAssistant`.
**Files affected**: `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`,
`.github/workflows/ci.yml`.
**Expected behavior**: `./gradlew assemblePlayStoreDebug assembleFullAssistantDebug` succeeds on a fresh checkout.
**Testing**: CI pipeline runs and passes on an empty/no-op commit across both flavors.
**Acceptance criteria**: Clean build, CI green, SDK versions verified.
**Failure conditions**: Build fails, CI misconfigured.
**Completion criteria**: Committed, CI green.

### Phase 2 — Module Skeleton & Manifest Queries
**Objective**: Create all ten modules from `05_SYSTEM_ARCHITECTURE.md` with minimal placeholder
content, correct inter-module dependency declarations, and `<queries>` package visibility setup.
**Prerequisites**: Phase 1 complete.
**Tasks**: Create each module's `build.gradle.kts`, one placeholder Kotlin file per module. In `:app/src/main/AndroidManifest.xml`, declare `<queries><intent><action android:name="android.intent.action.MAIN"/></intent></queries>` to support cross-app launching.
**Files affected**: `voice/`, `orchestrator/`, `ai/`, `tools/`, `android-integration/`,
`accessibility/`, `memory/`, `security/`, `core/`, `app/`.
**Expected behavior**: Full project graph builds; dependency direction matches the architecture
doc (e.g. `:app` may depend on `:orchestrator`, `:orchestrator` must not depend on `:app`).
**Testing**: A dependency-direction lint/script check asserting no disallowed module depends on `:app`.
**Acceptance criteria**: Graph builds; no disallowed dependency edges.
**Completion criteria**: Merged, CI green.

### Phase 3 — Core Shared Types
**Objective**: Implement `:core`'s shared result/error types used by every later module.
**Tasks**: `Result<T>` sealed class (`Success`, `Failure(reason: String, cause: Throwable?)`),
basic coroutine dispatcher-provider abstraction for testability.
**Testing**: Unit tests for the `Result` type's mapping/folding functions.
**Acceptance criteria**: 100% unit test coverage on this small, foundational type.

### Phase 4 — DI Graph Skeleton
**Objective**: Wire Hilt across all modules with placeholder bindings.
**Testing**: App launches with DI graph resolved (a runtime assertion, not just compile-time).
**Acceptance criteria**: App launches without a DI-related crash.

### Phase 5 — Placeholder UI
**Objective**: `MainActivity` renders a black Compose surface — the literal starting point for
V4's activation animation later, but with zero animation logic yet.
**Testing**: Compose UI test asserting the root surface renders and is black.
**Acceptance criteria**: App installs, launches, shows black screen, no crash on rotation.

### Phase 6 — CI Hardening & Documentation Sync
**Objective**: Ensure CI runs unit tests + lint on every push/PR; update `PROGRESS.md`.
**Acceptance criteria**: CI green; `PROGRESS.md` reflects V1 phase completion.

### Phase 7 — Stabilization
**Objective**: Fix any flakiness found in Phases 1–6, confirm a clean checkout builds and runs
reliably across at least two developer machines/CI runners.
**Completion criteria**: V1 exit criteria fully met; version locked.
