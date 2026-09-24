import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { NewTicket } from "@/app/(app)/tickets/new/NewTicket";
import { problem, ticket } from "../mocks/handlers";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push, replace: vi.fn() }) }));
vi.mock("sonner", () => ({ toast: { success: vi.fn() } }));

async function fillValidForm() {
  await userEvent.type(screen.getByLabelText(/title/i), "Printer jam");
  await userEvent.type(screen.getByLabelText(/description/i), "Paper stuck in tray two");
  await userEvent.selectOptions(screen.getByLabelText(/priority/i), "HIGH");
  await screen.findByRole("option", { name: "Bob Singh" });
  await userEvent.selectOptions(screen.getByLabelText(/assignee/i), "2");
}

describe("Create ticket", () => {
  beforeEach(() => push.mockReset());

  it("empty submit shows a modal listing all four problems and highlights fields", async () => {
    let posted = false;
    server.use(http.post("*/api/v1/tickets", () => ((posted = true), HttpResponse.json({}))));
    renderWithProviders(<NewTicket />);

    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveAccessibleName("Please check your input");
    const items = within(dialog).getAllByRole("listitem").map((li) => li.textContent);
    expect(items).toEqual([
      "Title is required.",
      "Description is required.",
      "Please choose a priority.",
      "Please choose an assignee.",
    ]);
    expect(screen.getByLabelText(/title/i)).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText(/assignee/i)).toHaveAttribute("aria-invalid", "true");
    expect(posted).toBe(false);
  });

  it("sends trimmed values and navigates to the new ticket", async () => {
    let body: unknown;
    server.use(
      http.post("*/api/v1/tickets", async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(ticket({ key: "TMS-7" }), { status: 201, headers: { ETag: '"0"' } });
      }),
    );
    renderWithProviders(<NewTicket />);
    await fillValidForm();
    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/tickets/TMS-7"));
    expect(body).toEqual({
      title: "Printer jam",
      description: "Paper stuck in tray two",
      priority: "HIGH",
      assigneeId: 2,
    });
  });

  it("shows backend field errors inline and in the modal", async () => {
    server.use(
      http.post("*/api/v1/tickets", () =>
        problem(400, {
          code: "VALIDATION_FAILED",
          title: "Please correct the highlighted fields",
          detail: "1 field needs attention.",
          errors: [{ field: "assigneeId", message: "Please choose a valid assignee." }],
        }),
      ),
    );
    renderWithProviders(<NewTicket />);
    await fillValidForm();
    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent("Please choose a valid assignee.");
    expect(screen.getByLabelText(/assignee/i)).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByLabelText(/title/i)).toHaveValue("Printer jam");
    expect(push).not.toHaveBeenCalled();
  });

  it("keeps the input and offers Retry when the server is unreachable", async () => {
    server.use(http.post("*/api/v1/tickets", () => HttpResponse.error()));
    renderWithProviders(<NewTicket />);
    await fillValidForm();
    await userEvent.click(screen.getByRole("button", { name: "Create ticket" }));

    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveAccessibleName("Can't reach the server");
    expect(within(dialog).getByRole("button", { name: "Retry" })).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toHaveValue("Paper stuck in tray two");
  });
});
