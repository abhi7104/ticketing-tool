import AxeBuilder from "@axe-core/playwright";
import { test as base } from "@playwright/test";
import { alertModal, expect, test } from "./fixtures";

async function expectNoSeriousViolations(page: import("@playwright/test").Page) {
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21aa"]).analyze();
  const serious = results.violations.filter((v) => v.impact === "serious" || v.impact === "critical");
  expect(serious.map((v) => `${v.id}: ${v.nodes.map((n) => n.target.join(" ")).join(", ")}`)).toEqual([]);
}

base("sign-in page is accessible", async ({ page }) => {
  await page.goto("/login");
  await expectNoSeriousViolations(page);
});

test.describe("Accessibility", () => {
  test("listing, create and detail pages", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await api.post(`/tickets/${ticket.key}/comments`, { body: "Checking accessibility." });

    await page.goto("/tickets?view=all");
    await expect(page.getByRole("heading", { name: "Tickets" })).toBeVisible();
    await expectNoSeriousViolations(page);

    await page.goto("/tickets/new");
    await expectNoSeriousViolations(page);
    await page.getByRole("button", { name: "Create ticket" }).click();
    await expect(alertModal(page)).toBeVisible();
    await expectNoSeriousViolations(page);

    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByRole("heading", { name: ticket.title })).toBeVisible();
    await expectNoSeriousViolations(page);
  });

  test("closed ticket page", async ({ page, api }) => {
    const ticket = await api.createTicket();
    for (const s of ["IN_PROGRESS", "RESOLVED", "CLOSED"]) await api.transition(ticket.key, s);
    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByText("This ticket is closed and read-only.")).toBeVisible();
    await expectNoSeriousViolations(page);
  });

  test("assigned view, badge and flow diagram (feature 002)", async ({ page, api }) => {
    const me = await api.me();
    const ticket = await api.createTicket({ assigneeId: me.id });
    await page.goto("/tickets?view=assigned");
    await expect(page.getByRole("banner").getByRole("link", { name: /Assigned to me, \d+ tickets? waiting/ })).toBeVisible();
    await expectNoSeriousViolations(page);

    await page.goto("/tickets?view=assigned&status=CLOSED");
    await expect(page.getByText("No tickets match")).toBeVisible();
    await expectNoSeriousViolations(page);

    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByRole("list", { name: "Ticket lifecycle" })).toBeVisible();
    await expectNoSeriousViolations(page);
  });

  test("a ticket can be created with the keyboard only", async ({ page, api }) => {
    const users = await api.users();
    await page.goto("/tickets/new");
    await page.getByLabel(/Title/).focus();
    await page.keyboard.type("Keyboard only ticket");
    await page.keyboard.press("Tab");
    await page.keyboard.type("Created without touching the mouse.");
    await page.keyboard.press("Tab");
    await page.getByLabel(/Priority/).selectOption("LOW");
    await page.keyboard.press("Tab");
    await page.getByLabel(/Assignee/).selectOption(String(users[0]!.id));
    await page.getByRole("button", { name: "Create ticket" }).focus();
    await page.keyboard.press("Enter");
    await expect(page).toHaveURL(/\/tickets\/TMS-\d+$/);
  });
});
