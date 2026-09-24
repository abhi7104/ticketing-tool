---

description: "Task list for Assigned to Me + Lifecycle Flow Diagram"
---

# Tasks: Assigned to Me + Lifecycle Flow Diagram

**Input**: Design documents from `/specs/002-assigned-to-me/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi-delta.yaml,
contracts/ui-contract.md, quickstart.md

**Tests**: INCLUDED. Constitution Principle VI requires test-first development: integration tests
on real PostgreSQL, Vitest, and Playwright for critical flows. The spec also sets automated
success criteria (SC-003, SC-004, SC-005, SC-007). Within each story, write the tests first and
confirm they fail before implementing.

**Organization**: Tasks are grouped by user story and laid out so the stories can be built **in
parallel**:

- **Shared pieces go in Foundational:** the contract merge, the migration, the pending rule and
  the query specifications.
- **Separate backend files per story:**
  - US1 changes the list endpoint.
  - US2 adds its own summary controller and service.
  - US3 adds only a new test file.
  - US4 needs no backend change.
- **Separate frontend files per story:**
  - US1 owns the filters, list-params hook and list page.
  - US2 owns the top bar and badge.
  - US4 owns the flow component and the detail-page slot.
  - The only cross-story edit is US3's no-results hint in `TicketList.tsx`, which runs after US1.

This is an **add-on** to feature 001, so existing behaviour must not change (FR-014).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story the task belongs to (US1–US4)

## Path Conventions

- Backend main: `backend/src/main/java/com/c2certi/tms/`
- Backend tests: `backend/src/test/java/com/c2certi/tms/` (extend `support.IntegrationTest`; use
  `fixtures`, `users`, `api`)
- Migrations: `backend/src/main/resources/db/migration/`
- Frontend: `frontend/src/`; tests in `frontend/tests/unit/` and `frontend/tests/e2e/` (E2E
  fixtures in `tests/e2e/fixtures.ts`: `api` = alice, `api2` = bob)
- Build and test backend with `JAVA_HOME=~/.sdkman/candidates/java/21.0.2-tem ./mvnw …`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Make the API contract the source of truth before any code.

- [X] T001 Merge `specs/002-assigned-to-me/contracts/openapi-delta.yaml` into the canonical contract `specs/001-support-ticket-management/contracts/openapi.yaml`: add `assigned` to the `view` enum on `GET /tickets` (update its description with the assigned-view ordering and pending rule), add `GET /tickets/summary` returning new schema `TicketSummaryCounts { assignedPending: int64 ≥ 0 }` with 401 response, and bump `info.version` to `1.1.0`
- [X] T002 Regenerate frontend API types with `npm run gen:api` in `frontend/` and commit the result in `frontend/src/lib/api/schema.d.ts`; add `export type TicketSummaryCounts = Schemas["TicketSummaryCounts"];` in `frontend/src/lib/api/types.ts` (depends on T001)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared schema and domain rules used by both the view (US1) and the count (US2).

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 [P] Create Flyway migration adding `priority_rank SMALLINT GENERATED ALWAYS AS (CASE priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END) STORED` to `ticket`, plus partial index `ix_ticket_assigned_queue ON ticket (assignee_id, priority_rank DESC, created_at, id) WHERE status IN ('OPEN','IN_PROGRESS')`, in `backend/src/main/resources/db/migration/V4__ticket_priority_rank.sql`
- [X] T004 [P] Add `boolean isPendingForAssignee()` (true only for OPEN and IN_PROGRESS) and `static Set<TicketStatus> pendingForAssignee()` to `backend/src/main/java/com/c2certi/tms/ticket/domain/TicketStatus.java`
- [X] T005 [P] Add unit tests asserting the pending set is exactly {OPEN, IN_PROGRESS} (all 4 statuses checked) in `backend/src/test/java/com/c2certi/tms/ticket/domain/TicketPendingStatusTest.java`
- [X] T006 Map the generated column read-only on the entity (`@Column(name = "priority_rank", insertable = false, updatable = false) private Short priorityRank;` with `@org.hibernate.annotations.Generated` so it is read back after insert/update, plus getter) in `backend/src/main/java/com/c2certi/tms/ticket/domain/Ticket.java` (depends on T003)
- [X] T007 Add `assignedTo(Long userId)` (assignee id equals) and `pendingForAssignee()` (`status IN TicketStatus.pendingForAssignee()`) specifications in `backend/src/main/java/com/c2certi/tms/ticket/repository/TicketSpecifications.java` (depends on T004)

**Checkpoint**: `./mvnw test` still green (120 tests) — confirms V4 applies and `ddl-auto=validate`
accepts the new mapping. Stories can now start **in parallel**.

---

## Phase 3: User Story 1 - See My Pending Work (Priority: P1) 🎯 MVP

**Goal**: A third listing view, "Assigned to me", showing only the signed-in user's Open/In
progress tickets, most urgent first, with a friendly empty state.

**Independent Test**: With tickets assigned to bob and alice in all 4 statuses, bob's
`?view=assigned` lists only his Open/In progress tickets in order Critical → Low, oldest first
within a priority, and shows "Nothing waiting on you" when empty.

### Tests for User Story 1 ⚠️ (write first, must fail)

- [X] T008 [P] [US1] Write integration test for `GET /api/v1/tickets?view=assigned` with fixtures for bob and alice in every status and priority. Assert:
  - only bob's OPEN/IN_PROGRESS tickets are returned (none RESOLVED/CLOSED, none of alice's);
  - a ticket bob raised and assigned to himself is included;
  - order is priority_rank DESC, then created_at ASC, then id ASC, and it stays stable across a page boundary (`size=2`);
  - `totalItems` is correct;
  - `view=mine` and `view=all` keep `updatedAt DESC` order;
  - `view=bogus` → 400 `VALIDATION_FAILED` with field `view`;
  - no session → 401.

  File: `backend/src/test/java/com/c2certi/tms/ticket/AssignedTicketsIntegrationTest.java`
- [X] T009 [P] [US1] Write component tests:
  - `TicketFilters` renders "My tickets | Assigned to me | All tickets" in that order, marks the active one with `aria-pressed`, and clicking "Assigned to me" calls `onChange({ view: "assigned" })`;
  - the list page shows the subtitle "Tickets waiting on you, most urgent first.";
  - with `view=assigned`, zero items and no filters (MSW), it shows the "Nothing waiting on you" empty state with a "View all tickets" link.

  Files: `frontend/tests/unit/TicketFilters.test.tsx` (extend) and `frontend/tests/unit/AssignedView.test.tsx` (new)
- [X] T010 [P] [US1] Write E2E. alice creates tickets assigned to bob with priorities Low, Critical, High and one Resolved; then bob (signs in with `E2E_USER2`):
  - opens "Assigned to me" and sees only the pending ones, Critical first;
  - opens a row and presses back to find the same view;
  - after reload, `?view=assigned` is still selected;
  - a user with nothing pending sees "Nothing waiting on you".

  File: `frontend/tests/e2e/assigned-to-me.spec.ts`

### Implementation for User Story 1

- [X] T011 [US1] Extend `view` validation to `mine|all|assigned` (message "View must be 'mine', 'assigned' or 'all'.") and add `boolean assignedOnly()` in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/TicketListQuery.java`
- [X] T012 [US1] In `TicketListService.list`, when `query.assignedOnly()`:
  - combine `assignedTo(currentUser.getId())` and `pendingForAssignee()` with the existing keyword/status specs;
  - sort by new constant `ASSIGNED_QUEUE_ORDER = Sort.by(desc("priorityRank"), asc("createdAt"), asc("id"))`;
  - leave the other views unchanged.

  File: `backend/src/main/java/com/c2certi/tms/ticket/service/TicketListService.java` (depends on T006, T007, T011)
