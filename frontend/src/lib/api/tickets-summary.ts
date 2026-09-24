"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketListRoot } from "./tickets";
import type { TicketSummaryCounts } from "./types";

/**
 * Counters for the navigation badge. The key sits under ["tickets"], so every ticket mutation
 * (which invalidates that prefix) refreshes the count too.
 */
export function useTicketSummary() {
  return useQuery({
    queryKey: [...ticketListRoot, "summary"],
    queryFn: async () => (await apiFetch<TicketSummaryCounts>("/tickets/summary")).data,
    staleTime: 15_000,
    refetchOnWindowFocus: true,
  });
}
