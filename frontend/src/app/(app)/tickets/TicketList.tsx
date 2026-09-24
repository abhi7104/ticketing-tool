"use client";

import { CheckCircle2, Inbox, Plus, SearchX } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef } from "react";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { EmptyState } from "@/components/common/EmptyState";
import { Pagination } from "@/components/common/Pagination";
import { TicketFilters } from "@/components/tickets/TicketFilters";
import { TicketTable, TicketTableSkeleton } from "@/components/tickets/TicketTable";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useTicketList, type TicketView } from "@/lib/api/tickets-list";
import { useTicketListParams } from "@/lib/hooks/useTicketListParams";

const SUBTITLES: Record<TicketView, string> = {
  mine: "Tickets you raised.",
  assigned: "Tickets waiting on you, most urgent first.",
  all: "Every ticket in the system.",
};

export function TicketList() {
  const { params, update, clearFilters } = useTicketListParams();
  const list = useTicketList(params);
  const alert = useAlert();
  const reportedError = useRef<unknown>(null);

  useEffect(() => {
    if (list.error && list.error !== reportedError.current) {
      reportedError.current = list.error;
      void alert.showError(list.error, { onRetry: () => void list.refetch() });
    }
  }, [alert, list]);

  const filtered = params.q.trim() !== "" || params.status !== null;
  const data = list.data;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Tickets</h1>
          <p className="mt-1 text-sm text-muted-foreground">{SUBTITLES[params.view]}</p>
        </div>
      </div>

      <TicketFilters params={params} onChange={update} />

      <Card className="overflow-hidden" aria-busy={list.isFetching}>
        {list.isPending ? (
          <TicketTableSkeleton />
        ) : !data || data.items.length === 0 ? (
          filtered ? (
            <EmptyState
              icon={SearchX}
              title="No tickets match"
              message={
                params.view === "assigned" && (params.status === "RESOLVED" || params.status === "CLOSED")
                  ? "Resolved and closed tickets aren't in your queue. Try All tickets."
                  : "Try a different keyword or status."
              }
              action={
                <Button variant="secondary" onClick={clearFilters}>
                  Clear filters
                </Button>
              }
            />
          ) : params.view === "assigned" ? (
            <EmptyState
              icon={CheckCircle2}
              title="Nothing waiting on you"
              message="Tickets assigned to you that are open or in progress will show up here."
              action={
                <Link href="/tickets?view=all" className={buttonVariants({ variant: "secondary" })}>
                  View all tickets
                </Link>
              }
            />
          ) : (
            <EmptyState
              icon={Inbox}
              title="No tickets yet"
              message={
                params.view === "mine"
                  ? "You haven't raised any tickets. Create your first one."
                  : "Nobody has raised a ticket yet."
              }
              action={
                <Link href="/tickets/new" className={buttonVariants()}>
                  <Plus aria-hidden />
                  Create ticket
                </Link>
              }
            />
          )
        ) : (
          <div className={list.isPlaceholderData ? "opacity-60 transition-opacity" : "transition-opacity"}>
            <TicketTable tickets={data.items} />
            <div className="border-t border-border">
              <Pagination
                page={data.page}
                size={data.size}
                totalItems={data.totalItems}
                totalPages={data.totalPages}
                onPageChange={(page) => update({ page })}
              />
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
