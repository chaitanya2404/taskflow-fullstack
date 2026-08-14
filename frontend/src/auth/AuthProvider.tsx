import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { authApi } from "../api/auth";
import { setUnauthorizedHandler } from "../api/client";
import type { AuthUser, LoginRequest, RegisterRequest } from "../api/types";
import { AuthContext, type AuthContextValue } from "./authContext";
import { clearSession, readSession, writeSession } from "./storage";

export function AuthProvider({ children }: { children: ReactNode }) {
  // Initialised straight from storage so a page reload doesn't flash the login
  // screen before the session is restored.
  const [user, setUser] = useState<AuthUser | null>(() => readSession()?.user ?? null);

  const logout = useCallback(() => {
    clearSession();
    setUser(null);
  }, []);

  /**
   * Any 401 from a protected endpoint means the token is no longer good —
   * typically expired. Dropping the session here is all that's needed: the
   * route guard reacts to `user` becoming null and redirects to /login.
   */
  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(null);
  }, [logout]);

  const login = useCallback(async (payload: LoginRequest) => {
    const session = await authApi.login(payload);
    writeSession(session);
    setUser(session.user);
  }, []);

  const register = useCallback(async (payload: RegisterRequest) => {
    const session = await authApi.register(payload);
    writeSession(session);
    setUser(session.user);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      isAdmin: user?.role === "ADMIN",
      login,
      register,
      logout,
    }),
    [user, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
