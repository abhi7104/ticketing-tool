import { alertModal, expect, test } from "./fixtures";

test.describe("Meaningful errors", () => {
  test("backend validation errors are shown when the UI is bypassed", async ({ api }) => {
    const res = await api.post("/tickets", { title: "x", description: "short", priority: "URGENT" });
    expect(res.status()).toBe(400);
    const problem = await res.json();
    expect(problem.code).toBe("VALIDATION_FAILED");
    expect(problem.errors.map((e: { field: string }) => e.field).sort()).toEqual(
      ["assigneeId", "description", "priority", "title"],
    );
    expect(JSON.stringify(problem)).not.toMatch(/Exception|at com\./);
  });

  test("server unreachable keeps the input and offers Retry", async ({ page, api }) => {
    const users = await api.users();
    await page.goto("/tickets/new");
    await page.getByLabel(/Title/).fill("Network down test");
    await page.getByLabel(/Description/).fill("Nothing should be lost.");
    await page.getByLabel(/Priority/).selectOption("LOW");
    await page.getByLabel(/Assignee/).selectOption(String(users[0]!.id));
    await page.route("**/api/v1/tickets", (route) => route.abort("internetdisconnected"));
    await page.getByRole("button", { name: "Create ticket" }).click();

    const modal = alertModal(page);
    await expect(modal).toContainText("Can't reach the server");
    await expect(modal.getByRole("button", { name: "Retry" })).toBeVisible();
    await modal.getByRole("button", { name: "Close" }).click();
    await expect(page.getByLabel(/Title/)).toHaveValue("Network down test");
  });

  test("unexpected server errors show a generic message with a reference", async ({ page, api }) => {
    const ticket = await api.createTicket();
    await page.route(`**/api/v1/tickets/${ticket.key}/comments`, (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/problem+json",
        body: JSON.stringify({ status: 500, code: "INTERNAL_ERROR", title: "Something went wrong", detail: "Please try again.", traceId: "ref-42" }),
      }),
    );
    await page.goto(`/tickets/${ticket.key}`);
    await page.getByLabel("Add a comment").fill("Will fail");
    await page.getByRole("button", { name: "Add comment" }).click();
    await expect(alertModal(page)).toContainText("Something went wrong");
    await expect(alertModal(page)).toContainText("ref-42");
  });

  test("an expired session sends the user to sign in", async ({ page }) => {
    await page.goto("/tickets");
    await page.context().addCookies([
      { name: "TMS_SESSION", value: "expired.token.value", url: page.url() },
    ]);
    await page.reload();
    await expect(page).toHaveURL(/\/login\?reason=expired/);
    await expect(alertModal(page)).toContainText("Session expired");
  });

  test("unknown ticket shows a friendly not-found page", async ({ page }) => {
    await page.goto("/tickets/TMS-999999999");
    await expect(page.getByRole("heading", { name: "Ticket not found" })).toBeVisible();
    await page.getByRole("link", { name: "Back to tickets" }).last().click();
    await expect(page).toHaveURL(/\/tickets$/);
  });
});
