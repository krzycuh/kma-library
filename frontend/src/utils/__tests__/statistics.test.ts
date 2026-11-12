import { describe, expect, it } from 'vitest';
import type { ReadingPeriodStats, ReadingStatistics } from '../../types/statistics';
import {
  averagePerYear,
  bestYear,
  formatAverage,
  maxCounted,
  parseYearParam,
  previousYearBooks,
  selectableYears,
  statsForYear,
  yearSeries,
} from '../statistics';

const period = (year: number | null, books: number): ReadingPeriodStats => ({
  year,
  books,
  uniqueBooks: books,
  ratedBooks: 0,
  averageRating: null,
  ratings: [],
  months: [],
  withoutMonth: 0,
  seasons: [],
  topAuthors: [],
});

const stats: ReadingStatistics = {
  currentYear: 2026,
  total: period(null, 30),
  years: [period(2025, 12), period(2024, 12), period(2022, 6)],
  withoutYear: 0,
};

describe('statistics utils', () => {
  it('picks the item with the highest count and ignores all-zero data', () => {
    expect(maxCounted([{ count: 0 }, { count: 3 }, { count: 2 }])).toEqual({ count: 3 });
    expect(maxCounted([{ count: 0 }, { count: 0 }])).toBeNull();
    expect(maxCounted([])).toBeNull();
  });

  it('keeps the first item on a tie', () => {
    expect(maxCounted([{ id: 'a', count: 2 }, { id: 'b', count: 2 }])?.id).toBe('a');
  });

  it('fills the gaps of the year axis with zeros', () => {
    expect(yearSeries(stats)).toEqual([
      { year: 2022, books: 6 },
      { year: 2023, books: 0 },
      { year: 2024, books: 12 },
      { year: 2025, books: 12 },
    ]);
    expect(yearSeries({ ...stats, years: [] })).toEqual([]);
  });

  it('offers every year with entries plus the current year, newest first', () => {
    expect(selectableYears(stats)).toEqual([2026, 2025, 2024, 2022]);
  });

  it('parses the year parameter only when it is selectable', () => {
    const years = [2026, 2025];
    expect(parseYearParam('2025', years)).toBe(2025);
    expect(parseYearParam('2019', years)).toBeNull();
    expect(parseYearParam('abc', years)).toBeNull();
    expect(parseYearParam(null, years)).toBeNull();
  });

  it('returns an empty block for a year without entries', () => {
    const empty = statsForYear(stats, 2026);
    expect(empty.books).toBe(0);
    expect(empty.months).toHaveLength(12);
    expect(empty.seasons.map((s) => s.season)).toEqual(['SPRING', 'SUMMER', 'AUTUMN', 'WINTER']);
    expect(statsForYear(stats, 2024).books).toBe(12);
  });

  it('averages books over years with entries', () => {
    expect(averagePerYear(stats)).toBe(10);
    expect(averagePerYear({ ...stats, years: [] })).toBeNull();
  });

  it('best year prefers the most recent one on a tie', () => {
    expect(bestYear(stats)?.year).toBe(2025);
    expect(bestYear({ ...stats, years: [] })).toBeNull();
  });

  it('finds the previous year count', () => {
    expect(previousYearBooks(stats, 2025)).toBe(12);
    expect(previousYearBooks(stats, 2024)).toBeNull();
  });

  it('formats averages with a comma', () => {
    expect(formatAverage(4.25)).toBe('4,3');
    expect(formatAverage(null)).toBe('—');
  });
});