- [X] T013 [P] [US1] Change `TicketView` to `"mine" | "assigned" | "all"` in `frontend/src/lib/api/tickets-list.ts`
- [X] T014 [P] [US1] Parse and write `view=assigned` in the URL (unknown values still fall back to `mine`) in `frontend/src/lib/hooks/useTicketListParams.ts`
- [X] T015 [P] [US1] Add the "Assigned to me" option between "My tickets" and "All tickets" in the view segmented control in `frontend/src/components/tickets/TicketFilters.tsx`
- [X] T016 [US1] Update `frontend/src/app/(app)/tickets/TicketList.tsx` (depends on T013–T015):
  - add a per-view subtitle ("Tickets you raised." / "Tickets waiting on you, most urgent first." / "Every ticket in the system.");
  - when `view=assigned` has no items and no filters, show `EmptyState` with the check-circle icon, the title "Nothing waiting on you", the text "Tickets assigned to you that are open or in progress will show up here." and a "View all tickets" link to `/tickets?view=all`.

**Checkpoint**: T008–T010 pass. MVP: a user can find their pending work.

---

## Phase 4: User Story 2 - Know How Much Is Waiting (Priority: P1)

**Goal**: A top-bar "Assigned to me" entry, visible after sign-in on every page, with a live
count badge (hidden at 0) that follows changes made in the app.

