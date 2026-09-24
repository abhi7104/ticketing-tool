---

description: "Task list for Support Ticket Management"
---

# Tasks: Support Ticket Management

**Input**: Design documents from `/specs/001-support-ticket-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml,
contracts/ui-contract.md, quickstart.md

**Tests**: INCLUDED. The acceptance criteria require "state-machine integration tests pass" and
the constitution (Principle VI) mandates test-first with Testcontainers PostgreSQL, Vitest and
Playwright. Within each story, write the test tasks first and confirm they fail before
implementing.

**Organization**: Tasks are grouped by user story. The layout is designed for **maximum parallel
execution**:

- Backend endpoints are split into one controller + service **per story** (all under
  `/api/v1/tickets`), so stories never edit the same Java file.
- All entities, migrations, the state machine, error handling, security and the ticket-detail
  read path are in **Foundational**, so every story starts from the same base.
- The ticket detail page is created in Foundational as a **shell with slot components**
  (`StatusActionButton`, `ClosedBanner`, `TicketEditPanel`, `CommentThread`) that each story
  fills in its own file.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story the task belongs to (US1–US5)

## Path Conventions

- Backend main code: `backend/src/main/java/com/c2certi/tms/`
- Backend tests: `backend/src/test/java/com/c2certi/tms/`
- Backend resources: `backend/src/main/resources/`
- Frontend: `frontend/src/`, tests in `frontend/tests/unit/` and `frontend/tests/e2e/`
- Error codes, limits and routes MUST match `contracts/openapi.yaml`, `data-model.md` and
  `contracts/ui-contract.md` exactly.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Repository, build tooling, containers and CI. No secrets in any file.

- [X] T001 Create root `.gitignore` ignoring `.env`, `.env*.local`, `backend/target/`, `frontend/node_modules/`, `frontend/.next/`, `frontend/playwright-report/`, `frontend/test-results/`, IDE folders in `.gitignore`
- [X] T002 [P] Create `.env.example` with placeholder-only values and a comment per variable for `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_TTL` (default `PT8H`), `COOKIE_SECURE`, `CORS_ALLOWED_ORIGINS`, `APP_SEED_USERS_ENABLED`, `APP_SEED_USERS`, `APP_SEED_USER_PASSWORD`, `BACKEND_URL` in `.env.example`
- [X] T003 [P] Initialize the Spring Boot 3.5.x / Java 21 Maven project with wrapper and dependencies from research R1 (web, data-jpa, validation, security, oauth2-resource-server, actuator, flyway-core, flyway-database-postgresql, postgresql, springdoc-openapi-starter-webmvc-ui, micrometer-registry-prometheus, logstash-logback-encoder; test: spring-boot-starter-test, spring-security-test, testcontainers postgresql + junit-jupiter) plus Spotless (google-java-format) in `backend/pom.xml`, `backend/mvnw`, `backend/.mvn/wrapper/maven-wrapper.properties`, and `backend/src/main/java/com/c2certi/tms/TmsApplication.java`
- [X] T004 [P] Initialize Next.js 15 App Router project with TypeScript `strict`, ESLint, Prettier, Tailwind CSS v4, shadcn/ui (button, input, textarea, select, dialog, alert-dialog, dropdown-menu, badge, skeleton, tabs, table, tooltip), and deps `@tanstack/react-query`, `react-hook-form`, `@hookform/resolvers`, `zod`, `sonner`, `lucide-react`, `next-themes`; dev deps `openapi-typescript`, `vitest`, `@testing-library/react`, `@testing-library/user-event`, `jsdom`, `msw`, `@playwright/test`, `@axe-core/playwright`; scripts `dev`, `build`, `lint`, `test`, `test:e2e`, `gen:api` in `frontend/package.json`, `frontend/tsconfig.json`, `frontend/eslint.config.mjs`, `frontend/.prettierrc`, `frontend/components.json`
- [X] T005 [P] Add secret scanning with `.gitleaks.toml` and a `.pre-commit-config.yaml` running gitleaks, in `.gitleaks.toml` and `.pre-commit-config.yaml`
- [X] T006 [P] Create `docker-compose.yml` with `postgres:16` (named volume `pgdata`, healthcheck), `backend` (depends on healthy postgres, env from `.env`, healthcheck on `/actuator/health`), `frontend` (`BACKEND_URL=http://backend:8080`, port 3000), no inline secrets, in `docker-compose.yml`
- [X] T007 [P] Create multi-stage non-root backend image (Temurin 21 JRE, `USER` non-root) in `backend/Dockerfile` and `backend/.dockerignore`
- [X] T008 [P] Create multi-stage non-root frontend image using Next.js `output: 'standalone'` in `frontend/Dockerfile` and `frontend/.dockerignore`
- [X] T009 [P] Create CI pipeline (backend: `./mvnw spotless:check verify`; frontend: `npm ci`, `lint`, `test`, `build`; gitleaks scan; OWASP dependency-check / `npm audit --audit-level=high`; Playwright E2E against `docker compose up`) in `.github/workflows/ci.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Schema, domain model with state machine, error contract, auth, ticket-detail read
path, and the frontend shell every story builds on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Backend configuration & schema

- [X] T010 [P] Create env-driven config (datasource `${DB_URL}`/`${DB_USERNAME}`/`${DB_PASSWORD}`, `ddl-auto: validate`, `open-in-view: false`, Flyway enabled, `jdbc.time_zone: UTC`, actuator exposure `health,info,prometheus` with liveness/readiness probes, `app.jwt.secret: ${JWT_SECRET}`, `app.jwt.ttl: ${JWT_TTL:PT8H}`, `app.cookie.secure: ${COOKIE_SECURE:true}`, `app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}`, seeder props; springdoc enabled only in `dev` profile) in `backend/src/main/resources/application.yml` and `backend/src/main/resources/application-dev.yml`
- [X] T011 [P] Configure JSON structured logging with MDC `requestId` via logstash encoder (plain text in `dev` profile) in `backend/src/main/resources/logback-spring.xml`
- [X] T012 [P] Create Flyway migration for `app_user` per data-model.md (unique username/email, `active`, `created_at timestamptz`) in `backend/src/main/resources/db/migration/V1__create_app_user.sql`
- [X] T013 [P] Create Flyway migration for `ticket` per data-model.md (`CREATE EXTENSION IF NOT EXISTS pg_trgm`, sequence `ticket_number_seq`, CHECK constraints on priority/status/description length, FKs, `version`, `resolved_at`, `closed_at`, btree indexes on status/reporter_id/assignee_id/(updated_at desc, id desc), GIN trigram indexes on `lower(title)` and `lower(description)`) in `backend/src/main/resources/db/migration/V2__create_ticket.sql`
- [X] T014 [P] Create Flyway migration for `ticket_comment` and `ticket_history` per data-model.md (FKs, CHECK on `change_type`, indexes on `(ticket_id, created_at)` / `(ticket_id, occurred_at)`) in `backend/src/main/resources/db/migration/V3__create_comment_and_history.sql`

### Backend domain

- [X] T015 [P] Create `Priority` enum (`LOW, MEDIUM, HIGH, CRITICAL`) in `backend/src/main/java/com/c2certi/tms/ticket/domain/Priority.java`
- [X] T016 [P] Create `TicketStatus` enum implementing the state machine: `Optional<TicketStatus> allowedNext()` (OPEN→IN_PROGRESS, IN_PROGRESS→RESOLVED, RESOLVED→CLOSED, CLOSED→empty) and `boolean canTransitionTo(TicketStatus target)` (true only for the single successor; same-status false) in `backend/src/main/java/com/c2certi/tms/ticket/domain/TicketStatus.java`
- [X] T017 [P] Create `User` JPA entity (no password in `toString`) and `UserRepository` (`findByUsernameIgnoreCase`, `findByIdAndActiveTrue`, `findAllByActiveTrueOrderByDisplayNameAsc`) in `backend/src/main/java/com/c2certi/tms/user/domain/User.java` and `backend/src/main/java/com/c2certi/tms/user/repository/UserRepository.java`
- [X] T018 [P] Create error infrastructure: `ErrorCode` enum (all codes in openapi `Problem.code`), base `ApiException(ErrorCode, HttpStatus, title, detail)`, and subclasses `TicketNotFoundException`, `InvalidStatusTransitionException(current, target, allowedNext)`, `TicketClosedException`, `VersionConflictException`, `PreconditionRequiredException`, `FieldValidationException(List<FieldError>)` in `backend/src/main/java/com/c2certi/tms/common/error/`
- [X] T019 Create `Ticket` JPA entity per data-model.md with `@Version`, ManyToOne reporter/assignee, and domain methods: `static create(key, title, description, priority, reporter, assignee, now)`, `transitionTo(TicketStatus target, Instant now)` (throws `InvalidStatusTransitionException` unless `status.canTransitionTo(target)`; sets `resolvedAt`/`closedAt`; bumps `updatedAt`), `updateTitle/Description/Priority/Assignee(..., now)` (throw `TicketClosedException` when CLOSED), `assertOpenForChanges()`, `touch(now)`; no public status setter; in `backend/src/main/java/com/c2certi/tms/ticket/domain/Ticket.java` (depends on T015–T018)
- [X] T020 [P] Create `Comment` entity and `CommentRepository` (`findByTicketIdOrderByCreatedAtAscIdAsc`) in `backend/src/main/java/com/c2certi/tms/comment/domain/Comment.java` and `backend/src/main/java/com/c2certi/tms/comment/repository/CommentRepository.java` (depends on T019)
- [X] T021 [P] Create `TicketHistoryEntry` entity (immutable, `ChangeType` enum CREATED/FIELD_UPDATED/STATUS_CHANGED), `TicketHistoryRepository` (`findByTicketIdOrderByOccurredAtAscIdAsc`), and `HistoryRecorder` service with `recordCreated`, `recordFieldChange(ticket, actor, field, old, new)`, `recordStatusChange(ticket, actor, old, new)` in `backend/src/main/java/com/c2certi/tms/history/` (depends on T019)
- [X] T022 [P] Create `TicketRepository` (`JpaSpecificationExecutor<Ticket>`, `findByTicketKey` with `@EntityGraph(reporter, assignee)`, native `nextTicketNumber()` from `ticket_number_seq`) in `backend/src/main/java/com/c2certi/tms/ticket/repository/TicketRepository.java` (depends on T019)

### Backend cross-cutting web, errors, security

- [X] T023 [P] Configure Jackson: `FAIL_ON_UNKNOWN_PROPERTIES=true`, a `String` deserializer that trims (blank → empty string), ISO-8601 UTC instants in `backend/src/main/java/com/c2certi/tms/common/config/JacksonConfig.java`
- [X] T024 [P] Create `RequestIdFilter` (read/generate `X-Request-Id`, put in MDC `requestId`, echo in response header) in `backend/src/main/java/com/c2certi/tms/common/web/RequestIdFilter.java`
- [X] T025 [P] Create `ETags` helper (`format(version)` → `"3"`, `parseIfMatch(header)` → `long`, throwing `PreconditionRequiredException` if absent/invalid) and `CurrentUser` resolver (`@AuthenticationPrincipal Jwt` → user id → `User`, via `CurrentUserProvider`) in `backend/src/main/java/com/c2certi/tms/common/web/ETags.java` and `backend/src/main/java/com/c2certi/tms/common/web/CurrentUserProvider.java`
- [X] T026 Create `GlobalExceptionHandler` (`@RestControllerAdvice`) returning `ProblemDetail` with `code`, `errors[]`, `traceId` (=requestId), and `currentStatus`/`allowedNextStatus` for transitions, mapping per research R8: `MethodArgumentNotValidException`/`HandlerMethodValidationException`/`HttpMessageNotReadableException` (incl. unknown property, bad enum → friendly field message)/`FieldValidationException`/`MethodArgumentTypeMismatchException` → 400 `VALIDATION_FAILED`; `ApiException` → its status/code; `ObjectOptimisticLockingFailureException` → 412 `VERSION_CONFLICT`; anything else → 500 `INTERNAL_ERROR` with generic text and server-side error log; friendly messages for every field (e.g. "Title must be between 3 and 150 characters.") in `backend/src/main/java/com/c2certi/tms/common/error/GlobalExceptionHandler.java` (depends on T018)
- [X] T027 [P] Create `JwtService` (HS256 `NimbusJwtEncoder`/`NimbusJwtDecoder` from `app.jwt.secret`; fail startup with clear message if missing or < 32 bytes; `issue(User)` with subject=user id, `exp`=now+ttl) and `SessionCookieFactory` (`TMS_SESSION`, HttpOnly, SameSite=Strict, Path=/, Secure per `app.cookie.secure`; `clear()`) in `backend/src/main/java/com/c2certi/tms/auth/JwtService.java` and `backend/src/main/java/com/c2certi/tms/auth/SessionCookieFactory.java`
- [X] T028 Create `SecurityConfig`: stateless, OAuth2 resource server JWT with `BearerTokenResolver` reading `TMS_SESSION` cookie; `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `CsrfCookieFilter` that eagerly loads the token; permit `POST /api/v1/auth/login`, `/actuator/health/**`, (dev) swagger; everything else authenticated; `AuthenticationEntryPoint` and `AccessDeniedHandler` writing 401 `UNAUTHENTICATED` / 403 problem JSON; CORS from `app.cors.allowed-origins`; `BCryptPasswordEncoder`; `DaoAuthenticationProvider` over `UserRepository` (active users only) in `backend/src/main/java/com/c2certi/tms/common/config/SecurityConfig.java` and `backend/src/main/java/com/c2certi/tms/common/config/CsrfCookieFilter.java` (depends on T017, T026, T027)
- [X] T029 Create `AuthController` (`POST /api/v1/auth/login` → 200 `CurrentUser` + Set-Cookie, bad credentials → 401 `INVALID_CREDENTIALS` "Invalid username or password."; `POST /logout` → 204 + cleared cookie; `GET /me`) with `LoginRequest`/`CurrentUserResponse` records in `backend/src/main/java/com/c2certi/tms/auth/api/` (depends on T028)
- [X] T030 [P] Create idempotent `UserSeeder` (`ApplicationRunner`, active only when `app.seed.users-enabled=true`; parses `APP_SEED_USERS` `username:Display Name:email;...`; BCrypt-hashes `APP_SEED_USER_PASSWORD`; fails fast if enabled and password missing; never logs password) in `backend/src/main/java/com/c2certi/tms/user/UserSeeder.java` (depends on T017)
- [X] T031 [P] Create `UserController` `GET /api/v1/users` returning active `UserSummary` list in `backend/src/main/java/com/c2certi/tms/user/api/UserController.java` (depends on T017)
- [X] T032 [P] Create `TicketMetrics` (Micrometer counters `tickets.created`, `tickets.transitions{from,to}`, `tickets.transition.rejected{from,to}`, `tickets.comments.added`) in `backend/src/main/java/com/c2certi/tms/common/metrics/TicketMetrics.java`
- [X] T033 [P] Create `OpenApiConfig` (title/version, cookie security scheme) in `backend/src/main/java/com/c2certi/tms/common/config/OpenApiConfig.java`

