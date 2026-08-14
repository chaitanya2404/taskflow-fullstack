import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { projectsApi } from "../api/projects";
import { ApiError } from "../api/client";
import type { ProjectRequest, ProjectResponse } from "../api/types";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { ErrorBanner } from "../components/ErrorBanner";
import { Modal } from "../components/Modal";
import { ProjectForm } from "../components/ProjectForm";
import { useAuth } from "../auth/useAuth";

export function ProjectsPage() {
  // Deleting a project is an ADMIN-only operation on the API, so the button is
  // only offered to admins rather than shown and then rejected with a 403.
  const { isAdmin } = useAuth();

  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingProject, setEditingProject] = useState<ProjectResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    projectsApi
      .list()
      .then(setProjects)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load projects"))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  function openCreate() {
    setEditingProject(null);
    setFormError(null);
    setModalMode("create");
  }

  function openEdit(project: ProjectResponse) {
    setEditingProject(project);
    setFormError(null);
    setModalMode("edit");
  }

  async function handleSubmit(payload: ProjectRequest) {
    setSubmitting(true);
    setFormError(null);
    try {
      if (modalMode === "edit" && editingProject) {
        const updated = await projectsApi.update(editingProject.id, payload);
        setProjects((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      } else {
        const created = await projectsApi.create(payload);
        setProjects((prev) => [...prev, created]);
      }
      setModalMode(null);
    } catch (e) {
      if (e instanceof ApiError && e.fieldErrors?.length) {
        setFormError(e.fieldErrors.map((fe) => fe.message).join(" "));
      } else {
        setFormError(e instanceof Error ? e.message : "Something went wrong");
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(project: ProjectResponse) {
    if (!confirm(`Delete "${project.name}" and all of its tasks? This cannot be undone.`)) return;
    try {
      await projectsApi.remove(project.id);
      setProjects((prev) => prev.filter((p) => p.id !== project.id));
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        alert("Deleting a project requires an ADMIN account.");
      } else {
        alert(e instanceof Error ? e.message : "Failed to delete project");
      }
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Projects</h1>
          <p className="page-subtitle">Organize your work into projects, then track tasks within each one.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          + New Project
        </button>
      </div>

      {loading && <LoadingSpinner label="Loading projects…" />}
      {!loading && error && <ErrorBanner message={error} onRetry={load} />}

      {!loading && !error && projects.length === 0 && (
        <div className="empty-state">
          <p>No projects yet. Create your first one to get started.</p>
          <button type="button" className="btn btn-primary" onClick={openCreate}>
            + New Project
          </button>
        </div>
      )}

      {!loading && !error && projects.length > 0 && (
        <div className="card-grid">
          {projects.map((project) => (
            <div className="card project-card" key={project.id}>
              <Link to={`/projects/${project.id}`} className="card-title-link">
                <h3>{project.name}</h3>
              </Link>
              <p className="card-description">{project.description || "No description provided."}</p>
              <div className="card-footer">
                <span className="task-count">
                  {project.taskCount} task{project.taskCount === 1 ? "" : "s"}
                </span>
                <div className="card-actions">
                  <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(project)}>
                    Edit
                  </button>
                  {isAdmin && (
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(project)}>
                      Delete
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {modalMode && (
        <Modal title={modalMode === "edit" ? "Edit project" : "New project"} onClose={() => setModalMode(null)}>
          <ProjectForm
            initial={editingProject ?? undefined}
            submitting={submitting}
            errorMessage={formError}
            onSubmit={handleSubmit}
            onCancel={() => setModalMode(null)}
          />
        </Modal>
      )}
    </div>
  );
}
