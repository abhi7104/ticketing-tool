"use client";

import { Inbox } from "lucide-react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { buttonVariants } from "@/components/ui/button";
import { useTicketSummary } from "@/lib/api/tickets-summary";
import { cn } from "@/lib/utils";

function accessibleName(count: number): string {
  if (count <= 0) return "Assigned to me";
  return `Assigned to me, ${count} ${count === 1 ? "ticket" : "tickets"} waiting`;
}

/** Top-bar entry to the "Assigned to me" view with the number of tickets waiting. */
export function AssignedLink() {
  const summary = useTicketSummary();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const count = summary.data?.assignedPending ?? 0;
  const active = pathname === "/tickets" && searchParams.get("view") === "assigned";

  return (
    <Link
      href="/tickets?view=assigned"
      aria-label={accessibleName(count)}
      aria-current={active ? "page" : undefined}
      className={cn(
        buttonVariants({ variant: "ghost", size: "sm" }),
        "relative",
        active && "bg-muted text-foreground",
      )}
    >
      <Inbox aria-hidden />
      <span className="hidden sm:inline">Assigned to me</span>
      {count > 0 && (
        <span
          data-badge
          aria-hidden
          className="min-w-5 rounded-full bg-primary px-1.5 text-center text-xs font-semibold leading-5 text-primary-foreground"
        >
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}
