import { TopBar } from "@/components/layout/TopBar";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <TopBar />
      <main className="mx-auto max-w-6xl px-4 py-6 sm:py-8">{children}</main>
    </>
  );
}
