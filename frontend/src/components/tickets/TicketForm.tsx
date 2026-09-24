"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm, type FieldErrors, type Path } from "react-hook-form";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { Button } from "@/components/ui/button";
import { Field, Input, Select, Textarea } from "@/components/ui/field";
import { isApiError } from "@/lib/api/client";
import { PRIORITIES, type UserSummary } from "@/lib/api/types";
import { PRIORITY_LABELS } from "@/lib/labels";
import { LIMITS, ticketFormSchema, type TicketFormValues } from "@/lib/validation/schemas";
import { AssigneeSelect } from "./AssigneeSelect";

const FIELD_ORDER: (keyof TicketFormValues)[] = ["title", "description", "priority", "assigneeId"];

export const EMPTY_TICKET_FORM: TicketFormValues = {
  title: "",
  description: "",
  priority: "",
  assigneeId: "",
};

interface TicketFormProps {
  mode: "create" | "edit";
  defaultValues?: TicketFormValues;
  submitLabel: string;
  /** Receives trimmed, validated values. Throwing an ApiError shows it and maps field errors. */
  onSubmit: (values: TicketFormValues, dirty: (keyof TicketFormValues)[]) => Promise<void>;
  onCancel?: () => void;
  onDirtyChange?: (dirty: boolean) => void;
  /** Handlers for the follow-up actions in error pop-ups (e.g. reload after a conflict). */
  onReload?: () => void;
  currentAssignee?: UserSummary;
}

export function TicketForm({
  mode,
  defaultValues = EMPTY_TICKET_FORM,
  submitLabel,
  onSubmit,
  onCancel,
  onDirtyChange,
  onReload,
  currentAssignee,
}: TicketFormProps) {
  const alert = useAlert();
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    watch,
    formState: { errors, isSubmitting, isDirty, dirtyFields },
  } = useForm<TicketFormValues>({
    resolver: zodResolver(ticketFormSchema),
    defaultValues,
    mode: "onTouched",
    shouldFocusError: false,
  });

  useEffect(() => onDirtyChange?.(isDirty), [isDirty, onDirtyChange]);

  const descriptionLength = watch("description")?.length ?? 0;

  const focusFirst = (fields: string[]) => {
    const first = FIELD_ORDER.find((f) => fields.includes(f));
    // Wait for the modal to hand focus back before moving it to the field.
    if (first) setTimeout(() => setFocus(first), 50);
  };

  // Not awaited: react-hook-form only publishes the errors (red fields) after this returns.
  const onInvalid = (formErrors: FieldErrors<TicketFormValues>) => {
    const fields = FIELD_ORDER.filter((f) => formErrors[f]);
    void alert
      .showValidation(fields.map((f) => String(formErrors[f]?.message)))
      .then(() => focusFirst(fields));
  };

  const submit = async (values: TicketFormValues) => {
    const dirty = (Object.keys(dirtyFields) as (keyof TicketFormValues)[]).filter(
      (k) => dirtyFields[k],
    );
    try {
      await onSubmit(values, mode === "create" ? FIELD_ORDER : dirty);
    } catch (error) {
      if (isApiError(error) && error.fieldErrors.length) {
        const known = error.fieldErrors.filter((f) =>
          (FIELD_ORDER as string[]).includes(f.field),
        );
        known.forEach((f) =>
          setError(f.field as Path<TicketFormValues>, { type: "server", message: f.message }),
        );
        void alert.showError(error).then(() => focusFirst(known.map((f) => f.field)));
        return;
      }
      void alert.showError(error, {
        onRetry: () => void handleSubmit(submit, onInvalid)(),
        onReload,
      });
    }
  };

  const describedBy = (name: keyof TicketFormValues) =>
    errors[name] ? `${name}-error` : undefined;

  return (
    <form noValidate onSubmit={handleSubmit(submit, onInvalid)} className="space-y-3">
      <Field id="title" label="Title" required error={errors.title?.message}>
        <Input
          id="title"
          placeholder="Short summary of the problem"
          maxLength={LIMITS.title.max + 20}
          aria-invalid={!!errors.title}
          aria-describedby={describedBy("title")}
          aria-required
          {...register("title")}
        />
      </Field>

      <Field
        id="description"
        label="Description"
        required
        error={errors.description?.message}
        hint={
          <span className={descriptionLength > LIMITS.description.max ? "text-danger" : undefined}>
            {descriptionLength.toLocaleString()} / {LIMITS.description.max.toLocaleString()}
          </span>
        }
      >
        <Textarea
          id="description"
          rows={6}
          placeholder="What happened? What did you expect? Steps to reproduce…"
          aria-invalid={!!errors.description}
          aria-describedby={describedBy("description")}
          aria-required
          {...register("description")}
        />
      </Field>

      <div className="grid gap-3 sm:grid-cols-2">
        <Field id="priority" label="Priority" required error={errors.priority?.message}>
          <Select
            id="priority"
            aria-invalid={!!errors.priority}
            aria-describedby={describedBy("priority")}
            aria-required
            {...register("priority")}
          >
            <option value="">Choose a priority</option>
            {PRIORITIES.map((p) => (
              <option key={p} value={p}>
                {PRIORITY_LABELS[p]}
              </option>
            ))}
          </Select>
        </Field>
        <Field id="assigneeId" label="Assignee" required error={errors.assigneeId?.message}>
          <AssigneeSelect
            id="assigneeId"
            current={currentAssignee}
            aria-invalid={!!errors.assigneeId}
            aria-describedby={describedBy("assigneeId")}
            aria-required
            {...register("assigneeId")}
          />
        </Field>
      </div>

      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        {onCancel && (
          <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            Cancel
          </Button>
        )}
        <Button type="submit" loading={isSubmitting} disabled={mode === "edit" && !isDirty}>
          {submitLabel}
        </Button>
      </div>
    </form>
  );
}
