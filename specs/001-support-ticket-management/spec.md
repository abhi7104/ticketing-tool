# Feature Specification: Support Ticket Management

**Feature Branch**: `001-support-ticket-management`

**Created**: 2026-09-21

**Status**: Draft

**Input**: User description: "Build a Support Ticket Management System where a user can create a
ticket with mandatory title, description, priority and assignee; list and view their raised
tickets and open any ticket's details from the listing; update title, description, priority and
assignee; add comments; search tickets by keyword and filter by status on the listing screen.
Data must persist end to end on create and update. Both front end and back end validate input;
errors caught on either side are shown as user-friendly custom pop-up modal alerts. The backend
must enforce the state machine OPEN → IN_PROGRESS → RESOLVED → CLOSED and reject all other
transitions. Post-implementation checklist: ticket created from UI, tickets listed, details
viewed, fields updated, assignee changed, comments added, search works, status filter works,
valid transitions work, invalid transitions rejected by backend, data survives restart, backend
validation works, UI shows meaningful errors, state-machine integration tests pass. UI should be
modern, user friendly and smooth."

## Clarifications

### Session 2026-09-21

- Q: How are users identified? → A: Username/password sign-in for pre-created users; "My
  tickets", reporter, comment author and history actor are tied to the signed-in user.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Raise a Support Ticket (Priority: P1)

A user opens the "New Ticket" form, enters a title, description, priority and assignee (all
mandatory), and submits. The ticket is saved with status OPEN, receives a readable ticket
reference (e.g. `TMS-1`), and the user is taken to the new ticket's detail view with a success
confirmation.

**Why this priority**: Creating tickets is the entry point of the whole system; without it no
other capability has data to work on.

**Independent Test**: Create a ticket from the UI, restart the application, and confirm the
ticket still exists with all entered values and status OPEN.

**Acceptance Scenarios**:

1. **Given** the New Ticket form, **When** the user fills all four mandatory fields with valid
   values and submits, **Then** the ticket is saved with status OPEN, a unique reference, creation
   time, and the user sees a success confirmation and the ticket's detail view.
2. **Given** the New Ticket form, **When** the user submits with any mandatory field empty or
   invalid, **Then** the form is not submitted and a pop-up modal lists each problem in plain
   language, and the offending fields are highlighted.
3. **Given** a request that bypasses the form's checks reaches the system with invalid data,
   **When** the system validates it, **Then** it is rejected, nothing is saved, and the UI shows
   the system's field-level reasons in the same pop-up modal style.
4. **Given** a ticket was created, **When** the application is restarted, **Then** the ticket and
   all its values are still present.

---

### User Story 2 - Browse, Search, Filter and View Tickets (Priority: P1)

A user sees a listing of their raised tickets showing reference, title, status, priority,
assignee and last-updated time. They can type a keyword to search, pick a status to filter, and
click any row to open the ticket's full details, including its comments and status history.

**Why this priority**: Users must be able to find and inspect tickets to act on them; together
with Story 1 this forms the minimum usable product.

**Independent Test**: Seed several tickets with different titles and statuses; verify the list
shows them, keyword search narrows results, the status filter narrows results, both combine,
and selecting a row opens the correct details.

**Acceptance Scenarios**:

1. **Given** existing tickets, **When** the user opens the listing, **Then** tickets are shown
   newest-updated first with reference, title, status, priority, assignee and last-updated time.
2. **Given** the listing, **When** the user enters a keyword, **Then** only tickets whose
   reference, title or description contain the keyword (case-insensitive) are shown.
3. **Given** the listing, **When** the user selects a status filter (or "All"), **Then** only
   tickets in that status are shown.
4. **Given** a keyword and a status filter are both applied, **Then** results satisfy both.
5. **Given** no ticket matches, **Then** a friendly empty state explains that nothing was found
   and offers to clear the search/filter.
6. **Given** the listing, **When** the user selects a ticket, **Then** the detail view opens
   showing all fields, current status, comments in chronological order, and created/updated
   times.
