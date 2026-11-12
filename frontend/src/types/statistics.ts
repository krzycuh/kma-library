export type Season = 'SPRING' | 'SUMMER' | 'AUTUMN' | 'WINTER';

export interface RatingCount {
  rating: number;
  count: number;
}

export interface MonthCount {
  month: number;
  count: number;
}

export interface SeasonCount {
  season: Season;
  count: number;
}

export interface AuthorCount {
  author: string;
  count: number;
}

export interface LongestBook {
  bookId: string;
  title: string;
  authors: string[];
  pageCount: number;
}

/** Statistics of one period: a calendar year, or all years together (year = null). */
export interface ReadingPeriodStats {
  year: number | null;
  books: number;
  uniqueBooks: number;
  ratedBooks: number;
  averageRating: number | null;
  ratings: RatingCount[];
  months: MonthCount[];
  withoutMonth: number;
  seasons: SeasonCount[];
  topAuthors: AuthorCount[];
  /** Pages of the readings whose book has a page count */
  pages: number;
  booksWithoutPages: number;
  pagesByMonth: MonthCount[];
  longestBook: LongestBook | null;
}

export interface ReadingStatistics {
  currentYear: number;
  total: ReadingPeriodStats;
  /** Years with at least one entry, newest first */
  years: ReadingPeriodStats[];
  withoutYear: number;
}
