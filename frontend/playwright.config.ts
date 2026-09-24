import { defineConfig, devices } from "@playwright/test";

/**
 * E2E tests run against a running stack (docker compose up, or backend + `npm run start`).
 * Credentials come from environment variables: E2E_USER / E2E_PASSWORD (and E2E_USER2 for a
 * second seeded user). Nothing is hard-coded.
 */
export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["html", { open: "never" }], ["list"]] : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] }, testMatch: /create-ticket|a11y/ },
  ],
});
