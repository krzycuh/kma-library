import { describe, expect, it } from 'vitest';
import { formatAuthors, formatDate, formatFinishedPeriod, formatRating } from '../format';

describe('format utils', () => {
  it('formats finished period with month', () => {
    expect(formatFinishedPeriod(2024, 3)).toBe('marzec 2024');
  });

  it('formats finished period with year only', () => {
    expect(formatFinishedPeriod(2024, null)).toBe('2024');
  });

  it('formats unknown finished period', () => {
    expect(formatFinishedPeriod(null, null)).toBe('nie pamiętam');
  });

  it('formats ISO date', () => {
    expect(formatDate('2024-03-05')).toMatch(/5 mar 2024/);
    expect(formatDate(null)).toBe('—');
  });

  it('joins authors', () => {
    expect(formatAuthors(['Stanisław Lem', 'Olga Tokarczuk'])).toBe('Stanisław Lem, Olga Tokarczuk');
  });

  it('formats rating', () => {
    expect(formatRating(4.25)).toBe('4.3');
    expect(formatRating(null)).toBe('—');
  });
});
