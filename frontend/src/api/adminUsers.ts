import apiClient from './client';
import type { PagedResponse } from '../types/common';
import type { UserSummary } from '../types/admin';

export const adminUsersApi = {
  list: async (params: { searchTerm?: string; page?: number; pageSize?: number } = {}): Promise<PagedResponse<UserSummary>> => {
    const response = await apiClient.get<PagedResponse<UserSummary>>('/admin/users', { params });
    return response.data;
  },

  setActive: async (id: string, active: boolean): Promise<UserSummary> => {
    const response = await apiClient.patch<UserSummary>(`/admin/users/${id}/active`, { active });
    return response.data;
  },

  /** Starts previewing the user's account (audited); the preview itself is a request header, see utils/viewAs.ts. */
  viewAs: async (id: string): Promise<UserSummary> => {
    const response = await apiClient.post<UserSummary>(`/admin/users/${id}/view-as`);
    return response.data;
  },

  resetPassword: async (id: string, newPassword: string): Promise<void> => {
    await apiClient.patch(`/admin/users/${id}/password`, { newPassword });
  },
};