**Independent Test**: bob has 3 pending tickets and the badge shows 3. Resolve one and the badge
shows 2. Reassign one to alice and it shows 1. At 0 the badge is hidden and the link remains.

### Tests for User Story 2 ⚠️ (write first, must fail)

- [X] T017 [P] [US2] Write integration test for `GET /api/v1/tickets/summary`. Assert:
  - `assignedPending` equals the database count of the user's OPEN/IN_PROGRESS assigned tickets;
  - it is 0 for a new user;
  - it decreases after a transition to RESOLVED and after reassignment to another user;
  - it increases after creating a ticket assigned to self;
  - it ignores other users' tickets;
  - no session → 401;
  - `/tickets/summary` is not treated as a ticket key.

  File: `backend/src/test/java/com/c2certi/tms/ticket/TicketSummaryIntegrationTest.java`
- [X] T018 [P] [US2] Write component tests for `AssignedLink`. Assert:
  - no badge at 0 and the accessible name is "Assigned to me";
  - at 3 the badge shows "3" and the accessible name is "Assigned to me, 3 tickets waiting";
  - at 120 it shows "99+";
  - `aria-current="page"` when the URL has `view=assigned`;
  - the link href is `/tickets?view=assigned`.

  File: `frontend/tests/unit/AssignedLink.test.tsx`
- [X] T019 [P] [US2] Write E2E. After bob signs in, the top bar shows the link with the correct count on `/tickets`, `/tickets/new` and a detail page. The count drops by 1 after bob resolves one ticket through the UI, and again after alice reassigns one away (bob reloads). File: `frontend/tests/e2e/assigned-badge.spec.ts`

### Implementation for User Story 2

- [X] T020 [P] [US2] Create record `TicketSummaryCountsResponse(long assignedPending)` in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/TicketSummaryCountsResponse.java`
- [X] T021 [US2] Implement `TicketSummaryService.summarize(User currentUser)` (read-only transaction; `tickets.count(assignedTo(id).and(pendingForAssignee()))`) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketSummaryService.java` (depends on T007, T020)
- [X] T022 [US2] Implement `TicketSummaryController` `GET /api/v1/tickets/summary` using `CurrentUserProvider` in `backend/src/main/java/com/c2certi/tms/ticket/api/TicketSummaryController.java` (depends on T021)
- [X] T023 [P] [US2] Create `useTicketSummary()` query (key `['tickets', 'summary']` so existing `['tickets']` invalidations refresh it; `staleTime` 15 s; `refetchOnWindowFocus: true`) in `frontend/src/lib/api/tickets-summary.ts`
- [X] T024 [US2] Create `AssignedLink` in `frontend/src/components/layout/AssignedLink.tsx` (depends on T023):
  - Lucide `Inbox` icon plus the text "Assigned to me" (text hidden below `sm`);
  - a count badge, hidden at 0 and shown as "99+" above 99;
  - an accessible name per ui-contract;
  - `aria-current="page"` when on the assigned view, detected with `usePathname` and `useSearchParams` inside a `Suspense` boundary.
- [X] T025 [US2] Render `AssignedLink` before the "New ticket" button in `frontend/src/components/layout/TopBar.tsx` (depends on T024)

**Checkpoint**: T017–T019 pass independently of US1's UI (the badge link works even before the
listing option exists).

