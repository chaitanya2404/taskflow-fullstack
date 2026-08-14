import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./useAuth";

/**
 * Route guard for everything behind login.
 *
 * <p>Remembers where the user was headed so that logging in returns them there
 * rather than always dumping them on the projects list — which matters when a
 * token expires mid-session or someone opens a deep link cold.
 */
export function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
