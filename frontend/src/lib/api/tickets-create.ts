"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketKey, ticketListRoot, toTicketWithEtag } from "./tickets";
import type { CreateTicketRequest, TicketDetail } from "./types";

export function useCreateTicket() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: CreateTicketRequest) => {
      const res = await apiFetch<TicketDetail>("/tickets", { method: "POST", body });
      return toTicketWithEtag(res.data, res.etag);
    },
    onSuccess: (created) => {
      queryClient.setQueryData(ticketKey(created.ticket.key), created);
      void queryClient.invalidateQueries({ queryKey: ticketListRoot });
    },
  });
}
