<!--
Sync Impact Report
==================
Version change: (unversioned template) → 1.0.0
Bump rationale: Initial ratification; all placeholders replaced with concrete governance.

Modified principles (template placeholder → new title):
  - [PRINCIPLE_1_NAME] → I. Mandated Technology Stack (NON-NEGOTIABLE)
  - [PRINCIPLE_2_NAME] → II. API-First REST Contracts
  - [PRINCIPLE_3_NAME] → III. Secrets & Environment-Driven Configuration (NON-NEGOTIABLE)
  - [PRINCIPLE_4_NAME] → IV. Security, Identity & Least Privilege
  - [PRINCIPLE_5_NAME] → V. Ticket Data Integrity & Auditability

Added principles:
  - VI. Test-First Quality Gates
  - VII. Observability & Operability
  - VIII. Performance, Accessibility & Simplicity

Added sections:
  - Technology & Platform Constraints (was [SECTION_2_NAME])
  - Development Workflow & Quality Gates (was [SECTION_3_NAME])

Removed sections: none

Templates requiring updates:
  - .specify/templates/plan-template.md      ✅ no edit needed (reads constitution at runtime)
  - .specify/templates/spec-template.md      ✅ no edit needed (reads constitution at runtime)
  - .specify/templates/tasks-template.md     ✅ no edit needed (reads constitution at runtime)

Deferred TODOs: none
-->

# Ticketing Management System Constitution

## Core Principles

### I. Mandated Technology Stack (NON-NEGOTIABLE)

The system MUST be built on the following stack. Deviations require a constitution amendment.

- **Backend**: Java 21 (LTS) with Spring Boot (current 3.x line), exposing a REST API.
  Use Spring Web, Spring Data JPA, Spring Security, Bean Validation, and Spring Boot Actuator.
  Build with Maven or Gradle via the committed wrapper (`mvnw`/`gradlew`).
- **Database**: PostgreSQL is the single system of record. Schema changes MUST be managed by
  versioned migrations (Flyway or Liquibase); `ddl-auto` MUST be `validate` or `none` outside
  local throwaway runs.
- **Frontend**: Next.js (App Router) with TypeScript in `strict` mode.
- The frontend MUST communicate with the backend only through the documented REST API; it MUST
  NOT access the database directly.
- Modern Java idioms (records for DTOs, sealed types, pattern matching, virtual threads where
  beneficial) are preferred over legacy equivalents.

**Rationale**: A fixed, mainstream stack keeps hiring, tooling, and long-term maintenance
predictable and prevents fragmentation across features.

### II. API-First REST Contracts

- Every backend capability MUST be defined as an OpenAPI 3 contract before or alongside
  implementation; the published spec is the source of truth for the frontend.
- APIs MUST be resource-oriented (`/api/v1/tickets`, `/api/v1/tickets/{id}/comments`), use
  correct HTTP verbs and status codes, and be versioned via a URI prefix (`/api/v{n}`).
- Errors MUST use a consistent structure (RFC 9457 Problem Details) with a machine-readable
  code; stack traces and internal details MUST NOT leak to clients.
- List endpoints MUST support pagination, sorting, and filtering (status, priority, assignee,
  date range) with bounded page sizes.
- Updates that can race (ticket status, assignment) MUST use optimistic locking (`@Version` /
  `ETag` + `If-Match`) and return `409`/`412` on conflict.
- JPA entities MUST NOT be exposed directly; request/response DTOs are mandatory.
- Breaking changes MUST ship under a new API version; the previous version MUST remain
  available for a documented deprecation window.

**Rationale**: Stable, explicit contracts let the frontend and backend evolve independently and
make integrations (email, chat, webhooks) safe.

### III. Secrets & Environment-Driven Configuration (NON-NEGOTIABLE)

- No secrets MUST ever be committed: no passwords, API keys, tokens, private keys, JWT signing
  keys, or production connection strings in source, tests, fixtures, Dockerfiles, or history.
- All configuration that varies per environment MUST be supplied through environment variables
  (or a secret manager injected as environment variables), following twelve-factor practice.
  Spring properties MUST reference them (e.g. `${DB_PASSWORD}`); Next.js MUST use `.env*.local`
  files that are git-ignored.
- Only browser-safe values MAY use the `NEXT_PUBLIC_` prefix; secrets MUST remain server-side.
- A committed `.env.example` MUST document every required variable with placeholder values only.
- The application MUST fail fast at startup when a required variable is missing.
- Automated secret scanning (e.g. gitleaks) MUST run in pre-commit and CI; any detected secret
  blocks the merge and MUST be rotated, not merely deleted.

**Rationale**: Leaked credentials are the most common and most damaging breach vector; making
configuration environment-driven keeps the same artifact deployable everywhere.

### IV. Security, Identity & Least Privilege

- All API endpoints MUST require authentication except explicitly listed public ones (health,
  login). Authentication MUST use a standard mechanism (OAuth2/OIDC or signed JWT via Spring
  Security); passwords, if stored locally, MUST be hashed with BCrypt or Argon2.
- Authorization MUST be role-based at minimum (e.g. `REQUESTER`, `AGENT`, `ADMIN`) and enforced
  on the server for every request, including object-level checks (a requester sees only their
  own tickets unless granted otherwise).
- All input MUST be validated server-side; queries MUST be parameterized; rendered user content
  (ticket descriptions, comments) MUST be sanitized against XSS.
- Attachments MUST be size- and type-restricted and stored outside the web root.
- Transport MUST be HTTPS in every non-local environment; CORS MUST allow only known origins.
- Dependencies MUST be scanned for known vulnerabilities in CI; critical/high findings block
  release.
- Personal data MUST be minimized and MUST NOT appear in logs.

