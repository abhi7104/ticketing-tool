import { describe, expect, it } from "vitest";
import { buildSteps, STATUS_FLOW } from "@/lib/status-flow";
import type { HistoryEntry, TicketStatus } from "@/lib/api/types";
import { ticket } from "../mocks/handlers";

const actor = { id: 1, displayName: "Alice Moore" };
const change = (id: number, from: TicketStatus, to: TicketStatus, at: string): HistoryEntry => ({
  id,
  actor,
  changeType: "STATUS_CHANGED",
  field: "status",
  oldValue: from,
  newValue: to,
  occurredAt: at,
});

const history = [
  { id: 1, actor, changeType: "CREATED", occurredAt: "2026-09-21T10:00:00Z" } as HistoryEntry,
  change(2, "OPEN", "IN_PROGRESS", "2026-09-21T11:00:00Z"),
  change(3, "IN_PROGRESS", "RESOLVED", "2026-09-21T12:00:00Z"),
  change(4, "RESOLVED", "CLOSED", "2026-09-21T13:00:00Z"),
];

describe("status flow", () => {
  it("matches the backend state machine order", () => {
    expect(STATUS_FLOW).toEqual(["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"]);
  });

  it.each([
    ["OPEN", ["current", "upcoming", "upcoming", "upcoming"], 1],
    ["IN_PROGRESS", ["done", "current", "upcoming", "upcoming"], 2],
    ["RESOLVED", ["done", "done", "current", "upcoming"], 3],
    ["CLOSED", ["done", "done", "done", "current"], 4],
  ] as const)("%s → %j", (status, states, historyLength) => {
    const steps = buildSteps(ticket({ status, history: history.slice(0, historyLength) }));
    expect(steps.map((s) => s.state)).toEqual(states);
  });

  it("uses creation time for Open and status-change times for later steps", () => {
    const steps = buildSteps(ticket({ status: "RESOLVED", createdAt: "2026-09-21T10:00:00Z", history: history.slice(0, 3) }));
    expect(steps.map((s) => s.reachedAt)).toEqual([
      "2026-09-21T10:00:00Z",
      "2026-09-21T11:00:00Z",
      "2026-09-21T12:00:00Z",
      undefined,
    ]);
  });

  it("marks only a closed ticket's last step as final", () => {
    expect(buildSteps(ticket({ status: "CLOSED", history })).map((s) => s.final)).toEqual([false, false, false, true]);
    expect(buildSteps(ticket({ status: "RESOLVED", history: history.slice(0, 3) })).some((s) => s.final)).toBe(false);
  });

  it("labels steps for people", () => {
    expect(buildSteps(ticket()).map((s) => s.label)).toEqual(["Open", "In progress", "Resolved", "Closed"]);
  });
});
