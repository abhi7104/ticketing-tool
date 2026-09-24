import type { Metadata } from "next";
import { TicketView } from "./TicketView";

export async function generateMetadata({ params }: { params: Promise<{ key: string }> }): Promise<Metadata> {
  const { key } = await params;
  return { title: decodeURIComponent(key) };
}

export default async function TicketPage({ params }: { params: Promise<{ key: string }> }) {
  const { key } = await params;
  return <TicketView ticketKey={decodeURIComponent(key)} />;
}
