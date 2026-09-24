"use client";

import * as AlertDialog from "@radix-ui/react-alert-dialog";
import { AlertTriangle, Info, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { AlertVariant } from "@/lib/errors/problem-to-alert";
import { cn } from "@/lib/utils";

export interface AlertAction {
  id: string;
  label: string;
  variant?: "primary" | "secondary" | "destructive";
}

export interface AlertModalProps {
  open: boolean;
  title: string;
  message?: string;
  items?: string[];
  variant: AlertVariant;
  actions: AlertAction[];
  onAction: (id: string | null) => void;
}

const ICONS = {
  error: { Icon: XCircle, className: "bg-danger/10 text-danger" },
  warning: { Icon: AlertTriangle, className: "bg-warning/15 text-warning" },
  info: { Icon: Info, className: "bg-primary/10 text-primary" },
} as const;

/**
 * The single pop-up used for every error and confirmation. Built on Radix AlertDialog:
 * focus is trapped, Esc dismisses, focus returns to the trigger, role="alertdialog".
 */
export function AlertModal({ open, title, message, items, variant, actions, onAction }: AlertModalProps) {
  const { Icon, className } = ICONS[variant];
  return (
    <AlertDialog.Root open={open} onOpenChange={(next) => !next && onAction(null)}>
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="modal-overlay fixed inset-0 z-50 bg-black/40 backdrop-blur-[2px]" />
        <AlertDialog.Content
          className="modal-content fixed left-1/2 top-1/2 z-50 w-[calc(100vw-2rem)] max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-border bg-surface p-6 shadow-2xl focus:outline-none"
        >
          <div className="flex gap-4">
            <div className={cn("flex size-10 shrink-0 items-center justify-center rounded-full", className)}>
              <Icon className="size-5" aria-hidden />
            </div>
            <div className="min-w-0 flex-1 space-y-2">
              <AlertDialog.Title className="text-base font-semibold text-foreground">
                {title}
              </AlertDialog.Title>
              <AlertDialog.Description asChild>
                <div className="space-y-2 text-sm text-muted-foreground">
                  {message && <p>{message}</p>}
                  {items && items.length > 0 && (
                    <ul className="list-disc space-y-1 pl-5">
                      {items.map((item, i) => (
                        <li key={i}>{item}</li>
                      ))}
                    </ul>
                  )}
                </div>
              </AlertDialog.Description>
            </div>
          </div>
          <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            {actions.map((action, index) => {
              const isLast = index === actions.length - 1;
              const button = (
                <Button
                  key={action.id}
                  variant={action.variant ?? (isLast ? "primary" : "secondary")}
                  onClick={() => onAction(action.id)}
                >
                  {action.label}
                </Button>
              );
              // Radix focuses the Cancel element on open, so a lone button must be the Cancel.
              return isLast && actions.length > 1 ? (
                <AlertDialog.Action key={action.id} asChild>
                  {button}
                </AlertDialog.Action>
              ) : (
                <AlertDialog.Cancel key={action.id} asChild>
                  {button}
                </AlertDialog.Cancel>
              );
            })}
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
