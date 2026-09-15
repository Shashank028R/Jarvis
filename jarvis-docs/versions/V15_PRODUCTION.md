# V15 — Production Release

## Purpose
Final compliance, documentation, and release-readiness pass. This is where the Accessibility API
Play policy question (flagged since `10_ACCESSIBILITY_ARCHITECTURE.md` and V7) must be finally
and explicitly resolved — at the latest.

## Goals
- User-facing privacy policy document, written from `14_PRIVACY.md`, published and linked in the
  Play Console listing.
- Final Accessibility API declaration/compliance decision made and acted on (either a compliant
  declaration is filed and accepted, or the distribution strategy is formally adjusted — e.g.,
  scoping V7–V9 automation features down further, or shipping via a distribution channel outside
  Play for those specific features — this decision is recorded as an ADR).
- Store listing content, screenshots, and required disclosures (data safety section, permissions
  justification) completed accurately against what the app actually does.
- Final full-system regression pass across all fifteen versions' test suites.

## Features
No new functional features; release packaging and compliance work.

## Dependencies
All prior versions locked.

## Architectural Changes
None expected, barring a compliance-driven scope adjustment to V7–V9 features decided during this
version (documented as an ADR if it occurs).

## New Components
- Privacy policy document.
- Play Console data-safety-section content, cross-checked against `14_PRIVACY.md`'s data
  classification table field-by-field.
- Release build configuration (signing, obfuscation review, final permission manifest audit).

## User Experience
First-run experience is reviewed end-to-end: permission requests are contextual and justified in
the moment they're needed (not front-loaded), consistent with least-privilege (Constitution Rule 5).

## Permissions
Final manifest audit: every declared permission has a justification traceable to a shipped,
user-facing feature; nothing speculative or unused remains declared.

## Security Considerations
Final sign-off against `13_SECURITY.md`'s complete checklist before submission.

## Testing
- Full regression suite (V1–V14) green.
- Play Console pre-launch report review (crashes, ANRs, accessibility scanner findings on
  JARVIS's own UI).
- Manual store-listing accuracy review (does every claim made in the listing match tested, real
  behavior — no aspirational marketing copy beyond what's shipped).

## Acceptance Criteria
- [ ] Privacy policy published and accurate against `14_PRIVACY.md`.
- [ ] Accessibility API compliance decision finalized, documented as an ADR, and acted upon in the
      actual submitted build.
- [ ] Data-safety section accurately reflects real data handling.
- [ ] Full regression suite green.
- [ ] Play Console pre-launch report shows no unresolved crashes/ANRs/critical accessibility-
      scanner findings on JARVIS's own UI.
- [ ] Permission manifest contains only permissions traceable to shipped features.

## Known Limitations
Documented plainly in the store listing and in-app: what JARVIS can and cannot do, especially
around cross-app automation reliability and offline scope, so user expectations match reality.

## Exit Criteria
All acceptance criteria met; submission approved (or, if targeting a non-Play distribution channel
for policy reasons, that channel's equivalent readiness criteria met); tag `v1.0.0`.

## Next Version Dependencies
None — this is the roadmap's terminal version. Anything further is a new roadmap.

---
## Phases
1. **Privacy policy & data-safety section** — written and cross-checked against `14_PRIVACY.md`.
2. **Accessibility compliance final decision** — the gating decision from V7, resolved definitively.
3. **Store listing content** — accurate, tested-behavior-only claims.
4. **Final permission manifest audit** — remove anything unjustified.
5. **Full regression + pre-launch report review** — fix any findings.
6. **Release build & signing** — production build configuration finalized.
7. **Submission & stabilization** — submit, monitor, tag `v1.0.0` on acceptance.
