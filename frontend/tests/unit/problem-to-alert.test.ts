import { describe, expect, it } from "vitest";
import { ApiError } from "@/lib/api/client";
import { problemToAlert, validationAlert } from "@/lib/errors/problem-to-alert";

const err = (init: Partial<ConstructorParameters<typeof ApiError>[0]> & { code: ApiError["code"] }) =>
  new ApiError({ status: 400, title: "t", detail: "d", ...init });

describe("problemToAlert", () => {
  it("lists every field message for VALIDATION_FAILED", () => {
    const a = problemToAlert(
      err({
        code: "VALIDATION_FAILED",
        title: "Please correct the highlighted fields",
        fieldErrors: [
          { field: "title", message: "Title is required." },
          { field: "assigneeId", message: "Please choose an assignee." },
        ],
      }),
    );
    expect(a.title).toBe("Please correct the highlighted fields");
    expect(a.items).toEqual(["Title is required.", "Please choose an assignee."]);
  });

  it.each([
    ["INVALID_CREDENTIALS", "Sign-in failed", "dismiss"],
    ["UNAUTHENTICATED", "Session expired", "signin"],
    ["TICKET_NOT_FOUND", "Ticket not found", "back"],
    ["INVALID_STATUS_TRANSITION", "Status change not allowed", "dismiss"],
    ["TICKET_CLOSED", "Ticket is closed", "dismiss"],
    ["VERSION_CONFLICT", "Ticket was updated by someone else", "reload"],
    ["PRECONDITION_REQUIRED", "Ticket was updated by someone else", "reload"],
    ["NETWORK_ERROR", "Can't reach the server", "retry"],
    ["TIMEOUT", "Can't reach the server", "retry"],
    ["FORBIDDEN", "Request blocked", "reload"],
    ["INTERNAL_ERROR", "Something went wrong", "dismiss"],
  ] as const)("%s → %s", (code, title, intent) => {
    const a = problemToAlert(err({ code }));
    expect(a.title).toBe(title);
    expect(a.intent).toBe(intent);
  });

  it("explains the allowed next status for invalid transitions", () => {
    const a = problemToAlert(
      err({ code: "INVALID_STATUS_TRANSITION", detail: "", currentStatus: "OPEN", allowedNextStatus: "IN_PROGRESS" }),
    );
    expect(a.message).toBe("This ticket is Open. It can only move to In progress.");
  });

  it("includes the trace reference for server errors", () => {
    const a = problemToAlert(err({ code: "INTERNAL_ERROR", traceId: "abc-123" }));
    expect(a.message).toContain("abc-123");
  });

  it("never exposes raw technical errors", () => {
    const a = problemToAlert(new TypeError("Cannot read properties of undefined (reading 'x')"));
    expect(JSON.stringify(a)).not.toMatch(/TypeError|undefined|reading/);
    expect(a.title).toBe("Something went wrong");
  });

  it("formats client validation problems", () => {
    expect(validationAlert(["A", "B"])).toMatchObject({ title: "Please check your input", items: ["A", "B"] });
  });
});
