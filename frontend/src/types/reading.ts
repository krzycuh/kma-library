import type { Book, BookReference } from './book';

export type ReadingStatus = 'FINISHED';

export interface ReadingEntry {
  id: string;
  book: Book;
  status: ReadingStatus;
  finishedYear: number | null;
  finishedMonth: number | null;
  rating: number | null;
  review: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReadingEntryRequest {
  book: BookReference;
  finishedYear?: number | null;
  finishedMonth?: number | null;
  rating?: number | null;
  review?: string | null;
}

export interface UpdateReadingEntryRequest {
  finishedYear?: number | null;
  finishedMonth?: number | null;
  rating?: number | null;
  review?: string | null;
}

export interface ReadingEntryListParams {
  year?: number;
  rating?: number;
  search?: string;
  bookId?: string;
  page?: number;
  pageSize?: number;
}

export interface ReadingSummary {
  year: number;
  booksThisYear: number;
  booksTotal: number;
  averageRating: number | null;
  topRated: ReadingEntry[];
  years: number[];
}
