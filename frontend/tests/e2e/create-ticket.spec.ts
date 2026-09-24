import { alertModal, expect, test, unique } from "./fixtures";

test.describe("Create ticket (US1)", () => {
  test("ticket can be created from the UI", async ({ page, api }) => {
    const users = await api.users();
    const title = unique("Laptop won't boot");
    await page.goto("/tickets/new");
    await page.getByLabel(/Title/).fill(title);
    await page.getByLabel(/Description/).fill("Black screen after the latest update.");
    await page.getByLabel(/Priority/).selectOption("HIGH");
    await page.getByLabel(/Assignee/).selectOption(String(users[0]!.id));
    await page.getByRole("button", { name: "Create ticket" }).click();

    await expect(page.getByText(/Ticket TMS-\d+ created/)).toBeVisible();
    await expect(page).toHaveURL(/\/tickets\/TMS-\d+$/);
    await expect(page.getByRole("heading", { name: title })).toBeVisible();
    await expect(page.getByText("Open").first()).toBeVisible();
    await expect(page.getByText("Black screen after the latest update.")).toBeVisible();
  });

  test("empty submit shows the validation pop-up", async ({ page }) => {
    await page.goto("/tickets/new");
    await page.getByRole("button", { name: "Create ticket" }).click();
    const modal = alertModal(page);
    await expect(modal).toContainText("Please check your input");
    await expect(modal).toContainText("Title is required.");
    await expect(modal).toContainText("Please choose an assignee.");
    await modal.getByRole("button", { name: "OK" }).click();
    await expect(page.getByLabel(/Title/)).toBeFocused();
    await expect(page).toHaveURL(/\/tickets\/new$/);
  });
});
