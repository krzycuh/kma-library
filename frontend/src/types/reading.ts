import type { Book, BookReference } from './book';

export type ReadingStatus = 'FINISHED';

/** Edition the reader used; shown and edited, but not aggregated in statistics */
export type ReadingFormat = 'PAPER' | 'EBOOK' | 'AUDIOBOOK';

export const READING_FORMATS: readonly ReadingFormat[] = ['PAPER', 'EBOOK', 'AUDIOBOOK'];

export interface ReadingEntry {
  id: string;
  book: Book;
  status: ReadingStatus;
  finishedYear: number | null;
  finishedMonth: number | null;
  rating: number | null;
  review: string | null;
  format: ReadingFormat | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReadingEntryRequest {
  book: BookReference;
  finishedYear?: number | null;
  finishedMonth?: number | null;
  rating?: number | null;
  review?: string | null;
  format?: ReadingFormat | null;
}

export interface UpdateReadingEntryRequest {
  finishedYear?: number | null;
  finishedMonth?: number | null;
  rating?: number | null;
  review?: string | null;
  format?: ReadingFormat | null;
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
