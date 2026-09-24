# Research: Support Ticket Management

**Feature**: `001-support-ticket-management` | **Date**: 2026-09-21

The stack is fixed by the constitution (Java 21 + Spring Boot + REST, PostgreSQL, Next.js). This
document records the decisions made within that stack. No NEEDS CLARIFICATION items remain.

---

## R1. Backend framework versions and build

- **Decision**: Java 21 (Temurin), Spring Boot 3.5.x, Maven with committed wrapper (`mvnw`).
  Starters: web, data-jpa, validation, security, oauth2-resource-server (JWT decoding only),
  actuator. Plus Flyway (`flyway-core`, `flyway-database-postgresql`), PostgreSQL JDBC driver,
  `springdoc-openapi-starter-webmvc-ui`, Micrometer Prometheus registry, logstash-logback-encoder
  for JSON logs.
- **Rationale**: Latest supported Boot 3.x line on Java 21 LTS; Maven is the most common Spring
  build and needs no extra plugins for this scope.
- **Alternatives**: Gradle (equally valid, no advantage here); Spring Boot 4 (newer, less mature
  ecosystem for springdoc at planning time).

## R2. Backend architecture

- **Decision**: Modular monolith, package-by-feature: `ticket`, `comment`, `history`, `user`,
  `auth`, `common` (errors, config, web). Within a feature: `api` (controllers + DTO records),
  `domain` (entities, enums, state machine), `service`, `repository`.
- **Rationale**: Constitution VIII (YAGNI, package-by-feature). Keeps the state machine in the
  domain layer so it cannot be bypassed by any controller.
- **Alternatives**: Hexagonal/ports-and-adapters (more ceremony than this scope warrants);
  layer-based packages (scatters features).

## R3. Status state machine enforcement

- **Decision**: `TicketStatus` enum declares its single allowed successor
  (`OPEN→IN_PROGRESS→RESOLVED→CLOSED`, `CLOSED→none`). `Ticket.transitionTo(target, actor)` is
  the only mutator of `status`; it throws `InvalidStatusTransitionException(current, target,
  allowedNext)` for any other target, including same-status. Status changes use a dedicated
  endpoint `POST /api/v1/tickets/{key}/transitions`; the field-update endpoint does not accept
  `status` (unknown properties rejected), so status cannot be changed any other way.
- **Error mapping**: `409 Conflict`, code `INVALID_STATUS_TRANSITION`, message naming current and
  allowed next status.
- **Testing**: parameterized unit test over all 16 (from, to) pairs; parameterized integration
  test (MockMvc + Testcontainers PostgreSQL) over the same 16 pairs asserting HTTP code, response
  code, and that the stored status/version are unchanged on rejection.
- **Alternatives**: Spring Statemachine (heavy for a linear 4-state flow); DB trigger (logic
  hidden from domain, hard to message nicely). A DB `CHECK` on the status value set is still
  added as defence in depth.

## R4. Concurrency / lost-update protection

- **Decision**: JPA `@Version` on `Ticket`. GET returns `ETag: "<version>"` and `version` in the
  body. `PATCH` and `POST .../transitions` require `If-Match`; missing → `428` code
  `PRECONDITION_REQUIRED`; stale → `412` code `VERSION_CONFLICT`. Adding a comment does not
  require `If-Match` (append-only) but is rejected if the ticket is CLOSED.
- **Rationale**: Constitution II; satisfies FR-011 and the concurrent-transition edge case.
- **Alternatives**: Last-write-wins (violates FR-011); pessimistic locks (unneeded contention).

## R5. Authentication and session handling

- **Decision**: `POST /api/v1/auth/login` validates username/password (BCrypt, Spring Security
  `AuthenticationManager`) and issues an HS256-signed JWT (8 h expiry, subject = user id) set as
  an `HttpOnly; Secure (non-local); SameSite=Strict; Path=/` cookie `TMS_SESSION`. The API is a
  Spring OAuth2 Resource Server whose `BearerTokenResolver` reads the cookie. Signing key comes
  from env `JWT_SECRET` (≥ 32 bytes, startup fails if missing). `POST /auth/logout` clears the
  cookie; `GET /auth/me` returns the current user.
- **CSRF**: Spring `CookieCsrfTokenRepository` (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header) for
  all state-changing requests, combined with `SameSite=Strict`. Because Spring Security 6 defers
  token generation, a small filter eagerly loads the token so the `XSRF-TOKEN` cookie is set on
  the first response (including `401`s), letting the login form send it.
- **Same-origin**: The browser only talks to the Next.js origin; Next.js `rewrites` proxy
  `/api/*` to `BACKEND_URL`, so cookies are first-party and CORS is not needed in normal
  operation (CORS allow-list from env `CORS_ALLOWED_ORIGINS` for direct access).
- **Frontend guard**: Next.js `middleware.ts` redirects to `/login` when the `TMS_SESSION`
  cookie is absent; any `401` from the API also redirects to `/login?reason=expired` with a
  friendly modal. The backend remains the authority.
- **Rationale**: Constitution IV requires a standard mechanism (signed JWT via Spring Security).
  Keeping the token in an HttpOnly cookie protects it from XSS, unlike `localStorage`.
- **Alternatives**: External OIDC provider (Keycloak) – more infrastructure than v1 needs;
  server-side HTTP session – not one of the constitution's named mechanisms; JWT in
  `localStorage` – XSS exposure.

## R6. Seeding users without committing secrets

