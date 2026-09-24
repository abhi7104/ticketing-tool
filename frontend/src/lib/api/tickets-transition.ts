"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketKey, ticketListRoot, toTicketWithEtag } from "./tickets";
import type { TicketDetail, TicketStatus } from "./types";

export function useTransitionTicket(key: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ target, etag }: { target: TicketStatus; etag: string }) => {
      const res = await apiFetch<TicketDetail>(
        `/tickets/${encodeURIComponent(key)}/transitions`,
        { method: "POST", body: { targetStatus: target }, ifMatch: etag },
      );
      return toTicketWithEtag(res.data, res.etag);
    },
    onSuccess: (updated) => {
      queryClient.setQueryData(ticketKey(key), updated);
      void queryClient.invalidateQueries({ queryKey: ticketListRoot });
    },
  });
}
