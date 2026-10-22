export type BookSource = 'MANUAL' | 'OPEN_LIBRARY' | 'GOOGLE_BOOKS';

export interface Book {
  id: string;
  title: string;
  authors: string[];
  isbn: string | null;
  publishedYear: number | null;
  coverUrl: string | null;
  source: BookSource;
  createdByUserId: string | null;
  createdAt: string;
}

/**
 * Either an existing catalog book (bookId) or data to create one (title + authors).
 */
export interface BookReference {
  bookId?: string;
  title?: string;
  authors?: string;
  isbn?: string;
  publishedYear?: number | null;
}

export interface CreateBookRequest {
  title: string;
  authors: string;
  isbn?: string;
  publishedYear?: number | null;
}

export interface UpdateBookRequest {
  title: string;
  authors: string;
  isbn?: string | null;
  publishedYear?: number | null;
  coverUrl?: string | null;
}
