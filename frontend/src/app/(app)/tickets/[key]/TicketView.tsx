"use client";

import * as Tabs from "@radix-ui/react-tabs";
import { ArrowLeft, FileQuestion } from "lucide-react";
import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { EmptyState } from "@/components/common/EmptyState";
import { ActivityLog } from "@/components/tickets/ActivityLog";
import { ClosedBanner } from "@/components/tickets/ClosedBanner";
import { CommentThread } from "@/components/tickets/CommentThread";
import { StatusActionButton } from "@/components/tickets/StatusActionButton";
import { StatusFlow } from "@/components/tickets/StatusFlow";
import { EditButton, TicketEditPanel } from "@/components/tickets/TicketEditPanel";
import { TicketDetails, TicketHeader } from "@/components/tickets/TicketHeader";
import { buttonVariants } from "@/components/ui/button";
import { Card, Skeleton } from "@/components/ui/card";
import { isApiError } from "@/lib/api/client";
import { useTicket } from "@/lib/api/tickets";

const tab =
  "border-b-2 border-transparent px-1 pb-2 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring data-[state=active]:border-primary data-[state=active]:text-foreground";

export function TicketView({ ticketKey }: { ticketKey: string }) {
  const query = useTicket(ticketKey);
  const alert = useAlert();
  const [editing, setEditing] = useState(false);
  const reported = useRef<unknown>(null);

  const notFound =
    isApiError(query.error) &&
    (query.error.code === "TICKET_NOT_FOUND" || query.error.code === "VALIDATION_FAILED");

  useEffect(() => {
    if (query.error && !notFound && query.error !== reported.current) {
      reported.current = query.error;
      void alert.showError(query.error, { onRetry: () => void query.refetch() });
    }
  }, [alert, notFound, query]);

  const back = (
    <Link
      href="/tickets"
      className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
    >
      <ArrowLeft className="size-4" aria-hidden />
      Back to tickets
    </Link>
  );

  if (notFound) {
    return (
      <Card>
        <EmptyState
          icon={FileQuestion}
          title="Ticket not found"
          message={`We couldn't find ticket ${ticketKey}. It may have a different reference.`}
          action={
            <Link href="/tickets" className={buttonVariants({ variant: "secondary" })}>
              Back to tickets
            </Link>
          }
        />
      </Card>
    );
  }

  if (!query.data) {
    return (
      <div role="status" aria-label="Loading ticket" className="space-y-4">
        {back}
        <Skeleton className="h-5 w-40" />
        <Skeleton className="h-8 w-3/4" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const { ticket, etag } = query.data;

  return (
    <div>
      {back}
      <div className="space-y-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <TicketHeader ticket={ticket} />
          <div className="flex shrink-0 flex-wrap gap-2">
            {!editing && <EditButton ticket={ticket} onEdit={() => setEditing(true)} />}
            <StatusActionButton ticket={ticket} etag={etag} />
          </div>
        </div>

        <StatusFlow ticket={ticket} />
        <ClosedBanner ticket={ticket} />
        <TicketEditPanel ticket={ticket} etag={etag} editing={editing} onEditingChange={setEditing} />

        <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
          <div className="space-y-6">
            <Card className="p-5 sm:p-6">
              <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Description
              </h2>
              <p className="whitespace-pre-wrap break-words leading-relaxed">{ticket.description}</p>
            </Card>

            <Card className="p-5 sm:p-6">
              <Tabs.Root defaultValue="comments">
                <Tabs.List className="mb-5 flex gap-6 border-b border-border" aria-label="Ticket sections">
                  <Tabs.Trigger value="comments" className={tab}>
                    Comments ({ticket.comments.length})
                  </Tabs.Trigger>
                  <Tabs.Trigger value="activity" className={tab}>
                    Activity ({ticket.history.length})
                  </Tabs.Trigger>
                </Tabs.List>
                <Tabs.Content value="comments" className="focus-visible:outline-none">
                  <CommentThread ticket={ticket} etag={etag} />
                </Tabs.Content>
                <Tabs.Content value="activity" className="focus-visible:outline-none">
                  <ActivityLog history={ticket.history} />
                </Tabs.Content>
              </Tabs.Root>
            </Card>
          </div>

          <Card className="h-fit px-5 py-3">
            <TicketDetails ticket={ticket} />
          </Card>
        </div>
      </div>
    </div>
  );
}
