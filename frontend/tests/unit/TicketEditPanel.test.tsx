import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { EditButton, TicketEditPanel } from "@/components/tickets/TicketEditPanel";
import { problem, ticket } from "../mocks/handlers";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

vi.mock("sonner", () => ({ toast: { success: vi.fn() } }));

describe("TicketEditPanel", () => {
  it("pre-fills current values and sends only changed fields with If-Match", async () => {
    let seen: { body: unknown; ifMatch: string | null } | undefined;
    server.use(
      http.patch("*/api/v1/tickets/TMS-1", async ({ request }) => {
        seen = { body: await request.json(), ifMatch: request.headers.get("If-Match") };
        return HttpResponse.json(ticket({ priority: "CRITICAL", version: 1 }));
      }),
    );
    const onEditingChange = vi.fn();
    renderWithProviders(
      <TicketEditPanel ticket={ticket()} etag='"0"' editing onEditingChange={onEditingChange} />,
    );
    expect(screen.getByLabelText(/title/i)).toHaveValue("Printer jam");
    await screen.findByRole("option", { name: "Bob Singh" });
    expect(screen.getByLabelText(/assignee/i)).toHaveValue("2");

    await userEvent.selectOptions(screen.getByLabelText(/priority/i), "CRITICAL");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => expect(seen).toEqual({ body: { priority: "CRITICAL" }, ifMatch: '"0"' }));
    await waitFor(() => expect(onEditingChange).toHaveBeenCalledWith(false));
  });

  it("validates like create", async () => {
    renderWithProviders(<TicketEditPanel ticket={ticket()} etag='"0"' editing onEditingChange={vi.fn()} />);
    await userEvent.clear(screen.getByLabelText(/title/i));
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));
    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent("Title is required.");
  });

  it("offers Reload on a version conflict and keeps edits until chosen", async () => {
    server.use(
      http.patch("*/api/v1/tickets/TMS-1", () =>
        problem(412, {
          code: "VERSION_CONFLICT",
          title: "Ticket was updated by someone else",
          detail: "This ticket changed since you opened it.",
        }),
      ),
    );
    renderWithProviders(<TicketEditPanel ticket={ticket()} etag='"0"' editing onEditingChange={vi.fn()} />);
    await userEvent.type(screen.getByLabelText(/title/i), " on floor 2");
    await userEvent.click(screen.getByRole("button", { name: "Save changes" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveAccessibleName("Ticket was updated by someone else");
    expect(screen.getByRole("button", { name: "Reload" })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.getByLabelText(/title/i)).toHaveValue("Printer jam on floor 2");
  });

  it("is not available for closed tickets", () => {
    const closed = ticket({ status: "CLOSED", allowedNextStatus: null });
    const { container } = renderWithProviders(
      <>
        <EditButton ticket={closed} onEdit={vi.fn()} />
        <TicketEditPanel ticket={closed} etag='"3"' editing onEditingChange={vi.fn()} />
      </>,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
