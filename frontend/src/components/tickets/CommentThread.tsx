"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { MessageSquare } from "lucide-react";
import { useForm } from "react-hook-form";
import { useAlert } from "@/components/alert-modal/AlertProvider";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/field";
import { useMe } from "@/lib/api/auth";
import { useAddComment } from "@/lib/api/comments";
import type { TicketDetail } from "@/lib/api/types";
import { formatDate, formatRelative, initials } from "@/lib/format";
import { commentSchema, LIMITS, type CommentFormValues } from "@/lib/validation/schemas";

interface Props {
  ticket: TicketDetail;
  etag: string;
}

export function CommentThread({ ticket }: Props) {
  const alert = useAlert();
  const me = useMe();
  const addComment = useAddComment(ticket.key, me.data);
  const closed = ticket.status === "CLOSED";

  const { register, handleSubmit, reset, watch, formState } = useForm<CommentFormValues>({
    resolver: zodResolver(commentSchema),
    defaultValues: { body: "" },
  });
  const body = watch("body") ?? "";
  const tooLong = body.length > LIMITS.comment.max;

  const submit = async (values: CommentFormValues) => {
    reset({ body: "" });
    try {
      await addComment.mutateAsync(values.body);
    } catch (error) {
      // The optimistic comment was rolled back; give the text back to the user.
      reset({ body: values.body });
      void alert.showError(error, { onRetry: () => void submit(values) });
    }
  };

  const onSubmit = handleSubmit(submit, (errs) => {
    void alert.showValidation([String(errs.body?.message)]);
  });

  return (
    <section aria-label="Comments" className="space-y-5">
      {ticket.comments.length === 0 ? (
        <p className="flex items-center gap-2 text-sm text-muted-foreground">
          <MessageSquare className="size-4" aria-hidden />
          No comments yet.
        </p>
      ) : (
        <ol className="space-y-4">
          {ticket.comments.map((c) => (
            <li key={c.id} className={c.id < 0 ? "flex gap-3 opacity-60" : "flex gap-3"}>
              <span
                aria-hidden
                className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary"
              >
                {initials(c.author.displayName)}
              </span>
              <div className="min-w-0 flex-1 rounded-xl border border-border bg-background px-4 py-3">
                <div className="mb-1 flex flex-wrap items-baseline gap-x-2 text-sm">
                  <span className="font-medium">{c.author.displayName}</span>
                  <time dateTime={c.createdAt} title={formatDate(c.createdAt)} className="text-xs text-muted-foreground">
                    {c.id < 0 ? "Sending…" : formatRelative(c.createdAt)}
                  </time>
                </div>
                {/* Rendered as text: markup in comments is never interpreted. */}
                <p className="whitespace-pre-wrap break-words text-sm leading-relaxed">{c.body}</p>
              </div>
            </li>
          ))}
        </ol>
      )}

      {closed ? (
        <p className="text-sm text-muted-foreground">Comments are disabled because this ticket is closed.</p>
      ) : (
        <form noValidate onSubmit={onSubmit} className="space-y-2">
          <label htmlFor="comment" className="sr-only">
            Add a comment
          </label>
          <Textarea
            id="comment"
            rows={3}
            placeholder="Write a comment…"
            aria-invalid={!!formState.errors.body || tooLong}
            onKeyDown={(e) => {
              if (e.key === "Enter" && (e.metaKey || e.ctrlKey)) void onSubmit();
            }}
            {...register("body")}
          />
          <div className="flex items-center justify-between gap-2">
            <span className={tooLong ? "text-xs text-danger" : "text-xs text-muted-foreground"}>
              {body.length.toLocaleString()} / {LIMITS.comment.max.toLocaleString()} · Ctrl+Enter to send
            </span>
            <Button type="submit" size="sm" disabled={body.trim().length === 0} loading={addComment.isPending}>
              Add comment
            </Button>
          </div>
        </form>
      )}
    </section>
  );
}