---

## Phase 5: User Story 3 - Narrow Down My Queue (Priority: P2)

**Goal**: Keyword search and status filter work inside "Assigned to me", with the same rules
as the other views, and still exclude other users' and finished tickets.

**Independent Test**: In bob's assigned view, `q` and `status` narrow results. OPEN and
IN_PROGRESS filter correctly, while RESOLVED and CLOSED give "No tickets match". A refresh
restores both filters.

### Tests for User Story 3 ⚠️ (write first, must fail)

- [X] T026 [P] [US3] Write integration test for `view=assigned` combined with filters. Assert:
  - `q` matches key, title and description case-insensitively;
  - literal `%`/`_` handling is unchanged;
  - `status=OPEN` and `status=IN_PROGRESS` narrow correctly;
  - `status=RESOLVED` and `status=CLOSED` return 0 items;
  - `q` + `status` combine;
  - matching tickets assigned to others never appear;
  - `size`/`page` paging is stable.

  File: `backend/src/test/java/com/c2certi/tms/ticket/AssignedTicketsFilterIntegrationTest.java`
- [X] T027 [P] [US3] Write E2E. In bob's "Assigned to me":
  - search a unique token and then pick "In progress": only the matching ticket shows;
  - pick "Closed": "No tickets match" plus the hint, and "Clear filters" returns to the unfiltered view;
  - reload: the URL restores `view`, `q` and `status`.

  File: `frontend/tests/e2e/assigned-filters.spec.ts`

### Implementation for User Story 3

- [X] T028 [US3] In the no-results state, when `view=assigned` and the status filter is RESOLVED or CLOSED, show the message "Resolved and closed tickets aren't in your queue. Try All tickets." instead of the generic hint, in `frontend/src/app/(app)/tickets/TicketList.tsx` (depends on T016; same file as US1)

**Checkpoint**: T026–T027 pass. The backend needs no change beyond US1, because the filter specs already
compose.

---

## Phase 6: User Story 4 - See Where a Ticket Is in Its Lifecycle (Priority: P2)

**Goal**: An arrow-style flow diagram on the ticket detail page. Done steps show the time they
were reached, the current step is highlighted, and upcoming steps are muted. It is accessible
and stacks vertically on phones.

**Independent Test**: For tickets in each of the 4 statuses, the diagram marks steps done,
current and upcoming correctly, shows the reached times, updates immediately after "Start
progress", and has no sideways scroll at 375 px.

### Tests for User Story 4 ⚠️ (write first, must fail)

- [X] T029 [P] [US4] Write unit tests for `buildSteps(ticket)`:
  - `STATUS_FLOW` equals `["OPEN","IN_PROGRESS","RESOLVED","CLOSED"]`;
  - for each of the 4 statuses, the step states are correct (a CLOSED ticket has all steps done and CLOSED marked `current` + `final`);
  - `reachedAt` is `createdAt` for OPEN and the `STATUS_CHANGED` history `occurredAt` for the others;
  - upcoming steps have no `reachedAt`.

  File: `frontend/tests/unit/status-flow.test.ts`
- [X] T030 [P] [US4] Write component tests for `StatusFlow`:
  - it renders `<ol aria-label="Ticket lifecycle">` with 4 `<li>` in order;
  - the current `<li>` has `aria-current="step"`;
  - screen-reader text includes "done, reached …", "current step" and "upcoming";
  - arrows are `aria-hidden`;
  - a closed ticket shows "Final state";
  - re-rendering with a new status moves `aria-current`.

  File: `frontend/tests/unit/StatusFlow.test.tsx`
- [X] T031 [P] [US4] Write E2E. On a new ticket the diagram shows Open as current. After clicking "Start progress", In progress becomes current and Open shows as done with a time. On a closed ticket, "Final state" is visible. At a 375 px viewport there is no horizontal page overflow (`document.documentElement.scrollWidth <= innerWidth`). File: `frontend/tests/e2e/status-flow-diagram.spec.ts`

### Implementation for User Story 4

