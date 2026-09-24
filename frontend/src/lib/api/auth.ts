"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiFetch } from "./client";
import type { CurrentUser } from "./types";

export const meKey = ["me"] as const;

export function useMe() {
  return useQuery({
    queryKey: meKey,
    queryFn: async () => (await apiFetch<CurrentUser>("/auth/me")).data,
    staleTime: 5 * 60_000,
  });
}

export function useLogin() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (credentials: { username: string; password: string }) =>
      (await apiFetch<CurrentUser>("/auth/login", { method: "POST", body: credentials })).data,
    onSuccess: (user) => {
      queryClient.clear();
      queryClient.setQueryData(meKey, user);
    },
  });
}

export function useLogout() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      await apiFetch<void>("/auth/logout", { method: "POST" });
    },
    onSettled: () => queryClient.clear(),
  });
}