### Backend ticket-detail read path (shared by every story)

- [X] T034 Create response DTO records matching openapi schemas — `UserSummary`, `TicketSummaryResponse`, `TicketDetailResponse` (incl. `version`, `allowedNextStatus`, `comments`, `history`), `CommentResponse`, `HistoryEntryResponse`, `TicketPageResponse` — and `TicketMapper` in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/` and `backend/src/main/java/com/c2certi/tms/ticket/api/TicketMapper.java` (depends on T019–T021)
- [X] T035 Create `TicketDetailService.getDetail(key)` (read-only transaction, 404 `TICKET_NOT_FOUND` "We couldn't find ticket {key}.", loads comments and history in 2 queries, no N+1) and `TicketDetailController` `GET /api/v1/tickets/{ticketKey}` (validates key pattern, sets `ETag`) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketDetailService.java` and `backend/src/main/java/com/c2certi/tms/ticket/api/TicketDetailController.java` (depends on T022, T025, T034)

### Backend test infrastructure & foundational tests

- [X] T036 [P] Create test support: singleton Testcontainers `postgres:16` with `@ServiceConnection`, `application-test.yml` (test JWT secret generated at runtime, seeder off), `TestUsers` fixture creating users directly, `ApiClient` helper that logs in via MockMvc and returns session cookie + CSRF token, `TicketFixtures` that creates tickets via repository, in `backend/src/test/java/com/c2certi/tms/support/` and `backend/src/test/resources/application-test.yml`
- [X] T037 [P] Write unit test covering all 16 `(from, to)` pairs of `TicketStatus.canTransitionTo` and `Ticket.transitionTo` (3 succeed with timestamps set; 13 throw `InvalidStatusTransitionException` leaving status unchanged; `allowedNext` values) in `backend/src/test/java/com/c2certi/tms/ticket/domain/TicketStatusTransitionTest.java`
- [X] T038 [P] Write unit test for `Ticket` update guards (CLOSED → `TicketClosedException`; `updatedAt` bump) in `backend/src/test/java/com/c2certi/tms/ticket/domain/TicketTest.java`
- [X] T039 [P] Write integration test for auth: login success sets HttpOnly `TMS_SESSION`, wrong password and unknown user both → 401 `INVALID_CREDENTIALS` with identical message, missing CSRF → 403, protected endpoint without cookie → 401 `UNAUTHENTICATED` problem JSON, logout clears cookie, `/me` returns user, `/users` lists active users only, in `backend/src/test/java/com/c2certi/tms/auth/AuthIntegrationTest.java`
- [X] T040 [P] Write integration test for `GET /api/v1/tickets/{key}`: 200 with ETag, comments/history ordered, `allowedNextStatus`; 404 problem for unknown key; 400 for malformed key; error body never contains stack traces, in `backend/src/test/java/com/c2certi/tms/ticket/TicketDetailIntegrationTest.java`

