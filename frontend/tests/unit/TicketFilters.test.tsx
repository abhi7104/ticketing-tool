import { act, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SEARCH_DEBOUNCE_MS, TicketFilters } from "@/components/tickets/TicketFilters";
import type { TicketListParams } from "@/lib/api/tickets-list";

const base: TicketListParams = { q: "", status: null, view: "mine", page: 0 };

describe("TicketFilters", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("debounces search input", () => {
    const onChange = vi.fn();
    render(<TicketFilters params={base} onChange={onChange} />);
    const input = screen.getByRole("searchbox", { name: "Search tickets" });

    fireEvent.change(input, { target: { value: "pri" } });
    fireEvent.change(input, { target: { value: "printer" } });
    act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS - 1));
    expect(onChange).not.toHaveBeenCalled();
    act(() => vi.advanceTimersByTime(1));
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith({ q: "printer" });
  });

  it("limits search to 100 characters", () => {
    render(<TicketFilters params={base} onChange={vi.fn()} />);
    expect(screen.getByRole("searchbox")).toHaveAttribute("maxLength", "100");
  });

  it("status chips and view toggle report changes and show pressed state", () => {
    const onChange = vi.fn();
    render(<TicketFilters params={{ ...base, status: "RESOLVED" }} onChange={onChange} />);

    expect(screen.getByRole("button", { name: "Resolved" })).toHaveAttribute("aria-pressed", "true");
    expect(screen.getByRole("button", { name: "My tickets" })).toHaveAttribute("aria-pressed", "true");

    fireEvent.click(screen.getByRole("button", { name: "In progress" }));
    expect(onChange).toHaveBeenLastCalledWith({ status: "IN_PROGRESS" });
    fireEvent.click(screen.getByRole("button", { name: "All" }));
    expect(onChange).toHaveBeenLastCalledWith({ status: null });
    fireEvent.click(screen.getByRole("button", { name: "All tickets" }));
    expect(onChange).toHaveBeenLastCalledWith({ view: "all" });
  });

  it("offers three views in order and reports the assigned view", () => {
    const onChange = vi.fn();
    render(<TicketFilters params={{ ...base, view: "assigned" }} onChange={onChange} />);
    const group = screen.getByRole("group", { name: "Which tickets" });
    const labels = Array.from(group.querySelectorAll("button")).map((b) => b.textContent);
    expect(labels).toEqual(["My tickets", "Assigned to me", "All tickets"]);
    expect(screen.getByRole("button", { name: "Assigned to me" })).toHaveAttribute("aria-pressed", "true");
    fireEvent.click(screen.getByRole("button", { name: "My tickets" }));
    expect(onChange).toHaveBeenLastCalledWith({ view: "mine" });
  });

  it("clear button empties the search", () => {
    const onChange = vi.fn();
    render(<TicketFilters params={{ ...base, q: "vpn" }} onChange={onChange} />);
    fireEvent.click(screen.getByRole("button", { name: "Clear search" }));
    act(() => vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS));
    expect(onChange).toHaveBeenCalledWith({ q: "" });
  });
});
