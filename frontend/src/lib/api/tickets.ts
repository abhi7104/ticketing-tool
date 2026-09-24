"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "./client";
import type { TicketDetail } from "./types";

export interface TicketWithEtag {
  ticket: TicketDetail;
  etag: string;
}

export const ticketKey = (key: string) => ["ticket", key] as const;
export const ticketListRoot = ["tickets"] as const;

export function toTicketWithEtag(data: TicketDetail, etag: string | null): TicketWithEtag {
  return { ticket: data, etag: etag ?? `"${data.version}"` };
}

export function useTicket(key: string) {
  return useQuery({
    queryKey: ticketKey(key),
    queryFn: async () => {
      const res = await apiFetch<TicketDetail>(`/tickets/${encodeURIComponent(key)}`);
      return toTicketWithEtag(res.data, res.etag);
    },
  });
}
