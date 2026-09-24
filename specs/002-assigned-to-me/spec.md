# Feature Specification: Assigned to Me

**Feature Branch**: `002-assigned-to-me`

**Created**: 2026-09-22

**Status**: Draft

**Input**: User description: "i want to add new feature to the existing implementation where user can view assigned user like assigned to me where action items are pending at thier end"

## Context

This feature extends the Support Ticket Management system (`specs/001-support-ticket-management`).
Today the ticket listing offers two views: **My tickets** (tickets the signed-in user raised) and
**All tickets**. Nothing shows a user the tickets that are *assigned to them* and still need
their action. Assignees must search the full list to find their work.

## Clarifications

### Session 2026-09-22

- Q: Which statuses count as "pending at their end"? → A: Open and In progress only. Resolved
  and Closed tickets leave the assignee's queue.
- Added with `/speckit-plan` input: "Assigned to me" must be reachable right after sign-in
  (top-bar entry on every page), and the ticket detail page shows the ticket's lifecycle as an
  arrow-style flow diagram highlighting the current state (User Story 4).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See My Pending Work (Priority: P1)

A signed-in user opens the listing and chooses **Assigned to me**. They see only tickets where
they are the assignee and an action is still pending on their side, most urgent first, so they
know what to work on next.

**Why this priority**: This is the core of the request: one place that answers "what do I need
to do?". Without it the feature delivers no value.

**Independent Test**: Seed tickets assigned to different users in every status; sign in as one
assignee, open "Assigned to me", and confirm only that user's pending tickets appear, in the
defined order.

**Acceptance Scenarios**:

1. **Given** tickets assigned to Bob with statuses Open, In progress, Resolved and Closed, and
   tickets assigned to others, **When** Bob opens "Assigned to me", **Then** he sees only his
   Open and In progress tickets, and none assigned to anyone else.
2. **Given** several pending tickets, **When** the view loads, **Then** they are ordered by
   priority (Critical first), and tickets with equal priority are ordered oldest-created first
   (longest waiting on top).
3. **Given** Bob raised a ticket and assigned it to himself, **When** he opens "Assigned to me",
   **Then** that ticket appears there, because the assignee is what counts.
4. **Given** Bob has no pending assigned tickets, **When** he opens the view, **Then** a friendly
   empty state says "Nothing waiting on you" and links to all tickets.
5. **Given** Bob is in the "Assigned to me" view, **When** he opens a ticket and returns, **Then**
   the same view, filters and page are still selected.

---

### User Story 2 - Know How Much Is Waiting (Priority: P1)

Wherever the user is in the app, they can see how many tickets are currently waiting on them,
without opening the view.

**Why this priority**: A visible count prompts action and makes the new view discoverable. It
costs little once Story 1 exists.

**Independent Test**: Assign three pending tickets to a user and check the count shows 3. Resolve
one, return to the listing, and check the count shows 2 without signing out.

**Acceptance Scenarios**:

1. **Given** Bob has 3 pending assigned tickets, **When** he views any page with the app's
   navigation, **Then** the "Assigned to me" entry shows the count 3.
2. **Given** Bob moves one of those tickets to a status that is no longer pending, or it is
   reassigned to someone else, **When** he next views the listing, **Then** the count shows 2.
3. **Given** Bob has no pending assigned tickets, **Then** no count badge is shown (not "0").
4. **Given** a screen-reader user, **Then** the count is announced with its meaning, e.g.
   "Assigned to me, 3 tickets waiting".

---

### User Story 3 - Narrow Down My Queue (Priority: P2)

Inside "Assigned to me", the user can search by keyword and filter by status, exactly like the
other views, to focus on a subset of their work.

**Why this priority**: Useful once the queue is long, but the view already works without it.

**Independent Test**: With several pending tickets assigned to one user, apply a keyword and a
status filter inside "Assigned to me" and confirm results match both conditions and still exclude
other users' tickets.

**Acceptance Scenarios**:

1. **Given** "Assigned to me" is selected, **When** the user types a keyword, **Then** only their
   pending tickets whose reference, title or description match are shown.
2. **Given** "Assigned to me" is selected, **When** the user picks Open or In progress, **Then** only their tickets in that status are shown.
3. **Given** "Assigned to me" is selected, **When** the user picks Resolved or Closed, **Then** the list is empty with the "No tickets match" state and a way to
   clear filters.
4. **Given** filters are applied, **When** the page is refreshed or the link is shared, **Then**
   the same view and filters are restored.

---

### User Story 4 - See Where a Ticket Is in Its Lifecycle (Priority: P2)

On the ticket detail page, the user sees the whole lifecycle as a flow diagram: Open → In
progress → Resolved → Closed, joined by arrows. Finished steps are marked done with the time the
ticket reached them, the current step stands out, and the steps still ahead look muted. The
user understands at a glance where the ticket is and what comes next.

**Why this priority**: It makes the strict status flow visible and easy to understand, but
tickets can already be viewed and moved without it.

**Independent Test**: Open tickets in each of the four statuses and check the diagram marks the
correct steps as done, current and upcoming, with arrows between steps and the time each done
step was reached.

**Acceptance Scenarios**:

1. **Given** an Open ticket, **Then** the diagram shows Open as current and In progress,
   Resolved, Closed as upcoming, joined by arrows in that order.
2. **Given** a ticket moved Open → In progress → Resolved, **Then** Open and In progress show as
   done with the date and time each was reached, Resolved shows as current, Closed as upcoming.
