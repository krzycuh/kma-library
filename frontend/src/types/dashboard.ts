import type { ReadingEntry } from './reading';
import type { ShelfItem } from './shelf';

export interface Dashboard {
  year: number;
  booksReadThisYear: number;
  booksReadTotal: number;
  averageRatingThisYear: number | null;
  lentCount: number;
  overdueCount: number;
  onShelfCount: number;
  recentReadingEntries: ReadingEntry[];
  activeLoans: ShelfItem[];
  defaultLibraryId: string | null;
}
