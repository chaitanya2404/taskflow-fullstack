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

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...(options?.headers ?? {}),
      },
      ...options,
    });
  } catch {
    throw new Error(
      "Could not reach the TaskFlow API. Is the backend running at " + API_BASE_URL + "?"
    );
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
