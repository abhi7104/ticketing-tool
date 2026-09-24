import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { AssignedLink } from "@/components/layout/AssignedLink";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

let search = new URLSearchParams();
let pathname = "/tickets";
vi.mock("next/navigation", () => ({
  useSearchParams: () => search,
  usePathname: () => pathname,
}));

function withCount(n: number) {
  server.use(http.get("*/api/v1/tickets/summary", () => HttpResponse.json({ assignedPending: n })));
}

describe("AssignedLink", () => {
  it("links to the assigned view and hides the badge at 0", async () => {
    withCount(0);
    renderWithProviders(<AssignedLink />);
    const link = await screen.findByRole("link", { name: "Assigned to me" });
    expect(link).toHaveAttribute("href", "/tickets?view=assigned");
    expect(link.querySelector("[data-badge]")).toBeNull();
  });

  it("shows the count with a meaningful accessible name", async () => {
    withCount(3);
    renderWithProviders(<AssignedLink />);
    const link = await screen.findByRole("link", { name: "Assigned to me, 3 tickets waiting" });
    expect(link.querySelector("[data-badge]")).toHaveTextContent("3");
  });

  it("uses singular wording for one ticket and caps the badge at 99+", async () => {
    withCount(1);
    const { unmount } = renderWithProviders(<AssignedLink />);
    expect(await screen.findByRole("link", { name: "Assigned to me, 1 ticket waiting" })).toBeInTheDocument();
    unmount();
    withCount(120);
    renderWithProviders(<AssignedLink />);
    const link = await screen.findByRole("link", { name: "Assigned to me, 120 tickets waiting" });
    expect(link.querySelector("[data-badge]")).toHaveTextContent("99+");
  });

  it("marks itself current on the assigned view only", async () => {
    withCount(2);
    search = new URLSearchParams("view=assigned");
    pathname = "/tickets";
    const { unmount } = renderWithProviders(<AssignedLink />);
    expect(await screen.findByRole("link", { name: /Assigned to me/ })).toHaveAttribute("aria-current", "page");
    unmount();
    search = new URLSearchParams("view=all");
    renderWithProviders(<AssignedLink />);
    expect(await screen.findByRole("link", { name: /Assigned to me/ })).not.toHaveAttribute("aria-current");
  });
});
