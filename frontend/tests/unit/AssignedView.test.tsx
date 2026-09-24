import { screen } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { TicketList } from "@/app/(app)/tickets/TicketList";
import { server } from "../mocks/server";
import { renderWithProviders } from "./render";

let search = new URLSearchParams();
vi.mock("next/navigation", () => ({
  useSearchParams: () => search,
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  usePathname: () => "/tickets",
}));

const emptyPage = { items: [], page: 0, size: 20, totalItems: 0, totalPages: 0 };

describe("Assigned to me view", () => {
  it("requests view=assigned and shows its subtitle and empty state", async () => {
    search = new URLSearchParams("view=assigned");
    let requested: string | null = null;
    server.use(
      http.get("*/api/v1/tickets", ({ request }) => {
        requested = new URL(request.url).searchParams.get("view");
        return HttpResponse.json(emptyPage);
      }),
    );
    renderWithProviders(<TicketList />);
    expect(screen.getByText("Tickets waiting on you, most urgent first.")).toBeInTheDocument();
    expect(await screen.findByText("Nothing waiting on you")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "View all tickets" })).toHaveAttribute("href", "/tickets?view=all");
    expect(requested).toBe("assigned");
  });

  it("explains that finished statuses are not in the queue", async () => {
    search = new URLSearchParams("view=assigned&status=CLOSED");
    server.use(http.get("*/api/v1/tickets", () => HttpResponse.json(emptyPage)));
    renderWithProviders(<TicketList />);
    expect(await screen.findByText("No tickets match")).toBeInTheDocument();
    expect(screen.getByText(/Resolved and closed tickets aren't in your queue/)).toBeInTheDocument();
  });

  it("keeps the generic no-results message in other views", async () => {
    search = new URLSearchParams("view=all&status=CLOSED");
    server.use(http.get("*/api/v1/tickets", () => HttpResponse.json(emptyPage)));
    renderWithProviders(<TicketList />);
    expect(await screen.findByText("No tickets match")).toBeInTheDocument();
    expect(screen.getByText("Try a different keyword or status.")).toBeInTheDocument();
  });
});
