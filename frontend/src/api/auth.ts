import { apiClient } from "./client";
import type { AuthResponse, AuthUser, LoginRequest, RegisterRequest } from "./types";

/** Splits the wire response into the token and the account it belongs to. */
function toSession(response: AuthResponse): { token: string; user: AuthUser } {
  return {
    token: response.token,
    user: {
      id: response.userId,
      username: response.username,
      email: response.email,
      role: response.role,
    },
  };
}

export const authApi = {
  register: (payload: RegisterRequest) =>
    apiClient.post<AuthResponse>("/api/auth/register", payload).then(toSession),

  login: (payload: LoginRequest) =>
    apiClient.post<AuthResponse>("/api/auth/login", payload).then(toSession),

  me: () => apiClient.get<AuthUser>("/api/auth/me"),
};
