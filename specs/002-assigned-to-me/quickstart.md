# Quickstart & Validation Guide: Assigned to Me + Lifecycle Flow Diagram

Proves feature 002 end to end. Contracts: [openapi-delta.yaml](./contracts/openapi-delta.yaml),
[ui-contract.md](./contracts/ui-contract.md). Setup and run steps are the same as
[feature 001 quickstart](../001-support-ticket-management/quickstart.md).

## Prerequisites

- Stack running (backend on :8080, frontend on :3000) against PostgreSQL 14+ with `.env` loaded.
- Seeded users `alice` and `bob`; password = `APP_SEED_USER_PASSWORD` from `.env`.

## 1. Automated tests

```bash
cd backend && JAVA_HOME=~/.sdkman/candidates/java/21.0.2-tem \
  ./mvnw test -Dtest='AssignedTicketsIntegrationTest,TicketSummaryIntegrationTest'
cd backend && ./mvnw test -Pperf          # includes the 500-assigned / 10,000-ticket check
cd frontend && npm test                    # StatusFlow, AssignedBadge, filters
cd frontend && set -a && . ../.env && set +a && npm run test:e2e   # whole suite, 001 + 002
```

Expected: all green, including every feature 001 test (SC-005).

## 2. Manual walkthrough

As **alice**, create 5 tickets assigned to **bob**: priorities Low, Critical, Medium, High,
High (in that order). Move one High ticket to In progress, then create one more assigned to bob
and move it to Resolved.

| # | Check | Steps | Expected |
|---|-------|-------|----------|
| 1 | Entry after sign-in | Sign in as bob | Top bar shows "Assigned to me" with badge **5** on every page |
| 2 | View content | Click it | URL `?view=assigned`; 5 tickets, none resolved, none assigned to others |
| 3 | Order | Look at the list | Critical, High (older first), High, Medium, Low |
| 4 | Filters | Search a title word; pick "In progress"; pick "Closed" | Narrowed results; Closed → "No tickets match" |
| 5 | Count follows changes | Open the Critical ticket → Start progress → Mark resolved; go back | Badge **4**; ticket gone from the view |
| 6 | Reassignment | Edit a ticket, assign to alice | Badge **3** for bob |
| 7 | Empty state | Resolve/reassign the rest | "Nothing waiting on you" with "View all tickets"; badge hidden |
| 8 | Flow diagram | Open tickets in each status | Arrows Open → In progress → Resolved → Closed; done steps show times; current highlighted; Closed shows "Final state" |
| 9 | Diagram updates | On an Open ticket click Start progress | Diagram moves current to In progress immediately |
| 10 | Phone layout | Narrow window to 375 px | Diagram stacks vertically, no sideways scroll; badge collapses to icon + count |
| 11 | Unchanged defaults | Sign out/in | Lands on "My tickets" as before |

## 3. API checks

Reuse the cookie/CSRF setup from the feature 001 quickstart (section 5), signed in as bob:

```bash
curl -s -b $JAR "$BASE/tickets/summary" | jq                       # {"assignedPending": 5}
curl -s -b $JAR "$BASE/tickets?view=assigned" | jq '.totalItems'     # same number
curl -s -b $JAR "$BASE/tickets?view=assigned&status=CLOSED" | jq '.totalItems'   # 0
curl -s -b $JAR "$BASE/tickets?view=bogus" | jq '{status,code,errors}'          # 400, field "view"
curl -s -o /dev/null -w '%{http_code}\n' "$BASE/tickets/summary"      # 401 without cookie
```
