"use client";

import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "next-themes";
import * as React from "react";
import { Toaster } from "sonner";
import { AlertProvider } from "@/components/alert-modal/AlertProvider";
import { isApiError } from "@/lib/api/client";

function redirectToSignIn() {
  if (window.location.pathname.startsWith("/login")) return;
  const next = `${window.location.pathname}${window.location.search}`;
  window.location.assign(`/login?reason=expired&next=${encodeURIComponent(next)}`);
}

function onUnauthenticated(error: unknown) {
  if (isApiError(error) && error.code === "UNAUTHENTICATED") redirectToSignIn();
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = React.useState(
    () =>
      new QueryClient({
        queryCache: new QueryCache({ onError: onUnauthenticated }),
        mutationCache: new MutationCache({ onError: onUnauthenticated }),
        defaultOptions: {
          queries: {
            staleTime: 15_000,
            refetchOnWindowFocus: true,
            // Client errors (4xx) will not fix themselves; only retry network/server failures.
            retry: (count, error) =>
              count < 2 && !(isApiError(error) && error.status >= 400 && error.status < 500),
          },
          mutations: { retry: false },
        },
      }),
  );

  return (
    <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
      <QueryClientProvider client={queryClient}>
        <AlertProvider>{children}</AlertProvider>
        <Toaster richColors closeButton position="top-right" />
      </QueryClientProvider>
    </ThemeProvider>
  );
}