3. **Given** a Closed ticket, **Then** every step shows as done and Closed is marked as the final
   state.
4. **Given** the user changes the status with the existing action button, **Then** the diagram
   updates at once to the new current step, with a short, smooth transition.
5. **Given** a screen-reader user, **Then** the diagram reads as an ordered list of steps with
   each step's state ("done", "current step", "upcoming").
6. **Given** a narrow phone screen, **Then** the diagram stays readable without sideways
   scrolling (it may stack vertically with downward arrows).

---

### Edge Cases

- A ticket is reassigned away from the user while they are viewing the list: it disappears on
  the next refresh; opening it still works (they can view any ticket) but it no longer counts.
- A user whose account is deactivated keeps assigned tickets; those tickets simply have no active
  assignee view until reassigned. No change to deactivation behavior.
- Very large queues are paginated like the other views (20 per page).
- Sorting ties (same priority and creation time) are broken by ticket reference so the order is
  stable between page loads.
- Backend rejects unknown view names with a clear validation message, as for existing views.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The ticket listing MUST offer a third view, **Assigned to me**, alongside "My
  tickets" and "All tickets".
- **FR-002**: "Assigned to me" MUST show only tickets whose current assignee is the signed-in
  user. This MUST be enforced by the system, not only by the screen.
- **FR-003**: "Assigned to me" MUST show only tickets whose status is **Open** or **In
  progress**. Resolved and Closed tickets are not pending for the assignee: their work ends when
  they resolve the ticket.
- **FR-004**: The view MUST order tickets by priority (Critical, High, Medium, Low), then by
  creation time (oldest first), then by reference.
- **FR-005**: Keyword search and status filter MUST work inside the view, combined with its
  assignee and pending-status rules (same matching rules as the existing listing).
- **FR-006**: The view MUST be paginated with the same page size and controls as the other views.
- **FR-007**: The selected view MUST be kept in the page address so refresh, back navigation and
  shared links restore it.
- **FR-008**: The app navigation MUST show the number of tickets currently pending for the
  signed-in user as assignee; the badge MUST be hidden when the number is 0.
- **FR-009**: The count MUST match the number of tickets the "Assigned to me" view shows with no
  keyword or status filter applied.
- **FR-010**: The count and view MUST reflect changes made by the user within the app (status
  change, reassignment, new ticket assigned to self) the next time the listing or navigation is
  shown, without signing in again.
- **FR-011**: The listing rows in this view MUST show the same columns as the other views, and
  opening a row MUST open the existing ticket detail page.
- **FR-012**: An empty "Assigned to me" view MUST show a friendly "Nothing waiting on you" state;
  a view emptied by filters MUST show the existing "No tickets match" state.
- **FR-013**: The count badge and view toggle MUST be accessible: keyboard operable, and the count
  announced with its meaning to assistive technology.
- **FR-014**: Existing views ("My tickets", "All tickets") and all behavior from feature 001 MUST
  remain unchanged, including the default view ("My tickets").
- **FR-015**: After sign-in, every page's navigation MUST include an "Assigned to me" entry that
  opens the view in one click (with the count from FR-008).
- **FR-016**: The ticket detail page MUST show a flow diagram of the four statuses in lifecycle
  order, joined by arrows, with each step marked done, current or upcoming.
- **FR-017**: Done steps MUST show when the ticket reached that status (creation time for Open;
  the recorded status change time for the others).
- **FR-018**: The diagram MUST be conveyed without relying on colour alone (icons and text
  state), MUST be available to assistive technology as an ordered list of steps, and MUST fit
  narrow screens without horizontal scrolling.
- **FR-019**: The diagram is informational only; status changes still happen only through the
  existing status action and backend rules.

### Key Entities

- **Ticket** (existing): uses its current assignee, status, priority and creation time. No new
  stored fields.
- **Pending status set**: Open and In progress (FR-003).
- **Assigned-to-me count**: a derived number per signed-in user; not stored.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can see what is waiting on them in one click from any page.
- **SC-002**: The "Assigned to me" view and its count appear within 1 second for a user with up to
  500 assigned tickets among 10,000 total.
- **SC-003**: In 100% of automated checks, the view never includes a ticket assigned to someone
  else or in a non-pending status, and the count equals the unfiltered view total.
- **SC-004**: After a user resolves or reassigns a pending ticket, the count shown on their next
  listing view is correct in 100% of automated checks.
- **SC-005**: All feature 001 acceptance tests still pass unchanged.
- **SC-006**: Zero serious or critical accessibility violations on the listing with the new view
  and badge, and on ticket detail pages with the flow diagram.
- **SC-007**: For tickets in each of the 4 statuses, the diagram marks done/current/upcoming steps
  correctly in 100% of automated checks.
- **SC-008**: First-time users identify a ticket's current state and next step from the diagram
  in under 5 seconds.

## Assumptions

- "Action items pending at their end" means tickets assigned to the user in status Open or In
  progress (clarified 2026-09-22, option A). Closing resolved tickets is not tracked by this view.
- The count updates when the user navigates or refreshes; live push updates (for changes made by
  other people) are out of scope.
- Notifications (email/chat) about new assignments are out of scope.
- No new permissions: any signed-in user can still view any ticket; the view only filters.
- "My tickets" stays the default view; "Assigned to me" is chosen explicitly from the listing or
  the top-bar entry.
- The flow diagram needs no new stored data: step times come from the ticket's existing creation
  time and status history.
