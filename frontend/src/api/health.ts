import apiClient from './client';

export interface BackendHealth {
  status: string;
  /** Commit SHA the backend was built from, or "dev" */
  version: string;
  releaseTag: string | null;
  buildTime: string | null;
}

export const healthApi = {
  get: async (): Promise<BackendHealth> => {
    const response = await apiClient.get<BackendHealth>('/health');
    return response.data;
  },
};
