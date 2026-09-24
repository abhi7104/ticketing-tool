"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback, useMemo } from "react";
import type { TicketListParams, TicketView } from "@/lib/api/tickets-list";
import { TICKET_STATUSES, type TicketStatus } from "@/lib/api/types";

const VIEWS: TicketView[] = ["mine", "assigned", "all"];

function parseView(value: string | null): TicketView {
  return VIEWS.includes(value as TicketView) ? (value as TicketView) : "mine";
}

/** Keeps listing filters in the URL so they survive refresh, back navigation and sharing. */
export function useTicketListParams() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const params = useMemo<TicketListParams>(() => {
    const status = searchParams.get("status");
    const page = Number(searchParams.get("page") ?? "0");
    return {
      q: (searchParams.get("q") ?? "").slice(0, 100),
      status: TICKET_STATUSES.includes(status as TicketStatus) ? (status as TicketStatus) : null,
      view: parseView(searchParams.get("view")),
      page: Number.isInteger(page) && page > 0 ? page : 0,
    };
  }, [searchParams]);

  const update = useCallback(
    (changes: Partial<TicketListParams>) => {
      const next = { ...params, ...changes };
      // Any filter change goes back to the first page.
      if (!("page" in changes)) next.page = 0;
      const search = new URLSearchParams();
      if (next.q.trim()) search.set("q", next.q.trim());
      if (next.status) search.set("status", next.status);
      if (next.view !== "mine") search.set("view", next.view);
      if (next.page > 0) search.set("page", String(next.page));
      const query = search.toString();
      router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
    },
    [params, pathname, router],
  );

  const clearFilters = useCallback(
    () => update({ q: "", status: null }),
    [update],
  );

  return { params, update, clearFilters };
}
