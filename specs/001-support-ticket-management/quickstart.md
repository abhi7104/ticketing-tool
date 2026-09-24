# Quickstart & Validation Guide: Support Ticket Management

Proves every acceptance criterion end to end. Contracts: [openapi.yaml](./contracts/openapi.yaml),
[ui-contract.md](./contracts/ui-contract.md). Data rules: [data-model.md](./data-model.md).

## Prerequisites

- Docker + Docker Compose v2
- For local dev without containers: JDK 21, Node.js LTS (≥ 20), a PostgreSQL 16 instance
- `curl` and `jq` for API checks

## 1. Configure environment (no secrets committed)

```bash
cp .env.example .env
# Edit .env and set real values for at least:
#   POSTGRES_PASSWORD, DB_PASSWORD, JWT_SECRET (>= 32 random bytes, e.g. `openssl rand -base64 48`),
#   APP_SEED_USERS (e.g. "alice:Alice Moore:alice@example.test;bob:Bob Singh:bob@example.test"),
#   APP_SEED_USER_PASSWORD
```

`.env` is git-ignored. The backend refuses to start if a required variable is missing.

## 2. Run the stack

```bash
docker compose up -d --build
docker compose ps            # postgres, backend, frontend all healthy
curl -s localhost:8080/actuator/health | jq .status   # "UP"
```

- UI: http://localhost:3000
- API docs (dev profile): http://localhost:8080/swagger-ui.html

## 3. Automated tests

```bash
# Backend: unit + integration (Testcontainers PostgreSQL; Docker must be running)
cd backend && ./mvnw verify

# Only the state machine matrix (16 pairs: 3 allowed, 13 rejected)
./mvnw -Dtest='TicketStatusTransitionTest,TicketTransitionIntegrationTest' test

# Frontend: unit/component tests, then E2E against the running compose stack
cd ../frontend && npm ci && npm test
npx playwright install --with-deps && npm run test:e2e
```

Expected: all green. The E2E suite covers each checklist item in section 4.

## 4. Acceptance checklist (manual walkthrough)

Sign in at `/login` as a seeded user (e.g. `alice`).

| # | Criterion | Steps | Expected |
|---|-----------|-------|----------|
| 1 | Ticket can be created from UI | New ticket → fill title, description, priority, assignee → Create | Toast "Ticket TMS-1 created"; detail page shows status **Open** |
| 2 | Tickets can be listed | Go to Tickets | New ticket listed under "My tickets" with key, status, priority, assignee |
| 3 | Ticket details can be viewed | Click the row | Detail page shows all fields, comments, activity |
| 4 | Ticket fields can be updated | Edit → change title/description/priority → Save | Toast "Changes saved"; values updated; activity shows old → new |
| 5 | Assignee can be changed | Edit → choose another assignee → Save | New assignee on detail and listing |
| 6 | Comments can be added | Type comment → Add comment | Appears with author/time; input cleared |
| 7 | Search works | Type part of a title or `TMS-1` in search | Only matching tickets remain; URL has `?q=` |
| 8 | Status filter works | Choose "In progress" | Only In-progress tickets; combines with search |
| 9 | Valid status transitions work | Start progress → Mark resolved → Close ticket | Each succeeds; activity logs each step; Closed ticket is read-only |
| 10 | Invalid transitions rejected by backend | See API check A below | `409 INVALID_STATUS_TRANSITION`, status unchanged |
| 11 | Data survives restart | `docker compose restart backend frontend` (or `down` then `up`, **without** `-v`) and reload | All tickets, comments, history still present |
| 12 | Backend validation works | See API check B below | `400 VALIDATION_FAILED` with per-field errors |
| 13 | UI shows meaningful errors | Submit create form empty; try to edit same ticket in two tabs; stop backend and save | Custom modal pop-ups per ui-contract (no browser alerts, no raw errors) |
| 14 | State-machine integration tests pass | Section 3 command | Green |

## 5. API checks (bypassing the UI)

```bash
BASE=http://localhost:3000/api/v1       # through the Next.js proxy
JAR=$(mktemp)
# Obtain CSRF cookie, then sign in
curl -s -c $JAR -b $JAR $BASE/auth/me >/dev/null
XSRF=$(awk '/XSRF-TOKEN/{print $7}' $JAR)
curl -s -c $JAR -b $JAR -H "X-XSRF-TOKEN: $XSRF" -H 'Content-Type: application/json' \
  -d "{\"username\":\"alice\",\"password\":\"$APP_SEED_USER_PASSWORD\"}" $BASE/auth/login | jq
H=(-c $JAR -b $JAR -H "X-XSRF-TOKEN: $XSRF" -H 'Content-Type: application/json')
```

**A. Invalid transition (OPEN → RESOLVED)**

```bash
ETAG=$(curl -s -D - -o /dev/null "${H[@]}" $BASE/tickets/TMS-2 | awk -F': ' '/^ETag/{print $2}' | tr -d '\r')
curl -s "${H[@]}" -H "If-Match: $ETAG" -d '{"targetStatus":"RESOLVED"}' \
  $BASE/tickets/TMS-2/transitions | jq '{status,code,currentStatus,allowedNextStatus}'
# → 409, INVALID_STATUS_TRANSITION, OPEN, IN_PROGRESS ; GET shows status still OPEN
```

**B. Backend validation (UI bypassed)**

```bash
curl -s "${H[@]}" -d '{"title":"x","description":"short","priority":"URGENT"}' $BASE/tickets \
  | jq '{status,code,errors}'
# → 400, VALIDATION_FAILED, errors for title, description, priority, assigneeId
```

**C. Unauthenticated access**

```bash
curl -s -o /dev/null -w '%{http_code}\n' $BASE/tickets   # → 401
```

**D. Stale update**

```bash
curl -s "${H[@]}" -X PATCH -H 'If-Match: "0"' -d '{"priority":"HIGH"}' $BASE/tickets/TMS-2 | jq .code
# → "VERSION_CONFLICT" (412)
```

## 6. Teardown

```bash
docker compose down        # keeps data volume
docker compose down -v     # also deletes the database volume
```
