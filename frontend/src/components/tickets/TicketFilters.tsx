"use client";

import { Search, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Input } from "@/components/ui/field";
import type { TicketListParams, TicketView } from "@/lib/api/tickets-list";
import { TICKET_STATUSES, type TicketStatus } from "@/lib/api/types";
import { STATUS_LABELS } from "@/lib/labels";
import { LIMITS } from "@/lib/validation/schemas";
import { cn } from "@/lib/utils";

export const SEARCH_DEBOUNCE_MS = 300;

interface Props {
  params: TicketListParams;
  onChange: (changes: Partial<TicketListParams>) => void;
}

const chip =
  "inline-flex h-8 items-center rounded-full border px-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring";

export function TicketFilters({ params, onChange }: Props) {
  const [search, setSearch] = useState(params.q);
  const lastSent = useRef(params.q);

  // Follow external URL changes (back button, "Clear filters").
  useEffect(() => {
    if (params.q !== lastSent.current) {
      lastSent.current = params.q;
      setSearch(params.q);
    }
  }, [params.q]);

  useEffect(() => {
    if (search === lastSent.current) return;
    const timer = setTimeout(() => {
      lastSent.current = search;
      onChange({ q: search });
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [search, onChange]);

  const views: { value: TicketView; label: string }[] = [
    { value: "mine", label: "My tickets" },
    { value: "assigned", label: "Assigned to me" },
    { value: "all", label: "All tickets" },
  ];
  const statuses: (TicketStatus | null)[] = [null, ...TICKET_STATUSES];

  return (
    <div className="space-y-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <div role="group" aria-label="Which tickets" className="inline-flex rounded-lg border border-border bg-muted p-1">
          {views.map((v) => (
            <button
              key={v.value}
              type="button"
              aria-pressed={params.view === v.value}
              onClick={() => onChange({ view: v.value })}
              className={cn(
                "rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
                params.view === v.value
                  ? "bg-surface text-foreground shadow-sm"
                  : "text-muted-foreground hover:text-foreground",
              )}
            >
              {v.label}
            </button>
          ))}
        </div>
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" aria-hidden />
          <Input
            type="search"
            aria-label="Search tickets"
            placeholder="Search by key, title or description"
            maxLength={LIMITS.search.max}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 pr-9 [&::-webkit-search-cancel-button]:hidden"
          />
          {search && (
            <button
              type="button"
              onClick={() => setSearch("")}
              aria-label="Clear search"
              className="absolute right-2 top-1/2 flex size-6 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground"
            >
              <X className="size-4" aria-hidden />
            </button>
          )}
        </div>
      </div>
      <div role="group" aria-label="Filter by status" className="flex flex-wrap gap-2">
        {statuses.map((s) => {
          const active = params.status === s;
          return (
            <button
              key={s ?? "ALL"}
              type="button"
              aria-pressed={active}
              onClick={() => onChange({ status: s })}
              className={cn(
                chip,
                active
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border bg-surface text-muted-foreground hover:text-foreground",
              )}
            >
              {s ? STATUS_LABELS[s] : "All"}
            </button>
          );
        })}
      </div>
    </div>
  );
}
