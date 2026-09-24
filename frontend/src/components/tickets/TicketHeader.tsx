import type { TicketDetail } from "@/lib/api/types";
import { formatDate } from "@/lib/format";
import { PriorityBadge } from "./PriorityBadge";
import { StatusBadge } from "./StatusBadge";

export function TicketHeader({ ticket }: { ticket: TicketDetail }) {
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="font-mono text-muted-foreground">{ticket.key}</span>
        <StatusBadge status={ticket.status} />
        <PriorityBadge priority={ticket.priority} />
      </div>
      <h1 className="break-words text-2xl font-semibold tracking-tight">{ticket.title}</h1>
      <p className="text-sm text-muted-foreground">
        Raised by <span className="font-medium text-foreground">{ticket.reporter.displayName}</span>{" "}
        on <time dateTime={ticket.createdAt}>{formatDate(ticket.createdAt)}</time>
      </p>
    </div>
  );
}

export function TicketDetails({ ticket }: { ticket: TicketDetail }) {
  const rows: [string, React.ReactNode][] = [
    ["Status", <StatusBadge key="s" status={ticket.status} />],
    ["Priority", <PriorityBadge key="p" priority={ticket.priority} />],
    ["Assignee", ticket.assignee.displayName],
    ["Reporter", ticket.reporter.displayName],
    ["Created", formatDate(ticket.createdAt)],
    ["Updated", formatDate(ticket.updatedAt)],
  ];
  if (ticket.resolvedAt) rows.push(["Resolved", formatDate(ticket.resolvedAt)]);
  if (ticket.closedAt) rows.push(["Closed", formatDate(ticket.closedAt)]);

  return (
    <dl className="divide-y divide-border text-sm">
      {rows.map(([label, value]) => (
        <div key={label} className="flex items-center justify-between gap-4 py-2.5">
          <dt className="text-muted-foreground">{label}</dt>
          <dd className="text-right font-medium">{value}</dd>
        </div>
      ))}
    </dl>
  );
}
