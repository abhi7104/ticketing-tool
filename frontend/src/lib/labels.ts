import type { Priority, TicketStatus } from "./api/types";

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "In progress",
  RESOLVED: "Resolved",
  CLOSED: "Closed",
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  CRITICAL: "Critical",
};

/** Button label for moving a ticket into the given status. */
export const TRANSITION_ACTION_LABELS: Record<TicketStatus, string> = {
  OPEN: "Reopen",
  IN_PROGRESS: "Start progress",
  RESOLVED: "Mark resolved",
  CLOSED: "Close ticket",
};
