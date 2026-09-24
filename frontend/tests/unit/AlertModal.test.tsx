import { act, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { ApiError } from "@/lib/api/client";
import { renderWithProviders } from "./render";

let api: ReturnType<typeof useAlert>;
function Capture() {
  api = useAlert();
  return <button>trigger</button>;
}

describe("AlertModal", () => {
  it("renders an accessible alert dialog and closes with Esc", async () => {
    renderWithProviders(<Capture />);
    let result: Promise<string | null>;
    act(() => {
      result = api.showAlert({ title: "Heads up", message: "Something happened", variant: "info", actions: [{ id: "ok", label: "OK" }] });
    });
    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveAccessibleName("Heads up");
    expect(dialog).toHaveAccessibleDescription("Something happened");
    await waitFor(() => expect(screen.getByRole("button", { name: "OK" })).toHaveFocus());
    await userEvent.keyboard("{Escape}");
    await expect(result!).resolves.toBeNull();
    expect(screen.queryByRole("alertdialog")).not.toBeInTheDocument();
  });

  it("shows queued alerts one after another", async () => {
    renderWithProviders(<Capture />);
    act(() => {
      void api.showAlert({ title: "First", variant: "info", actions: [{ id: "ok", label: "OK" }] });
      void api.showAlert({ title: "Second", variant: "info", actions: [{ id: "ok", label: "OK" }] });
    });
    expect(await screen.findByRole("alertdialog")).toHaveAccessibleName("First");
    await userEvent.click(screen.getByRole("button", { name: "OK" }));
    expect(await screen.findByRole("alertdialog")).toHaveAccessibleName("Second");
  });

  it("maps API errors to friendly content with a list of problems", async () => {
    renderWithProviders(<Capture />);
    act(() => {
      void api.showError(
        new ApiError({
          status: 400,
          code: "VALIDATION_FAILED",
          title: "Please correct the highlighted fields",
          detail: "2 fields need attention.",
          fieldErrors: [
            { field: "title", message: "Title is required." },
            { field: "priority", message: "Please choose a priority." },
          ],
        }),
      );
    });
    const dialog = await screen.findByRole("alertdialog");
    expect(dialog).toHaveTextContent("Title is required.");
    expect(dialog).toHaveTextContent("Please choose a priority.");
    expect(dialog).not.toHaveTextContent("VALIDATION_FAILED");
  });

  it("offers Retry for network failures and calls the handler", async () => {
    renderWithProviders(<Capture />);
    let retried = false;
    act(() => {
      void api.showError(
        new ApiError({ status: 0, code: "NETWORK_ERROR", title: "x", detail: "y" }),
        { onRetry: () => (retried = true) },
      );
    });
    await userEvent.click(await screen.findByRole("button", { name: "Retry" }));
    expect(retried).toBe(true);
  });

  it("confirm resolves true only when confirmed", async () => {
    renderWithProviders(<Capture />);
    let answer: Promise<boolean>;
    act(() => {
      answer = api.confirm({ title: "Close?", message: "Final", confirmLabel: "Close ticket", destructive: true });
    });
    await userEvent.click(await screen.findByRole("button", { name: "Close ticket" }));
    await expect(answer!).resolves.toBe(true);
  });
});
