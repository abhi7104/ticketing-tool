# Data Model: Support Ticket Management

**Feature**: `001-support-ticket-management` | **Date**: 2026-09-21

All timestamps are `timestamptz` stored in UTC. All text inputs are trimmed before validation;
whitespace-only counts as empty. Rules apply identically on frontend (Zod) and backend (Bean
Validation, authoritative).

## Entity: User (`app_user`)

| Field | Type | Rules |
|-------|------|-------|
| id | bigint identity | PK |
| username | varchar(50) | unique, not null, lowercase |
| display_name | varchar(100) | not null |
| email | varchar(254) | unique, not null |
| password_hash | varchar(100) | not null, BCrypt; never serialized to API or logs |
| active | boolean | not null, default true; only active users can sign in or be assigned |
| created_at | timestamptz | not null |

Pre-created via env-driven seeder (research R6). No self-registration.

## Entity: Ticket (`ticket`)

| Field | Type | Rules |
|-------|------|-------|
| id | bigint identity | PK, internal only |
| ticket_key | varchar(20) | unique, not null, `TMS-<n>` from `ticket_number_seq`; immutable |
| title | varchar(150) | not null, 3–150 chars |
| description | text | not null, 10–5,000 chars (CHECK on length) |
| priority | varchar(10) | not null, CHECK in (`LOW`,`MEDIUM`,`HIGH`,`CRITICAL`) |
| status | varchar(15) | not null, CHECK in (`OPEN`,`IN_PROGRESS`,`RESOLVED`,`CLOSED`), default `OPEN` |
| reporter_id | bigint | FK → app_user, not null; set from signed-in user; immutable |
| assignee_id | bigint | FK → app_user, not null; must reference an active user |
| created_at | timestamptz | not null |
| updated_at | timestamptz | not null; bumped on field update, status change, and new comment |
| resolved_at | timestamptz | nullable; set on transition to RESOLVED |
| closed_at | timestamptz | nullable; set on transition to CLOSED |
| version | bigint | not null; optimistic lock, exposed as ETag |

**Indexes**: unique(ticket_key); btree(status); btree(reporter_id); btree(assignee_id);
btree(updated_at desc, id desc); GIN `gin_trgm_ops` on `lower(title)` and `lower(description)`.

**Editable via update**: title, description, priority, assignee — only when status ≠ CLOSED.
`status` is changed only through the transition operation.

## Entity: Comment (`ticket_comment`)

| Field | Type | Rules |
|-------|------|-------|
| id | bigint identity | PK |
| ticket_id | bigint | FK → ticket, not null |
| author_id | bigint | FK → app_user, not null; signed-in user |
| body | text | not null, 1–2,000 chars |
| created_at | timestamptz | not null |

Append-only (no edit/delete). Rejected when ticket is CLOSED. Index: (ticket_id, created_at).

## Entity: Ticket History Entry (`ticket_history`)

| Field | Type | Rules |
|-------|------|-------|
| id | bigint identity | PK |
| ticket_id | bigint | FK → ticket, not null |
| actor_id | bigint | FK → app_user, not null |
| change_type | varchar(20) | CHECK in (`CREATED`,`FIELD_UPDATED`,`STATUS_CHANGED`) |
| field_name | varchar(30) | nullable; `title`,`description`,`priority`,`assignee`,`status` |
| old_value | text | nullable (assignee stored as display name + id) |
| new_value | text | nullable |
| occurred_at | timestamptz | not null |

Immutable: insert-only; written in the same transaction as the change it records. One row per
changed field. Index: (ticket_id, occurred_at).

## Enumerations

- **Priority**: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- **Status**: `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`.

## State Machine (Status)

```text
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED (terminal)
```

| From \ To | OPEN | IN_PROGRESS | RESOLVED | CLOSED |
|-----------|------|-------------|----------|--------|
| OPEN | ✗ | ✓ | ✗ | ✗ |
| IN_PROGRESS | ✗ | ✗ | ✓ | ✗ |
| RESOLVED | ✗ | ✗ | ✗ | ✓ |
| CLOSED | ✗ | ✗ | ✗ | ✗ |

3 allowed, 13 rejected (`409 INVALID_STATUS_TRANSITION`, ticket unchanged). Enforced in the
domain entity; every accepted transition writes a `STATUS_CHANGED` history row and sets
`resolved_at` / `closed_at` as applicable.

## Validation Limits (single source for FE and BE)

| Input | Required | Min | Max | Other |
|-------|----------|-----|-----|-------|
| title | yes | 3 | 150 | trimmed |
| description | yes | 10 | 5,000 | trimmed |
| priority | yes | – | – | enum Priority |
| assigneeId | yes | – | – | existing active user |
| comment body | yes | 1 | 2,000 | trimmed |
| search `q` | no | – | 100 | literal match, wildcards escaped |
| page size | no | 1 | 100 | default 20 |
| username / password (login) | yes | 1 | 50 / 128 | generic failure message |

## Relationships

- User 1 — * Ticket (as reporter); User 1 — * Ticket (as assignee)
- Ticket 1 — * Comment; User 1 — * Comment (as author)
- Ticket 1 — * TicketHistoryEntry; User 1 — * TicketHistoryEntry (as actor)
