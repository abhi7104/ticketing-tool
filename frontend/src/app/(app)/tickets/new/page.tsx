import type { Metadata } from "next";
import { NewTicket } from "./NewTicket";

export const metadata: Metadata = { title: "New ticket" };

export default function NewTicketPage() {
  return <NewTicket />;
}
