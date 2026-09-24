import { expect, test, unique } from "./fixtures";

test.describe("Comments (US5)", () => {
  test("comments can be added and survive reload", async ({ page, api }) => {
    const ticket = await api.createTicket();
    const text = unique("Restarted the printer");
    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByRole("button", { name: "Add comment" })).toBeDisabled();
    await page.getByLabel("Add a comment").fill(text);
    await page.getByRole("button", { name: "Add comment" }).click();

    await expect(page.getByText(text)).toBeVisible();
    await expect(page.getByLabel("Add a comment")).toHaveValue("");
    await page.reload();
    await expect(page.getByText(text)).toBeVisible();
    await expect(page.getByRole("tab", { name: "Comments (1)" })).toBeVisible();
  });

  test("markup in comments is shown as text", async ({ page, api }) => {
    const ticket = await api.createTicket();
    const res = await api.post(`/tickets/${ticket.key}/comments`, { body: "<img src=x onerror=alert(1)>" });
    expect(res.status()).toBe(201);
    await page.goto(`/tickets/${ticket.key}`);
    await expect(page.getByText("<img src=x onerror=alert(1)>")).toBeVisible();
  });
});
