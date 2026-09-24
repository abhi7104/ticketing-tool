import { CheckCircle2, Circle, CircleDot, Lock } from "lucide-react";
import type { TicketStatus } from "@/lib/api/types";
import { STATUS_LABELS } from "@/lib/labels";
import { cn } from "@/lib/utils";

const STYLES: Record<TicketStatus, { className: string; Icon: typeof Circle }> = {
  OPEN: { className: "bg-sky-100 text-sky-800 dark:bg-sky-500/15 dark:text-sky-300", Icon: Circle },
  IN_PROGRESS: {
    className: "bg-amber-100 text-amber-900 dark:bg-amber-500/15 dark:text-amber-300",
    Icon: CircleDot,
  },
  RESOLVED: {
    className: "bg-emerald-100 text-emerald-800 dark:bg-emerald-500/15 dark:text-emerald-300",
    Icon: CheckCircle2,
  },
  CLOSED: { className: "bg-slate-200 text-slate-700 dark:bg-slate-500/20 dark:text-slate-300", Icon: Lock },
};

export function StatusBadge({ status, className }: { status: TicketStatus; className?: string }) {
  const { className: tone, Icon } = STYLES[status];
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-medium",
        tone,
        className,
      )}
    >
      <Icon className="size-3.5" aria-hidden />
      {STATUS_LABELS[status]}
    </span>
  );
}
