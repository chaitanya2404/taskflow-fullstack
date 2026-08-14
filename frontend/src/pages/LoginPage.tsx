import { useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/useAuth";

type Mode = "login" | "register";

interface RedirectState {
  from?: { pathname?: string };
}

export function LoginPage() {
  const { isAuthenticated, login, register } = useAuth();
  const location = useLocation();

  const [mode, setMode] = useState<Mode>("login");
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  // Where the user was trying to go before the guard bounced them here.
  const redirectTo = (location.state as RedirectState | null)?.from?.pathname ?? "/";

  if (isAuthenticated) {
    return <Navigate to={redirectTo} replace />;
  }

  function switchMode(next: Mode) {
    setMode(next);
    setError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      if (mode === "register") {
        await register({ username: username.trim(), email: email.trim(), password });
      } else {
        await login({ username: username.trim(), password });
      }
      // On success the guard re-renders and <Navigate> above takes over.
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.length) {
        setError(err.fieldErrors.map((fe) => fe.message).join(" "));
      } else if (err instanceof Error) {
        setError(err.message);
      } else {
        setError("Something went wrong. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <span className="brand-mark">TF</span>
          <h1>TaskFlow</h1>
          <p className="page-subtitle">
            {mode === "login"
              ? "Sign in to see your projects and tasks."
              : "Create an account to start tracking your work."}
          </p>
        </div>

        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={mode === "login"}
            className={`auth-tab ${mode === "login" ? "auth-tab-active" : ""}`}
            onClick={() => switchMode("login")}
          >
            Sign in
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={mode === "register"}
            className={`auth-tab ${mode === "register" ? "auth-tab-active" : ""}`}
            onClick={() => switchMode("register")}
          >
            Create account
          </button>
        </div>

        <form onSubmit={handleSubmit} className="form">
          {error && (
            <div className="form-error" role="alert">
              {error}
            </div>
          )}

          <label className="field">
            <span>Username</span>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              placeholder="yourname"
              required
              minLength={mode === "register" ? 3 : undefined}
              maxLength={50}
              autoFocus
            />
          </label>

          {mode === "register" && (
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                placeholder="you@example.com"
                required
                maxLength={254}
              />
            </label>
          )}

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={mode === "register" ? "new-password" : "current-password"}
              placeholder={mode === "register" ? "At least 8 characters" : "••••••••"}
              required
              minLength={mode === "register" ? 8 : undefined}
              maxLength={72}
            />
          </label>

          <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
            {submitting
              ? mode === "register"
                ? "Creating account…"
                : "Signing in…"
              : mode === "register"
                ? "Create account"
                : "Sign in"}
          </button>
        </form>

        <p className="auth-footnote">
          {mode === "login" ? (
            <>
              No account yet?{" "}
              <button type="button" className="link-btn" onClick={() => switchMode("register")}>
                Create one
              </button>
            </>
          ) : (
            <>
              Already registered?{" "}
              <button type="button" className="link-btn" onClick={() => switchMode("login")}>
                Sign in
              </button>
            </>
          )}
        </p>
      </div>
    </div>
  );
}
