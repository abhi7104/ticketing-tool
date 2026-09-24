import type { Metadata } from "next";
import { Suspense } from "react";
import { TicketList } from "./TicketList";

export const metadata: Metadata = { title: "Tickets" };

export default function TicketsPage() {
  return (
    <Suspense>
      <TicketList />
    </Suspense>
  );
}