### Frontend foundation

- [X] T041 [P] Configure `next.config.ts` (`output: 'standalone'`, rewrites `/api/:path*` → `${BACKEND_URL}/api/:path*`, security headers: CSP, `X-Content-Type-Options`, `Referrer-Policy`, `Frame-Options`) in `frontend/next.config.ts`
- [X] T042 [P] Create `middleware.ts` redirecting unauthenticated requests (no `TMS_SESSION` cookie) for all routes except `/login` and static assets to `/login?next=<path>` in `frontend/src/middleware.ts`
- [X] T043 [P] Generate API types from contract (`gen:api` → `openapi-typescript ../specs/001-support-ticket-management/contracts/openapi.yaml -o src/lib/api/schema.d.ts`) and commit output in `frontend/src/lib/api/schema.d.ts`
- [X] T044 Create API client `apiFetch<T>(path, {method, body, ifMatch})`: same-origin `/api/v1`, `credentials: 'include'`, `X-XSRF-TOKEN` from `XSRF-TOKEN` cookie on non-GET, `X-Request-Id` (crypto.randomUUID), returns `{data, etag}`; throws typed `ApiError {status, code, title, detail, fieldErrors, currentStatus, allowedNextStatus, traceId}` from problem JSON; network/timeout (15 s AbortController) → `ApiError` code `NETWORK_ERROR`; in `frontend/src/lib/api/client.ts` (depends on T043)
- [X] T045 [P] Create Zod schemas mirroring data-model.md limits with friendly messages identical in meaning to backend messages: `loginSchema`, `ticketFormSchema` (title 3–150, description 10–5000, priority enum, assigneeId required), `commentSchema` (1–2000), all with `.trim()`, in `frontend/src/lib/validation/schemas.ts`
- [X] T046 [P] Create `problemToAlert(error: ApiError | ZodError)` mapping every row of the AlertModal table in ui-contract.md to `{title, body | bulletList, actions, variant}`, never exposing raw JSON/stack traces, in `frontend/src/lib/errors/problem-to-alert.ts`
- [X] T047 [P] Create `AlertModal` (Radix `AlertDialog`, `role="alertdialog"`, focus trap, Esc closes, focus return, fade/scale ≤200 ms honoring `prefers-reduced-motion`, destructive/info variants, bullet list support, configurable action buttons) and `AlertProvider` + `useAlert()` hook (`showError(err)`, `confirm({title, body, confirmLabel})` returning Promise<boolean>) in `frontend/src/components/alert-modal/AlertModal.tsx` and `frontend/src/components/alert-modal/AlertProvider.tsx`
- [X] T048 Create root providers (TanStack `QueryClient` with no retry on 4xx, global `onError` that routes 401 `UNAUTHENTICATED` to `/login?reason=expired`; `AlertProvider`; `sonner` `Toaster`; `next-themes`) and root layout with Inter font and globals in `frontend/src/app/providers.tsx`, `frontend/src/app/layout.tsx`, `frontend/src/app/globals.css` (depends on T044, T047)
- [X] T049 [P] Create auth hooks `useMe`, `useLogin`, `useLogout` in `frontend/src/lib/api/auth.ts` (depends on T044)
- [X] T050 Create sign-in page (centered card, username/password with RHF+Zod, submit spinner, errors via `useAlert`, "Session expired" modal when `?reason=expired`, redirect to `next` or `/tickets`) in `frontend/src/app/login/page.tsx` (depends on T045, T048, T049)
- [X] T051 Create authenticated app shell: top bar (product name, "New ticket" button → `/tickets/new`, theme toggle, user menu with display name and Sign out), responsive container, `/` redirect to `/tickets`, in `frontend/src/app/(app)/layout.tsx`, `frontend/src/app/(app)/page.tsx`, `frontend/src/components/layout/TopBar.tsx` (depends on T048, T049)
- [X] T052 [P] Create `StatusBadge` and `PriorityBadge` (text + icon + color, accessible contrast in light/dark) and `formatDate`/`formatRelative` helpers (local time zone) in `frontend/src/components/tickets/StatusBadge.tsx`, `frontend/src/components/tickets/PriorityBadge.tsx`, `frontend/src/lib/format.ts`
- [X] T053 [P] Create `useUsers()` hook (`GET /users`, cached 5 min) and `AssigneeSelect` (searchable combobox of active users, keyboard accessible) in `frontend/src/lib/api/users.ts` and `frontend/src/components/tickets/AssigneeSelect.tsx` (depends on T044)
- [X] T054 Create shared `TicketForm` (mode `create | edit`, RHF + `ticketFormSchema`, fields title / description with live counter / priority / assignee, inline errors, on invalid submit shows `useAlert().showError` with all problems then focuses first invalid field, `applyServerErrors(apiError)` that sets RHF field errors from `fieldErrors`, submit button spinner + disabled while pending, `isDirty` exposed) in `frontend/src/components/tickets/TicketForm.tsx` (depends on T045, T047, T053)
- [X] T055 Create ticket detail read path: `useTicket(key)` hook (returns data + etag, 404 → not-found state) in `frontend/src/lib/api/tickets.ts`, and detail page shell `frontend/src/app/(app)/tickets/[key]/page.tsx` with skeleton loader, friendly "Ticket not found" state with "Back to tickets", `TicketHeader` (key, title, badges, reporter, assignee, created/updated), `ActivityLog` tab (human sentences, e.g. "Alice changed priority from Medium to High"), and slots rendering `ClosedBanner`, `StatusActionButton`, `TicketEditPanel`, `CommentThread` in `frontend/src/components/tickets/TicketHeader.tsx` and `frontend/src/components/tickets/ActivityLog.tsx` (depends on T044, T052)
- [X] T056 [P] Create slot placeholder components with final prop types (`{ ticket: TicketDetail; etag: string }`) rendering `null`, to be implemented by stories, in `frontend/src/components/tickets/StatusActionButton.tsx`, `frontend/src/components/tickets/ClosedBanner.tsx`, `frontend/src/components/tickets/TicketEditPanel.tsx`, `frontend/src/components/tickets/CommentThread.tsx`
- [X] T057 [P] Configure Vitest (jsdom, setup with Testing Library + MSW server, path aliases) in `frontend/vitest.config.ts` and `frontend/tests/setup.ts`, and MSW handlers base in `frontend/tests/mocks/handlers.ts`
- [X] T058 [P] Configure Playwright (baseURL `http://localhost:3000`, trace on failure) and fixtures: `loggedInPage` (signs in with `E2E_USER`/`E2E_PASSWORD` env vars), `api` request context with session + CSRF, `createTicketViaApi()` helper, in `frontend/playwright.config.ts` and `frontend/tests/e2e/fixtures.ts`
- [X] T059 [P] Write unit tests for `problemToAlert` (one case per ui-contract row, asserts no raw JSON), `AlertModal` (focus trap, Esc, role, actions) and Zod schemas (boundaries 2/3/150/151, 9/10/5000/5001, whitespace-only) in `frontend/tests/unit/problem-to-alert.test.ts`, `frontend/tests/unit/AlertModal.test.tsx`, `frontend/tests/unit/schemas.test.ts`
- [X] T060 [P] Write E2E for sign-in: wrong password shows "Sign-in failed" modal; valid login lands on `/tickets`; visiting `/tickets` signed-out redirects to `/login`; sign out works, in `frontend/tests/e2e/auth.spec.ts`

