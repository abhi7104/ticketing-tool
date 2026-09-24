"use client";

import { useRouter } from "next/navigation";
import type { KeyboardEvent } from "react";
import { Skeleton } from "@/components/ui/card";
import type { TicketSummary } from "@/lib/api/types";
import { formatDate, formatRelative } from "@/lib/format";
import { PriorityBadge } from "./PriorityBadge";
import { StatusBadge } from "./StatusBadge";

interface Props {
  tickets: TicketSummary[];
}

export function TicketTable({ tickets }: Props) {
  const router = useRouter();
  const open = (key: string) => router.push(`/tickets/${key}`);
  const onKey = (e: KeyboardEvent, key: string) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      open(key);
    }
  };

  return (
    <>
      {/* Desktop */}
      <table className="hidden w-full text-sm md:table">
        <thead>
          <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
            <th scope="col" className="px-4 py-3 font-medium">Key</th>
            <th scope="col" className="px-4 py-3 font-medium">Title</th>
            <th scope="col" className="px-4 py-3 font-medium">Status</th>
            <th scope="col" className="px-4 py-3 font-medium">Priority</th>
            <th scope="col" className="px-4 py-3 font-medium">Assignee</th>
            <th scope="col" className="px-4 py-3 text-right font-medium">Updated</th>
          </tr>
        </thead>
        <tbody>
          {tickets.map((t) => (
            <tr
              key={t.key}
              tabIndex={0}
              role="link"
              aria-label={`${t.key}: ${t.title}`}
              onClick={() => open(t.key)}
              onKeyDown={(e) => onKey(e, t.key)}
              className="cursor-pointer border-b border-border transition-colors last:border-0 hover:bg-muted/60 focus-visible:bg-muted focus-visible:outline-none"
            >
              <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-muted-foreground">{t.key}</td>
              <td className="max-w-md truncate px-4 py-3 font-medium">{t.title}</td>
              <td className="px-4 py-3"><StatusBadge status={t.status} /></td>
              <td className="px-4 py-3"><PriorityBadge priority={t.priority} /></td>
              <td className="whitespace-nowrap px-4 py-3">{t.assignee.displayName}</td>
              <td className="whitespace-nowrap px-4 py-3 text-right text-muted-foreground">
                <time dateTime={t.updatedAt} title={formatDate(t.updatedAt)}>
                  {formatRelative(t.updatedAt)}
                </time>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Mobile */}
      <ul className="divide-y divide-border md:hidden">
        {tickets.map((t) => (
          <li key={t.key}>
            <button
              type="button"
              onClick={() => open(t.key)}
              className="w-full space-y-2 px-4 py-3 text-left transition-colors hover:bg-muted/60 focus-visible:bg-muted focus-visible:outline-none"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="font-mono text-xs text-muted-foreground">{t.key}</span>
                <StatusBadge status={t.status} />
              </div>
              <p className="font-medium">{t.title}</p>
              <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
                <PriorityBadge priority={t.priority} />
                <span>
                  {t.assignee.displayName} · <time dateTime={t.updatedAt}>{formatRelative(t.updatedAt)}</time>
                </span>
              </div>
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}

export function TicketTableSkeleton({ rows = 6 }: { rows?: number }) {
  return (
    <div className="divide-y divide-border" aria-label="Loading tickets" role="status">
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="flex items-center gap-4 px-4 py-4">
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-4 flex-1" />
          <Skeleton className="hidden h-5 w-20 rounded-full sm:block" />
          <Skeleton className="hidden h-4 w-24 md:block" />
        </div>
      ))}
    </div>
  );
}
