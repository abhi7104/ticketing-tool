import { alertModal, expect, test } from "./fixtures";

test.describe("Status lifecycle (US3)", () => {
  test("valid transitions work through the UI and closed tickets are read-only", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.goto(`/tickets/${ticket.key}`);

    await page.getByRole("button", { name: "Start progress" }).click();
    await expect(page.getByText("Status changed to In progress")).toBeVisible();
    await page.getByRole("button", { name: "Mark resolved" }).click();
    await expect(page.getByText("Status changed to Resolved")).toBeVisible();
    await page.getByRole("button", { name: "Close ticket" }).click();
    await alertModal(page).getByRole("button", { name: "Close ticket" }).click();
    await expect(page.getByText("This ticket is closed and read-only.")).toBeVisible();

    await expect(page.getByRole("button", { name: "Edit" })).toHaveCount(0);
    await expect(page.getByLabel("Add a comment")).toHaveCount(0);
    await expect(page.getByRole("button", { name: /Start progress|Mark resolved|Close ticket/ })).toHaveCount(0);

    await page.getByRole("tab", { name: /Activity/ }).click();
    await expect(page.getByText(/changed status from Open to In progress/)).toBeVisible();
    await expect(page.getByText(/changed status from Resolved to Closed/)).toBeVisible();
  });

  test("invalid transitions are rejected by the backend", async ({ page, api }) => {
    const ticket = await api.createTicket();
    const skip = await api.transition(ticket.key, "RESOLVED");
    expect(skip.status()).toBe(409);
    const problem = await skip.json();
    expect(problem.code).toBe("INVALID_STATUS_TRANSITION");
    expect(problem.currentStatus).toBe("OPEN");
    expect(problem.allowedNextStatus).toBe("IN_PROGRESS");

    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByRole("button", { name: "Start progress" })).toBeVisible();
    await expect(page.getByRole("definition").filter({ hasText: "Open" }).first()).toBeVisible();
  });

  test("a stale page shows a friendly pop-up instead of changing status twice", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByRole("button", { name: "Start progress" })).toBeVisible();
    expect((await api.transition(ticket.key, "IN_PROGRESS")).status()).toBe(200);

    await page.getByRole("button", { name: "Start progress" }).click();
    await expect(alertModal(page)).toContainText("Ticket was updated by someone else");
  });
});
