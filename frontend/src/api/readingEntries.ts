import apiClient from './client';
import type { PagedResponse } from '../types/common';
import type {
  CreateReadingEntryRequest,
  ReadingEntry,
  ReadingEntryListParams,
  ReadingSummary,
  UpdateReadingEntryRequest,
} from '../types/reading';

export const readingEntriesApi = {
  list: async (params: ReadingEntryListParams = {}): Promise<PagedResponse<ReadingEntry>> => {
    const response = await apiClient.get<PagedResponse<ReadingEntry>>('/reading-entries', { params });
    return response.data;
  },

  summary: async (year?: number): Promise<ReadingSummary> => {
    const response = await apiClient.get<ReadingSummary>('/reading-entries/summary', { params: { year } });
    return response.data;
  },

  get: async (id: string): Promise<ReadingEntry> => {
    const response = await apiClient.get<ReadingEntry>(`/reading-entries/${id}`);
    return response.data;
  },

  create: async (data: CreateReadingEntryRequest): Promise<ReadingEntry> => {
    const response = await apiClient.post<ReadingEntry>('/reading-entries', data);
    return response.data;
  },

  update: async (id: string, data: UpdateReadingEntryRequest): Promise<ReadingEntry> => {
    const response = await apiClient.put<ReadingEntry>(`/reading-entries/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/reading-entries/${id}`);
  },
};
