export type Role = "USER" | "ADMIN";

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

/** The account fields the UI needs; kept separate from the token itself. */
export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: Role;
}

/**
 * Body of POST /api/auth/register and POST /api/auth/login. Note the account id
 * arrives as `userId` here (the response also carries token metadata), and is
 * normalised to AuthUser.id by the auth API module.
 */
export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: number;
  username: string;
  email: string;
  role: Role;
}

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
