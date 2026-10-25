import apiClient from './client';
import type { PagedResponse } from '../types/common';
import type { AddShelfItemRequest, Library, LibraryMember, ShelfItem, ShelfItemListParams } from '../types/shelf';

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

  rename: async (libraryId: string, name: string): Promise<Library> => {
    const response = await apiClient.patch<Library>(`/libraries/${libraryId}`, { name });
    return response.data;
  },

  members: async (libraryId: string): Promise<LibraryMember[]> => {
    const response = await apiClient.get<LibraryMember[]>(`/libraries/${libraryId}/members`);
    return response.data;
  },

  /** Owner only: adds an existing user (by e-mail) as a member. */
  invite: async (libraryId: string, email: string): Promise<LibraryMember> => {
    const response = await apiClient.post<LibraryMember>(`/libraries/${libraryId}/members`, { email });
    return response.data;
  },

  /** Owner removes a member, or a member leaves (userId = own id). */
  removeMember: async (libraryId: string, userId: string): Promise<void> => {
    await apiClient.delete(`/libraries/${libraryId}/members/${userId}`);
  },
};
