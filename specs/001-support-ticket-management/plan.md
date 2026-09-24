# Implementation Plan: Support Ticket Management

**Branch**: `001-support-ticket-management` | **Date**: 2026-09-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-support-ticket-management/spec.md`

## Summary

Deliver a web-based support ticket system: signed-in users create tickets (title, description,
priority, assignee — all mandatory), list them ("My tickets" / "All tickets") with keyword
search and status filter, open details, edit fields and reassign, comment, and advance status
strictly along OPEN → IN_PROGRESS → RESOLVED → CLOSED. The Spring Boot REST backend is the
authority for validation, the state machine, optimistic locking and audit history, persisting to
PostgreSQL via Flyway-managed schema. The Next.js frontend mirrors validation and renders every
FE- or BE-detected error in one accessible custom `AlertModal`, driven by RFC 9457 problem
responses with stable error codes.

**Core acceptance criteria (user-supplied, all mandatory)**: ticket created from UI · tickets
listed · details viewed · fields updated · assignee changed · comments added · search works ·
status filter works · valid transitions work · invalid transitions rejected by backend · data
survives restart · backend validation works · UI shows meaningful errors · state-machine
integration tests pass · modern, user-friendly, smooth UI. Each maps to a verification step in
[quickstart.md §4](./quickstart.md) and an automated test (see Acceptance Traceability below).

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript 5.x strict on Node.js LTS (frontend)

**Primary Dependencies**: Spring Boot 3.5.x (Web, Data JPA, Validation, Security, OAuth2
Resource Server for JWT, Actuator), Flyway, springdoc-openapi, Micrometer Prometheus; Next.js 15
App Router, Tailwind CSS v4, shadcn/ui (Radix), TanStack Query v5, React Hook Form + Zod,
sonner, openapi-typescript

**Storage**: PostgreSQL 16 (Flyway migrations, `ddl-auto=validate`, `pg_trgm` for search)

**Testing**: JUnit 5, AssertJ, Spring Boot Test + MockMvc, Testcontainers (PostgreSQL); Vitest +
Testing Library + MSW; Playwright E2E

**Target Platform**: Linux containers (Docker Compose locally); modern evergreen browsers,
desktop and mobile widths

**Project Type**: Web application (separate `backend/` and `frontend/`)

**Performance Goals**: List/search/filter < 1 s for 10,000 tickets (SC-002); read API p95
< 300 ms (constitution VIII)

**Constraints**: No committed secrets, env-only config, fail-fast on missing vars; WCAG 2.1 AA;
state machine enforced server-side; optimistic locking on ticket mutations; UI transitions
≤ 200 ms with reduced-motion support

**Scale/Scope**: ~10k tickets, tens of concurrent users, 4 screens (login, list, create,
detail), ~10 REST operations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Pre-design | Post-design |
|-----------|------|------------|-------------|
| I. Mandated stack | Java 21 + Spring Boot REST, PostgreSQL + Flyway, Next.js TS strict; FE only via REST | ✅ | ✅ (research R1, R10; Next.js proxies `/api` to REST, no Server Actions/DB access) |
| II. API-first REST | OpenAPI 3 contract, `/api/v1`, RFC 9457 errors with codes, pagination/filter, `@Version` + ETag/If-Match, DTOs not entities | ✅ | ✅ ([openapi.yaml](./contracts/openapi.yaml), R4, R8) |
| III. Secrets & env config | No secrets committed; `.env.example`; `NEXT_PUBLIC_` only for safe values; fail fast; gitleaks | ✅ | ✅ (R5 `JWT_SECRET`, R6 seeded users via env, R12) |
| IV. Security & least privilege | Auth on all endpoints except login/health; signed JWT via Spring Security; BCrypt; server-side validation; XSS-safe rendering; CSRF; CORS allow-list; dependency scans | ✅ | ✅ (R5, R8; object-level: any signed-in user may act on any ticket per spec assumption — no per-object restriction required in v1) |
| V. Data integrity & audit | Server-enforced state machine; immutable history (who/what/old/new/when UTC); `TMS-n` keys; no hard delete; `timestamptz`; DB constraints; SLA timestamps | ✅ | ✅ ([data-model.md](./data-model.md): CHECKs, FKs, `resolved_at`/`closed_at`, no delete endpoints) |
| VI. Test-first | Unit + Testcontainers integration (no H2) + contract; Vitest + Playwright critical flows; CI gate | ✅ | ✅ (R11; 16-pair transition matrix) |
| VII. Observability | JSON logs + request ID, Actuator health/Prometheus, business metrics, non-root containers | ✅ | ✅ (R12; counters `tickets.created`, `tickets.transitions{from,to}`, `tickets.transition.rejected`) |
| VIII. Perf, a11y, simplicity | Indexed filters, no N+1, WCAG 2.1 AA, responsive, explicit states; modular monolith | ✅ | ✅ (R7 indexes, entity graphs for detail; R10 Radix; package-by-feature R2) |

**Result**: PASS — no violations; Complexity Tracking not required.

## Project Structure

### Documentation (this feature)

```text
specs/001-support-ticket-management/
├── plan.md              # This file
├── research.md          # Phase 0 decisions (R1–R12)
├── data-model.md        # Entities, state machine, validation limits
├── quickstart.md        # Run + acceptance validation guide
├── contracts/
│   ├── openapi.yaml     # REST contract (source of truth for FE types)
│   └── ui-contract.md   # Routes, screens, AlertModal error mapping
├── checklists/
│   └── requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
backend/
├── mvnw, pom.xml, Dockerfile
└── src/
    ├── main/
    │   ├── java/com/c2certi/tms/
    │   │   ├── TmsApplication.java
    │   │   ├── common/          # config (security, CORS, CSRF, OpenAPI), error (ProblemDetail
    │   │   │                    #   advice, ErrorCode, exceptions), web (request-id filter), time
    │   │   ├── auth/            # api (AuthController, LoginRequest), JwtService, cookie resolver
    │   │   ├── user/            # domain (User), repository, UserSeeder, api (UserController)
    │   │   ├── ticket/          # domain (Ticket, TicketStatus state machine, Priority),
    │   │   │                    #   repository + Specifications, service, api (controller, DTOs)
    │   │   ├── comment/         # domain, repository, service, api
    │   │   └── history/         # domain (TicketHistoryEntry), repository, HistoryRecorder
    │   └── resources/
    │       ├── application.yml  # all env-driven placeholders, no secrets
    │       └── db/migration/    # V1__users.sql, V2__tickets.sql, V3__comments_history.sql, ...
    └── test/java/com/c2certi/tms/
        ├── ticket/              # TicketStatusTransitionTest (unit, 16 pairs),
        │                        #   TicketTransitionIntegrationTest (16 pairs, Testcontainers),
        │                        #   TicketCrudIntegrationTest, TicketSearchIntegrationTest,
        │                        #   TicketValidationIntegrationTest, PersistenceAcrossRestartTest
        ├── comment/, auth/, user/
        ├── contract/            # OpenApiContractTest
        └── support/             # PostgresContainerConfig, auth test helpers

