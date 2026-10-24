import apiClient from './client';
import type { Book, BookLookup, CreateBookRequest, MergeBooksResult, UpdateBookRequest } from '../types/book';

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

  /** Local catalog first, then the external catalog. 404 when nobody knows the ISBN. */
  lookup: async (isbn: string): Promise<BookLookup> => {
    const response = await apiClient.get<BookLookup>('/books/lookup', { params: { isbn } });
    return response.data;
  },

  /** Returns the catalog entry for the ISBN, creating it from external metadata if needed. */
  importByIsbn: async (isbn: string): Promise<Book> => {
    const response = await apiClient.post<Book>('/books/import', { isbn });
    return response.data;
  },
};

export const adminBooksApi = {
  /** Moves reading entries and copies of `id` to `targetId`, then deletes `id`. */
  mergeInto: async (id: string, targetId: string): Promise<MergeBooksResult> => {
    const response = await apiClient.post<MergeBooksResult>(`/admin/books/${id}/merge-into/${targetId}`);
    return response.data;
  },
};
