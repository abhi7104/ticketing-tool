import { expect, test } from "@playwright/test";
import { PASSWORD, USER } from "./fixtures";

test.describe("Sign-in", () => {
  test("signed-out visitors are sent to the sign-in page", async ({ page }) => {
    await page.goto("/tickets");
    await expect(page).toHaveURL(/\/login\?next=%2Ftickets/);
  });

  test("wrong password shows the sign-in failed pop-up", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Username").fill(USER());
    await page.getByLabel("Password").fill("definitely-wrong");
    await page.getByRole("button", { name: "Sign in" }).click();
    const modal = page.getByRole("alertdialog");
    await expect(modal).toContainText("Sign-in failed");
    await expect(modal).toContainText("Invalid username or password.");
  });

  test("valid sign-in lands on tickets and sign-out works", async ({ page }) => {
    await page.goto("/login");
    await page.getByLabel("Username").fill(USER());
    await page.getByLabel("Password").fill(PASSWORD());
    await page.getByRole("button", { name: "Sign in" }).click();
    await expect(page).toHaveURL(/\/tickets$/);
    await expect(page.getByRole("heading", { name: "Tickets" })).toBeVisible();

    await page.getByRole("button", { name: "Account menu" }).click();
    await page.getByRole("menuitem", { name: "Sign out" }).click();
    await expect(page).toHaveURL(/\/login/);
    await page.goto("/tickets");
    await expect(page).toHaveURL(/\/login/);
  });
});