- **Decision**: Flyway creates the `app_user` table only. An idempotent `UserSeeder`
  (`ApplicationRunner`, enabled by `APP_SEED_USERS_ENABLED=true`) reads `APP_SEED_USERS` in the
  form `username:Display Name:email;...` and a shared initial password from
  `APP_SEED_USER_PASSWORD`, BCrypt-hashes it, and inserts users that don't yet exist.
  `.env.example` documents these with placeholders only.
- **Rationale**: Constitution III (no committed secrets, env-driven, fail fast); spec assumption
  that accounts are pre-created.
- **Alternatives**: Flyway insert with hashed password (hash is still a committed credential);
  admin UI (out of scope).

## R7. Persistence, schema, search

- **Decision**: PostgreSQL 16; Flyway forward-only migrations; `spring.jpa.hibernate.ddl-auto=
  validate`; `open-in-view=false`. Timestamps `timestamptz` (UTC). Ticket key: `TMS-` + value
  of sequence `ticket_number_seq`, stored in unique column `ticket_key`.
- **Search**: case-insensitive substring match on `ticket_key`, `title`, `description` using
  `ILIKE` with `%`/`_`/`\` escaped, combined with optional `status` and `reporter` (My tickets)
  predicates via JPA Specifications; `pg_trgm` GIN indexes on `title` and `description` keep
  SC-002 (<1 s at 10k tickets); B-tree indexes on `status`, `reporter_id`, `updated_at`.
- **Pagination**: `page` (0-based), `size` (default 20, max 100), sort fixed to
  `updated_at DESC, id DESC`.
- **Alternatives**: PostgreSQL full-text search (`tsvector`) – word-based, wouldn't match partial
  keys like `TMS-4`; Elasticsearch – violates YAGNI.

## R8. Error contract

- **Decision**: RFC 9457 `application/problem+json` via Spring `ProblemDetail`, extended with
  `code` (stable machine code) and `errors` (array of `{field, message}`) and `traceId`.
  Global `@RestControllerAdvice` maps: Bean Validation / malformed JSON / unknown property →
  `400 VALIDATION_FAILED`; not found → `404 TICKET_NOT_FOUND`; invalid transition →
  `409 INVALID_STATUS_TRANSITION`; closed ticket edit/comment → `409 TICKET_CLOSED`; stale
  version → `412 VERSION_CONFLICT`; missing If-Match → `428 PRECONDITION_REQUIRED`; bad login →
  `401 INVALID_CREDENTIALS`; no/expired session → `401 UNAUTHENTICATED`; anything else →
  `500 INTERNAL_ERROR` with generic text (details only in logs).
- **Rationale**: Constitution II; lets the UI render the same modal for FE- and BE-detected
  errors (FR-016/FR-017).

## R9. Validation parity FE ↔ BE

- **Decision**: Rules defined once in the spec (FR-002–FR-004, FR-012) and implemented twice:
  Bean Validation annotations + custom trimming deserializer on DTO records (backend,
  authoritative) and Zod schemas (frontend). A shared table of limits lives in
  [data-model.md](./data-model.md); contract tests assert the backend limits and Vitest tests
  assert the Zod limits against the same values.
- **Alternatives**: Generating Zod from OpenAPI (possible later; adds tooling now).

## R10. Frontend stack and UX

- **Decision**: Next.js 15 (App Router) + TypeScript strict; Tailwind CSS v4 + shadcn/ui (Radix
  primitives) for accessible components; `AlertModal` built on Radix `AlertDialog` for all error
  pop-ups; `sonner` toasts for non-blocking success; TanStack Query v5 for server state
  (caching, optimistic invalidation, retry disabled for 4xx); React Hook Form + Zod for forms;
  `openapi-typescript` generates API types from `contracts/openapi.yaml`; `lucide-react` icons;
  light/dark theme; skeleton loaders; debounced (300 ms) search synced to URL query params
  (`?q=&status=&view=&page=`) so filters are shareable and survive refresh.
- **Rationale**: Modern, smooth, accessible (WCAG 2.1 AA via Radix), minimal custom plumbing.
- **Alternatives**: MUI (heavier, harder theming); Redux (unneeded for server state); Server
  Actions for mutations (would bypass the REST contract the constitution mandates).

## R11. Testing strategy

- **Backend**: JUnit 5 + AssertJ unit tests (state machine, validation, services); Spring Boot
  `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL integration tests for every endpoint,
  migrations, security, and the 16-pair state machine matrix; a persistence-across-restart test
  that closes and restarts the application context against the same container; contract test
  comparing the generated `/v3/api-docs` against `contracts/openapi.yaml` paths/schemas.
- **Frontend**: Vitest + Testing Library for components (AlertModal, forms, filters, status
  action); MSW for API mocking; Playwright E2E against docker-compose stack covering the full
  acceptance checklist.
- **CI gates**: build, Spotless/Checkstyle, ESLint/Prettier, tests, gitleaks, OWASP
  dependency-check / `npm audit`, Flyway validate.

## R12. Runtime and local environment

- **Decision**: `docker-compose.yml` with `postgres` (named volume `pgdata` → data survives
  restarts), `backend`, `frontend`; all config through `.env` (git-ignored) based on committed
  `.env.example`. Images run as non-root. Actuator `health` (liveness/readiness) and
  `prometheus` endpoints; JSON logs with `X-Request-Id` correlation header generated by Next.js
  proxy/fetch wrapper and propagated via MDC.
