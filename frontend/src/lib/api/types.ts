import type { components } from "./schema";

type Schemas = components["schemas"];

export type TicketStatus = Schemas["TicketStatus"];
export type Priority = Schemas["Priority"];
export type UserSummary = Schemas["UserSummary"];
export type CurrentUser = Schemas["CurrentUser"];
export type TicketSummary = Schemas["TicketSummary"];
export type TicketDetail = Schemas["TicketDetail"];
export type TicketPage = Schemas["TicketPage"];
export type Comment = Schemas["Comment"];
export type HistoryEntry = Schemas["HistoryEntry"];
export type Problem = Schemas["Problem"];
export type FieldError = Schemas["FieldError"];
export type ErrorCode = Problem["code"];
export type CreateTicketRequest = Schemas["CreateTicketRequest"];
export type UpdateTicketRequest = Schemas["UpdateTicketRequest"];
export type TicketSummaryCounts = Schemas["TicketSummaryCounts"];

export const TICKET_STATUSES: TicketStatus[] = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"];
export const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
