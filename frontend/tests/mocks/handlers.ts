import { http, HttpResponse } from "msw";
import type { CurrentUser, TicketDetail, UserSummary } from "@/lib/api/types";

export const ME: CurrentUser = { id: 1, username: "alice", displayName: "Alice Moore", email: "alice@example.test" };
export const USERS: UserSummary[] = [
  { id: 1, displayName: "Alice Moore", email: "alice@example.test" },
  { id: 2, displayName: "Bob Singh", email: "bob@example.test" },
];

export function ticket(overrides: Partial<TicketDetail> = {}): TicketDetail {
  return {
    key: "TMS-1",
    title: "Printer jam",
    description: "Paper stuck in tray two",
    status: "OPEN",
    priority: "MEDIUM",
    assignee: USERS[1]!,
    reporter: USERS[0]!,
    createdAt: "2026-09-21T10:00:00Z",
    updatedAt: "2026-09-21T10:00:00Z",
    resolvedAt: null,
    closedAt: null,
    version: 0,
    allowedNextStatus: "IN_PROGRESS",
    comments: [],
    history: [],
    ...overrides,
  };
}

export function problem(status: number, body: Record<string, unknown>) {
  return HttpResponse.json(
    { type: "about:blank", status, traceId: "trace-123", ...body },
    { status, headers: { "Content-Type": "application/problem+json" } },
  );
}

export const handlers = [
  http.get("*/api/v1/auth/me", () => HttpResponse.json(ME)),
  http.get("*/api/v1/users", () => HttpResponse.json(USERS)),
];
