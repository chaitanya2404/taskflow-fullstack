import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { projectsApi } from "../api/projects";
import { tasksApi } from "../api/tasks";
import { ApiError } from "../api/client";
import type { ProjectResponse, TaskRequest, TaskResponse, TaskStatus } from "../api/types";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { ErrorBanner } from "../components/ErrorBanner";
import { Modal } from "../components/Modal";
import { TaskForm } from "../components/TaskForm";
import { PriorityBadge } from "../components/Badges";

const statusFilters: Array<{ label: string; value: TaskStatus | "ALL" }> = [
  { label: "All", value: "ALL" },
  { label: "To Do", value: "TODO" },
  { label: "In Progress", value: "IN_PROGRESS" },
  { label: "Done", value: "DONE" },
];

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const id = Number(projectId);

  const [project, setProject] = useState<ProjectResponse | null>(null);
  const [tasks, setTasks] = useState<TaskResponse[]>([]);
  const [statusFilter, setStatusFilter] = useState<TaskStatus | "ALL">("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalMode, setModalMode] = useState<"create" | "edit" | null>(null);
  const [editingTask, setEditingTask] = useState<TaskResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function load() {
    setLoading(true);
    setError(null);
    Promise.all([projectsApi.get(id), tasksApi.list({ projectId: id })])
      .then(([proj, projectTasks]) => {
        setProject(proj);
        setTasks(projectTasks);
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load project"))
      .finally(() => setLoading(false));
  }

  useEffect(load, [id]);

  const visibleTasks = statusFilter === "ALL" ? tasks : tasks.filter((t) => t.status === statusFilter);

  function openCreate() {
    setEditingTask(null);
    setFormError(null);
    setModalMode("create");
  }

  function openEdit(task: TaskResponse) {
    setEditingTask(task);
    setFormError(null);
    setModalMode("edit");
  }

  async function handleSubmit(payload: TaskRequest) {
    setSubmitting(true);
    setFormError(null);
    try {
      if (modalMode === "edit" && editingTask) {
        const updated = await tasksApi.update(editingTask.id, payload);
        setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      } else {
        const created = await tasksApi.create(payload);
        setTasks((prev) => [...prev, created]);
        setProject((prev) => (prev ? { ...prev, taskCount: prev.taskCount + 1 } : prev));
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

  async function handleDelete(task: TaskResponse) {
    if (!confirm(`Delete task "${task.title}"?`)) return;
    try {
      await tasksApi.remove(task.id);
      setTasks((prev) => prev.filter((t) => t.id !== task.id));
      setProject((prev) => (prev ? { ...prev, taskCount: Math.max(0, prev.taskCount - 1) } : prev));
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to delete task");
    }
  }

  async function handleQuickStatusChange(task: TaskResponse, status: TaskStatus) {
    try {
      const updated = await tasksApi.update(task.id, {
        title: task.title,
        description: task.description ?? "",
        status,
        priority: task.priority,
        dueDate: task.dueDate,
        projectId: task.projectId,
      });
      setTasks((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (e) {
      alert(e instanceof Error ? e.message : "Failed to update task status");
    }
  }

  if (loading) {
    return (
      <div className="page">
        <LoadingSpinner label="Loading project…" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="page">
        <Link to="/" className="back-link">
          ← Back to projects
        </Link>
        <ErrorBanner message={error} onRetry={load} />
      </div>
    );
  }

  if (!project) return null;

  return (
    <div className="page">
      <Link to="/" className="back-link">
        ← Back to projects
      </Link>

      <div className="page-header">
        <div>
          <h1>{project.name}</h1>
          {project.description && <p className="page-subtitle">{project.description}</p>}
        </div>
        <button type="button" className="btn btn-primary" onClick={openCreate}>
          + New Task
        </button>
      </div>

      <div className="filter-bar">
        {statusFilters.map((f) => (
          <button
            key={f.value}
            type="button"
            className={`chip ${statusFilter === f.value ? "chip-active" : ""}`}
            onClick={() => setStatusFilter(f.value)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {visibleTasks.length === 0 && (
        <div className="empty-state">
          <p>
            {tasks.length === 0
              ? "No tasks yet. Add the first task for this project."
              : "No tasks match this filter."}
          </p>
        </div>
      )}

      {visibleTasks.length > 0 && (
        <table className="task-table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Due date</th>
              <th aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {visibleTasks.map((task) => (
              <tr key={task.id}>
                <td>
                  <div className="task-title">{task.title}</div>
                  {task.description && <div className="task-description">{task.description}</div>}
                </td>
                <td>
                  <select
                    className={`status-select status-select-${task.status.toLowerCase()}`}
                    value={task.status}
                    onChange={(e) => handleQuickStatusChange(task, e.target.value as TaskStatus)}
                    aria-label={`Change status for ${task.title}`}
                  >
                    <option value="TODO">To Do</option>
                    <option value="IN_PROGRESS">In Progress</option>
                    <option value="DONE">Done</option>
                  </select>
                </td>
                <td>
                  <PriorityBadge priority={task.priority} />
                </td>
                <td>{task.dueDate ?? "—"}</td>
                <td>
                  <div className="card-actions">
                    <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEdit(task)}>
                      Edit
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(task)}>
                      Delete
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {modalMode && (
        <Modal title={modalMode === "edit" ? "Edit task" : "New task"} onClose={() => setModalMode(null)}>
          <TaskForm
            projectId={id}
            initial={editingTask ?? undefined}
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
