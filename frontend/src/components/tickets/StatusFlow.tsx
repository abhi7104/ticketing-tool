import { Check, ChevronDown, ChevronRight, Circle, CircleDot, Lock } from "lucide-react";
import { Fragment } from "react";
import type { TicketDetail } from "@/lib/api/types";
import { formatDate } from "@/lib/format";
import { buildSteps, type LifecycleStep } from "@/lib/status-flow";
import { cn } from "@/lib/utils";

const NODE: Record<LifecycleStep["state"], string> = {
  done: "border-success/40 bg-success/10 text-foreground",
  current: "status-current border-primary bg-primary text-primary-foreground shadow-md shadow-primary/25",
  upcoming: "border-dashed border-border bg-surface text-muted-foreground",
};

function StepIcon({ step }: { step: LifecycleStep }) {
  if (step.state === "done") return <Check className="size-4 text-success" aria-hidden />;
  if (step.state === "upcoming") return <Circle className="size-4" aria-hidden />;
  return step.final ? <Lock className="size-4" aria-hidden /> : <CircleDot className="size-4" aria-hidden />;
}

function srState(step: LifecycleStep): string {
  if (step.state === "done") return `, done, reached ${formatDate(step.reachedAt)}`;
  if (step.state === "current") return step.final ? ", current step, final state" : ", current step";
  return ", upcoming";
}

function Connector({ filled }: { filled: boolean }) {
  return (
    <span
      data-connector
      aria-hidden="true"
      className={cn(
        "status-connector flex shrink-0 items-center justify-center",
        filled ? "text-success" : "text-muted-foreground/60",
      )}
    >
      {/* Arrow to the right on wider screens, downward on phones. */}
      <span className="hidden items-center sm:flex">
        <span className={cn("h-0.5 w-6 lg:w-10", filled ? "bg-success" : "border-t-2 border-dashed border-muted-foreground/40")} />
        <ChevronRight className="-ml-1.5 size-5" />
      </span>
      <ChevronDown className="size-5 sm:hidden" />
    </span>
  );
}

/**
 * Arrow-style lifecycle diagram (Open → In progress → Resolved → Closed). Informational only:
 * status changes still go through the status action button and the backend state machine.
 */
export function StatusFlow({ ticket }: { ticket: TicketDetail }) {
  const steps = buildSteps(ticket);
  return (
    <ol
      aria-label="Ticket lifecycle"
      className="flex flex-col items-stretch gap-1 rounded-xl border border-border bg-surface p-3 shadow-sm sm:flex-row sm:items-center sm:gap-0 sm:p-4"
    >
      {steps.map((step, index) => (
        <Fragment key={step.status}>
          <li
            aria-current={step.state === "current" ? "step" : undefined}
            className={cn(
              "status-node flex min-w-0 flex-1 items-center gap-2 rounded-lg border px-3 py-2 transition-colors duration-200",
              NODE[step.state],
            )}
          >
            <span
              className={cn(
                "flex size-7 shrink-0 items-center justify-center rounded-full",
                step.state === "current" ? "bg-primary-foreground/15" : "bg-muted",
              )}
            >
              <StepIcon step={step} />
            </span>
            <span className="min-w-0">
              <span data-step-label className="block truncate text-sm font-medium">
                {step.label}
              </span>
              <span
                className={cn(
                  "block truncate text-xs",
                  step.state === "current" ? "text-primary-foreground/80" : "text-muted-foreground",
                )}
                aria-hidden
              >
                {step.final
                  ? "Final state"
                  : step.state === "upcoming"
                    ? "Upcoming"
                    : step.state === "current"
                      ? "Current"
                      : formatDate(step.reachedAt)}
              </span>
              <span className="sr-only">{srState(step)}</span>
            </span>
          </li>
          {index < steps.length - 1 && <Connector filled={step.state === "done"} />}
        </Fragment>
      ))}
    </ol>
  );
}
