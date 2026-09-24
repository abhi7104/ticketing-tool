# Implementation Plan: Assigned to Me + Lifecycle Flow Diagram

**Branch**: `002-assigned-to-me` | **Date**: 2026-09-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-assigned-to-me/spec.md`

## Summary

An add-on to the ticket system from feature 001. After signing in, every page's top bar shows
**Assigned to me** with a count of the user's pending work, meaning tickets assigned to them in
Open or In progress. The link opens a new listing view that shows only those tickets, most
urgent first, with the existing search, status filter and pagination. The ticket detail page
gains an arrow-style **lifecycle flow diagram** (Open → In progress → Resolved → Closed). Done
steps show when the ticket reached them, and the current step is highlighted.

Technical approach:

- **Backend**: add `view=assigned` to the existing list endpoint and a new
  `GET /api/v1/tickets/summary` count. Both are enforced on the server with one shared rule for
  what "pending" means.
- **Sorting**: priority-first order via a generated `priority_rank` column (Flyway V4, additive).
- **Frontend**: a third option in the view toggle, a top-bar link with a badge, and a new
  `StatusFlow` component. The diagram needs no backend change, because step times come from the
  existing status history.

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript 5 strict on Node.js 20 (frontend). Unchanged.

**Primary Dependencies**: Unchanged (Spring Boot 3.5, Flyway, Next.js 15, TanStack Query,
Tailwind, Radix, Lucide). No new libraries; the diagram is plain semantic HTML and Tailwind.

**Storage**: PostgreSQL (≥ 14; 16 in containers). One additive migration: a generated column
and a partial index.

**Testing**: JUnit 5 + MockMvc + real PostgreSQL (Testcontainers or embedded fallback); Vitest +
Testing Library + MSW; Playwright + axe.

**Target Platform**: Unchanged (Linux containers; evergreen browsers, desktop and mobile).

**Project Type**: Web application (existing `backend/` + `frontend/`).

**Performance Goals**: Assigned view and count each < 1 s for a user with 500 assigned tickets
among 10,000 (SC-002).

**Constraints**: Additive only. No change to existing views, defaults, endpoints or response
shapes (FR-014, SC-005). The API stays under `/api/v1`. WCAG 2.1 AA. Motion ≤ 200 ms and
respects reduced-motion.

**Scale/Scope**:
- Backend: 1 changed endpoint, 1 new endpoint, 1 migration.
- Frontend: 3 new components (`AssignedLink` + badge, `StatusFlow`, assigned empty state) and 3
  changed ones (filters, list page, top bar).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Pre-design | Post-design |
|-----------|------|------------|-------------|
| I. Mandated stack | Java 21/Spring Boot REST, PostgreSQL + Flyway, Next.js via REST only | ✅ | ✅ No new tech; V4 migration; FE uses REST only |
| II. API-first REST | Contract first; `/api/v1`; RFC 9457 errors; non-breaking change or new version | ✅ | ✅ [openapi-delta.yaml](./contracts/openapi-delta.yaml) adds an enum value and one endpoint (backward compatible); merged into the canonical contract before FE type generation |
| III. Secrets & env config | No new secrets or config | ✅ | ✅ None added |
| IV. Security & least privilege | Server-side enforcement; authenticated | ✅ | ✅ Assignee and pending rules applied in the service (FR-002); summary requires a session |
| V. Data integrity & audit | Migrations forward-only; DB backs invariants; no status change outside state machine | ✅ | ✅ Generated column cannot drift; diagram is read-only (FR-019) |
| VI. Test-first | Integration tests on real PostgreSQL; Vitest; Playwright critical flows | ✅ | ✅ See research R7 and Acceptance Traceability |
| VII. Observability | Logs/metrics unchanged | ✅ | ✅ No new failure modes; existing request-id logging covers the new endpoint |
| VIII. Perf, a11y, simplicity | Indexed queries; WCAG 2.1 AA; no unneeded components | ✅ | ✅ Partial index for the queue; ordered-list semantics and `aria-current="step"`; no diagram library |

**Result**: PASS, with no violations, so Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/002-assigned-to-me/
├── plan.md              # This file
├── research.md          # Decisions R1–R7
├── data-model.md        # priority_rank column, pending set, derived queue and steps
├── quickstart.md        # Validation guide
├── contracts/
│   ├── openapi-delta.yaml   # API additions (merge into 001 canonical contract)
│   └── ui-contract.md       # Top-bar entry, view control, flow diagram
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (changes to the existing repository)

```text
backend/src/main/resources/db/migration/
└── V4__ticket_priority_rank.sql                 # NEW generated column + partial index

