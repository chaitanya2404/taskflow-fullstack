import { readToken } from "../auth/storage";
import type { ApiErrorBody } from "./types";

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;
  fieldErrors: ApiErrorBody["fieldErrors"];

  constructor(body: ApiErrorBody) {
    super(body.message || `Request failed with status ${body.status}`);
    this.status = body.status;
    this.fieldErrors = body.fieldErrors;
  }
}

/**
 * Called when the API rejects a token (expired, revoked, or tampered with).
 * AuthProvider registers a handler that drops the stored session, which in turn
 * makes the route guard send the user back to the login page.
 */
let onUnauthorized: (() => void) | null = null;

export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler;
}

/** Login/register failures are reported in-form, not treated as a session expiry. */
function isAuthEndpoint(path: string): boolean {
  return path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register");
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = readToken();

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options?.headers ?? {}),
      },
    });
  } catch {
    throw new Error(
      "Could not reach the TaskFlow API. Is the backend running at " + API_BASE_URL + "?"
    );
  }

  if (response.status === 401 && !isAuthEndpoint(path)) {
    onUnauthorized?.();
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    throw new ApiError(
      data ?? { status: response.status, error: response.statusText, message: response.statusText }
    );
  }

  return data as T;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path, { method: "GET" }),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
};