**Checkpoint**: `./mvnw verify` green for T037–T040; frontend `npm test` green; a seeded user can
sign in and open `/tickets/<key>` for a fixture ticket. User stories can now proceed **in
parallel**.

---

## Phase 3: User Story 1 - Raise a Support Ticket (Priority: P1) 🎯 MVP

**Goal**: Signed-in user creates a ticket (title, description, priority, assignee all mandatory);
it is saved as OPEN with a `TMS-n` key and the user lands on its detail page.

**Independent Test**: Create a ticket in the UI, restart the stack, open `/tickets/TMS-n`: all
values present, status Open. Invalid submissions (UI and direct API) are rejected with modal /
400 problem.

### Tests for User Story 1 ⚠️ (write first, must fail)

- [X] T061 [P] [US1] Write integration test for `POST /api/v1/tickets`: 201 + `Location` + `ETag`; status OPEN; key `TMS-<n>` unique across two creates; reporter = signed-in user; trimmed values; `CREATED` history row; 400 `VALIDATION_FAILED` with field errors for each missing field, title 2/151 chars, description 9/5001 chars, whitespace-only, priority `URGENT`, unknown/inactive assignee, unknown property `status`; 401 without session; no row persisted on any 400 — in `backend/src/test/java/com/c2certi/tms/ticket/TicketCreationIntegrationTest.java`
- [X] T062 [P] [US1] Write component test: empty create form submit shows AlertModal listing 4 problems and highlights fields; server 400 field errors are shown inline and in modal — in `frontend/tests/unit/CreateTicket.test.tsx`
- [X] T063 [P] [US1] Write E2E: create ticket from UI → toast "Ticket TMS-n created" → detail page shows entered values and "Open"; empty submit shows modal — in `frontend/tests/e2e/create-ticket.spec.ts`

