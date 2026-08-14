export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";

export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface ProjectResponse {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  taskCount: number;
}

export interface ProjectRequest {
  name: string;
  description: string;
}

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  createdAt: string;
  projectId: number;
  projectName: string;
}

export interface TaskRequest {
  title: string;
  description: string;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  projectId: number;
}

export interface FieldErrorDetail {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path?: string;
  fieldErrors?: FieldErrorDetail[];
}
