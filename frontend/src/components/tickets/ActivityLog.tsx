import { History } from "lucide-react";
import type { HistoryEntry, Priority, TicketStatus } from "@/lib/api/types";
import { formatDate, formatRelative } from "@/lib/format";
import { PRIORITY_LABELS, STATUS_LABELS } from "@/lib/labels";

function describe(entry: HistoryEntry): string {
  const who = entry.actor.displayName;
  switch (entry.changeType) {
    case "CREATED":
      return `${who} created the ticket`;
    case "STATUS_CHANGED":
      return `${who} changed status from ${STATUS_LABELS[entry.oldValue as TicketStatus] ?? entry.oldValue} to ${STATUS_LABELS[entry.newValue as TicketStatus] ?? entry.newValue}`;
    default:
      switch (entry.field) {
        case "priority":
          return `${who} changed priority from ${PRIORITY_LABELS[entry.oldValue as Priority] ?? entry.oldValue} to ${PRIORITY_LABELS[entry.newValue as Priority] ?? entry.newValue}`;
        case "assignee":
          return `${who} reassigned the ticket from ${entry.oldValue} to ${entry.newValue}`;
        case "title":
          return `${who} renamed the ticket from “${entry.oldValue}” to “${entry.newValue}”`;
        case "description":
          return `${who} updated the description`;
        default:
          return `${who} updated ${entry.field ?? "the ticket"}`;
      }
  }
}

export function ActivityLog({ history }: { history: HistoryEntry[] }) {
  return (
    <ol className="space-y-4" aria-label="Activity">
      {history.map((entry) => (
        <li key={entry.id} className="flex gap-3 text-sm">
          <span className="mt-0.5 flex size-6 shrink-0 items-center justify-center rounded-full bg-muted">
            <History className="size-3.5 text-muted-foreground" aria-hidden />
          </span>
          <div>
            <p>{describe(entry)}</p>
            <time dateTime={entry.occurredAt} title={formatDate(entry.occurredAt)} className="text-xs text-muted-foreground">
              {formatRelative(entry.occurredAt)}
            </time>
          </div>
        </li>
      ))}
    </ol>
  );
}
