import type { ErrorCode, FieldError, Problem, TicketStatus } from "./types";

export const API_BASE = "/api/v1";
const TIMEOUT_MS = 15_000;

export type ClientErrorCode = ErrorCode | "NETWORK_ERROR" | "TIMEOUT";

/** Every failed API call surfaces as an ApiError, whether the server or the network failed. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: ClientErrorCode;
  readonly title: string;
  readonly detail: string;
  readonly fieldErrors: FieldError[];
  readonly currentStatus?: TicketStatus;
  readonly allowedNextStatus?: TicketStatus | null;
  readonly traceId?: string;

  constructor(init: {
    status: number;
    code: ClientErrorCode;
    title: string;
    detail: string;
    fieldErrors?: FieldError[];
    currentStatus?: TicketStatus;
    allowedNextStatus?: TicketStatus | null;
    traceId?: string;
  }) {
    super(init.detail);
    this.name = "ApiError";
    this.status = init.status;
    this.code = init.code;
    this.title = init.title;
    this.detail = init.detail;
    this.fieldErrors = init.fieldErrors ?? [];
    this.currentStatus = init.currentStatus;
    this.allowedNextStatus = init.allowedNextStatus;
    this.traceId = init.traceId;
  }
}

export function isApiError(value: unknown): value is ApiError {
  return value instanceof ApiError;
}

export interface ApiResponse<T> {
  data: T;
  etag: string | null;
  status: number;
}

export interface ApiRequest {
  method?: "GET" | "POST" | "PATCH";
  body?: unknown;
  ifMatch?: string | null;
  signal?: AbortSignal;
}

function readCookie(name: string): string | null {
  if (typeof document === "undefined") return null;
  const match = document.cookie.split("; ").find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

function newRequestId(): string {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

/** The backend sets XSRF-TOKEN on any response; fetch once if the cookie is not there yet. */
async function ensureCsrfToken(): Promise<string | null> {
  let token = readCookie("XSRF-TOKEN");
  if (!token) {
    await fetch(`${API_BASE}/auth/me`, { credentials: "include" }).catch(() => undefined);
    token = readCookie("XSRF-TOKEN");
  }
  return token;
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: Partial<Problem> = {};
  const type = response.headers.get("content-type") ?? "";
  if (type.includes("json")) {
    problem = (await response.json().catch(() => ({}))) as Partial<Problem>;
  }
  if (problem.code) {
    return new ApiError({
      status: response.status,
      code: problem.code,
      title: problem.title ?? "Something went wrong",
      detail: problem.detail ?? "Please try again.",
      fieldErrors: problem.errors ?? [],
      currentStatus: problem.currentStatus,
      allowedNextStatus: problem.allowedNextStatus,
      traceId: problem.traceId,
    });
  }
  return new ApiError({
    status: response.status,
    code: response.status === 401 ? "UNAUTHENTICATED" : "INTERNAL_ERROR",
    title: "Something went wrong",
    detail: "Please try again.",
    traceId: response.headers.get("x-request-id") ?? undefined,
  });
}

export async function apiFetch<T>(path: string, request: ApiRequest = {}): Promise<ApiResponse<T>> {
  const method = request.method ?? "GET";
  const headers: Record<string, string> = {
    Accept: "application/json",
    "X-Request-Id": newRequestId(),
  };
  if (request.body !== undefined) headers["Content-Type"] = "application/json";
  if (request.ifMatch) headers["If-Match"] = request.ifMatch;
  if (method !== "GET") {
    const token = await ensureCsrfToken();
    if (token) headers["X-XSRF-TOKEN"] = token;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort("timeout"), TIMEOUT_MS);
  request.signal?.addEventListener("abort", () => controller.abort(request.signal?.reason));

  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      credentials: "include",
      body: request.body === undefined ? undefined : JSON.stringify(request.body),
      signal: controller.signal,
    });
  } catch {
    const timedOut = controller.signal.reason === "timeout";
    throw new ApiError({
      status: 0,
      code: timedOut ? "TIMEOUT" : "NETWORK_ERROR",
      title: "Can't reach the server",
      detail: "Check your connection and try again.",
    });
  } finally {
    clearTimeout(timer);
  }

  if (!response.ok) throw await toApiError(response);

  const text = response.status === 204 ? "" : await response.text();
  return {
    data: (text ? JSON.parse(text) : undefined) as T,
    etag: response.headers.get("etag"),
    status: response.status,
  };
}