backend/src/main/java/com/c2certi/tms/ticket/
├── domain/TicketStatus.java                     # CHANGE + isPendingForAssignee()
├── domain/Ticket.java                           # CHANGE + read-only priorityRank mapping
├── repository/TicketSpecifications.java         # CHANGE + assignedTo(), pendingForAssignee()
├── api/dto/TicketListQuery.java                 # CHANGE view pattern mine|all|assigned
├── api/dto/TicketSummaryCountsResponse.java     # NEW
├── service/TicketListService.java               # CHANGE assigned view predicates + sort
├── service/TicketSummaryService.java            # NEW count
└── api/TicketSummaryController.java             # NEW GET /api/v1/tickets/summary

backend/src/test/java/com/c2certi/tms/ticket/
├── AssignedTicketsIntegrationTest.java          # NEW
├── TicketSummaryIntegrationTest.java            # NEW
├── domain/TicketStatusTransitionTest.java       # CHANGE pending-set assertions
└── TicketSearchPerformanceTest.java             # CHANGE assigned-queue case

specs/001-support-ticket-management/contracts/openapi.yaml   # CHANGE merge delta

frontend/src/
├── lib/api/schema.d.ts                          # REGENERATED
├── lib/api/tickets-list.ts                      # CHANGE TicketView adds "assigned"
├── lib/api/tickets-summary.ts                   # NEW useTicketSummary()
├── lib/hooks/useTicketListParams.ts             # CHANGE parse view=assigned
├── lib/status-flow.ts                           # NEW STATUS_FLOW + buildSteps(ticket)
├── components/layout/AssignedLink.tsx           # NEW top-bar link + badge
├── components/layout/TopBar.tsx                 # CHANGE render AssignedLink
├── components/tickets/TicketFilters.tsx         # CHANGE third view option
├── components/tickets/StatusFlow.tsx            # NEW arrow flow diagram
├── app/(app)/tickets/TicketList.tsx             # CHANGE subtitle + assigned empty state
└── app/(app)/tickets/[key]/TicketView.tsx       # CHANGE render StatusFlow

frontend/tests/
├── unit/StatusFlow.test.tsx                     # NEW
├── unit/status-flow.test.ts                     # NEW step derivation
├── unit/AssignedLink.test.tsx                   # NEW
├── unit/TicketFilters.test.tsx                  # CHANGE third option
├── e2e/assigned-to-me.spec.ts                   # NEW
└── e2e/status-flow-diagram.spec.ts              # NEW (+ axe via a11y.spec.ts)
```

**Structure Decision**: Extend the existing web-app layout. The new backend behaviour lives in
new files where possible (summary controller and service). Existing files get small, additive
edits. The new frontend components are independent, so the list work and the diagram work can
be built in parallel.

## Key Design Decisions (details in research.md)

1. **Reuse the list endpoint** with `view=assigned` (R1). Non-breaking, and it inherits search,
   filters and paging.
2. **One pending rule** (`TicketStatus.isPendingForAssignee`) serves both the view and the
   count, so they always match (FR-009, R2).
3. **`priority_rank` generated column** gives correct priority order through standard paging and
   uses an index (R3).
4. **Count endpoint `/tickets/summary`** is cached under `['tickets','summary']`. Existing
   mutations already invalidate `['tickets']`, so the badge refreshes after every change
   (FR-010, R4).
5. **`StatusFlow`** is informational, built from `createdAt` plus status history. It is an
   accessible ordered list that switches between horizontal and vertical layouts (R6).

## Acceptance Traceability

| Requirement | Verification |
|-------------|--------------|
| FR-001, FR-007, FR-015 view + top-bar entry, URL state | E2E `assigned-to-me.spec.ts`; `TicketFilters.test.tsx`; `AssignedLink.test.tsx` |
| FR-002, FR-003 ownership + pending set (server-enforced) | `AssignedTicketsIntegrationTest` |
| FR-004 order | `AssignedTicketsIntegrationTest` (priority, age, tie-break across pages) |
| FR-005, FR-006 search/filter/paging | `AssignedTicketsIntegrationTest`; E2E |
| FR-008, FR-009, FR-010 count | `TicketSummaryIntegrationTest`; E2E badge after resolve/reassign |
| FR-011, FR-012 rows + empty states | E2E; list page unit test |
| FR-013, FR-018 accessibility | `AssignedLink.test.tsx`, `StatusFlow.test.tsx`; axe in `a11y.spec.ts` |
| FR-014, SC-005 no regressions | Full feature 001 backend + E2E suites |
| FR-016, FR-017, FR-019, SC-007 flow diagram | `status-flow.test.ts`, `StatusFlow.test.tsx`, E2E `status-flow-diagram.spec.ts` |
| SC-002 performance | `TicketSearchPerformanceTest` (perf profile) |

## Complexity Tracking

No constitution violations; section intentionally empty.
