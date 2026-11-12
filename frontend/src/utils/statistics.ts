import type { ReadingPeriodStats, ReadingStatistics } from '../types/statistics';

export interface Counted {
  count: number;
}

/** The item with the highest count, or null when nothing was counted at all. Ties go to the first one. */
export function maxCounted<T extends Counted>(items: T[]): T | null {
  let best: T | null = null;
  for (const item of items) {
    if (item.count > 0 && (best === null || item.count > best.count)) best = item;
  }
  return best;
}

/** Statistics of the year, or an empty block for a year without entries (e.g. a fresh current year). */
export function statsForYear(stats: ReadingStatistics, year: number): ReadingPeriodStats {
  return (
    stats.years.find((y) => y.year === year) ?? {
      year,
      books: 0,
      uniqueBooks: 0,
      ratedBooks: 0,
      averageRating: null,
      ratings: [1, 2, 3, 4, 5].map((rating) => ({ rating, count: 0 })),
      months: Array.from({ length: 12 }, (_, i) => ({ month: i + 1, count: 0 })),
      withoutMonth: 0,
      seasons: (['SPRING', 'SUMMER', 'AUTUMN', 'WINTER'] as const).map((season) => ({ season, count: 0 })),
      topAuthors: [],
      pages: 0,
      booksWithoutPages: 0,
      pagesByMonth: Array.from({ length: 12 }, (_, i) => ({ month: i + 1, count: 0 })),
      longestBook: null,
    }
  );
}

/** Books per year on a continuous time axis: years without entries between the first and the last count as zero. */
export function yearSeries(stats: ReadingStatistics): { year: number; books: number }[] {
  if (stats.years.length === 0) return [];
  const byYear = new Map(stats.years.map((y) => [y.year as number, y.books]));
  const first = Math.min(...byYear.keys());
  const last = Math.max(...byYear.keys());
  return Array.from({ length: last - first + 1 }, (_, i) => ({ year: first + i, books: byYear.get(first + i) ?? 0 }));
}

/** Years to offer in the period picker: every year with entries plus the current year, newest first. */
export function selectableYears(stats: ReadingStatistics): number[] {
  const years = new Set(stats.years.map((y) => y.year as number));
  years.add(stats.currentYear);
  return [...years].sort((a, b) => b - a);
}

/** Reads the `year` query parameter: a year from the picker, or null for all years. */
export function parseYearParam(raw: string | null, years: number[]): number | null {
  if (!raw) return null;
  const year = Number(raw);
  return years.includes(year) ? year : null;
}

/** Books per year averaged over the years that have entries, 1 decimal. */
export function averagePerYear(stats: ReadingStatistics): number | null {
  if (stats.years.length === 0) return null;
  const dated = stats.years.reduce((sum, y) => sum + y.books, 0);
  return Math.round((dated / stats.years.length) * 10) / 10;
}

/** The year with most books (the most recent one on a tie), or null without any dated entry. */
export function bestYear(stats: ReadingStatistics): ReadingPeriodStats | null {
  return maxCounted(stats.years.map((y) => ({ ...y, count: y.books })));
}

/** Books read in the year before the given one; null when that year has no entries. */
export function previousYearBooks(stats: ReadingStatistics, year: number): number | null {
  return stats.years.find((y) => y.year === year - 1)?.books ?? null;
}

/** Pages per book over the readings with a known page count, rounded. */
export function averagePages(period: ReadingPeriodStats): number | null {
  const counted = period.books - period.booksWithoutPages;
  return counted > 0 ? Math.round(period.pages / counted) : null;
}

/** Formats an average with one decimal, in Polish notation (comma). */
export function formatAverage(value: number | null): string {
  if (value == null) return '—';
  return value.toFixed(1).replace('.', ',');
}
