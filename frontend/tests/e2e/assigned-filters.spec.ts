import { expect, test, unique } from "./fixtures";

test.describe("Filters inside Assigned to me (feature 002 US3)", () => {
  test("search and status narrow my queue; finished statuses explain themselves", async ({ api, api2, page2 }) => {
    const bob = await api2.me();
    const token = unique("otter").split(" ")[1]!;
    const open = await api.createTicket({ title: `Open ${token}`, assigneeId: bob.id });
    const started = await api.createTicket({ title: `Started ${token}`, assigneeId: bob.id });
    expect((await api.transition(started.key, "IN_PROGRESS")).status()).toBe(200);

    await page2.goto(`/tickets?view=assigned&q=${token}`);
    await expect(page2.getByRole("link", { name: new RegExp(`${open.key}:`) })).toBeVisible();
    await expect(page2.getByRole("link", { name: new RegExp(`${started.key}:`) })).toBeVisible();

    const statusGroup = page2.getByRole("group", { name: "Filter by status" });
    await statusGroup.getByRole("button", { name: "In progress" }).click();
    await expect(page2.getByRole("link", { name: new RegExp(`${started.key}:`) })).toBeVisible();
    await expect(page2.getByRole("link", { name: new RegExp(`${open.key}:`) })).toHaveCount(0);

    await page2.reload();
    await expect(page2).toHaveURL(/view=assigned/);
    await expect(page2).toHaveURL(/status=IN_PROGRESS/);
    await expect(statusGroup.getByRole("button", { name: "In progress" })).toHaveAttribute("aria-pressed", "true");

    await statusGroup.getByRole("button", { name: "Closed" }).click();
    await expect(page2.getByText("No tickets match")).toBeVisible();
    await expect(page2.getByText(/Resolved and closed tickets aren't in your queue/)).toBeVisible();
    await page2.getByRole("button", { name: "Clear filters" }).click();
    await expect(page2).toHaveURL(/\/tickets\?view=assigned$/);
  });
});
