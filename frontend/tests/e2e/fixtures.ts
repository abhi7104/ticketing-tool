import {
  expect,
  request as playwrightRequest,
  test as base,
  type APIRequestContext,
  type Page,
} from "@playwright/test";

function env(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (!value) throw new Error(`Set ${name} to run the E2E tests (see .env.example)`);
  return value;
}

export const USER = () => env("E2E_USER");
export const USER2 = () => env("E2E_USER2", "bob");
export const PASSWORD = () => env("E2E_PASSWORD");

export const unique = (label: string) => `${label} ${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;

/** API client authenticated exactly like the browser: session cookie + double-submit CSRF. */
export class Api {
  constructor(readonly ctx: APIRequestContext) {}

  private async csrf(): Promise<Record<string, string>> {
    const state = await this.ctx.storageState();
    const token = state.cookies.find((c) => c.name === "XSRF-TOKEN")?.value;
    return token ? { "X-XSRF-TOKEN": token } : {};
  }

  async get(path: string) {
    return this.ctx.get(`/api/v1${path}`);
  }

  async post(path: string, data: unknown, headers: Record<string, string> = {}) {
    return this.ctx.post(`/api/v1${path}`, { data, headers: { ...(await this.csrf()), ...headers } });
  }

  async patch(path: string, data: unknown, headers: Record<string, string> = {}) {
    return this.ctx.patch(`/api/v1${path}`, { data, headers: { ...(await this.csrf()), ...headers } });
  }

  async users(): Promise<{ id: number; displayName: string }[]> {
    return (await this.get("/users")).json();
  }

  async createTicket(overrides: Record<string, unknown> = {}) {
    const users = await this.users();
    const res = await this.post("/tickets", {
      title: unique("E2E ticket"),
      description: "Created by an end-to-end test.",
      priority: "MEDIUM",
      assigneeId: users[0]!.id,
      ...overrides,
    });
    expect(res.status(), await res.text()).toBe(201);
    return res.json() as Promise<{ key: string; title: string; version: number }>;
  }

  async me(): Promise<{ id: number; displayName: string }> {
    return (await this.get("/auth/me")).json();
  }

  async assignedPending(): Promise<number> {
    return (await (await this.get("/tickets/summary")).json()).assignedPending;
  }

  async transition(key: string, targetStatus: string) {
    const current = await (await this.get(`/tickets/${key}`)).json();
    return this.post(`/tickets/${key}/transitions`, { targetStatus }, { "If-Match": `"${current.version}"` });
  }
}

export async function apiLogin(baseURL: string, username: string): Promise<Api> {
  const ctx = await playwrightRequest.newContext({ baseURL });
  await ctx.get("/api/v1/auth/me"); // receives the XSRF-TOKEN cookie
  const api = new Api(ctx);
  const res = await api.post("/auth/login", { username, password: PASSWORD() });
  expect(res.status(), "E2E sign-in failed; check E2E_USER / E2E_PASSWORD").toBe(200);
  return api;
}

type Fixtures = { api: Api; api2: Api; page: Page; page2: Page };

async function signedInPage(
  browser: import("@playwright/test").Browser,
  api: Api,
  projectUse: import("@playwright/test").TestInfo["project"]["use"],
) {
  const context = await browser.newContext({ ...projectUse, storageState: await api.ctx.storageState() });
  const page = await context.newPage();
  page.on("dialog", (dialog) => {
    throw new Error(`Native browser dialog used: ${dialog.type()} "${dialog.message()}"`);
  });
  return { context, page };
}

export const test = base.extend<Fixtures>({
  api: async ({ baseURL }, use) => {
    const api = await apiLogin(baseURL!, USER());
    await use(api);
    await api.ctx.dispose();
  },
  api2: async ({ baseURL }, use) => {
    const api = await apiLogin(baseURL!, USER2());
    await use(api);
    await api.ctx.dispose();
  },
  /** A page that is already signed in as E2E_USER and fails on any native browser dialog. */
  page: async ({ browser, api }, use, testInfo) => {
    const { context, page } = await signedInPage(browser, api, testInfo.project.use);
    await use(page);
    await context.close();
  },
  /** A page signed in as the second user (E2E_USER2). */
  page2: async ({ browser, api2 }, use, testInfo) => {
    const { context, page } = await signedInPage(browser, api2, testInfo.project.use);
    await use(page);
    await context.close();
  },
});

export { expect };

/** The custom error/confirmation pop-up. */
export const alertModal = (page: Page) => page.getByRole("alertdialog");
