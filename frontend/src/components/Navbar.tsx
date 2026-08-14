import { NavLink } from "react-router-dom";
import { useAuth } from "../auth/useAuth";

export function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <NavLink to={isAuthenticated ? "/" : "/login"} className="brand">
          <span className="brand-mark">TF</span>
          <span>TaskFlow</span>
        </NavLink>

        {isAuthenticated && user && (
          <div className="navbar-right">
            <nav className="nav-links">
              <NavLink to="/" end className={({ isActive }) => (isActive ? "active" : "")}>
                Projects
              </NavLink>
            </nav>
            <div className="user-chip" title={user.email}>
              <span className="user-name">{user.username}</span>
              {isAdmin && <span className="role-badge">ADMIN</span>}
            </div>
            <button type="button" className="btn btn-secondary btn-sm" onClick={logout}>
              Log out
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
