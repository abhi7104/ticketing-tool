import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { StatusActionButton } from "@/components/tickets/StatusActionButton";
import type { TicketStatus } from "@/lib/api/types";
import { problem, ticket } from "../mocks/handlers";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

vi.mock("sonner", () => ({ toast: { success: vi.fn() } }));

const NEXT: Record<TicketStatus, TicketStatus | null> = {
  OPEN: "IN_PROGRESS",
  IN_PROGRESS: "RESOLVED",
  RESOLVED: "CLOSED",
  CLOSED: null,
};

describe("StatusActionButton", () => {
  it.each([
    ["OPEN", "Start progress"],
    ["IN_PROGRESS", "Mark resolved"],
    ["RESOLVED", "Close ticket"],
  ] as const)("%s offers only '%s'", (status, label) => {
    renderWithProviders(
      <StatusActionButton ticket={ticket({ status, allowedNextStatus: NEXT[status] })} etag='"0"' />,
    );
    expect(screen.getAllByRole("button")).toHaveLength(1);
    expect(screen.getByRole("button")).toHaveTextContent(label);
  });

  it("renders nothing for closed tickets", () => {
    const { container } = renderWithProviders(
      <StatusActionButton ticket={ticket({ status: "CLOSED", allowedNextStatus: null })} etag='"3"' />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it("sends the target status with If-Match", async () => {
    let seen: { body: unknown; ifMatch: string | null } | undefined;
    server.use(
      http.post("*/api/v1/tickets/TMS-1/transitions", async ({ request }) => {
        seen = { body: await request.json(), ifMatch: request.headers.get("If-Match") };
        return HttpResponse.json(ticket({ status: "IN_PROGRESS", allowedNextStatus: "RESOLVED", version: 1 }));
      }),
    );
    renderWithProviders(<StatusActionButton ticket={ticket()} etag='"0"' />);
    await userEvent.click(screen.getByRole("button", { name: /start progress/i }));
    await waitFor(() => expect(seen).toEqual({ body: { targetStatus: "IN_PROGRESS" }, ifMatch: '"0"' }));
  });

  it("asks for confirmation before closing", async () => {
    let called = false;
    server.use(
      http.post("*/api/v1/tickets/TMS-1/transitions", () => ((called = true), HttpResponse.json(ticket()))),
    );
    renderWithProviders(
      <StatusActionButton ticket={ticket({ status: "RESOLVED", allowedNextStatus: "CLOSED" })} etag='"2"' />,
    );
    await userEvent.click(screen.getByRole("button", { name: /close ticket/i }));
    expect(await screen.findByRole("alertdialog")).toHaveAccessibleName("Close this ticket?");
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(called).toBe(false);
  });

  it("explains a rejected transition in the modal", async () => {
    server.use(
      http.post("*/api/v1/tickets/TMS-1/transitions", () =>
        problem(409, {
          code: "INVALID_STATUS_TRANSITION",
          title: "Status change not allowed",
          detail: "A ticket that is Resolved can't move to In progress. It can only move to Closed.",
          currentStatus: "RESOLVED",
          allowedNextStatus: "CLOSED",
        }),
      ),
      http.get("*/api/v1/tickets/TMS-1", () => HttpResponse.json(ticket())),
    );
    renderWithProviders(<StatusActionButton ticket={ticket()} etag='"0"' />);
    await userEvent.click(screen.getByRole("button", { name: /start progress/i }));
    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveAccessibleName("Status change not allowed");
    expect(dialog).toHaveTextContent("It can only move to Closed.");
  });
});
