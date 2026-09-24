"use client";

import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { TicketForm } from "@/components/tickets/TicketForm";
import { Card } from "@/components/ui/card";
import { useCreateTicket } from "@/lib/api/tickets-create";
import type { Priority } from "@/lib/api/types";

export function NewTicket() {
  const router = useRouter();
  const create = useCreateTicket();

  return (
    <div className="mx-auto max-w-2xl">
      <Link
        href="/tickets"
        className="mb-4 inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden />
        Back to tickets
      </Link>
      <h1 className="text-2xl font-semibold tracking-tight">New ticket</h1>
      <p className="mt-1 text-sm text-muted-foreground">
        All fields are required. You can update them later.
      </p>
      <Card className="mt-6 p-5 sm:p-6">
        <TicketForm
          mode="create"
          submitLabel="Create ticket"
          onCancel={() => router.push("/tickets")}
          onSubmit={async (values) => {
            const created = await create.mutateAsync({
              title: values.title,
              description: values.description,
              priority: values.priority as Priority,
              assigneeId: Number(values.assigneeId),
            });
            toast.success(`Ticket ${created.ticket.key} created`);
            router.push(`/tickets/${created.ticket.key}`);
          }}
        />
      </Card>
    </div>
  );
}
