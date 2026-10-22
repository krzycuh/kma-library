import apiClient from './client';
import type { Book, CreateBookRequest, UpdateBookRequest } from '../types/book';

export const booksApi = {
  search: async (search: string, limit = 10): Promise<Book[]> => {
    const response = await apiClient.get<Book[]>('/books', { params: { search, limit } });
    return response.data;
  },

  get: async (id: string): Promise<Book> => {
    const response = await apiClient.get<Book>(`/books/${id}`);
    return response.data;
  },

  create: async (data: CreateBookRequest): Promise<Book> => {
    const response = await apiClient.post<Book>('/books', data);
    return response.data;
  },

  update: async (id: string, data: UpdateBookRequest): Promise<Book> => {
    const response = await apiClient.put<Book>(`/books/${id}`, data);
    return response.data;
  },
};
