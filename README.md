# Ticketing Management System

A support-ticket system: users sign in, raise tickets (title, description, priority and
assignee are all required), browse and search them, edit and reassign them, comment on them,
and move them through a strictly enforced lifecycle:

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
```

The backend rejects every other status change; closed tickets are read-only.

Feature 002 adds:

- **Assigned to me**: a top-bar entry on every page (with a count of tickets waiting) and a
  listing view showing the signed-in user's Open and In progress tickets, most urgent first.
  Backed by `GET /api/v1/tickets?view=assigned` and `GET /api/v1/tickets/summary`.
- **Lifecycle flow diagram** on each ticket: Open → In progress → Resolved → Closed with arrows,
  the time each finished step was reached, and the current step highlighted.

| Part | Stack |
|------|-------|
| Backend | Java 21, Spring Boot 3.5 (REST, JPA, Security, Validation, Actuator), Flyway |
| Database | PostgreSQL 16 |
| Frontend | Next.js 15 (App Router, TypeScript strict), Tailwind CSS, Radix UI, TanStack Query |

Design documents live in [`specs/001-support-ticket-management/`](specs/001-support-ticket-management/)
and [`specs/002-assigned-to-me/`](specs/002-assigned-to-me/)
(spec, plan, data model, [REST contract](specs/001-support-ticket-management/contracts/openapi.yaml),
[UI contract](specs/001-support-ticket-management/contracts/ui-contract.md)) and the project
rules in [`.specify/memory/constitution.md`](.specify/memory/constitution.md).

## Architecture

```
Browser ──► Next.js (:3000) ──/api/* proxy──► Spring Boot (:8080) ──► PostgreSQL (:5432)
```

- The browser only talks to the Next.js origin; `/api` is forwarded to the backend, so the
  session cookie is first-party. The session is an HttpOnly, SameSite=Strict cookie holding a
  signed JWT; state-changing requests also need the double-submit CSRF token.
- The backend is a package-by-feature modular monolith (`ticket`, `comment`, `history`, `user`,
  `auth`, `common`). Each user story has its own controller and service.
- Every ticket change writes an immutable history row in the same transaction. Updates and
  status changes use optimistic locking (`ETag` / `If-Match`), so concurrent edits never
  overwrite each other silently.
- All errors are RFC 9457 problem responses with a stable `code` and per-field `errors`; the UI
  shows every error — its own validation or the backend's — in one accessible pop-up.

## Configuration (no secrets in git)

All configuration comes from environment variables. Copy the template and fill in real values:

```bash
cp .env.example .env      # .env is git-ignored
openssl rand -base64 48   # use for JWT_SECRET
```

The backend refuses to start when `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` or `JWT_SECRET` is
missing, or when `JWT_SECRET` is shorter than 32 bytes. User accounts are created at startup
from `APP_SEED_USERS` with the initial password `APP_SEED_USER_PASSWORD`.

## Run with Docker

```bash
docker compose up -d --build
open http://localhost:3000        # sign in as e.g. alice with APP_SEED_USER_PASSWORD
```

Data is kept in the `pgdata` volume: `docker compose down` keeps it, `down -v` deletes it.

## Run for development

```bash
# 1. PostgreSQL 16 reachable at DB_URL (e.g. `docker compose up -d postgres`)
# 2. Backend
cd backend && set -a && . ../.env && set +a && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
#    Swagger UI (dev profile only): http://localhost:8080/swagger-ui.html
# 3. Frontend
cd frontend && npm ci && npm run dev
```

## Tests

```bash
cd backend && ./mvnw verify            # unit + integration (real PostgreSQL) + contract tests
cd backend && ./mvnw test -Pperf       # 10,000-ticket search performance check
cd frontend && npm test                # component and unit tests (Vitest)
cd frontend && npm run test:e2e        # Playwright E2E against a running stack
                                       # needs E2E_USER, E2E_USER2, E2E_PASSWORD
```

Backend integration tests use Testcontainers when Docker is available and otherwise fall back
to embedded PostgreSQL 16 binaries (set `TMS_TEST_DB=embedded` to force it). They never use H2.
The state-machine tests (`TicketTransitionIntegrationTest`) send all 16 status pairs through the
real HTTP endpoint and check that only the 3 allowed moves succeed.

See [quickstart.md](specs/001-support-ticket-management/quickstart.md) for the full acceptance
walkthrough.
