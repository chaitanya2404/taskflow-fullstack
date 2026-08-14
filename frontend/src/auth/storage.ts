import type { AuthUser } from "../api/types";

/**
 * Persisted session.
 *
 * localStorage keeps the user logged in across reloads and tabs. The trade-off
 * is that a successful XSS could read the token — the more hardened option is a
 * httpOnly, SameSite cookie issued by the backend, which JavaScript cannot read
 * at all. That needs CSRF protection and a cookie-aware backend, so this project
 * uses the bearer-token form and documents the choice (see README).
 */
const STORAGE_KEY = "taskflow.auth";

export interface StoredSession {
  token: string;
  user: AuthUser;
}

export function readSession(): StoredSession | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;

    const parsed = JSON.parse(raw) as Partial<StoredSession>;
    if (!parsed?.token || !parsed?.user?.username) return null;

    return parsed as StoredSession;
  } catch {
    // Corrupt or unreadable (e.g. storage disabled) — treat as logged out.
    return null;
  }
}

export function writeSession(session: StoredSession): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    // Storage full or blocked; the session simply won't survive a reload.
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // nothing useful to do
  }
}

/** Read by the API client on every request. */
export function readToken(): string | null {
  return readSession()?.token ?? null;
}