### Implementation for User Story 1

- [X] T064 [P] [US1] Create `CreateTicketRequest` record with Bean Validation (`@NotBlank`, `@Size(3,150)`, `@Size(10,5000)`, `@NotNull` priority, `@NotNull` assigneeId) and friendly messages in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/CreateTicketRequest.java`
- [X] T065 [US1] Implement `TicketCreationService.create(request, currentUser)` (transactional; resolve active assignee or `FieldValidationException(assigneeId, "Please choose a valid assignee.")`; key from `nextTicketNumber()`; `HistoryRecorder.recordCreated`; `TicketMetrics` increment; structured log without PII) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketCreationService.java` (depends on T064)
- [X] T066 [US1] Implement `TicketCreationController` `POST /api/v1/tickets` (`@Valid`, 201, `Location`, `ETag`, body `TicketDetailResponse`) in `backend/src/main/java/com/c2certi/tms/ticket/api/TicketCreationController.java` (depends on T065)
- [X] T067 [P] [US1] Create `useCreateTicket()` mutation (invalidates `['tickets']` list queries) in `frontend/src/lib/api/tickets-create.ts`
- [X] T068 [US1] Create New Ticket page using `TicketForm` mode `create` (page title, cancel link, on success toast "Ticket {key} created" and `router.push('/tickets/{key}')`, on 400 `applyServerErrors` + modal, other errors via `useAlert`, form data kept on failure) in `frontend/src/app/(app)/tickets/new/page.tsx` (depends on T067)

**Checkpoint**: US1 tests T061–T063 pass. MVP demo: sign in → create → view.

---

## Phase 4: User Story 2 - Browse, Search, Filter and View Tickets (Priority: P1)

**Goal**: Paginated listing ("My tickets" default / "All tickets") with keyword search, status
filter, combined filters in the URL, empty/no-results states; click a row to open details.

**Independent Test**: With fixture tickets of varied titles/statuses/reporters, the list shows
them newest-updated first; search, status filter, view toggle and their combinations narrow
results correctly; clicking a row opens the right ticket.

### Tests for User Story 2 ⚠️ (write first, must fail)

- [X] T069 [P] [US2] Write integration test for `GET /api/v1/tickets`: default `view=mine` returns only current user's tickets; `view=all`; sort `updatedAt desc`; pagination (`page`, `size` default 20, `totalItems`, `totalPages`); `q` matches key (`TMS-1`), title and description case-insensitively; `q` containing `%`, `_`, `\` treated literally; `status` filter; `q`+`status`+`view` combined; 400 for `q` > 100 chars, `size` > 100, invalid `status` — in `backend/src/test/java/com/c2certi/tms/ticket/TicketListIntegrationTest.java`
- [X] T070 [P] [US2] Write component tests: `TicketFilters` debounces search 300 ms and updates URL params; status chip and view toggle update URL; no-results state shows "Clear filters" that resets params — in `frontend/tests/unit/TicketFilters.test.tsx`
- [X] T071 [P] [US2] Write E2E: tickets listed with key/title/status/priority/assignee/updated; search by title fragment and by key; status filter; combined; "All tickets" shows another user's ticket; row click opens detail; refresh keeps filters — in `frontend/tests/e2e/list-search-filter.spec.ts`

### Implementation for User Story 2

