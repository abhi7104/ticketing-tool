"use client";

import { Pencil } from "lucide-react";
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { ticketKey } from "@/lib/api/tickets";
import { useUpdateTicket } from "@/lib/api/tickets-update";
import type { Priority, TicketDetail, UpdateTicketRequest } from "@/lib/api/types";
import type { TicketFormValues } from "@/lib/validation/schemas";
import { TicketForm } from "./TicketForm";

interface Props {
  ticket: TicketDetail;
  etag: string;
  editing: boolean;
  onEditingChange: (editing: boolean) => void;
}

function toFormValues(ticket: TicketDetail): TicketFormValues {
  return {
    title: ticket.title,
    description: ticket.description,
    priority: ticket.priority,
    assigneeId: String(ticket.assignee.id),
  };
}

export function EditButton({ ticket, onEdit }: { ticket: TicketDetail; onEdit: () => void }) {
  if (ticket.status === "CLOSED") return null;
  return (
    <Button variant="secondary" onClick={onEdit}>
      <Pencil aria-hidden />
      Edit
    </Button>
  );
}

/** Inline editor for title, description, priority and assignee (hidden for closed tickets). */
export function TicketEditPanel({ ticket, etag, editing, onEditingChange }: Props) {
  const queryClient = useQueryClient();
  const update = useUpdateTicket(ticket.key);
  const [dirty, setDirty] = useState(false);
  // Remount the form with fresh values after a reload.
  const [formVersion, setFormVersion] = useState(0);

  useEffect(() => {
    if (!editing || !dirty) return;
    const warn = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [editing, dirty]);

  const reload = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: ticketKey(ticket.key) });
    setFormVersion((v) => v + 1);
  }, [queryClient, ticket.key]);

  if (!editing || ticket.status === "CLOSED") return null;

  return (
    <Card className="p-5 sm:p-6">
      <h2 className="mb-4 text-base font-semibold">Edit ticket</h2>
      <TicketForm
        key={`${ticket.key}-${formVersion}`}
        mode="edit"
        submitLabel="Save changes"
        defaultValues={toFormValues(ticket)}
        currentAssignee={ticket.assignee}
        onDirtyChange={setDirty}
        onReload={() => void reload()}
        onCancel={() => onEditingChange(false)}
        onSubmit={async (values, dirtyFields) => {
          const changes: UpdateTicketRequest = {};
          if (dirtyFields.includes("title")) changes.title = values.title;
          if (dirtyFields.includes("description")) changes.description = values.description;
          if (dirtyFields.includes("priority")) changes.priority = values.priority as Priority;
          if (dirtyFields.includes("assigneeId")) changes.assigneeId = Number(values.assigneeId);
          if (Object.keys(changes).length === 0) {
            onEditingChange(false);
            return;
          }
          await update.mutateAsync({ changes, etag });
          toast.success("Changes saved");
          onEditingChange(false);
        }}
      />
    </Card>
  );
}
