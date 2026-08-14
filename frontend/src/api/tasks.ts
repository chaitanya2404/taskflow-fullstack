import { apiClient } from "./client";
import type { TaskRequest, TaskResponse, TaskStatus } from "./types";

export interface TaskFilters {
  projectId?: number;
  status?: TaskStatus;
}

function buildQuery(filters?: TaskFilters): string {
  if (!filters) return "";
  const params = new URLSearchParams();
  if (filters.projectId !== undefined) params.set("projectId", String(filters.projectId));
  if (filters.status !== undefined) params.set("status", filters.status);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

export const tasksApi = {
  list: (filters?: TaskFilters) => apiClient.get<TaskResponse[]>(`/api/tasks${buildQuery(filters)}`),
  get: (id: number) => apiClient.get<TaskResponse>(`/api/tasks/${id}`),
  create: (payload: TaskRequest) => apiClient.post<TaskResponse>("/api/tasks", payload),
  update: (id: number, payload: TaskRequest) =>
    apiClient.put<TaskResponse>(`/api/tasks/${id}`, payload),
  remove: (id: number) => apiClient.delete<void>(`/api/tasks/${id}`),
};
