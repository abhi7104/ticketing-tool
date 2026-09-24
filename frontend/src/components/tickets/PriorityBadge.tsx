import { ChevronDown, ChevronsUp, ChevronUp, Minus } from "lucide-react";
import type { Priority } from "@/lib/api/types";
import { PRIORITY_LABELS } from "@/lib/labels";
import { cn } from "@/lib/utils";

const STYLES: Record<Priority, { className: string; Icon: typeof Minus }> = {
  LOW: { className: "text-slate-600 dark:text-slate-300", Icon: ChevronDown },
  MEDIUM: { className: "text-sky-700 dark:text-sky-300", Icon: Minus },
  HIGH: { className: "text-orange-700 dark:text-orange-300", Icon: ChevronUp },
  CRITICAL: { className: "text-red-700 dark:text-red-300 font-semibold", Icon: ChevronsUp },
};

export function PriorityBadge({ priority, className }: { priority: Priority; className?: string }) {
  const { className: tone, Icon } = STYLES[priority];
  return (
    <span className={cn("inline-flex items-center gap-1 whitespace-nowrap text-xs font-medium", tone, className)}>
      <Icon className="size-4" aria-hidden />
      {PRIORITY_LABELS[priority]}
    </span>
  );
}