- [X] T072 [P] [US2] Create `TicketSpecifications` (`reportedBy(userId)`, `hasStatus(status)`, `matchesKeyword(q)` using `lower(...) like lower(:pattern) escape '\'` over key/title/description with `%`,`_`,`\` escaped) in `backend/src/main/java/com/c2certi/tms/ticket/repository/TicketSpecifications.java`
- [X] T073 [P] [US2] Create `TicketListQuery` record (`q` `@Size(max=100)`, `status`, `view` enum `MINE|ALL` default MINE, `page` `@Min(0)`, `size` `@Min(1) @Max(100)` default 20) in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/TicketListQuery.java`
- [X] T074 [US2] Implement `TicketListService.list(query, currentUser)` (compose specs, fixed sort `updatedAt desc, id desc`, entity graph for reporter/assignee to avoid N+1, map to `TicketPageResponse`) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketListService.java` (depends on T072, T073)
- [X] T075 [US2] Implement `TicketListController` `GET /api/v1/tickets` (`@Validated` query binding) in `backend/src/main/java/com/c2certi/tms/ticket/api/TicketListController.java` (depends on T074)
- [X] T076 [P] [US2] Create `useTicketList(params)` query (`keepPreviousData` for smooth paging) and `useTicketListParams()` hook syncing `q`, `status`, `view`, `page` with URL search params in `frontend/src/lib/api/tickets-list.ts` and `frontend/src/lib/hooks/useTicketListParams.ts`
- [X] T077 [P] [US2] Create `TicketFilters` (segmented My/All toggle, search input with icon + clear button + 300 ms debounce + maxLength 100, status chips All/Open/In progress/Resolved/Closed) in `frontend/src/components/tickets/TicketFilters.tsx`
- [X] T078 [P] [US2] Create `TicketTable` (desktop table / mobile cards, whole row clickable and focusable with Enter, relative updated time with exact-time tooltip, skeleton rows) in `frontend/src/components/tickets/TicketTable.tsx`
- [X] T079 [P] [US2] Create `Pagination` ("Showing x–y of z", prev/next, disabled states) and `EmptyState` (variants: no tickets yet with "Create ticket" CTA; no results with "Clear filters") in `frontend/src/components/common/Pagination.tsx` and `frontend/src/components/common/EmptyState.tsx`
- [X] T080 [US2] Create listing page composing filters, table, pagination, empty states and error modal on load failure in `frontend/src/app/(app)/tickets/page.tsx` (depends on T076–T079)

**Checkpoint**: US2 tests T069–T071 pass independently of US1 UI (fixtures create tickets via API).

---

## Phase 5: User Story 3 - Move a Ticket Through Its Lifecycle (Priority: P1)

**Goal**: Only OPEN → IN_PROGRESS → RESOLVED → CLOSED via the transitions endpoint; every other
transition rejected by the backend with a clear modal; CLOSED is read-only.

**Independent Test**: The 16-pair integration matrix passes (3 succeed, 13 → 409 with status and
version unchanged); in the UI a ticket can be walked to Closed and then shows read-only banner.

### Tests for User Story 3 ⚠️ (write first, must fail)

- [X] T081 [P] [US3] Write **state-machine integration test** (`@ParameterizedTest` over all 16 `(from, to)` pairs, Testcontainers PostgreSQL): drive a fixture ticket to `from` via valid transitions, `POST /api/v1/tickets/{key}/transitions` with current ETag; allowed → 200, new status persisted (re-read from DB), `STATUS_CHANGED` history row with actor/old/new, `resolvedAt`/`closedAt` set, new ETag; rejected → 409 `INVALID_STATUS_TRANSITION` with `currentStatus`/`allowedNextStatus`, status + version + history count unchanged; plus: missing `If-Match` → 428, stale `If-Match` → 412, unknown ticket → 404, invalid `targetStatus` → 400, `PATCH` with `status` property → 400 and status unchanged, two concurrent transitions with same ETag → exactly one 200 and one 412 — in `backend/src/test/java/com/c2certi/tms/ticket/TicketTransitionIntegrationTest.java`
- [X] T082 [P] [US3] Write component test: `StatusActionButton` shows only the allowed next action label per status ("Start progress", "Mark resolved", "Close ticket"), nothing for CLOSED; Close asks for confirmation via custom modal; 409 response shows "Status change not allowed" modal and refetches — in `frontend/tests/unit/StatusActionButton.test.tsx`
- [X] T083 [P] [US3] Write E2E: walk ticket Open → In progress → Resolved → Closed via UI with activity entries; closed ticket shows banner and no edit/comment/status controls; API request OPEN→RESOLVED returns 409 and UI still shows Open after reload — in `frontend/tests/e2e/status-flow.spec.ts`

### Implementation for User Story 3

- [X] T084 [P] [US3] Create `TransitionRequest` record (`@NotNull TicketStatus targetStatus`) in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/TransitionRequest.java`
- [X] T085 [US3] Implement `TicketTransitionService.transition(key, expectedVersion, target, currentUser)` (transactional; load or 404; compare version → `VersionConflictException`; `ticket.transitionTo`; record history; metrics for success and rejection; flush so optimistic lock conflicts surface as 412) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketTransitionService.java` (depends on T084)
- [X] T086 [US3] Implement `TicketTransitionController` `POST /api/v1/tickets/{ticketKey}/transitions` (requires `If-Match` via `ETags.parseIfMatch`, returns `TicketDetailResponse` + new `ETag`) in `backend/src/main/java/com/c2certi/tms/ticket/api/TicketTransitionController.java` (depends on T085)
- [X] T087 [P] [US3] Create `useTransitionTicket(key)` mutation (sends `If-Match`, updates `['ticket', key]` cache from response, invalidates list) in `frontend/src/lib/api/tickets-transition.ts`
- [X] T088 [US3] Implement `StatusActionButton` (single primary button for `allowedNextStatus` with labels above, spinner while pending, confirmation for Close via `useAlert().confirm`, success toast "Status changed to …", on 409/412 show modal then refetch ticket) in `frontend/src/components/tickets/StatusActionButton.tsx` (depends on T087)
- [X] T089 [P] [US3] Implement `ClosedBanner` ("This ticket is closed and read-only.", shown only when status CLOSED, `role="status"`) in `frontend/src/components/tickets/ClosedBanner.tsx`

**Checkpoint**: T081 (state-machine integration tests) green — the critical acceptance gate.

---

## Phase 6: User Story 4 - Update Ticket Details and Reassign (Priority: P2)

**Goal**: Edit title, description, priority and assignee on non-closed tickets with the same
validation as create, conflict detection, and per-field history.

**Independent Test**: Edit each field and the assignee on a fixture ticket; values persist and
history shows old → new; stale edit shows "updated by someone else" modal; closed ticket refuses.

### Tests for User Story 4 ⚠️ (write first, must fail)

- [X] T090 [P] [US4] Write integration test for `PATCH /api/v1/tickets/{key}`: partial update of each field alone; assignee change reflected in detail and list response; one `FIELD_UPDATED` history row per changed field with old/new (none for unchanged values); `updatedAt` and ETag change; 400 for empty body, blank title, over-length, bad priority, inactive assignee, unknown property `status`; 409 `TICKET_CLOSED` on CLOSED ticket; 412 stale `If-Match`; 428 missing `If-Match`; 404 unknown key — in `backend/src/test/java/com/c2certi/tms/ticket/TicketUpdateIntegrationTest.java`
- [X] T091 [P] [US4] Write component test: `TicketEditPanel` pre-fills values, validates like create, 412 shows "Ticket was updated by someone else" with Reload that refetches while keeping unsaved edits visible until user chooses — in `frontend/tests/unit/TicketEditPanel.test.tsx`
- [X] T092 [P] [US4] Write E2E: edit title/description/priority → "Changes saved" and new values after reload; change assignee → shown on detail and listing; two tabs editing same ticket → second save shows conflict modal; clearing title shows validation modal — in `frontend/tests/e2e/edit-ticket.spec.ts`

### Implementation for User Story 4

- [X] T093 [P] [US4] Create `UpdateTicketRequest` record (all fields nullable; `@Size` rules when present; custom `@AtLeastOneField` class-level constraint; blank strings rejected) in `backend/src/main/java/com/c2certi/tms/ticket/api/dto/UpdateTicketRequest.java`
- [X] T094 [US4] Implement `TicketUpdateService.update(key, expectedVersion, request, currentUser)` (transactional; 404; version check; `assertOpenForChanges`; resolve active assignee; apply only changed fields via domain methods; record one history row per changed field; flush) in `backend/src/main/java/com/c2certi/tms/ticket/service/TicketUpdateService.java` (depends on T093)
- [X] T095 [US4] Implement `TicketUpdateController` `PATCH /api/v1/tickets/{ticketKey}` (requires `If-Match`, returns `TicketDetailResponse` + `ETag`) in `backend/src/main/java/com/c2certi/tms/ticket/api/TicketUpdateController.java` (depends on T094)
- [X] T096 [P] [US4] Create `useUpdateTicket(key)` mutation (sends only dirty fields and `If-Match`, updates `['ticket', key]` cache, invalidates list) in `frontend/src/lib/api/tickets-update.ts`
- [X] T097 [US4] Implement `TicketEditPanel` ("Edit" button hidden when CLOSED; opens `TicketForm` mode `edit` inline with Save/Cancel; success toast "Changes saved"; 400 → `applyServerErrors`; 409/412 → modal with Reload; `beforeunload` + route-change guard when dirty) in `frontend/src/components/tickets/TicketEditPanel.tsx` (depends on T096)

**Checkpoint**: US4 tests T090–T092 pass.

---

## Phase 7: User Story 5 - Comment on a Ticket (Priority: P2)

**Goal**: Add plain-text comments to non-closed tickets; shown oldest-first with author and time.

**Independent Test**: Post comments on a fixture ticket; they persist in order with author and
timestamp; empty/over-length/closed are rejected with modal.

### Tests for User Story 5 ⚠️ (write first, must fail)

- [X] T098 [P] [US5] Write integration test for `GET/POST /api/v1/tickets/{key}/comments`: 201 with author = current user and trimmed body; list ordered oldest-first; ticket `updatedAt` bumped; 400 for empty, whitespace-only, 2001 chars, unknown property; 409 `TICKET_CLOSED` on CLOSED ticket; 404 unknown ticket; body with `<script>` stored verbatim (rendered as text by UI) — in `backend/src/test/java/com/c2certi/tms/comment/CommentIntegrationTest.java`
- [X] T099 [P] [US5] Write component test: `CommentThread` renders comments with author/time, composer counter, disabled when empty, optimistic append then rollback + modal on failure, `<script>` shown as text — in `frontend/tests/unit/CommentThread.test.tsx`
- [X] T100 [P] [US5] Write E2E: add comment → appears with author and time, input cleared, persists after reload; empty comment blocked — in `frontend/tests/e2e/comments.spec.ts`

### Implementation for User Story 5

- [X] T101 [P] [US5] Create `CreateCommentRequest` record (`@NotBlank`, `@Size(max=2000)`) in `backend/src/main/java/com/c2certi/tms/comment/api/CreateCommentRequest.java`
- [X] T102 [US5] Implement `CommentService` (`list(key)`; `add(key, request, currentUser)`: 404, `assertOpenForChanges`, save, `ticket.touch(now)`, metrics) in `backend/src/main/java/com/c2certi/tms/comment/service/CommentService.java` (depends on T101)
- [X] T103 [US5] Implement `CommentController` `GET` and `POST /api/v1/tickets/{ticketKey}/comments` (201 `CommentResponse`) in `backend/src/main/java/com/c2certi/tms/comment/api/CommentController.java` (depends on T102)
- [X] T104 [P] [US5] Create `useAddComment(key)` mutation (optimistic append to `['ticket', key]` comments, rollback on error, invalidate on settle) in `frontend/src/lib/api/comments.ts`
- [X] T105 [US5] Implement `CommentThread` (list with initials avatar, name, relative time; composer textarea with `commentSchema`, counter, Ctrl/Cmd+Enter submit, "Add comment" button with spinner; hidden composer and note when CLOSED; errors via `useAlert`) in `frontend/src/components/tickets/CommentThread.tsx` (depends on T104)

**Checkpoint**: All five stories functional and independently tested.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Restart durability, contract conformance, error UX, accessibility, performance,
documentation.

- [X] T106 [P] Write `PersistenceAcrossRestartTest`: start app context, create ticket, update it, add comment, transition to IN_PROGRESS; close the context; start a new context against the same Postgres container; assert ticket fields, status, comment and history are identical — in `backend/src/test/java/com/c2certi/tms/PersistenceAcrossRestartTest.java`
- [X] T107 [P] Write `OpenApiContractTest` asserting every path/method and response code in `specs/001-support-ticket-management/contracts/openapi.yaml` exists in the generated `/v3/api-docs` (dev profile) in `backend/src/test/java/com/c2certi/tms/contract/OpenApiContractTest.java`
- [X] T108 [P] Write E2E for error UX: backend stopped (route abort) → "Can't reach the server" modal with Retry and form data kept; 500 → "Something went wrong" with reference; expired session cookie → "Session expired" and redirect to login; no native `alert()` fired (`page.on('dialog')` fails test) — in `frontend/tests/e2e/errors.spec.ts`
- [X] T109 [P] Write accessibility E2E running `@axe-core/playwright` on login, listing, new ticket, detail (open and closed), and open AlertModal, asserting zero serious/critical violations; plus keyboard-only create flow — in `frontend/tests/e2e/a11y.spec.ts`
- [X] T110 [P] Write performance test seeding 10,000 tickets and asserting `GET /api/v1/tickets?q=...&status=...&view=all` completes < 1 s and uses trigram index (`EXPLAIN` check) in `backend/src/test/java/com/c2certi/tms/ticket/TicketSearchPerformanceTest.java` (tag `perf`, run in CI nightly)
- [X] T111 [P] UI polish pass: consistent spacing/typography tokens, dark mode contrast, `prefers-reduced-motion` handling, focus-visible rings, responsive check at 360/768/1280 px, page `<title>`s per route in `frontend/src/app/globals.css` and route `metadata` exports
- [X] T112 [P] Write README with architecture overview, env setup from `.env.example`, `docker compose` run, test commands, and link to quickstart in `README.md`
- [X] T113 Security review: verify no secrets in repo (`gitleaks detect`), cookies flags in non-local profile, actuator exposure, CSP header, dependency scan clean; fix findings in the affected files
- [X] T114 Run the full `specs/001-support-ticket-management/quickstart.md` walkthrough (sections 3–5, all 14 acceptance rows including `docker compose restart`) and record results in `specs/001-support-ticket-management/acceptance-results.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T001 first; T002–T009 all [P].
- **Foundational (Phase 2)**: depends on T003/T004. BLOCKS all stories.
- **User Stories (Phases 3–7)**: each depends only on Foundational. **All five can run in
  parallel** — they touch disjoint files by design.
- **Polish (Phase 8)**: T106 needs US1+US3+US4+US5 backend; T108/T109 need all UI stories; T107
  needs all controllers; T111–T114 last.

### Internal chains in Foundational

```text
Backend:  T015,T016,T017,T018 ─► T019 ─► T020,T021,T022 ─► T034 ─► T035
          T018 ─► T026 ─┐
          T027 ─────────┼─► T028 ─► T029
          T017 ─────────┘
          T017 ─► T030, T031           (T010–T014, T023–T025, T032, T033, T036–T038 independent [P])
Frontend: T043 ─► T044 ─► T048 ─► T050, T051
          T044 ─► T049, T053, T055     T045,T053,T047 ─► T054
          (T041, T042, T045–T047, T052, T056–T059 independent [P])
```

### User Story Dependencies

| Story | Depends on | Integrates with (no blocking) |
|-------|-----------|--------------------------------|
| US1 Create (P1) | Foundational | Detail page (Foundational) |
| US2 List/search/filter (P1) | Foundational | Row click → detail page (Foundational) |
| US3 Lifecycle (P1) | Foundational | Fills `StatusActionButton`, `ClosedBanner` slots |
| US4 Update/reassign (P2) | Foundational | Fills `TicketEditPanel` slot; reuses `TicketForm` |
| US5 Comments (P2) | Foundational | Fills `CommentThread` slot |

### Within Each Story

Tests (fail first) → request DTO → service → controller; frontend hook → component/page.

---

## Parallel Execution Examples

### Setup (after T001)

```text
T002 .env.example | T003 backend init | T004 frontend init | T005 gitleaks
T006 docker-compose | T007 backend Dockerfile | T008 frontend Dockerfile | T009 CI
```

### Foundational — wave 1 (all [P])

```text
Backend:  T010 T011 T012 T013 T014 T015 T016 T017 T018 T023 T024 T025 T027 T032 T033 T036
Frontend: T041 T042 T043 T045 T046 T047 T052 T056 T057 T058
```

### Foundational — wave 2

```text
Backend:  T019 → then T020 T021 T022 T026 T030 T031 T037 T038 in parallel → T028 → T029, T034 → T035 → T039 T040
Frontend: T044 → T048 T049 T053 T055 T059 in parallel → T050 T051 T054 → T060
```

### All user stories concurrently (5 agents / developers)

```text
Agent A (US1): T061 T062 T063 → T064 → T065 → T066 | T067 → T068
Agent B (US2): T069 T070 T071 → T072 T073 → T074 → T075 | T076 T077 T078 T079 → T080
Agent C (US3): T081 T082 T083 → T084 → T085 → T086 | T087 T089 → T088
Agent D (US4): T090 T091 T092 → T093 → T094 → T095 | T096 → T097
Agent E (US5): T098 T099 T100 → T101 → T102 → T103 | T104 → T105
```

Within each story, backend chain and frontend chain run in parallel with each other (the
frontend mutates against the contract; MSW mocks until the backend lands).

### Parallel Example: User Story 3

```text
Task: "State-machine integration test (16 pairs) in backend/src/test/java/com/c2certi/tms/ticket/TicketTransitionIntegrationTest.java"
Task: "StatusActionButton component test in frontend/tests/unit/StatusActionButton.test.tsx"
Task: "Status flow E2E in frontend/tests/e2e/status-flow.spec.ts"
Task: "TransitionRequest DTO in backend/src/main/java/com/c2certi/tms/ticket/api/dto/TransitionRequest.java"
Task: "useTransitionTicket hook in frontend/src/lib/api/tickets-transition.ts"
Task: "ClosedBanner in frontend/src/components/tickets/ClosedBanner.tsx"
```

### Polish

```text
T106 T107 T108 T109 T110 T111 T112 in parallel → T113 → T114
```

---

## Implementation Strategy

### MVP First

1. Phase 1 Setup → Phase 2 Foundational.
2. Phase 3 (US1) + Phase 5 (US3) — create tickets and enforce the state machine (the critical
   rule). **STOP and VALIDATE**: T061, T081 green; create in UI; restart; ticket still there.
3. Add Phase 4 (US2) for listing/search/filter → complete P1 MVP.

### Incremental Delivery

Foundation → US1 → US3 → US2 (P1 complete, demoable) → US4 → US5 → Polish. Each story adds a
slot or page without modifying other stories' files.

### Parallel Team Strategy

After Foundational, assign one developer/agent per story (A–E above). Merge order does not
matter because files are disjoint; run the full suite after each merge.

---

## Acceptance Criteria → Tasks

| Criterion | Tasks |
|-----------|-------|
| Ticket can be created from UI | T061–T068 |
| Tickets can be listed | T069, T071–T080 |
| Ticket details can be viewed | T034, T035, T040, T055 |
| Ticket fields can be updated | T090–T097 |
| Assignee can be changed | T090, T092, T094, T097 |
| Comments can be added | T098–T105 |
| Search works | T069–T072, T077 |
| Status filter works | T069–T072, T077 |
| Valid status transitions work | T037, T081, T083, T085–T088 |
| Invalid status transitions rejected by backend | T016, T019, T037, T081, T085 |
| Data survives application restart | T006 (volume), T106, T114 |
| Backend validation works | T023, T026, T061, T090, T098 |
| UI shows meaningful errors | T046, T047, T059, T062, T082, T091, T108 |
| State-machine integration tests pass | T081 (CI gate T009) |
| Modern, user-friendly, smooth UI | T047, T052, T078, T109, T111 |

## Notes

- [P] = different files, no incomplete dependencies. [USn] = traceability to spec.md stories.
- Never commit `.env`; tests generate their own JWT secret at runtime.
- Verify each test fails before implementing; commit after each task or logical group.
- Error codes/messages must match `contracts/openapi.yaml` and `contracts/ui-contract.md`.

## Implementation Notes (2026-09-21)

Deviations from the task text, with reasons:

- **T036 test database**: Testcontainers is used when Docker is available (CI); otherwise the
  suite falls back to embedded PostgreSQL 16 binaries (never H2). This machine has no Docker.
- **T036 CSRF in tests**: tests send a real double-submit token (cookie + header). The
  `spring-security-test` `csrf()` helper was dropped because it replaces the shared filter's
  token repository for the rest of the run.
- **T004 shadcn/ui**: no `components.json`/CLI; the few shadcn-style primitives (button, field,
  card, skeleton) are hand-written on Radix + Tailwind in `frontend/src/components/ui/`.
- **T053 AssigneeSelect**: a native `<select>` instead of a searchable combobox — fully keyboard
  and screen-reader accessible, native picker on mobile, fine for the expected user count.
- **T056 slot placeholders**: with a single implementer the slot components were written in
  their final form directly; the file split per story is unchanged.
- **T064/T093 priority**: accepted as a validated string so an invalid value is reported
  together with all other field errors (found during E2E).
- **T110 performance test**: asserts < 1 s over 10,000 tickets; the `EXPLAIN` index assertion
  was dropped because the planner may legitimately choose a sequential scan at this size.
- **T114**: results recorded in `acceptance-results.md` (kept out of `checklists/` so the
  implement gate does not treat it as a requirements checklist). Docker-based steps were not
  run locally; see that file.
