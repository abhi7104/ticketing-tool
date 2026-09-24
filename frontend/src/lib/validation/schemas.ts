import { z } from "zod";
import { PRIORITIES } from "@/lib/api/types";

/**
 * Mirrors the backend rules in data-model.md. The backend re-validates everything; these checks
 * give instant feedback with the same messages.
 */
export const LIMITS = {
  title: { min: 3, max: 150 },
  description: { min: 10, max: 5000 },
  comment: { min: 1, max: 2000 },
  search: { max: 100 },
} as const;

export const MESSAGES = {
  titleRequired: "Title is required.",
  titleLength: "Title must be between 3 and 150 characters.",
  descriptionRequired: "Description is required.",
  descriptionLength: "Description must be between 10 and 5,000 characters.",
  priorityRequired: "Please choose a priority.",
  assigneeRequired: "Please choose an assignee.",
  commentRequired: "Comment can't be empty.",
  commentLength: "Comment must be at most 2,000 characters.",
  usernameRequired: "Username is required.",
  passwordRequired: "Password is required.",
} as const;

export const ticketFormSchema = z.object({
  // pipe(): the length rule only runs once the field is non-empty, so one message per field.
  title: z
    .string()
    .trim()
    .min(1, MESSAGES.titleRequired)
    .pipe(
      z.string().min(LIMITS.title.min, MESSAGES.titleLength).max(LIMITS.title.max, MESSAGES.titleLength),
    ),
  description: z
    .string()
    .trim()
    .min(1, MESSAGES.descriptionRequired)
    .pipe(
      z
        .string()
        .min(LIMITS.description.min, MESSAGES.descriptionLength)
        .max(LIMITS.description.max, MESSAGES.descriptionLength),
    ),
  priority: z
    .string()
    .refine((v) => (PRIORITIES as string[]).includes(v), MESSAGES.priorityRequired),
  assigneeId: z.string().regex(/^[1-9]\d*$/, MESSAGES.assigneeRequired),
});

export type TicketFormValues = z.input<typeof ticketFormSchema>;

export const commentSchema = z.object({
  body: z
    .string()
    .trim()
    .min(LIMITS.comment.min, MESSAGES.commentRequired)
    .max(LIMITS.comment.max, MESSAGES.commentLength),
});

export type CommentFormValues = z.input<typeof commentSchema>;

export const loginSchema = z.object({
  username: z.string().trim().min(1, MESSAGES.usernameRequired).max(50),
  password: z.string().min(1, MESSAGES.passwordRequired).max(128),
});

export type LoginFormValues = z.input<typeof loginSchema>;
