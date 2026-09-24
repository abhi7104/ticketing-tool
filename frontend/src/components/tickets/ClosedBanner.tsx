import { Lock } from "lucide-react";
import type { TicketDetail } from "@/lib/api/types";

export function ClosedBanner({ ticket }: { ticket: TicketDetail }) {
  if (ticket.status !== "CLOSED") return null;
  return (
    <div
      role="status"
      className="flex items-center gap-3 rounded-xl border border-border bg-muted px-4 py-3 text-sm"
    >
      <Lock className="size-4 shrink-0 text-muted-foreground" aria-hidden />
      <p>
        <span className="font-medium">This ticket is closed and read-only.</span>{" "}
        <span className="text-muted-foreground">Fields, comments and status can no longer change.</span>
      </p>
    </div>
  );
}
