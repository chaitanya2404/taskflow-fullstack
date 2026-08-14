import { createContext } from "react";
import type { AuthUser, LoginRequest, RegisterRequest } from "../api/types";

export interface AuthContextValue {
  user: AuthUser | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (payload: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => void;
}

/**
 * Kept in its own module, separate from <AuthProvider>, so that the provider
 * file only exports components — otherwise React Fast Refresh cannot hot-reload
 * it (oxlint's react/only-export-components rule).
 */
export const AuthContext = createContext<AuthContextValue | null>(null);
