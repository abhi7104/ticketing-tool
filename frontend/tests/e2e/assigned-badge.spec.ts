import { expect, test, unique } from "./fixtures";

const badgeLink = (page: import("@playwright/test").Page) =>
  page.getByRole("banner").getByRole("link", { name: /^Assigned to me/ });

const label = (n: number) =>
  n === 0 ? "Assigned to me" : `Assigned to me, ${n} ${n === 1 ? "ticket" : "tickets"} waiting`;

/**
 * Other specs create tickets for the same user in parallel, so the badge is compared with the
 * server's current count (retrying until they agree) instead of fixed numbers. Exact counting
 * is covered by the backend integration tests.
 */
async function expectBadgeMatchesServer(page: import("@playwright/test").Page, api2: import("./fixtures").Api) {
  await expect(async () => {
    await page.reload();
    await expect(badgeLink(page)).toHaveAccessibleName(label(await api2.assignedPending()), { timeout: 2000 });
  }).toPass({ timeout: 15_000 });
}

test.describe("Assigned to me badge (feature 002 US2)", () => {
  test("is on every page after sign-in and follows changes", async ({ api, api2, page2 }) => {
    const bob = await api2.me();
    const one = await api.createTicket({ title: unique("Badge one"), assigneeId: bob.id });
    const two = await api.createTicket({ title: unique("Badge two"), assigneeId: bob.id });

    for (const path of ["/tickets", "/tickets/new", `/tickets/${one.key}`]) {
      await page2.goto(path);
      await expect(badgeLink(page2)).toBeVisible();
      await expectBadgeMatchesServer(page2, api2);
    }

    // Resolving through the UI refreshes the badge without a manual reload.
    await page2.getByRole("button", { name: "Start progress" }).click();
    await expect(page2.getByText("Status changed to In progress")).toBeVisible();
    // Track every count the page receives; after resolving, the badge must show the latest one.
    const counts: number[] = [];
    page2.on("response", async (r) => {
      if (r.url().includes("/api/v1/tickets/summary") && r.ok()) counts.push((await r.json()).assignedPending);
    });
    await page2.getByRole("button", { name: "Mark resolved" }).click();
    await expect(page2.getByText("Status changed to Resolved")).toBeVisible();
    await expect(async () => {
      expect(counts.length).toBeGreaterThan(0);
      await expect(badgeLink(page2)).toHaveAccessibleName(label(counts.at(-1)!), { timeout: 500 });
    }).toPass({ timeout: 10_000 });

    // Reassigned away by someone else: visible after reload.
    const alice = await api.me();
    const res = await api.patch(`/tickets/${two.key}`, { assigneeId: alice.id }, { "If-Match": '"0"' });
    expect(res.status()).toBe(200);
    await expectBadgeMatchesServer(page2, api2);

    await badgeLink(page2).click();
    await expect(page2).toHaveURL(/\/tickets\?view=assigned$/);
    await expect(badgeLink(page2)).toHaveAttribute("aria-current", "page");
  });
});
