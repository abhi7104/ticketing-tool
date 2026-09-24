# Data Model: Assigned to Me + Lifecycle Flow Diagram

**Feature**: `002-assigned-to-me` | **Date**: 2026-09-22

Builds on `specs/001-support-ticket-management/data-model.md`. No new tables. One derived
column and one index on `ticket`.

## Change: Ticket (`ticket`)

| Field | Type | Rules |
|-------|------|-------|
| priority_rank | smallint, generated | `GENERATED ALWAYS AS (CASE priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END) STORED`; never written by the application; mapped read-only |

**New index** (partial): `ix_ticket_assigned_queue ON ticket (assignee_id, priority_rank DESC,
created_at, id) WHERE status IN ('OPEN', 'IN_PROGRESS')`.

**Migration**: `V4__ticket_priority_rank.sql` — forward-only, additive; existing rows get their
rank computed by PostgreSQL when the column is added.

## Derived concept: Pending status set

| Status | Pending for assignee? |
|--------|-----------------------|
| OPEN | yes |
| IN_PROGRESS | yes |
| RESOLVED | no |
| CLOSED | no |

Defined once in the backend (`TicketStatus.isPendingForAssignee()`), used by both the view and
the count.

## Derived concept: Assigned queue

- **Rows**: tickets where `assignee_id = <signed-in user>` AND status is pending, AND the optional
  keyword and status filters.
- **Order**: `priority_rank DESC, created_at ASC, id ASC`.
- **Count**: the same rows with no keyword or status filter. Never stored.

## Derived concept: Lifecycle step (frontend only)

| Field | Source |
|-------|--------|
| status | fixed order OPEN → IN_PROGRESS → RESOLVED → CLOSED |
| state | `done` if the step comes before the ticket's current status; `current` if equal; `upcoming` otherwise (a CLOSED ticket shows every step done, CLOSED as current/final) |
| reachedAt | OPEN: ticket `createdAt`; others: `occurredAt` of the history entry with `changeType = STATUS_CHANGED` and `newValue = status`; empty for upcoming steps |

## Validation changes

| Input | Rule |
|-------|------|
| `view` query parameter | one of `mine`, `all`, `assigned` (default `mine`); other values → 400 `VALIDATION_FAILED` with field `view` |

All other limits are unchanged from feature 001.
