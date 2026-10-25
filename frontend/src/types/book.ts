export type BookSource = 'MANUAL' | 'OPEN_LIBRARY' | 'GOOGLE_BOOKS' | 'NATIONAL_LIBRARY';

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
  coverUrl?: string;
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

/**
 * Bibliographic data from an external catalog (Open Library / Google Books).
 */
export interface BookMetadata {
  isbn: string;
  title: string;
  authors: string[];
  publishedYear: number | null;
  coverUrl: string | null;
  source: BookSource;
}

/**
 * ISBN lookup result: `book` if the ISBN is already in our catalog, otherwise `metadata`.
 */
export interface BookLookup {
  isbn: string;
  book: Book | null;
  metadata: BookMetadata | null;
}

export interface MergeBooksResult {
  target: Book;
  movedReadingEntries: number;
  movedShelfItems: number;
}
