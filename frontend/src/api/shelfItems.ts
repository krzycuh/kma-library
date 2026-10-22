import apiClient from './client';
import type { LendShelfItemRequest, ReturnLoanRequest, ShelfItem, UpdateShelfItemRequest } from '../types/shelf';

export const shelfItemsApi = {
  get: async (id: string): Promise<ShelfItem> => {
    const response = await apiClient.get<ShelfItem>(`/shelf-items/${id}`);
    return response.data;
  },

  update: async (id: string, data: UpdateShelfItemRequest): Promise<ShelfItem> => {
    const response = await apiClient.put<ShelfItem>(`/shelf-items/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/shelf-items/${id}`);
  },

  lend: async (id: string, data: LendShelfItemRequest): Promise<ShelfItem> => {
    const response = await apiClient.post<ShelfItem>(`/shelf-items/${id}/loans`, data);
    return response.data;
  },

  return: async (id: string, data: ReturnLoanRequest = {}): Promise<ShelfItem> => {
    const response = await apiClient.post<ShelfItem>(`/shelf-items/${id}/return`, data);
    return response.data;
  },
};