7. **Given** the user navigates to a ticket that does not exist, **Then** a friendly "ticket not
   found" message is shown with a way back to the listing.
8. **Given** a signed-in user, **When** they open the listing, **Then** "My tickets" is shown by
   default and they can switch to "All tickets".
9. **Given** a user who is not signed in, **When** they open any ticket screen, **Then** they are
   sent to the sign-in screen; after a valid sign-in they reach the listing.

---

### User Story 3 - Move a Ticket Through Its Lifecycle (Priority: P1)

From the ticket detail view, the user advances the ticket's status. Only the next status in the
fixed flow OPEN → IN_PROGRESS → RESOLVED → CLOSED is allowed. Any other change (skipping,
moving backwards, re-opening, or changing a CLOSED ticket) is refused by the system itself, not
just hidden in the UI.

**Why this priority**: The user identified strict lifecycle enforcement as critical; it
guarantees consistent support reporting.

**Independent Test**: Walk one ticket through every valid step and confirm each succeeds and
persists; attempt every invalid transition directly against the system and confirm each is
rejected with a clear reason and no data change.

**Acceptance Scenarios**:

1. **Given** an OPEN ticket, **When** the user moves it to IN_PROGRESS, **Then** the status is
   saved and shown, and a history entry records the change, who made it and when.
2. **Given** an IN_PROGRESS ticket, **When** moved to RESOLVED, **Then** it succeeds; **given** a
   RESOLVED ticket, **when** moved to CLOSED, **then** it succeeds.
3. **Given** any ticket, **When** a transition other than the single next step is requested (e.g.
   OPEN → RESOLVED, OPEN → CLOSED, IN_PROGRESS → OPEN, RESOLVED → IN_PROGRESS, CLOSED → any,
   or same status → same status), **Then** the system rejects it, the status is unchanged, and the
   UI shows a pop-up explaining the allowed next status.
4. **Given** the ticket detail view, **Then** the UI offers only the valid next status as an
   action (and none for CLOSED tickets).

---

### User Story 4 - Update Ticket Details and Reassign (Priority: P2)

On the detail view, the user edits the title, description, priority and/or assignee and saves.
Changes are validated with the same rules as creation, persisted, and recorded in history.

**Why this priority**: Tickets evolve as more is learned; reassignment is central to support
work, but the system is usable for intake and tracking without it.

**Independent Test**: Edit each field on an existing ticket, restart, and confirm the new values
persist and history shows old and new values.

**Acceptance Scenarios**:

1. **Given** a non-CLOSED ticket, **When** the user changes any editable field to a valid value
   and saves, **Then** the change is persisted, the last-updated time changes, and a success
   confirmation is shown.
2. **Given** the edit form, **When** the user clears a mandatory field or exceeds a length limit,
   **Then** saving is blocked and a pop-up modal explains the problem.
3. **Given** a ticket, **When** the user changes the assignee to another valid user, **Then** the
   new assignee appears on the detail view and listing, and the change is in history.
4. **Given** two users edit the same ticket, **When** the second saves after the first,
   **Then** the second is told the ticket changed in the meantime and asked to reload, and no
   update is silently lost.
5. **Given** a CLOSED ticket, **Then** its fields cannot be edited and any attempt is rejected by
   the system with a clear message.

---

### User Story 5 - Comment on a Ticket (Priority: P2)

On the detail view, the user writes a comment and posts it. The comment appears immediately at
the end of the thread with author and time and is persisted.

**Why this priority**: Comments are how progress is communicated; valuable but not required to
create and track tickets.

**Independent Test**: Add comments to a ticket, restart, and confirm they persist in order with
author and timestamp.

**Acceptance Scenarios**:

1. **Given** a non-CLOSED ticket, **When** the user posts a non-empty comment, **Then** it is
   saved and displayed with author and timestamp, and the input is cleared.
2. **Given** an empty or whitespace-only comment, or one over the length limit, **When** the user
   posts, **Then** it is rejected with a pop-up modal message.
