import { useState, type FormEvent } from "react";
import type { TaskPriority, TaskRequest, TaskResponse, TaskStatus } from "../api/types";

const statuses: TaskStatus[] = ["TODO", "IN_PROGRESS", "DONE"];
const priorities: TaskPriority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

export function TaskForm({
  projectId,
  initial,
  submitting,
  errorMessage,
  onSubmit,
  onCancel,
}: {
  projectId: number;
  initial?: TaskResponse;
  submitting: boolean;
  errorMessage: string | null;
  onSubmit: (payload: TaskRequest) => void;
  onCancel: () => void;
}) {
  const [title, setTitle] = useState(initial?.title ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [status, setStatus] = useState<TaskStatus>(initial?.status ?? "TODO");
  const [priority, setPriority] = useState<TaskPriority>(initial?.priority ?? "MEDIUM");
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? "");

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit({
      title: title.trim(),
      description: description.trim(),
      status,
      priority,
      dueDate: dueDate || null,
      projectId,
    });
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      {errorMessage && <div className="form-error">{errorMessage}</div>}
      <label className="field">
        <span>Title</span>
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="e.g. Design homepage mockups"
          required
          maxLength={150}
          autoFocus
        />
      </label>
      <label className="field">
        <span>Description</span>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Add any extra detail…"
          rows={3}
          maxLength={2000}
        />
      </label>
      <div className="field-row">
        <label className="field">
          <span>Status</span>
          <select value={status} onChange={(e) => setStatus(e.target.value as TaskStatus)}>
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s.replace("_", " ")}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          <span>Priority</span>
          <select value={priority} onChange={(e) => setPriority(e.target.value as TaskPriority)}>
            {priorities.map((p) => (
              <option key={p} value={p}>
                {p}
              </option>
            ))}
          </select>
        </label>
      </div>
      <label className="field">
        <span>Due date</span>
        <input type="date" value={dueDate ?? ""} onChange={(e) => setDueDate(e.target.value)} />
      </label>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Saving…" : initial ? "Save changes" : "Create task"}
        </button>
      </div>
    </form>
  );
}