- [X] T032 [P] [US4] Create `STATUS_FLOW` and `buildSteps(ticket: TicketDetail): { status, label, state: "done" | "current" | "upcoming", final: boolean, reachedAt?: string }[]` from `createdAt` and `history` in `frontend/src/lib/status-flow.ts`
- [X] T033 [P] [US4] Add step animations to `frontend/src/app/globals.css`: a `status-current` ring pulse, a connector fill transition (≤ 200 ms) and a node fade/scale-in. The existing reduced-motion rule must disable them.
- [X] T034 [US4] Create `StatusFlow` per `specs/002-assigned-to-me/contracts/ui-contract.md` in `frontend/src/components/tickets/StatusFlow.tsx` (depends on T032, T033):
  - pill nodes with an icon per state (check / status icon / circle; lock for Closed), the label and the reached time;
  - chevron-arrow connectors: filled after done steps, dashed for upcoming;
  - horizontal from `sm`, vertical with down arrows below;
  - `sr-only` state text and `aria-current="step"`.
- [X] T035 [US4] Render `<StatusFlow ticket={ticket} />` directly under the header block and above `ClosedBanner` in `frontend/src/app/(app)/tickets/[key]/TicketView.tsx` (depends on T034)

**Checkpoint**: T029–T031 pass. Status changes made through the existing button update the diagram
at once, because the diagram reads the same cached ticket.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T036 [P] Write consistency integration test: build a random mix of 50 tickets across 3 users, then for each user assert `summary.assignedPending == GET ?view=assigned totalItems`, and that it still matches after a batch of transitions and reassignments (SC-003, SC-004), in `backend/src/test/java/com/c2certi/tms/ticket/AssignedCountConsistencyIntegrationTest.java`
- [X] T037 [P] Extend the perf test: seed 10,000 tickets with 500 assigned to one user in mixed statuses, then assert `?view=assigned` (with and without `q`) and `/tickets/summary` each respond in < 1 s (SC-002), in `backend/src/test/java/com/c2certi/tms/ticket/TicketSearchPerformanceTest.java`
- [X] T038 [P] Extend the accessibility E2E. Check the assigned view (empty and filled), the top bar with a badge, and detail pages with the flow diagram in Open and Closed state, on desktop and mobile, each with zero serious/critical axe violations (SC-006). File: `frontend/tests/e2e/a11y.spec.ts`
- [X] T039 [P] Document the new view, badge and flow diagram (features list and the `/tickets/summary` endpoint) in `README.md`
- [X] T040 Run the full regression and fix any failure in the file that caused it: backend `./mvnw verify` (all feature 001 + 002 tests), frontend `npm run lint`, `npx tsc --noEmit`, `npm test`, `npm run build`, and the whole Playwright suite against the running stack (SC-005)
- [X] T041 Walk through `specs/002-assigned-to-me/quickstart.md` sections 2–3 on the live stack and record results in `specs/002-assigned-to-me/acceptance-results.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 → T002.
- **Foundational (Phase 2)**: starts after T001 (backend tasks do not need T002). BLOCKS all
  stories.
- **US1, US2, US4**: independent; all start after Foundational. **Run in parallel.**
- **US3**: its tests (T026, T027) can start after Foundational; T028 needs T016 (US1).
- **Polish**: T036 needs US1 + US2; T038 needs US1, US2, US4; T040–T041 last.

### Internal chains

```text
Setup:        T001 ─► T002
Foundational: T003 ─► T006
              T004 ─► T007        (T005 independent)
