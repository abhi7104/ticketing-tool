"use client";

import { ArrowRight } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { Button } from "@/components/ui/button";
import { ticketKey } from "@/lib/api/tickets";
import { useTransitionTicket } from "@/lib/api/tickets-transition";
import type { TicketDetail } from "@/lib/api/types";
import { STATUS_LABELS, TRANSITION_ACTION_LABELS } from "@/lib/labels";

interface Props {
  ticket: TicketDetail;
  etag: string;
}

/** Offers only the single next status the backend allows; nothing once closed. */
export function StatusActionButton({ ticket, etag }: Props) {
  const alert = useAlert();
  const queryClient = useQueryClient();
  const transition = useTransitionTicket(ticket.key);
  const next = ticket.allowedNextStatus;
  if (!next) return null;

  const run = async () => {
    if (next === "CLOSED") {
      const ok = await alert.confirm({
        title: "Close this ticket?",
        message: "Closed tickets are read-only: fields, comments and status can't change afterwards.",
        confirmLabel: "Close ticket",
        destructive: true,
      });
      if (!ok) return;
    }
    try {
      await transition.mutateAsync({ target: next, etag });
      toast.success(`Status changed to ${STATUS_LABELS[next]}`);
    } catch (error) {
      await alert.showError(error, {
        onRetry: () => void run(),
        onReload: () => void queryClient.invalidateQueries({ queryKey: ticketKey(ticket.key) }),
      });
      // Whatever went wrong, show the ticket as the server now has it.
      void queryClient.invalidateQueries({ queryKey: ticketKey(ticket.key) });
    }
  };

  return (
    <Button onClick={() => void run()} loading={transition.isPending}>
      {TRANSITION_ACTION_LABELS[next]}
      {!transition.isPending && <ArrowRight aria-hidden />}
    </Button>
  );
}
