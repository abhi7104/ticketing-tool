import { isApiError } from "@/lib/api/client";
import { STATUS_LABELS } from "@/lib/labels";

export type AlertVariant = "error" | "warning" | "info";

/** Which follow-up the modal should offer, if any. */
export type AlertIntent = "dismiss" | "retry" | "reload" | "signin" | "back";

export interface AlertContent {
  title: string;
  message?: string;
  items?: string[];
  variant: AlertVariant;
  intent: AlertIntent;
}

const GENERIC: AlertContent = {
  title: "Something went wrong",
  message: "Please try again.",
  variant: "error",
  intent: "dismiss",
};

/**
 * Turns any error (API problem, network failure, client validation) into user-facing modal
 * content. Raw payloads, stack traces and technical messages are never shown.
 */
export function problemToAlert(error: unknown): AlertContent {
  if (!isApiError(error)) return GENERIC;

  switch (error.code) {
    case "VALIDATION_FAILED":
      return {
        title: error.title || "Please correct the highlighted fields",
        items: error.fieldErrors.length
          ? error.fieldErrors.map((f) => f.message)
          : [error.detail],
        variant: "warning",
        intent: "dismiss",
      };
    case "INVALID_CREDENTIALS":
      return {
        title: "Sign-in failed",
        message: "Invalid username or password.",
        variant: "error",
        intent: "dismiss",
      };
    case "UNAUTHENTICATED":
      return {
        title: "Session expired",
        message: "Please sign in again.",
        variant: "info",
        intent: "signin",
      };
    case "FORBIDDEN":
      return {
        title: "Request blocked",
        message: "Your security token expired. Reload the page and try again.",
        variant: "warning",
        intent: "reload",
      };
    case "TICKET_NOT_FOUND":
    case "NOT_FOUND":
      return {
        title: "Ticket not found",
        message: error.detail,
        variant: "info",
        intent: "back",
      };
    case "INVALID_STATUS_TRANSITION": {
      const current = error.currentStatus ? STATUS_LABELS[error.currentStatus] : undefined;
      const next = error.allowedNextStatus ? STATUS_LABELS[error.allowedNextStatus] : undefined;
      const fallback = current
        ? next
          ? `This ticket is ${current}. It can only move to ${next}.`
          : `This ticket is ${current} and can no longer change status.`
        : "That status change isn't allowed.";
      return {
        title: "Status change not allowed",
        message: error.detail || fallback,
        variant: "warning",
        intent: "dismiss",
      };
    }
    case "TICKET_CLOSED":
      return {
        title: "Ticket is closed",
        message: "Closed tickets can't be changed.",
        variant: "info",
        intent: "dismiss",
      };
    case "VERSION_CONFLICT":
    case "PRECONDITION_REQUIRED":
      return {
        title: "Ticket was updated by someone else",
        message: "Reload to see the latest version. Your edits are kept in the form until you do.",
        variant: "warning",
        intent: "reload",
      };
    case "NETWORK_ERROR":
    case "TIMEOUT":
      return {
        title: "Can't reach the server",
        message: "Check your connection and try again. Your input has been kept.",
        variant: "error",
        intent: "retry",
      };
    default:
      return {
        ...GENERIC,
        message: error.traceId
          ? `Please try again. Reference: ${error.traceId}`
          : GENERIC.message,
      };
  }
}

/** Modal content for problems found by client-side validation before any request is sent. */
export function validationAlert(messages: string[]): AlertContent {
  return {
    title: "Please check your input",
    items: messages,
    variant: "warning",
    intent: "dismiss",
  };
}
