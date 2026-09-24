import type { TicketDetail, TicketStatus } from "./api/types";
import { STATUS_LABELS } from "./labels";

/** Lifecycle order; must match the backend state machine (OPEN → IN_PROGRESS → RESOLVED → CLOSED). */
export const STATUS_FLOW: readonly TicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];

export type StepState = "done" | "current" | "upcoming";

export interface LifecycleStep {
  status: TicketStatus;
  label: string;
  state: StepState;
  /** True for the last step of a closed ticket. */
  final: boolean;
  /** When the ticket reached this status; undefined for upcoming steps. */
  reachedAt?: string;
}

/** Derives the flow diagram steps from data the detail endpoint already returns. */
export function buildSteps(ticket: TicketDetail): LifecycleStep[] {
  const currentIndex = STATUS_FLOW.indexOf(ticket.status);
  const reached = new Map<string, string>();
  for (const entry of ticket.history) {
    if (entry.changeType === "STATUS_CHANGED" && entry.newValue) {
      reached.set(entry.newValue, entry.occurredAt);
    }
  }

  return STATUS_FLOW.map((status, index) => {
    const state: StepState = index < currentIndex ? "done" : index === currentIndex ? "current" : "upcoming";
    const reachedAt =
      state === "upcoming" ? undefined : status === "OPEN" ? ticket.createdAt : reached.get(status);
    return {
      status,
      label: STATUS_LABELS[status],
      state,
      final: status === "CLOSED" && state === "current",
      reachedAt,
    };
  });
}