3. **Given** a CLOSED ticket, **Then** new comments are not accepted and the reason is shown.

---

### Edge Cases

- Leading/trailing whitespace in title, description or comment is trimmed; whitespace-only input
  counts as empty.
- Priority or assignee values not in the allowed lists are rejected by the system.
- Search keywords with special characters are treated as literal text, never cause errors.
- Very long search input is limited to 100 characters.
- The system is unreachable or times out: a pop-up states the action could not be completed, the
  user's entered data is kept in the form, and they can retry.
- Double-clicking Submit/Save does not create duplicate tickets or comments.
- Two users change status at the same time: only one transition succeeds; the other is told the
  ticket changed and is shown its current status.
- Large lists are paginated so the listing remains fast.
- Text containing markup or scripts is shown as plain text, never executed.

## Requirements *(mandatory)*

### Functional Requirements

**Ticket creation**

- **FR-001**: Users MUST be able to create a ticket by providing title, description, priority
  and assignee; all four are mandatory.
- **FR-002**: Title MUST be 3–150 characters; description MUST be 10–5,000 characters (after
  trimming).
- **FR-003**: Priority MUST be one of LOW, MEDIUM, HIGH, CRITICAL.
- **FR-004**: Assignee MUST be selected from the list of existing users in the system.
- **FR-005**: New tickets MUST start in status OPEN, record the creating user as reporter, and
  receive a unique, human-readable reference (e.g. `TMS-42`) plus creation and last-updated
  timestamps.

**Listing, search, filter, details**

- **FR-006**: Users MUST be able to view a paginated list of tickets (default 20 per page) with
  reference, title, status, priority, assignee and last-updated time, sorted by most recently
  updated first.
- **FR-007**: Users MUST be able to search by keyword, matching case-insensitively against
  ticket reference, title and description.
- **FR-008**: Users MUST be able to filter the list by status (OPEN, IN_PROGRESS, RESOLVED,
  CLOSED, or All); filter and search MUST combine.
- **FR-009**: Users MUST be able to open any ticket from the list to see all its fields, status,
  reporter, assignee, timestamps, comments (oldest first) and change history.
- **FR-009a**: The listing MUST default to "My tickets" (tickets raised by the signed-in user),
  and users MUST be able to switch to "All tickets"; the view choice MUST combine with search and
  status filter.

**Sign-in**

- **FR-009b**: Users MUST sign in with a username and password before accessing any ticket
  screen or ticket data; unauthenticated access MUST be refused and redirected to the sign-in
  screen.
- **FR-009c**: User accounts are pre-created (seeded); self-registration is not offered. The
  signed-in user is recorded as reporter of tickets they create, author of their comments, and
  actor on history entries.
- **FR-009d**: Failed sign-in MUST show a generic "invalid username or password" message in the
  pop-up modal style without revealing which part was wrong; users MUST be able to sign out, and
  an expired session MUST return the user to sign-in with a friendly message.
- **FR-009e**: Passwords MUST never be displayed, logged or stored in readable form.

**Updates and assignment**

- **FR-010**: Users MUST be able to update title, description, priority and assignee of any
  non-CLOSED ticket, subject to the same rules as FR-002–FR-004.
- **FR-011**: The system MUST detect conflicting concurrent updates and reject the later one
  with a message asking the user to reload, rather than overwriting.

**Comments**

- **FR-012**: Users MUST be able to add comments (1–2,000 characters after trimming) to any
  non-CLOSED ticket; each comment records author and timestamp. Comments cannot be edited or
  deleted in this version.

**Status lifecycle**

- **FR-013**: The system MUST enforce exactly these transitions: OPEN → IN_PROGRESS,
  IN_PROGRESS → RESOLVED, RESOLVED → CLOSED. Every other transition, including no-op and backward
  moves, MUST be rejected by the system regardless of the client used.
- **FR-014**: Rejected transitions MUST leave the ticket unchanged and return a message naming
  the current status and the allowed next status (if any).
