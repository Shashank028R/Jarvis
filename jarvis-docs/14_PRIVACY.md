# 14 — Privacy

## Data Classification

| Class | Examples | Leaves Device? |
|---|---|---|
| Public/non-sensitive | App names, generic settings state | May be sent to AI as needed |
| Personal, request-scoped | The user's current spoken question, a screen summary for the active task | Sent to AI only for the active request, not retained by the relay beyond serving it |
| Sensitive | Notification bodies, screen content, contact names in a task, memory items | Sent only when required for the specific request being served, and only after the relevant capability (notifications, screen reading) has been explicitly enabled by the user |
| Highly sensitive — never transmitted | Passwords/credentials, OTP/2FA codes, payment card data | Redacted at the source (`11_SCREEN_UNDERSTANDING.md` privacy filter); never included in any AI request payload |

## Retention

- Conversation content: not retained by the AI provider relay beyond serving the request (subject
  to the provider's own API data-handling terms, which must be reviewed and linked in an ADR
  before V2 ships).
- On-device memory: per `12_MEMORY_ARCHITECTURE.md` lifecycle and user-configurable expiry.
- Audit log: local only, user-viewable and user-clearable.

## User Controls

- A single, discoverable "Privacy & Data" settings screen covering: what's stored, what's been
  sent to the AI recently (a lightweight, human-readable activity view, not raw payload dumps),
  and one-tap controls to disable notification reading, screen reading, and memory independently.
- Enabling `NotificationListenerService` access or `AccessibilityService` access always routes
  through the OS's own special-access settings screens (this cannot be hidden or streamlined away
  — it's an OS-level, deliberately-friction-y grant by design) and JARVIS explains why it's asking
  before sending the user there.

## Data Minimization in Practice

- The AI Planner is given the smallest context that plausibly answers the current request (bounded
  conversation window, retrieved memory subset, not full history) — this is a privacy control as
  much as a cost/latency one.
- Screen/notification data is used transiently for the active task and not folded into memory
  automatically (`12_MEMORY_ARCHITECTURE.md`).

## Children/Sensitive Populations

JARVIS is designed as a general personal-productivity assistant, not a product directed at
children. Age-gating and any special handling would be evaluated separately if the product's
audience were ever redefined; it is out of scope for the current roadmap.

## Third-Party Data Sharing

No user data is sold or shared with advertisers. The only "third party" in the data path is the AI
model provider (for the request needed to answer/plan) and, if enabled, a web search provider — both
disclosed plainly in the app's privacy policy once one is drafted (a documentation gap tracked in
`DOCUMENTATION_REVIEW.md`: a user-facing privacy policy document is a Play Store submission
requirement and must be written from this spec before V15).
