"use client";

import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "./client";
import type { UserSummary } from "./types";

export function useUsers() {
  return useQuery({
    queryKey: ["users"],
    queryFn: async () => (await apiFetch<UserSummary[]>("/users")).data,
    staleTime: 5 * 60_000,
  });
}
