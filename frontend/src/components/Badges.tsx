import type { TaskPriority, TaskStatus } from "../api/types";

const statusLabels: Record<TaskStatus, string> = {
  TODO: "To Do",
  IN_PROGRESS: "In Progress",
  DONE: "Done",
};

export function StatusBadge({ status }: { status: TaskStatus }) {
  return <span className={`badge badge-status-${status.toLowerCase()}`}>{statusLabels[status]}</span>;
}

export function PriorityBadge({ priority }: { priority: TaskPriority }) {
  return <span className={`badge badge-priority-${priority.toLowerCase()}`}>{priority}</span>;
}
