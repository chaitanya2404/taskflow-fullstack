import { useState, type FormEvent } from "react";
import type { ProjectRequest, ProjectResponse } from "../api/types";

export function ProjectForm({
  initial,
  submitting,
  errorMessage,
  onSubmit,
  onCancel,
}: {
  initial?: ProjectResponse;
  submitting: boolean;
  errorMessage: string | null;
  onSubmit: (payload: ProjectRequest) => void;
  onCancel: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    onSubmit({ name: name.trim(), description: description.trim() });
  }

  return (
    <form onSubmit={handleSubmit} className="form">
      {errorMessage && <div className="form-error">{errorMessage}</div>}
      <label className="field">
        <span>Project name</span>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g. Website Revamp"
          required
          maxLength={120}
          autoFocus
        />
      </label>
      <label className="field">
        <span>Description</span>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="What is this project about?"
          rows={4}
          maxLength={1000}
        />
      </label>
      <div className="form-actions">
        <button type="button" className="btn btn-secondary" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? "Saving…" : initial ? "Save changes" : "Create project"}
        </button>
      </div>
    </form>
  );
}
