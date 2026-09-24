"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketKey, ticketListRoot, toTicketWithEtag } from "./tickets";
import type { TicketDetail, UpdateTicketRequest } from "./types";

export function useUpdateTicket(key: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ changes, etag }: { changes: UpdateTicketRequest; etag: string }) => {
      const res = await apiFetch<TicketDetail>(`/tickets/${encodeURIComponent(key)}`, {
        method: "PATCH",
        body: changes,
        ifMatch: etag,
      });
      return toTicketWithEtag(res.data, res.etag);
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(ticketKey(key), updated);
      void queryClient.invalidateQueries({ queryKey: ticketListRoot });
    },
  });
}
