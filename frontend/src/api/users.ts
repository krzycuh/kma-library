import apiClient from './client';
import type { ChangePasswordRequest, UpdateProfileRequest, User } from '../types/auth';

export const usersApi = {
  updateProfile: async (data: UpdateProfileRequest): Promise<User> => {
    const response = await apiClient.put<User>('/users/me', data);
    return response.data;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<void> => {
    await apiClient.patch('/users/me/password', data);
  },
};
