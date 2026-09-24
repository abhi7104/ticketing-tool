import { alertModal, expect, test, unique } from "./fixtures";

test.describe("Edit and reassign (US4)", () => {
  test("ticket fields can be updated", async ({ page, api }) => {
    const ticket = await api.createTicket();
    const newTitle = unique("Renamed ticket");
    await page.goto(`/tickets/${ticket.key}`);
    await page.getByRole("button", { name: "Edit" }).click();
    await page.getByLabel(/Title/).fill(newTitle);
    await page.getByLabel(/Description/).fill("A clearer description of the problem.");
    await page.getByLabel(/Priority/).selectOption("CRITICAL");
    await page.getByRole("button", { name: "Save changes" }).click();

    await expect(page.getByText("Changes saved")).toBeVisible();
    await page.reload();
    await expect(page.getByRole("heading", { name: newTitle })).toBeVisible();
    await expect(page.getByText("A clearer description of the problem.")).toBeVisible();
    await expect(page.getByText("Critical").first()).toBeVisible();
  });

  test("assignee can be changed", async ({ page, api }) => {
    const users = await api.users();
    const ticket = await api.createTicket({ assigneeId: users[0]!.id });
    const target = users[1]!;
    await page.goto(`/tickets/${ticket.key}`);
    await page.getByRole("button", { name: "Edit" }).click();
    await page.getByLabel(/Assignee/).selectOption(String(target.id));
    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(page.getByText("Changes saved")).toBeVisible();

    const details = page.locator("dl");
    await expect(details).toContainText(target.displayName);
    await page.goto(`/tickets?view=all&q=${ticket.key}`);
    await expect(page.getByRole("link", { name: new RegExp(`${ticket.key}:`) })).toContainText(target.displayName);
  });

  test("clearing the title shows the validation pop-up", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.goto(`/tickets/${ticket.key}`);
    await page.getByRole("button", { name: "Edit" }).click();
    await page.getByLabel(/Title/).fill("");
    await page.getByRole("button", { name: "Save changes" }).click();
    await expect(alertModal(page)).toContainText("Title is required.");
  });

  test("editing a ticket someone else just changed shows a conflict pop-up", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.goto(`/tickets/${ticket.key}`);
    await page.getByRole("button", { name: "Edit" }).click();
    const other = await api.patch(`/tickets/${ticket.key}`, { priority: "LOW" }, { "If-Match": '"0"' });
    expect(other.status()).toBe(200);

    await page.getByLabel(/Title/).fill(unique("My stale edit"));
    await page.getByRole("button", { name: "Save changes" }).click();
    const modal = alertModal(page);
    await expect(modal).toContainText("Ticket was updated by someone else");
    await modal.getByRole("button", { name: "Reload" }).click();
    await expect(page.getByLabel(/Priority/)).toHaveValue("LOW");
  });
});
