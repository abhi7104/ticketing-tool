import { expect, test, unique } from "./fixtures";

test.describe("Assigned to me view (feature 002 US1)", () => {
  test("shows only my pending tickets, most urgent first, and keeps the view", async ({ api, api2, page2 }) => {
    const bob = await api2.me();
    const token = unique("queue").split(" ")[1]!;
    const low = await api.createTicket({ title: `Low ${token}`, priority: "LOW", assigneeId: bob.id });
    const critical = await api.createTicket({ title: `Critical ${token}`, priority: "CRITICAL", assigneeId: bob.id });
    const high = await api.createTicket({ title: `High ${token}`, priority: "HIGH", assigneeId: bob.id });
    const done = await api.createTicket({ title: `Done ${token}`, priority: "CRITICAL", assigneeId: bob.id });
    for (const s of ["IN_PROGRESS", "RESOLVED"]) expect((await api.transition(done.key, s)).status()).toBe(200);
    const notMine = await api.createTicket({ title: `Alice ${token}`, priority: "CRITICAL" });

    await page2.goto("/tickets");
    await page2.getByRole("group", { name: "Which tickets" }).getByRole("button", { name: "Assigned to me" }).click();
    await expect(page2).toHaveURL(/view=assigned/);
    await expect(page2.getByText("Tickets waiting on you, most urgent first.")).toBeVisible();
    await page2.getByRole("searchbox", { name: "Search tickets" }).fill(token);

    const rows = page2.getByRole("link", { name: new RegExp(`: .*${token}`) });
    await expect(rows).toHaveCount(3);
    await expect(rows.nth(0)).toContainText(critical.key);
    await expect(rows.nth(1)).toContainText(high.key);
    await expect(rows.nth(2)).toContainText(low.key);
    await expect(page2.getByRole("link", { name: new RegExp(`${done.key}:`) })).toHaveCount(0);
    await expect(page2.getByRole("link", { name: new RegExp(`${notMine.key}:`) })).toHaveCount(0);

    await rows.nth(0).click();
    await expect(page2).toHaveURL(new RegExp(`/tickets/${critical.key}$`));
    await page2.goBack();
    await expect(page2).toHaveURL(/view=assigned/);
    await expect(page2.getByRole("button", { name: "Assigned to me" })).toHaveAttribute("aria-pressed", "true");

    await page2.reload();
    await expect(page2.getByRole("button", { name: "Assigned to me" })).toHaveAttribute("aria-pressed", "true");
    await expect(rows).toHaveCount(3);
  });
});

test("an empty queue shows the friendly empty state", async ({ page }) => {
  // The backend's empty-queue behaviour is covered by integration tests; here the list response
  // is stubbed so the check does not depend on what other users have assigned.
  await page.route("**/api/v1/tickets?*view=assigned*", (route) =>
    route.fulfill({ json: { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 } }),
  );
  await page.goto("/tickets?view=assigned");
  await expect(page.getByText("Nothing waiting on you")).toBeVisible();
  await page.getByRole("link", { name: "View all tickets" }).click();
  await expect(page).toHaveURL(/view=all/);
});
