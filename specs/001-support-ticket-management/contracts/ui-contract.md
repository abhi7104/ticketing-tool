# UI Contract: Support Ticket Management

Defines the screens, routes and user-facing behavior the frontend guarantees. API calls refer
to [openapi.yaml](./openapi.yaml).

## Routes

| Route | Screen | Auth | API used |
|-------|--------|------|----------|
| `/login` | Sign-in form | public | `POST /auth/login` |
| `/` | Redirects to `/tickets` | required | – |
| `/tickets?q=&status=&view=&page=` | Ticket listing | required | `GET /tickets` |
| `/tickets/new` | Create ticket form | required | `GET /users`, `POST /tickets` |
| `/tickets/[key]` | Ticket detail (view, edit, status, comments, history) | required | `GET/PATCH /tickets/{key}`, `POST .../transitions`, `POST .../comments` |

Unauthenticated access to any required route redirects to `/login?next=<route>`.

## Layout

- App shell: top bar with product name, "New ticket" primary button, signed-in user menu
  (display name, Sign out), light/dark toggle. Responsive down to 360 px width.
- Status and priority rendered as colored badges with text (never color alone).

## Listing (`/tickets`)

- Segmented control: **My tickets** (default) | **All tickets**.
- Search box (placeholder "Search by key, title or description"), debounced 300 ms, max 100 chars,
  clear (×) button.
- Status filter chips/select: All, Open, In progress, Resolved, Closed.
- Filters are reflected in the URL and persist on refresh/back navigation.
- Table (desktop) / cards (mobile): Key, Title, Status, Priority, Assignee, Updated (relative
  time with exact time on hover). Entire row is clickable and keyboard-focusable.
- Pagination controls with "Showing x–y of z".
- States: skeleton rows while loading; empty state ("No tickets yet – create your first ticket");
  no-results state ("No tickets match" + "Clear filters" button).

## Create (`/tickets/new`) and Edit (on detail page)

- Fields: Title (text), Description (textarea with character counter), Priority (select),
  Assignee (native select of active users — keyboard/screen-reader friendly, native picker on
  mobile). All marked required.
- Validate on blur and on submit using the limits in [data-model.md](../data-model.md). Space for
  each field's error line is always reserved so an appearing message never shifts buttons.
- On submit with errors: fields highlighted with inline messages **and** an `AlertModal` listing
  all problems; focus moves to the first invalid field after dismissal.
- Submit button shows a spinner and is disabled while pending (no duplicate submissions).
- Success: toast "Ticket TMS-n created" / "Changes saved"; create navigates to the detail page.
- Unsaved-changes guard (browser `beforeunload`) while an edited form is dirty.

## Detail (`/tickets/[key]`)

- Header: key, title, status badge, priority badge, reporter, assignee, created/updated times.
- **Status action**: a single primary button for `allowedNextStatus` (e.g. "Start progress",
  "Mark resolved", "Close ticket"), with a confirmation for Close (final). Hidden for CLOSED
  tickets, which show a "Closed – read only" banner.
- **Edit** button switches header fields into an inline form (not available when CLOSED).
- **Comments**: chronological thread with author avatar/initials, name, time; composer textarea
  with counter and "Add comment" button (disabled when empty or CLOSED).
- **Activity/history** tab: chronological list "Alice changed priority from Medium to High".
- Not found: friendly page with "Back to tickets".

## AlertModal (error pop-up) — single component for all errors

| Trigger | Title | Body | Actions |
|---------|-------|------|---------|
| FE validation failure | "Please check your input" | bullet list of field messages | OK |
| `400 VALIDATION_FAILED` | problem.title | problem.errors as list (fields also highlighted) | OK |
| `401 INVALID_CREDENTIALS` | "Sign-in failed" | "Invalid username or password." | OK |
| `401 UNAUTHENTICATED` | "Session expired" | "Please sign in again." | Sign in |
| `403 FORBIDDEN` (CSRF token missing/expired) | "Request blocked" | "Your security token expired. Reload the page and try again." | Reload / Cancel |
| `404 TICKET_NOT_FOUND` | "Ticket not found" | problem.detail | Back to tickets |
| `409 INVALID_STATUS_TRANSITION` | "Status change not allowed" | problem.detail (names current and allowed status) | OK (ticket refetched) |
| `409 TICKET_CLOSED` | "Ticket is closed" | "Closed tickets can't be changed." | OK (ticket refetched) |
| `412 VERSION_CONFLICT` | "Ticket was updated by someone else" | "Reload to see the latest version. Your edits are kept in the form." | Reload / Cancel |
| Network error / timeout | "Can't reach the server" | "Check your connection and try again. Your input has been kept." | Retry / Close |
| `500` / unknown | "Something went wrong" | "Please try again. Reference: {traceId}" | OK |

- Built on an accessible dialog: focus trapped, `Esc` closes, focus returns to trigger,
  `role="alertdialog"` with labelled title/description.
- Never displays stack traces, SQL, or raw JSON.
- Browser-native `alert()`/`confirm()` are never used.

## Motion & polish

- Transitions ≤ 200 ms (modal fade/scale, toast slide); respects `prefers-reduced-motion`.
- Optimistic UI only for comment posting (rolled back with an AlertModal on failure);
  other mutations update from the server response.
