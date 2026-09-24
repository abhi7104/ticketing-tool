# Research: Assigned to Me + Lifecycle Flow Diagram

**Feature**: `002-assigned-to-me` | **Date**: 2026-09-22

This is an add-on to feature 001. The stack, security, error contract and test setup are
unchanged (see `specs/001-support-ticket-management/research.md`). Only the new decisions are
recorded here. No NEEDS CLARIFICATION items remain.

---

## R1. How the new view reaches the API

- **Decision**: Extend the existing listing endpoint: `GET /api/v1/tickets?view=assigned`. The
  `view` parameter accepts `mine | all | assigned`; `mine` stays the default.
- **Rationale**: Search, status filter and pagination already work on that endpoint (FR-005,
  FR-006) and the response shape is identical (FR-011). Adding an enum value to an optional query
  parameter is backward compatible, so `/api/v1` needs no version bump (constitution II).
- **Alternatives**: New `/api/v1/me/assigned-tickets` endpoint — would duplicate search, filter
  and paging logic for no gain.

## R2. Pending rule and ownership, enforced on the server

- **Decision**: For `view=assigned`, `TicketListService` adds two predicates:
  `assignee.id = <signed-in user>` and `status IN (OPEN, IN_PROGRESS)`. A `status` filter is
  combined with AND, so `status=RESOLVED|CLOSED` returns an empty page (spec US3 scenario 3).
  The pending set lives in one place: `TicketStatus.isPendingForAssignee()`.
- **Rationale**: FR-002 and FR-003 must hold even when the UI is bypassed; a single definition
  keeps the list and the count in agreement (FR-009).
- **Alternatives**: Filtering on the client — violates FR-002.

## R3. Sort order: priority first

- **Decision**: Add a stored generated column `priority_rank SMALLINT GENERATED ALWAYS AS (CASE
  priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END) STORED`
  (Flyway `V4`), mapped read-only on `Ticket`. The assigned view sorts by
  `priorityRank DESC, createdAt ASC, id ASC`. Other views keep `updatedAt DESC, id DESC`.
- **Rationale**: Priority is stored as text, so sorting by the column is alphabetical. A
  `CASE` expression cannot be passed through Spring Data `Pageable` with Criteria
  specifications. The generated column is always correct (no application code writes it), and
  it can be indexed. `id` order equals `TMS-n` number order, so the tie-break is by reference
  (FR-004) without string-sorting keys ("TMS-10" < "TMS-9").
- **Alternatives**: Change priority to a numeric column (breaking data change); sort in memory
  (breaks pagination); native SQL query (loses the shared specification code).
- **Index**: `ix_ticket_assigned_queue (assignee_id, priority_rank DESC, created_at, id) WHERE
  status IN ('OPEN','IN_PROGRESS')` — a partial index covering exactly this view and the count.
- **PostgreSQL 14**: stored generated columns and partial indexes exist since 12, so the version
  installed locally is fine.

## R4. The waiting count

- **Decision**: New endpoint `GET /api/v1/tickets/summary` → `{ "assignedPending": 3 }`. It
  counts with the same specification as the view without keyword/status filters (FR-009). The
  literal path `/tickets/summary` takes precedence over `/tickets/{ticketKey}` in Spring MVC, and
  `summary` can never be a valid ticket key.
- **Rationale**: The badge must render on every page (FR-008, FR-015) without fetching a ticket
  page. A summary object leaves room for later counters without new endpoints.
- **Freshness (FR-010)**: The frontend caches the count under the query key
  `['tickets', 'summary']`. Every existing ticket mutation already invalidates the `['tickets']`
  prefix, so status changes, edits (reassignment) and creation refresh the badge automatically.
  The count is also refetched on window focus. Live push updates are out of scope.
- **Alternatives**: Read `totalItems` from a `view=assigned&size=1` call — works but couples the
  badge to paging parameters and loads a row per page view.

## R5. Where the entry point lives in the UI

- **Decision**:
  - Listing: the segmented view control becomes **My tickets | Assigned to me | All tickets**
    (URL `?view=assigned`, FR-007).
  - Top bar (every signed-in page, FR-015): an "Assigned to me" link with a count badge
    (hidden at 0, FR-008). On narrow screens it collapses to an icon plus the badge, with an
    accessible label "Assigned to me, 3 tickets waiting" (FR-013).
  - The login redirect target stays `/tickets` (My tickets default, FR-014).
- **Empty states**: view empty with no filters → "Nothing waiting on you" + link to All tickets;
  with filters → existing "No tickets match" (FR-012).

## R6. Lifecycle flow diagram

- **Decision**: A new presentational component `StatusFlow` on the ticket detail page, placed
  under the header and above the closed banner. Built with semantic HTML + Tailwind + Lucide
  icons — no diagram library.
  - Markup: `<ol aria-label="Ticket lifecycle">` with one `<li>` per status; the current step has
    `aria-current="step"`; each step has visually hidden text "done" / "current step" /
    "upcoming" (FR-018).
  - Visual: pill-shaped step nodes joined by chevron-arrow connectors. Done = filled check icon
    plus the time reached; current = primary colour, ring and subtle pulse (disabled under
    `prefers-reduced-motion`); upcoming = muted outline. Closed-as-current shows a lock icon and
    "Final state".
  - Layout: horizontal from the `sm` breakpoint; vertical stack with downward arrows below it,
    so phones never scroll sideways.
  - Motion: when the status changes, the connector fills and the new current node fades/scales in
    (≤ 200 ms, constitution VIII).
- **Step times (FR-017)**: derived on the client from data the detail endpoint already returns:
  Open = `createdAt`; each later status = `occurredAt` of the `STATUS_CHANGED` history entry whose
  `newValue` is that status. No backend change.
- **Order source**: the step order is a frontend constant `STATUS_FLOW = ['OPEN','IN_PROGRESS',
  'RESOLVED','CLOSED']` matching the backend state machine; a unit test pins it.
- **Alternatives**: React Flow / Mermaid (heavy, poor accessibility for a fixed 4-step line);
  making the diagram clickable to change status (duplicates the action button and invites
  invalid clicks; FR-019 keeps it informational).

## R7. Testing

- **Backend**: `AssignedTicketsIntegrationTest` (ownership, pending set, all 4 status filters,
  ordering incl. tie-breaks across pages, search inside view, `view` validation) and
  `TicketSummaryIntegrationTest` (count equals unfiltered view total; changes after transition,
  reassignment and creation; 401 without session). Migration covered by context start
  (`ddl-auto=validate` checks the new mapped column).
- **Performance**: extend the `perf` test: 10,000 tickets with 500 assigned to one user; view and
  count each < 1 s (SC-002).
- **Frontend**: Vitest for `StatusFlow` (4 statuses × done/current/upcoming, times, a11y
  attributes), `AssignedBadge` (hidden at 0, label text), filters with the third view.
- **E2E**: `assigned-to-me.spec.ts` (bob sees only his Open/In progress tickets, badge count
  changes after resolve and reassignment, empty state) and `status-flow-diagram.spec.ts`; axe
  runs on both pages. Feature 001 E2E suite must stay green (SC-005).
