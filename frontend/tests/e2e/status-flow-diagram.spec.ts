import { expect, test } from "./fixtures";

const flow = (page: import("@playwright/test").Page) => page.getByRole("list", { name: "Ticket lifecycle" });

test.describe("Lifecycle flow diagram (feature 002 US4)", () => {
  test("marks done, current and upcoming steps and updates after a status change", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.goto(`/tickets/${ticket.key}`);

    const steps = flow(page).getByRole("listitem");
    await expect(steps).toHaveCount(4);
    await expect(steps.nth(0)).toHaveAttribute("aria-current", "step");
    await expect(steps.nth(1)).toContainText("upcoming");

    await page.getByRole("button", { name: "Start progress" }).click();
    await expect(steps.nth(1)).toHaveAttribute("aria-current", "step");
    await expect(steps.nth(0)).not.toHaveAttribute("aria-current", "step");
    await expect(steps.nth(0)).toContainText(/done, reached/);
  });

  test("closed tickets show every step done and a final state", async ({ page, api }) => {
    const ticket = await api.createTicket();
    for (const s of ["IN_PROGRESS", "RESOLVED", "CLOSED"]) expect((await api.transition(ticket.key, s)).status()).toBe(200);
    await page.goto(`/tickets/${ticket.key}`);
    const steps = flow(page).getByRole("listitem");
    await expect(steps.nth(3)).toHaveAttribute("aria-current", "step");
    await expect(flow(page)).toContainText("Final state");
    for (const i of [0, 1, 2]) await expect(steps.nth(i)).toContainText("done");
  });

  test("fits a phone screen without sideways scrolling", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.setViewportSize({ width: 375, height: 800 });
    await page.goto(`/tickets/${ticket.key}`);
    await expect(flow(page)).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow).toBeLessThanOrEqual(0);
  });
});