US1 backend:  T011 ─► T012 (also needs T006, T007)
US1 frontend: T013, T014, T015 ─► T016
US2 backend:  T020 ─► T021 ─► T022
US2 frontend: T023 ─► T024 ─► T025
US3:          T016 ─► T028
US4:          T032, T033 ─► T034 ─► T035
```

### User Story Dependencies

| Story | Depends on | Files it owns |
|-------|------------|---------------|
| US1 Assigned view (P1) | Foundational | `TicketListQuery`, `TicketListService`, `tickets-list.ts`, `useTicketListParams.ts`, `TicketFilters.tsx`, `TicketList.tsx` |
| US2 Count badge (P1) | Foundational, T002 | `TicketSummary*` (new), `tickets-summary.ts`, `AssignedLink.tsx`, `TopBar.tsx` |
| US3 Filters in view (P2) | Foundational; T028 after T016 | new test files; one edit in `TicketList.tsx` |
| US4 Flow diagram (P2) | none beyond Setup | `status-flow.ts`, `StatusFlow.tsx`, `globals.css`, `TicketView.tsx` |

---

## Parallel Execution Examples

### Wave 1 (after T001)

```text
T002 regenerate types | T003 migration | T004 pending rule | T005 pending unit test
```

### Wave 2 (after Foundational): four agents at once

```text
Agent A (US1): T008 T009 T010 → T011 → T012 | T013 T014 T015 → T016
Agent B (US2): T017 T018 T019 → T020 → T021 → T022 | T023 → T024 → T025
Agent C (US3): T026 T027 → (wait for T016) → T028
Agent D (US4): T029 T030 T031 → T032 T033 → T034 → T035
```

Within each agent, the backend chain and the frontend chain run side by side (the frontend works
against the contract; MSW mocks until the backend lands).

### Parallel Example: User Story 4 (fully frontend)

```text
Task: "Unit tests for buildSteps in frontend/tests/unit/status-flow.test.ts"
Task: "Component tests in frontend/tests/unit/StatusFlow.test.tsx"
Task: "E2E in frontend/tests/e2e/status-flow-diagram.spec.ts"
Task: "STATUS_FLOW + buildSteps in frontend/src/lib/status-flow.ts"
Task: "Step animations in frontend/src/app/globals.css"
```

### Wave 3 (Polish)

```text
T036 T037 T038 T039 in parallel → T040 → T041
```

---

## Implementation Strategy

### MVP First

1. Phase 1 → Phase 2.
2. US1 (the view) + US2 (the top-bar entry and count). Together they deliver "after signing in I
   can see what's assigned to me". **STOP and VALIDATE** with T008, T017 and the quickstart rows 1–7.

### Incremental Delivery

Foundation → US1 → US2 (P1 done, demo) → US4 (flow diagram) → US3 (filter polish) → Polish. Each
step is additive; feature 001 behaviour stays unchanged throughout (FR-014).

### Parallel Team Strategy

After Foundational, give one agent/developer each to US1, US2 and US4. US3 can run its tests
early and pick up T028 once US1's list page lands.

---

## Requirement → Tasks

| Requirement | Tasks |
|-------------|-------|
| FR-001, FR-007 third view, URL state | T009, T010, T013–T016 |
| FR-002, FR-003 ownership + pending set | T004, T005, T007, T008, T012 |
| FR-004 order | T003, T006, T008, T012 |
| FR-005, FR-006 search/filter/paging | T026, T027, T028 |
| FR-008, FR-009, FR-010 count | T017, T019, T020–T025, T036 |
| FR-011, FR-012 rows + empty states | T009, T016, T028 |
| FR-013, FR-018 accessibility | T018, T030, T034, T038 |
| FR-014 no regressions | T040 |
| FR-015 entry after sign-in | T019, T024, T025 |
| FR-016, FR-017, FR-019 flow diagram | T029–T035 |
| SC-002 performance | T003, T037 |

## Notes

- [P] means the task touches different files and has no incomplete dependencies. [USn] traces the task to a user story in spec.md.
- Write each test first and see it fail before implementing.
- Error codes and messages must match the canonical contract after T001.
- Never commit `.env`. No new configuration or secrets are needed for this feature.

## Implementation Notes (2026-09-22)

- **T001**: the canonical contract now has 12 operations; `OpenApiContractTest` was updated from 11
  to 12.
- **T006**: `priorityRank` uses Hibernate `@Generated(event = {INSERT, UPDATE})` so the value is
  read back from PostgreSQL.
- **T010**: the empty-queue E2E stubs the list response (shared DB makes "a user with nothing
  assigned" unreliable); backend emptiness is covered by `AssignedTicketsIntegrationTest`.
- **T019**: the badge E2E compares the badge with the latest `/tickets/summary` response the page
  received instead of fixed numbers, because other specs assign tickets to the same user in parallel.
- **T034**: after a visual review, the arrows before upcoming steps were darkened for visibility.
