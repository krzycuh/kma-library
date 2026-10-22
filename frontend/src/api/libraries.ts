import apiClient from './client';
import type { PagedResponse } from '../types/common';
import type { AddShelfItemRequest, Library, ShelfItem, ShelfItemListParams } from '../types/shelf';

export const librariesApi = {
  list: async (): Promise<Library[]> => {
    const response = await apiClient.get<Library[]>('/libraries');
    return response.data;
  },

  items: async (libraryId: string, params: ShelfItemListParams = {}): Promise<PagedResponse<ShelfItem>> => {
    const response = await apiClient.get<PagedResponse<ShelfItem>>(`/libraries/${libraryId}/items`, { params });
    return response.data;
  },

  addItem: async (libraryId: string, data: AddShelfItemRequest): Promise<ShelfItem> => {
    const response = await apiClient.post<ShelfItem>(`/libraries/${libraryId}/items`, data);
    return response.data;
  },

  activeLoans: async (libraryId: string): Promise<ShelfItem[]> => {
    const response = await apiClient.get<ShelfItem[]>(`/libraries/${libraryId}/loans`);
    return response.data;
  },
};
