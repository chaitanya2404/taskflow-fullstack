import { NavLink } from "react-router-dom";

export function Navbar() {
  return (
    <header className="navbar">
      <div className="navbar-inner">
        <NavLink to="/" className="brand">
          <span className="brand-mark">TF</span>
          <span>TaskFlow</span>
        </NavLink>
        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => (isActive ? "active" : "")}>
            Projects
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
