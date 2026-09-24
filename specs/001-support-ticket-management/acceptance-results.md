# Acceptance Results: Support Ticket Management

**Date**: 2026-09-21
**Environment**: local, no Docker available. PostgreSQL 16.4 (official binaries), backend jar
(Java 21.0.2), frontend Next.js 15.5 standalone build, Playwright Chromium + Pixel 7 emulation.

## Automated suites

| Suite | Result |
|-------|--------|
| Backend `./mvnw test` (unit + integration on real PostgreSQL + contract + restart) | 120 passed, 0 failed |
| Backend `./mvnw test -Pperf` (10,000 tickets, search + status filter) | passed (< 1 s per query) |
| Frontend `npm test` (Vitest) | 65 passed, 0 failed |
| Frontend `npm run lint`, `tsc --noEmit`, `next build` | clean |
| Playwright E2E against the running stack | 33 passed, 0 failed |

## Acceptance checklist

| # | Criterion | Evidence | Result |
|---|-----------|----------|--------|
| 1 | Ticket can be created from UI | E2E `create-ticket.spec.ts`; `TicketCreationIntegrationTest` | ✅ |
| 2 | Tickets can be listed | E2E `list-search-filter.spec.ts`; `TicketListIntegrationTest` | ✅ |
| 3 | Ticket details can be viewed | E2E row click → detail; `TicketDetailIntegrationTest` | ✅ |
| 4 | Ticket fields can be updated | E2E `edit-ticket.spec.ts`; `TicketUpdateIntegrationTest` | ✅ |
| 5 | Assignee can be changed | E2E "assignee can be changed" (detail + listing) | ✅ |
| 6 | Comments can be added | E2E `comments.spec.ts`; `CommentIntegrationTest` | ✅ |
| 7 | Search works | E2E title fragment + key search; wildcard-literal integration test | ✅ |
| 8 | Status filter works | E2E filter + combined with search + survives reload | ✅ |
| 9 | Valid status transitions work | E2E full Open → Closed flow; 3 allowed pairs in matrix test | ✅ |
| 10 | Invalid transitions rejected by backend | 13 rejected pairs (409, status/version/history unchanged); E2E API check | ✅ |
| 11 | Data survives application restart | `PersistenceAcrossRestartTest`; live stack: PostgreSQL **and** backend restarted, 49 tickets identical (content hash match) | ✅ |
| 12 | Backend validation works | 400 `VALIDATION_FAILED` with all field errors when the UI is bypassed | ✅ |
| 13 | UI shows meaningful errors | E2E `errors.spec.ts` (network down, 500 with reference, expired session, not found); no native dialogs | ✅ |
| 14 | State-machine integration tests pass | `TicketTransitionIntegrationTest`: 24 tests incl. 16-pair matrix and concurrency | ✅ |
| – | Modern, user-friendly, smooth UI | axe: zero serious/critical WCAG 2.1 AA violations on all pages (desktop + mobile); keyboard-only create flow; screenshots reviewed | ✅ |

## Defects found and fixed during validation

1. `snakeyaml` declared with test scope removed the YAML parser from the runtime jar; the
   packaged app could not read `application.yml`. Removed the override.
2. An invalid priority stopped JSON parsing, so only one error was reported. Priority is now
   validated as a string with every other field (`reportsEveryInvalidFieldAtOnce…` test).
3. Validation-on-blur inserted an error line that moved the Save button during a click, so the
   click was lost. Error lines now reserve their space.
4. Single-button pop-ups did not receive keyboard focus (Radix focuses only Cancel). A lone
   button is now rendered as the Cancel element.
5. The logo link had no accessible name on mobile widths. Added `aria-label`.

## Not verified here

- `docker compose` and the Dockerfiles were not run: Docker is not installed on this machine.
  CI (`.github/workflows/ci.yml`) builds and runs them.
- gitleaks and OWASP dependency-check were not run locally (tools not installed); they run in CI.
  A manual scan found no generated secret value in the repository.
