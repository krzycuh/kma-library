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

  resetPassword: async (id: string, newPassword: string): Promise<void> => {
    await apiClient.patch(`/admin/users/${id}/password`, { newPassword });
  },
};
