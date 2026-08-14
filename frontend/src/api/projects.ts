import { apiClient } from "./client";
import type { ProjectRequest, ProjectResponse } from "./types";

export const projectsApi = {
  list: () => apiClient.get<ProjectResponse[]>("/api/projects"),
  get: (id: number) => apiClient.get<ProjectResponse>(`/api/projects/${id}`),
  create: (payload: ProjectRequest) => apiClient.post<ProjectResponse>("/api/projects", payload),
  update: (id: number, payload: ProjectRequest) =>
    apiClient.put<ProjectResponse>(`/api/projects/${id}`, payload),
  remove: (id: number) => apiClient.delete<void>(`/api/projects/${id}`),
};
