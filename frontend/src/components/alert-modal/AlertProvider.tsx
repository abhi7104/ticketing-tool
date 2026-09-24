"use client";

import * as React from "react";
import {
  problemToAlert,
  validationAlert,
  type AlertContent,
  type AlertVariant,
} from "@/lib/errors/problem-to-alert";
import { AlertModal, type AlertAction } from "./AlertModal";

interface AlertRequest {
  title: string;
  message?: string;
  items?: string[];
  variant: AlertVariant;
  actions: AlertAction[];
}

export interface ErrorHandlers {
  onRetry?: () => void;
  onReload?: () => void;
  onBack?: () => void;
}

interface AlertApi {
  /** Shows a modal and resolves with the chosen action id (null when dismissed). */
  showAlert: (request: AlertRequest) => Promise<string | null>;
  /** Maps any error to friendly modal content, with retry/reload/back actions when useful. */
  showError: (error: unknown, handlers?: ErrorHandlers) => Promise<void>;
  /** Shows client-side validation problems. */
  showValidation: (messages: string[]) => Promise<void>;
  confirm: (options: {
    title: string;
    message: string;
    confirmLabel: string;
    destructive?: boolean;
  }) => Promise<boolean>;
}

const AlertContext = React.createContext<AlertApi | null>(null);

interface Pending extends AlertRequest {
  resolve: (id: string | null) => void;
}

function signInUrl(): string {
  const next = `${window.location.pathname}${window.location.search}`;
  return `/login?reason=expired&next=${encodeURIComponent(next)}`;
}

export function AlertProvider({ children }: { children: React.ReactNode }) {
  const [queue, setQueue] = React.useState<Pending[]>([]);
  const current = queue[0];

  const showAlert = React.useCallback(
    (request: AlertRequest) =>
      new Promise<string | null>((resolve) => {
        setQueue((q) => [...q, { ...request, resolve }]);
      }),
    [],
  );

  const showContent = React.useCallback(
    async (content: AlertContent, handlers: ErrorHandlers = {}) => {
      const actions: AlertAction[] = [];
      switch (content.intent) {
        case "retry":
          if (handlers.onRetry) actions.push({ id: "close", label: "Close" }, { id: "retry", label: "Retry" });
          break;
        case "reload":
          actions.push({ id: "close", label: "Cancel" }, { id: "reload", label: "Reload" });
          break;
        case "signin":
          actions.push({ id: "signin", label: "Sign in" });
          break;
        case "back":
          actions.push({ id: "back", label: "Back to tickets" });
          break;
      }
      if (actions.length === 0) actions.push({ id: "ok", label: "OK" });

      const choice = await showAlert({ ...content, actions });
      if (choice === "retry") handlers.onRetry?.();
      if (choice === "reload") (handlers.onReload ?? (() => window.location.reload()))();
      if (choice === "signin" || (content.intent === "signin" && choice === null)) {
        window.location.assign(signInUrl());
      }
      if (choice === "back") (handlers.onBack ?? (() => window.location.assign("/tickets")))();
    },
    [showAlert],
  );

  const api = React.useMemo<AlertApi>(
    () => ({
      showAlert,
      showError: (error, handlers) => showContent(problemToAlert(error), handlers),
      showValidation: (messages) => showContent(validationAlert(messages)),
      confirm: async ({ title, message, confirmLabel, destructive }) => {
        const choice = await showAlert({
          title,
          message,
          variant: destructive ? "warning" : "info",
          actions: [
            { id: "cancel", label: "Cancel" },
            { id: "confirm", label: confirmLabel, variant: destructive ? "destructive" : "primary" },
          ],
        });
        return choice === "confirm";
      },
    }),
    [showAlert, showContent],
  );

  const handleAction = (id: string | null) => {
    if (!current) return;
    const handled = current;
    handled.resolve(id);
    // Button click and Radix close both report; only the first one dequeues.
    setQueue((q) => (q[0] === handled ? q.slice(1) : q));
  };

  return (
    <AlertContext.Provider value={api}>
      {children}
      {current && (
        <AlertModal
          open
          title={current.title}
          message={current.message}
          items={current.items}
          variant={current.variant}
          actions={current.actions}
          onAction={handleAction}
        />
      )}
    </AlertContext.Provider>
  );
}

export function useAlert(): AlertApi {
  const ctx = React.useContext(AlertContext);
  if (!ctx) throw new Error("useAlert must be used inside <AlertProvider>");
  return ctx;
}
