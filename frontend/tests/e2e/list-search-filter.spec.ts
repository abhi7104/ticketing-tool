import { expect, test, unique } from "./fixtures";

test.describe("List, search and filter (US2)", () => {
  test("tickets are listed and details open from the list", async ({ page, api }) => {
    const ticket = await api.createTicket({ title: unique("Listed ticket") });
    await page.goto("/tickets");
    const row = page.getByRole("link", { name: new RegExp(`${ticket.key}:`) });
    await expect(row).toBeVisible();
    await expect(row).toContainText("Open");
    await row.click();
    await expect(page).toHaveURL(new RegExp(`/tickets/${ticket.key}$`));
    await expect(page.getByRole("heading", { name: ticket.title })).toBeVisible();
  });

  test("search by title fragment and by key", async ({ page, api }) => {
    const token = unique("zebra").split(" ")[1]!;
    const match = await api.createTicket({ title: `Printer ${token} jam` });
    const other = await api.createTicket({ title: unique("Unrelated") });

    await page.goto("/tickets");
    await page.getByRole("searchbox", { name: "Search tickets" }).fill(token.toUpperCase());
    await expect(page).toHaveURL(new RegExp(`q=${token.toUpperCase()}`));
    await expect(page.getByRole("link", { name: new RegExp(`${match.key}:`) })).toBeVisible();
    await expect(page.getByRole("link", { name: new RegExp(`${other.key}:`) })).toHaveCount(0);

    await page.getByRole("searchbox", { name: "Search tickets" }).fill(other.key);
    await expect(page.getByRole("link", { name: new RegExp(`${other.key}:`) })).toBeVisible();
  });

  test("status filter works and combines with search; filters survive reload", async ({ page, api }) => {
    const token = unique("walrus").split(" ")[1]!;
    const open = await api.createTicket({ title: `Open ${token}` });
    const started = await api.createTicket({ title: `Started ${token}` });
    expect((await api.transition(started.key, "IN_PROGRESS")).status()).toBe(200);

    await page.goto(`/tickets?q=${token}`);
    await page.getByRole("group", { name: "Filter by status" }).getByRole("button", { name: "In progress" }).click();
    await expect(page.getByRole("link", { name: new RegExp(`${started.key}:`) })).toBeVisible();
    await expect(page.getByRole("link", { name: new RegExp(`${open.key}:`) })).toHaveCount(0);

    await page.reload();
    await expect(page.getByRole("button", { name: "In progress" })).toHaveAttribute("aria-pressed", "true");
    await expect(page.getByRole("link", { name: new RegExp(`${started.key}:`) })).toBeVisible();

    await page.getByRole("button", { name: "Resolved" }).click();
    await expect(page.getByText("No tickets match")).toBeVisible();
    await page.getByRole("button", { name: "Clear filters" }).click();
    await expect(page).toHaveURL(/\/tickets$/);
  });

  test("My tickets vs All tickets", async ({ page, api2 }) => {
    const theirs = await api2.createTicket({ title: unique("Someone else's") });
    await page.goto(`/tickets?q=${encodeURIComponent(theirs.key)}`);
    await expect(page.getByText("No tickets match")).toBeVisible();
    await page.getByRole("button", { name: "All tickets" }).click();
    await expect(page.getByRole("link", { name: new RegExp(`${theirs.key}:`) })).toBeVisible();
  });
});
