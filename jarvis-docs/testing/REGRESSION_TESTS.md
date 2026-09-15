# Regression Test Suite

Every test listed here must pass on every CI run from the version noted onward, permanently. This
file is appended to as each version locks — never pruned, except when a documented, deliberate
breaking change (Constitution Rule 8) removes a capability and the corresponding test is retired
with a note explaining why and pointing to the ADR that authorized the removal.

## Format

```
[VN] Test name — one-line description of what it guards against
```

## Suite (populated as versions lock)

```
[V1] clean_checkout_builds — project builds from a fresh clone with documented commands
[V1] module_dependency_direction — no disallowed inter-module dependency edges
[V2] conversation_multiturn_coherence — replies remain contextually coherent across a fixed turn set
[V2] no_api_key_in_client_artifact — build-artifact scan finds no embedded API key
[V3] voice_state_machine_transitions — all defined transitions behave correctly, including barge-in
[V3] record_audio_denied_graceful_fallback — denial doesn't crash, falls back to text UI
[V4] activation_animation_state_sync — animation stays synced to TTS start/stop across states
[V5] wake_word_toggle_off_stops_service — disabling the toggle fully stops mic access immediately
[V5] pre_wake_audio_not_transmitted — only post-wake-word audio ever leaves the local buffer
[V6] policy_engine_full_matrix — every risk-tier × permission-state combination behaves correctly
[V6] ungranted_tool_unavailable_to_planner — model never offered a tool it lacks permission for
[V7] sensitive_target_exclusion — password/payment fields never targetable by accessibility tools
[V7] no_confirmation_no_execution — HIGH-risk accessibility action never executes without a fresh confirm
[V8] privacy_filter_full_redaction — planted sensitive fields never reach the AI request payload
[V9] adapt_retry_bound_enforced — a deliberately-looping plan is bounded and reported, not infinite
[V9] partial_failure_accurate_reporting — partial multi-step failure reported precisely, not collapsed
[V10] injection_via_search_result_blocked — crafted search content can't bypass the confirmation gate
[V11] memory_global_off_blocks_all_writes — verified with an explicit attempted-write test
[V11] memory_store_encrypted_at_rest — raw DB file is not plaintext-readable
[V12] offline_tool_gating — network-dependent tools correctly unavailable when offline, no hangs
[V13] full_prompt_injection_suite — formalized adversarial-content suite, all vectors blocked
[V13] full_schema_fuzzing_suite — malformed tool-call inputs rejected without crash
```

(Continue appending as V14/V15 and beyond lock.)