**Rationale**: Tickets routinely contain customer PII and internal system details; the OWASP
Top 10 is the baseline, not the goal.

### V. Ticket Data Integrity & Auditability

- Ticket lifecycle MUST follow an explicit state machine (e.g. `OPEN → IN_PROGRESS →
  ON_HOLD → RESOLVED → CLOSED`, with `REOPENED`); invalid transitions MUST be rejected by the
  domain layer, not only the UI.
- Every change to a ticket (status, priority, assignee, fields, comments) MUST produce an
  immutable audit/history record capturing who, what, old value, new value, and when (UTC).
- Tickets MUST have a human-readable unique key (e.g. `TMS-1042`) in addition to the internal ID.
- Business data MUST NOT be hard-deleted by default; soft delete or archival is required, with
  retention governed by policy.
- Timestamps MUST be stored as `timestamptz` in UTC and converted only at the presentation layer.
- Database constraints (foreign keys, NOT NULL, unique, check) MUST back domain invariants.
- SLA-relevant timestamps (created, first response, resolved) MUST be recorded to support
  SLA tracking and reporting.

**Rationale**: A ticketing tool is a system of record for support commitments; history and
correctness matter more than convenience.

### VI. Test-First Quality Gates

- New behavior MUST be accompanied by tests written before or with the implementation; a bug fix
  MUST include a regression test that fails without the fix.
- Backend: JUnit 5 unit tests for domain logic; integration tests with Testcontainers
  PostgreSQL (not H2) for repositories, migrations, and REST endpoints; contract tests verifying
  the OpenAPI spec.
- Frontend: component tests (e.g. Jest/Vitest + Testing Library) and end-to-end tests
  (Playwright) for critical flows: create ticket, assign, comment, change status, search.
- CI MUST run the full suite on every pull request; a failing test blocks merge.
- Coverage on domain/service layers SHOULD be at least 80%; coverage MUST NOT decrease on a PR
  without written justification.

**Rationale**: Workflow rules and permissions are easy to break silently; automated tests are
the only scalable guard.

### VII. Observability & Operability

- Logs MUST be structured (JSON) and include a correlation/request ID propagated from the
  frontend through the backend.
- Spring Boot Actuator MUST expose health (liveness/readiness) and metrics (Micrometer,
  Prometheus format); sensitive actuator endpoints MUST be secured.
- Key business metrics (tickets created, backlog by status, SLA breaches, response times) MUST
  be emitted.
- Both applications MUST be containerized, run as non-root, and be configurable solely by
  environment variables (Principle III).

**Rationale**: Support tooling must itself be supportable; operators need to diagnose issues
without code changes.

### VIII. Performance, Accessibility & Simplicity

- Read API endpoints SHOULD respond within 300 ms at p95 under expected load; queries MUST be
  indexed for common filters and N+1 queries MUST be avoided.
- The frontend MUST meet WCAG 2.1 AA, be responsive (desktop and mobile), and handle loading,
  empty, and error states explicitly.
- Start with a modular monolith (package-by-feature: tickets, users, comments, attachments,
  notifications); new services, caches, or queues MUST be justified by a measured need (YAGNI).

**Rationale**: Agents live in the tool all day; speed and accessibility directly affect
resolution times, while premature distribution adds cost without value.

## Technology & Platform Constraints

- **Languages/Runtimes**: Java 21 LTS; Node.js active LTS for Next.js; TypeScript strict.
- **Backend frameworks**: Spring Boot 3.x, Spring Web (REST), Spring Data JPA, Spring Security,
  Bean Validation, Actuator, springdoc-openapi.
- **Database**: PostgreSQL (supported major version); Flyway or Liquibase for migrations;
  migrations are forward-only and never edited once merged.
- **Frontend**: Next.js App Router, TypeScript, ESLint + Prettier; API client generated from or
  typed against the OpenAPI spec.
- **Configuration**: environment variables only; `.env.example` committed, real `.env*` files
  git-ignored.
- **Code style**: backend formatted and linted automatically (e.g. Spotless/Checkstyle);
  frontend via ESLint/Prettier; formatting is enforced in CI.
- **Packaging**: Docker images per application; a `docker-compose` file for local development
  with PostgreSQL, using non-secret local defaults supplied through `.env`.

## Development Workflow & Quality Gates

- All changes go through pull requests with at least one approving review; direct pushes to
  the main branch are prohibited.
- Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, ...).
- Every feature follows the Spec Kit flow: specify → clarify → plan → tasks → implement; the
  plan MUST include a Constitution Check against these principles.
- CI gates (all MUST pass before merge): build, lint/format, unit + integration + contract
  tests, secret scan, dependency vulnerability scan, and migration validation against a clean
  PostgreSQL.
- Reviewers MUST verify: no secrets or hard-coded environment values, API contract updated,
  migration included for schema changes, audit history for ticket mutations, and tests present.

## Governance

- This constitution supersedes all other development practices for this project. Where a
  guideline conflicts with it, the constitution wins.
- Amendments require a pull request that edits this file, states the rationale, updates the
  Sync Impact Report, and includes a migration plan for any affected existing code.
- Versioning follows semantic versioning:
  - **MAJOR**: removal or backward-incompatible redefinition of a principle or mandated
    technology.
  - **MINOR**: a new principle or section, or materially expanded guidance.
  - **PATCH**: clarifications, wording, and typo fixes.
- Every plan and pull request MUST include a compliance check; any justified deviation MUST be
  recorded in the plan's Complexity Tracking section with the simpler alternative rejected.
- Compliance is reviewed at each feature's planning gate and before each release.

**Version**: 1.0.0 | **Ratified**: 2026-09-21 | **Last Amended**: 2026-09-21