frontend/
├── package.json, next.config.ts (rewrites /api → BACKEND_URL), middleware.ts, Dockerfile
├── src/
│   ├── app/
│   │   ├── login/page.tsx
│   │   └── (app)/               # authenticated shell layout
│   │       ├── tickets/page.tsx
│   │       ├── tickets/new/page.tsx
│   │       └── tickets/[key]/page.tsx
│   ├── components/
│   │   ├── ui/                  # shadcn primitives
│   │   ├── alert-modal/         # AlertModal + useAlert provider (single error surface)
│   │   └── tickets/             # TicketTable, TicketFilters, TicketForm, StatusBadge,
│   │                            #   PriorityBadge, StatusActionButton, CommentThread, ActivityLog
│   ├── lib/
│   │   ├── api/                 # fetch client (CSRF header, If-Match, request id, problem parsing),
│   │   │                        #   generated schema.d.ts, query hooks
│   │   ├── validation/          # Zod schemas mirroring data-model limits
│   │   └── errors/              # problem → AlertModal content mapping
│   └── styles/
└── tests/
    ├── unit/                    # Vitest component + schema tests
    └── e2e/                     # Playwright: one spec per acceptance criterion group

docker-compose.yml               # postgres (named volume), backend, frontend
.env.example                     # placeholders only
.gitignore                       # .env, .env*.local, build outputs
.gitleaks.toml
```

**Structure Decision**: Web application layout with independent `backend/` (Maven, Spring Boot)
and `frontend/` (Next.js) projects plus root-level compose and env templates. The backend is a
package-by-feature modular monolith per constitution VIII.

## Key Design Decisions (see research.md for rationale)

1. **State machine lives in the domain** (`Ticket.transitionTo`) and is reachable only through
   `POST /tickets/{key}/transitions`; PATCH rejects `status` as an unknown property. Rejections
   return `409 INVALID_STATUS_TRANSITION` with `currentStatus` / `allowedNextStatus`.
2. **Optimistic locking** via `@Version` ↔ `ETag`/`If-Match` on PATCH and transitions.
3. **One error surface**: backend `ProblemDetail` + `code` + `errors[]`; frontend maps codes to
   `AlertModal` content ([ui-contract.md](./contracts/ui-contract.md)) and highlights fields.
4. **Auth**: HttpOnly signed-JWT cookie, CSRF cookie/header, same-origin via Next.js rewrites.
5. **History** written in the same transaction as each change; detail endpoint returns comments
   and history using fetch joins to avoid N+1.
6. **Persistence across restart** guaranteed by PostgreSQL named volume and verified by an
   integration test that restarts the Spring context against the same container.

## Acceptance Traceability

| Acceptance criterion | Spec refs | Automated verification |
|----------------------|-----------|------------------------|
| Ticket created from UI | US1, FR-001–005 | `TicketCrudIntegrationTest`, E2E `create-ticket.spec.ts` |
| Tickets listed | US2, FR-006, FR-009a | `TicketSearchIntegrationTest`, E2E `list-search-filter.spec.ts` |
| Details viewed | US2, FR-009 | `TicketCrudIntegrationTest`, E2E `ticket-detail.spec.ts` |
| Fields updated | US4, FR-010 | `TicketCrudIntegrationTest`, E2E `edit-ticket.spec.ts` |
| Assignee changed | US4, FR-010 | same as above (assignee case) |
| Comments added | US5, FR-012 | `CommentIntegrationTest`, E2E `comments.spec.ts` |
| Search works | US2, FR-007 | `TicketSearchIntegrationTest`, E2E |
| Status filter works | US2, FR-008 | `TicketSearchIntegrationTest`, E2E |
| Valid transitions work | US3, FR-013 | `TicketTransitionIntegrationTest` (3 allowed), E2E `status-flow.spec.ts` |
| Invalid transitions rejected by backend | US3, FR-013–015 | `TicketTransitionIntegrationTest` (13 rejected, state unchanged) |
| Data survives restart | FR-020, SC-004 | `PersistenceAcrossRestartTest`, quickstart §4 #11 |
| Backend validation works | FR-017, SC-005 | `TicketValidationIntegrationTest`, `CommentIntegrationTest` |
| UI shows meaningful errors | FR-016, FR-018, SC-006 | Vitest `AlertModal`/error-mapping tests, E2E `errors.spec.ts` |
| State-machine integration tests pass | SC-003 | CI gate on `TicketTransitionIntegrationTest` |
| Modern, user-friendly, smooth UI | FR-022, SC-007 | ui-contract review, Playwright + axe accessibility checks |

## Complexity Tracking

No constitution violations; section intentionally empty.
