"use client";

import * as React from "react";
import { Select } from "@/components/ui/field";
import { useUsers } from "@/lib/api/users";

import type { UserSummary } from "@/lib/api/types";

type Props = React.SelectHTMLAttributes<HTMLSelectElement> & {
  /** The ticket's current assignee, shown even before (or if not in) the loaded user list. */
  current?: UserSummary;
};

/** Native select of active users: keyboard- and screen-reader-friendly, native picker on mobile. */
export const AssigneeSelect = React.forwardRef<HTMLSelectElement, Props>(({ current, ...props }, ref) => {
  const users = useUsers();
  const showCurrent = current && !users.data?.some((u) => u.id === current.id);
  return (
    <Select ref={ref} {...props}>
      <option value="">{users.isLoading ? "Loading people…" : "Choose an assignee"}</option>
      {showCurrent && <option value={String(current.id)}>{current.displayName}</option>}
      {users.data?.map((user) => (
        <option key={user.id} value={String(user.id)}>
          {user.displayName}
        </option>
      ))}
    </Select>
  );
});
AssigneeSelect.displayName = "AssigneeSelect";
