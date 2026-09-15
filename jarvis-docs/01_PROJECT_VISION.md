# 01 — Project Vision

## What JARVIS Is

JARVIS is a personal AI assistant Android application. Its long-term purpose is to let a user
speak naturally — "Jarvis, play Believer by Imagine Dragons on YouTube and set a 15-minute timer"
— and have the assistant understand the intent, plan the steps, execute what Android legitimately
permits, verify the result, and report back in natural language.

## What JARVIS Is Not

- **Not a scripted command list.** JARVIS does not ship as a fixed grammar of recognized phrases.
  Natural language is interpreted by an AI planner that selects from a registry of tools (see
  `08_AGENT_AND_TOOL_SYSTEM.md`).
- **Not a jailbreak or root tool.** JARVIS operates within the Android permission and sandboxing
  model. It does not attempt to bypass Android security to gain capabilities normal apps lack.
- **Not a demo.** It is engineered incrementally with real tests, real failure handling, and real
  security review at every stage, because it is designed to actually run on a user's daily driver
  phone with access to their notifications, apps, and (eventually) accessibility tree.
- **Not an impersonation of a copyrighted character.** The name and aesthetic are inspired by the
  general "AI assistant" archetype; the personality, voice, and visuals are original creations
  (see `03_USER_EXPERIENCE.md`).

## The Core Interaction Loop

```
UNDERSTAND → PLAN → ACT → OBSERVE → VERIFY → ADAPT → ACT AGAIN → COMPLETE
```

This loop, not a chatbot request/response pair, is the fundamental unit of JARVIS behavior once
the agent capabilities (V9+) exist. Earlier versions implement simplified subsets of this loop
(e.g., V2 is UNDERSTAND → respond, with no ACT/OBSERVE stage at all).

## Why Incremental Versions

A system that combines cloud AI reasoning, on-device voice pipelines, a wake-word detector, and
Accessibility-based UI automation has enormous surface area for silent failure. Building it in one
pass produces something that is impossible to debug because no one knows which layer broke.
Building it version-by-version, with each version locked and tested before the next begins,
produces a system where a regression can always be attributed to the current version's changes.

## Success Criteria for the Project as a Whole

1. A user can hold a natural conversation with JARVIS and get accurate, current answers.
2. A user can ask JARVIS to perform a defined set of on-device actions and have them succeed
   reliably, with clear feedback when they cannot.
3. A user can trust that JARVIS will not take a consequential action without confirmation, will
   not leak sensitive screen or notification content, and will not be trivially hijacked by
   malicious content in a web page or another app's UI (prompt injection via screen content).
4. The codebase remains extensible: adding tool #40 does not require touching tools #1–39.
