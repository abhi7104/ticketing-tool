"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "./client";
import { ticketKey, ticketListRoot, type TicketWithEtag } from "./tickets";
import type { Comment, CurrentUser } from "./types";

/** Posts a comment, showing it immediately and rolling back if the server rejects it. */
export function useAddComment(key: string, author: CurrentUser | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (body: string) =>
      (
        await apiFetch<Comment>(`/tickets/${encodeURIComponent(key)}/comments`, {
          method: "POST",
          body: { body },
        })
      ).data,
    onMutate: async (body) => {
      await queryClient.cancelQueries({ queryKey: ticketKey(key) });
      const previous = queryClient.getQueryData<TicketWithEtag>(ticketKey(key));
      if (previous && author) {
        const optimistic: Comment = {
          id: -Date.now(),
          author: { id: author.id, displayName: author.displayName, email: author.email },
          body,
          createdAt: new Date().toISOString(),
        };
        queryClient.setQueryData<TicketWithEtag>(ticketKey(key), {
          ...previous,
          ticket: { ...previous.ticket, comments: [...previous.ticket.comments, optimistic] },
        });
      }
      return { previous };
    },
    onError: (_error, _body, context) => {
      if (context?.previous) queryClient.setQueryData(ticketKey(key), context.previous);
    },
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ticketKey(key) });
      void queryClient.invalidateQueries({ queryKey: ticketListRoot });
    },
  });
}
