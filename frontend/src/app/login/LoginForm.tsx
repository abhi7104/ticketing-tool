"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef } from "react";
import { useForm } from "react-hook-form";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Field, Input } from "@/components/ui/field";
import { useLogin } from "@/lib/api/auth";
import { loginSchema, type LoginFormValues } from "@/lib/validation/schemas";

function safeNext(next: string | null): string {
  // Only allow same-site relative paths to avoid open redirects.
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/tickets";
}

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const alert = useAlert();
  const login = useLogin();
  const shownExpired = useRef(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  useEffect(() => {
    if (searchParams.get("reason") === "expired" && !shownExpired.current) {
      shownExpired.current = true;
      void alert.showAlert({
        title: "Session expired",
        message: "Please sign in again to continue.",
        variant: "info",
        actions: [{ id: "ok", label: "OK" }],
      });
    }
  }, [alert, searchParams]);

  const onValid = async (values: LoginFormValues) => {
    try {
      await login.mutateAsync(values);
      router.replace(safeNext(searchParams.get("next")));
    } catch (error) {
      void alert.showError(error, { onRetry: () => void onValid(values) });
    }
  };

  return (
    <Card className="p-6">
      <form
        noValidate
        onSubmit={handleSubmit(onValid, (errs) => {
          void alert.showValidation(Object.values(errs).map((e) => String(e?.message)));
        })}
        className="space-y-4"
      >
        <Field id="username" label="Username" error={errors.username?.message}>
          <Input
            id="username"
            autoComplete="username"
            autoFocus
            aria-invalid={!!errors.username}
            aria-describedby={errors.username ? "username-error" : undefined}
            {...register("username")}
          />
        </Field>
        <Field id="password" label="Password" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            aria-describedby={errors.password ? "password-error" : undefined}
            {...register("password")}
          />
        </Field>
        <Button type="submit" className="w-full" size="lg" loading={login.isPending}>
          Sign in
        </Button>
      </form>
    </Card>
  );
}
