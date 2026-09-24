"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketListRoot } from "./tickets";
import type { TicketPage, TicketStatus } from "./types";

export type TicketView = "mine" | "assigned" | "all";

export interface TicketListParams {
  q: string;
  status: TicketStatus | null;
  view: TicketView;
  page: number;
}

export const PAGE_SIZE = 20;

export function useTicketList(params: TicketListParams) {
  return useQuery({
    queryKey: [...ticketListRoot, params],
    queryFn: async ({ signal }) => {
      const search = new URLSearchParams({
        view: params.view,
        page: String(params.page),
        size: String(PAGE_SIZE),
      });
      if (params.q.trim()) search.set("q", params.q.trim());
      if (params.status) search.set("status", params.status);
      return (await apiFetch<TicketPage>(`/tickets?${search}`, { signal })).data;
    },
    placeholderData: keepPreviousData,
  });
}
