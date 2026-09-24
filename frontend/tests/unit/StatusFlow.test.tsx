import { render, screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusFlow } from "@/components/tickets/StatusFlow";
import type { HistoryEntry } from "@/lib/api/types";
import { ticket } from "../mocks/handlers";

const actor = { id: 1, displayName: "Alice Moore" };
const toInProgress: HistoryEntry = {
  id: 2,
  actor,
  changeType: "STATUS_CHANGED",
  field: "status",
  oldValue: "OPEN",
  newValue: "IN_PROGRESS",
  occurredAt: "2026-09-21T11:00:00Z",
};

describe("StatusFlow", () => {
  it("renders an ordered list of the four steps with the current one marked", () => {
    render(<StatusFlow ticket={ticket({ status: "IN_PROGRESS", history: [toInProgress] })} />);
    const list = screen.getByRole("list", { name: "Ticket lifecycle" });
    expect(list.tagName).toBe("OL");
    const items = within(list).getAllByRole("listitem");
    expect(items.map((li) => li.querySelector("[data-step-label]")?.textContent)).toEqual([
      "Open",
      "In progress",
      "Resolved",
      "Closed",
    ]);
    expect(items[1]).toHaveAttribute("aria-current", "step");
    expect(items.filter((li) => li.hasAttribute("aria-current"))).toHaveLength(1);
  });

  it("describes each state in text for screen readers", () => {
    render(<StatusFlow ticket={ticket({ status: "IN_PROGRESS", history: [toInProgress] })} />);
    const items = screen.getAllByRole("listitem");
    expect(items[0]).toHaveTextContent(/done, reached/);
    expect(items[1]).toHaveTextContent("current step");
    expect(items[2]).toHaveTextContent("upcoming");
  });

  it("hides the arrows from assistive technology", () => {
    const { container } = render(<StatusFlow ticket={ticket()} />);
    const arrows = container.querySelectorAll("[data-connector]");
    expect(arrows).toHaveLength(3);
    arrows.forEach((a) => expect(a).toHaveAttribute("aria-hidden", "true"));
  });

  it("shows Final state for closed tickets", () => {
    render(<StatusFlow ticket={ticket({ status: "CLOSED", allowedNextStatus: null })} />);
    expect(screen.getByText("Final state")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")[3]).toHaveAttribute("aria-current", "step");
  });

  it("moves the current marker when the status changes", () => {
    const { rerender } = render(<StatusFlow ticket={ticket()} />);
    expect(screen.getAllByRole("listitem")[0]).toHaveAttribute("aria-current", "step");
    rerender(<StatusFlow ticket={ticket({ status: "IN_PROGRESS", history: [toInProgress] })} />);
    expect(screen.getAllByRole("listitem")[1]).toHaveAttribute("aria-current", "step");
    expect(screen.getAllByRole("listitem")[0]).not.toHaveAttribute("aria-current");
  });
});