- **FR-015**: CLOSED is final: closed tickets MUST NOT accept field edits, comments or status
  changes.

**Validation and errors**

- **FR-016**: The UI MUST validate all input before sending and show problems in a custom
  pop-up modal alert (not browser-native alerts) with a clear title, plain-language message per
  problem, and a dismiss action; invalid fields MUST also be highlighted inline.
- **FR-017**: The system MUST independently validate all input and, on failure, return a
  structured error with an overall message and per-field messages that the UI displays in the
  same pop-up modal style.
- **FR-018**: Error messages MUST be user-friendly and never expose technical details (e.g.
  internal errors, stack traces); unexpected failures show a generic "something went wrong,
  please try again" message.
- **FR-019**: Successful create, update, comment and status actions MUST give a brief,
  non-blocking success confirmation.

**Persistence and history**

- **FR-020**: All tickets, comments and changes MUST be persisted durably on each successful
  action and survive application restarts.
- **FR-021**: Every change to status, title, description, priority or assignee MUST be recorded
  in an immutable history entry with who, what changed (old and new value) and when.

**Experience**

- **FR-022**: The UI MUST be modern, responsive (desktop and mobile widths), keyboard-accessible,
  and show clear loading, empty and error states; actions show progress and prevent duplicate
  submission.

### Key Entities

- **Ticket**: A support request. Attributes: reference, title, description, priority, status,
  reporter, assignee, created time, last-updated time, version (for conflict detection).
- **User**: A person who signs in, raises tickets, can be assigned tickets, and comments.
  Attributes: username, display name, email, protected password credential.
- **Comment**: A message on a ticket. Attributes: ticket, author, text, created time.
- **Ticket History Entry**: An immutable record of a change. Attributes: ticket, actor, change
  type (field update or status change), field name, old value, new value, time.
- **Status** (fixed values): OPEN, IN_PROGRESS, RESOLVED, CLOSED, with the allowed transitions in
  FR-013.
- **Priority** (fixed values): LOW, MEDIUM, HIGH, CRITICAL.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a valid ticket from the UI in under 1 minute.
- **SC-002**: Search and status filter results appear within 1 second for up to 10,000 tickets.
- **SC-003**: 100% of invalid status transitions (all 13 non-allowed pairs among the 4 statuses,
  including the 4 same-status pairs) are rejected by the system with no data change, verified by automated
  integration tests; 100% of the 3 valid transitions succeed.
- **SC-004**: 100% of created and updated tickets and comments are present with identical values
  after an application restart.
- **SC-005**: 100% of invalid inputs (missing mandatory fields, over-length text, invalid priority
  or assignee) are rejected by the system even when UI checks are bypassed.
- **SC-006**: Every validation or system failure a user can trigger results in a readable pop-up
  message; no raw technical error is ever shown.
- **SC-006a**: 100% of ticket data requests without a valid signed-in session are refused.
- **SC-007**: At least 90% of first-time users can create, find, update and comment on a ticket
  without assistance.
- **SC-008**: Every item in the post-implementation checklist (ticket created from UI, listed,
  details viewed, fields updated, assignee changed, comment added, search, status filter, valid
  transitions, invalid transitions rejected, restart persistence, backend validation, meaningful
  UI errors, state-machine integration tests) is demonstrated as passing.

## Assumptions

- Users sign in with username and password (clarified 2026-09-21, option A). Accounts and their
  initial passwords are pre-created; seeded credentials are supplied through environment
  configuration, never committed.
- Assignees are chosen from the same pre-populated list of users; user administration
  (adding/removing users, password reset, self-registration) is out of scope for this version.
- Any user may change status, edit fields and comment on any non-CLOSED ticket; role-based
  restrictions beyond this are out of scope for this version.
- Re-opening a closed or resolved ticket is out of scope because the mandated flow is strictly
  forward.
- Attachments, notifications (email/chat), SLAs dashboards, and ticket deletion are out of scope.
- Comments are plain text; rich text formatting is out of scope.
- The product is used in English; all times are displayed in the user's local time zone.
