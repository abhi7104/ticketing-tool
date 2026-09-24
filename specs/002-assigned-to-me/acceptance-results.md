# Acceptance Results: Assigned to Me + Lifecycle Flow Diagram

**Date**: 2026-09-22
**Environment**: local PostgreSQL 14.24 (the user's installed server, `.env` credentials), backend
jar on Java 21.0.2 (dev profile), frontend Next.js 15.5 standalone build, Playwright Chromium +
Pixel 7 emulation. Migration `V4__ticket_priority_rank` applied cleanly to the existing database.

## Automated suites

| Suite | Result |
|-------|--------|
| Backend `./mvnw spotless:check verify` (features 001 + 002) | 140 passed, 0 failed |
| Backend `./mvnw test -Pperf` (incl. 500 assigned of 10,000 tickets) | 2 passed; each request < 1 s |
| Frontend `npm test` (Vitest) | 86 passed, 0 failed |
| Frontend lint, `tsc --noEmit`, `next build` | clean |
| Playwright E2E, whole suite (001 + 002, desktop + mobile), run 3 times | 42 passed each run |

## Live API checks (quickstart section 3, signed in as bob)

| Check | Result |
|-------|--------|
| `GET /tickets/summary` equals `GET /tickets?view=assigned` `totalItems` | 25 = 25 ✅ |
| `view=assigned&status=CLOSED` | 0 items ✅ |
| Assigned view ordered by priority | sorted (25 items) ✅ |
| `view=bogus` | 400 `VALIDATION_FAILED`, field `view` ✅ |
| `/tickets/summary` without session | 401 ✅ |

## Requirements

| Requirement | Evidence | Result |
|-------------|----------|--------|
| FR-001, FR-007 third view, URL state | `TicketFilters.test.tsx`, E2E `assigned-to-me.spec.ts` (back + reload keep view) | ✅ |
| FR-002, FR-003 only my Open/In progress tickets, server-enforced | `AssignedTicketsIntegrationTest` | ✅ |
| FR-004 order: priority → oldest → reference, stable across pages | `AssignedTicketsIntegrationTest`, E2E order check | ✅ |
| FR-005, FR-006 search, status filter, paging in the view | `AssignedTicketsFilterIntegrationTest`, E2E `assigned-filters.spec.ts` | ✅ |
| FR-008–FR-010 count badge follows changes | `TicketSummaryIntegrationTest`, `AssignedCountConsistencyIntegrationTest`, E2E `assigned-badge.spec.ts` | ✅ |
| FR-011, FR-012 rows and empty states | `AssignedView.test.tsx`, E2E empty state | ✅ |
| FR-013, FR-018 accessibility | `AssignedLink.test.tsx`, `StatusFlow.test.tsx`, axe in `a11y.spec.ts` (0 serious/critical) | ✅ |
| FR-014, SC-005 feature 001 unchanged | all 001 backend + E2E tests pass unchanged | ✅ |
| FR-015 entry after sign-in on every page | E2E badge on `/tickets`, `/tickets/new`, detail page | ✅ |
| FR-016, FR-017, FR-019, SC-007 flow diagram | `status-flow.test.ts`, `StatusFlow.test.tsx`, E2E `status-flow-diagram.spec.ts` (incl. 375 px, no overflow) | ✅ |
| SC-002 performance | perf test | ✅ |

## Notes

- The E2E "empty queue" check stubs the list response, because other users can assign tickets
  to anyone in a shared database. The backend's empty-queue behaviour is covered by
  `AssignedTicketsIntegrationTest.emptyQueue`.
- The E2E badge check compares the badge with the latest count the page received, because other
  specs create tickets for the same user in parallel. Exact counting is covered by backend tests.
- Test runs created E2E tickets (titles like "E2E ticket …", "Critical muc4…") in the local
  `tms` database.
