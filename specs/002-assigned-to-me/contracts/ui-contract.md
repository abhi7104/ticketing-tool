# UI Contract: Assigned to Me + Lifecycle Flow Diagram

Additions to `specs/001-support-ticket-management/contracts/ui-contract.md`. Everything not
mentioned stays as it is.

## Routes

| Route | Change |
|-------|--------|
| `/tickets?view=assigned&q=&status=&page=` | New view value; restores on refresh, back and shared links |
| `/tickets/[key]` | Adds the lifecycle flow diagram |

The sign-in redirect still lands on `/tickets` ("My tickets").

## Top bar (every signed-in page)

```
[T Tickets]                 [📥 Assigned to me (3)]  [+ New ticket]  [☾]  [AM]
```

- Link "Assigned to me" → `/tickets?view=assigned`; shows `aria-current="page"` while that view
  is open.
- Badge shows the `assignedPending` count; hidden when 0; shows "99+" above 99.
- Accessible name: "Assigned to me, 3 tickets waiting" (or "Assigned to me" at 0).
- Below the `sm` breakpoint the text collapses to the inbox icon plus badge; the accessible name
  stays the same.
- The count refreshes after any ticket change made in the app and on window focus.

## Listing view control

```
( My tickets | Assigned to me | All tickets )
```

- `aria-pressed` on the active option, as today. Default remains "My tickets".
- Subtitle per view: "Tickets you raised." / "Tickets waiting on you, most urgent first." /
  "Every ticket in the system."
- Status chips stay the same in every view. In "Assigned to me", Resolved and Closed give the
  existing "No tickets match" state.
- Empty "Assigned to me" with no filters:
  - Icon: check-circle. Title: "Nothing waiting on you". Text: "Tickets assigned to you that
    are open or in progress will show up here." Action: "View all tickets" → `?view=all`.

## Ticket detail: lifecycle flow diagram

Placed directly under the ticket header, above the closed banner.

Desktop (≥ `sm`):

```
 ┌────────────┐      ┌───────────────┐      ┌────────────┐      ┌──────────┐
 │ ✓ Open     │ ───▶ │ ● In progress │ ───▶ │ ○ Resolved │ ───▶ │ ○ Closed │
 │ 21 Sep 10:02│      │ 21 Sep 11:40   │      │            │      │          │
 └────────────┘      └───────────────┘      └────────────┘      └──────────┘
    done                current                upcoming            upcoming
```

Phone (< `sm`): the same steps stacked vertically with downward arrows; no horizontal scroll.

| State | Look | Icon | Screen reader text |
|-------|------|------|--------------------|
| done | success tint, solid border; connector after it filled | check | "Open, done, reached 21 Sep 2026 10:02" |
| current | primary colour, 2 px ring, gentle pulse (off with reduced motion) | status icon (Closed: lock + "Final state") | "In progress, current step" (`aria-current="step"`) |
| upcoming | muted outline, dashed connector | empty circle | "Resolved, upcoming" |

- Semantics: `<ol aria-label="Ticket lifecycle">`, one `<li>` per step. Arrows are decorative
  (`aria-hidden`).
- State never depends on colour alone (icon and text).
- Updates immediately after a successful status change; connector fill and node change animate in
  ≤ 200 ms.
- Informational only: steps are not buttons. The existing single status action button stays the
  only way to change status.
