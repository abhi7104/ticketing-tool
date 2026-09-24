import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { delay, http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { CommentThread } from "@/components/tickets/CommentThread";
import { ticketKey, type TicketWithEtag } from "@/lib/api/tickets";
import { ME, problem, ticket } from "../mocks/handlers";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

function renderThread(detail = ticket()) {
  const utils = renderWithProviders(<CommentThread ticket={detail} etag='"0"' />);
  utils.queryClient.setQueryData<TicketWithEtag>(ticketKey(detail.key), { ticket: detail, etag: '"0"' });
  return utils;
}

describe("CommentThread", () => {
  it("renders comments with author and shows markup as plain text", () => {
    renderThread(
      ticket({
        comments: [
          { id: 1, author: { id: 2, displayName: "Bob Singh" }, body: "<script>alert(1)</script>", createdAt: "2026-09-21T10:00:00Z" },
        ],
      }),
    );
    expect(screen.getByText("Bob Singh")).toBeInTheDocument();
    expect(screen.getByText("<script>alert(1)</script>")).toBeInTheDocument();
    expect(document.querySelector("section script")).toBeNull();
  });

  it("disables the button for empty input and shows a counter", async () => {
    renderThread();
    expect(screen.getByRole("button", { name: "Add comment" })).toBeDisabled();
    await userEvent.type(screen.getByLabelText("Add a comment"), "Hello");
    expect(screen.getByText(/^5 \/ 2,000/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Add comment" })).toBeEnabled();
  });

  it("posts the comment and clears the input", async () => {
    let body: unknown;
    server.use(
      http.post("*/api/v1/tickets/TMS-1/comments", async ({ request }) => {
        body = await request.json();
        await delay(20);
        return HttpResponse.json(
          { id: 10, author: ME, body: "Restarted it", createdAt: new Date().toISOString() },
          { status: 201 },
        );
      }),
      http.get("*/api/v1/tickets/TMS-1", () => HttpResponse.json(ticket())),
    );
    renderThread();
    await userEvent.type(screen.getByLabelText("Add a comment"), "  Restarted it  ");
    await userEvent.click(screen.getByRole("button", { name: "Add comment" }));
    await waitFor(() => expect(body).toEqual({ body: "Restarted it" }));
    expect(screen.getByLabelText("Add a comment")).toHaveValue("");
  });

  it("rolls back and restores the text when the server rejects it", async () => {
    server.use(
      http.post("*/api/v1/tickets/TMS-1/comments", () =>
        problem(409, { code: "TICKET_CLOSED", title: "Ticket is closed", detail: "Closed tickets can't be changed." }),
      ),
      http.get("*/api/v1/tickets/TMS-1", () => HttpResponse.json(ticket())),
    );
    renderThread();
    await userEvent.type(screen.getByLabelText("Add a comment"), "Too late");
    await userEvent.click(screen.getByRole("button", { name: "Add comment" }));
    expect(await screen.findByRole("alertdialog")).toHaveAccessibleName("Ticket is closed");
    expect(screen.getByLabelText("Add a comment")).toHaveValue("Too late");
  });

  it("hides the composer for closed tickets", () => {
    renderThread(ticket({ status: "CLOSED", allowedNextStatus: null }));
    expect(screen.queryByLabelText("Add a comment")).not.toBeInTheDocument();
    expect(screen.getByText(/comments are disabled/i)).toBeInTheDocument();
  });
});
