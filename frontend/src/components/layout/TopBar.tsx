"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { LogOut, Plus, Ticket } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { Button, buttonVariants } from "@/components/ui/button";
import { useLogout, useMe } from "@/lib/api/auth";
import { initials } from "@/lib/format";
import { Suspense } from "react";
import { AssignedLink } from "./AssignedLink";
import { ThemeToggle } from "./ThemeToggle";

export function TopBar() {
  const router = useRouter();
  const alert = useAlert();
  const me = useMe();
  const logout = useLogout();

  const signOut = async () => {
    try {
      await logout.mutateAsync();
      router.replace("/login");
    } catch (error) {
      await alert.showError(error);
    }
  };

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-surface/85 backdrop-blur">
      <div className="mx-auto flex h-14 max-w-6xl items-center gap-3 px-4">
        <Link href="/tickets" aria-label="Tickets home" className="flex items-center gap-2 font-semibold tracking-tight">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Ticket className="size-4" aria-hidden />
          </span>
          <span className="hidden sm:inline">Tickets</span>
        </Link>
        <div className="ml-auto flex items-center gap-1 sm:gap-2">
          {/* useSearchParams needs a Suspense boundary on statically rendered pages. */}
          <Suspense>
            <AssignedLink />
          </Suspense>
          <Link href="/tickets/new" className={buttonVariants({ size: "sm" })}>
            <Plus aria-hidden />
            New ticket
          </Link>
          <ThemeToggle />
          <DropdownMenu.Root>
            <DropdownMenu.Trigger asChild>
              <Button variant="ghost" size="icon" aria-label="Account menu" className="rounded-full">
                <span className="flex size-8 items-center justify-center rounded-full bg-muted text-xs font-semibold">
                  {me.data ? initials(me.data.displayName) : "…"}
                </span>
              </Button>
            </DropdownMenu.Trigger>
            <DropdownMenu.Portal>
              <DropdownMenu.Content
                align="end"
                sideOffset={8}
                className="z-50 min-w-52 rounded-xl border border-border bg-surface p-1 shadow-xl"
              >
                {me.data && (
                  <div className="px-3 py-2">
                    <p className="text-sm font-medium">{me.data.displayName}</p>
                    <p className="text-xs text-muted-foreground">{me.data.email}</p>
                  </div>
                )}
                <DropdownMenu.Separator className="my-1 h-px bg-border" />
                <DropdownMenu.Item
                  onSelect={() => void signOut()}
                  className="flex cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-sm outline-none data-[highlighted]:bg-muted"
                >
                  <LogOut className="size-4" aria-hidden />
                  Sign out
                </DropdownMenu.Item>
              </DropdownMenu.Content>
            </DropdownMenu.Portal>
          </DropdownMenu.Root>
        </div>
      </div>
    </header>
  );
}
