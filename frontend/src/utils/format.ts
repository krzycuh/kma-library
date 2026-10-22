import dayjs from 'dayjs';
import 'dayjs/locale/pl';
import { t } from '../i18n';

dayjs.locale('pl');

/**
 * Formats an ISO date (YYYY-MM-DD) as a human readable Polish date.
 */
export function formatDate(value: string | null | undefined): string {
  if (!value) return '—';
  return dayjs(value).format('D MMM YYYY');
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '—';
  return dayjs(value).format('D MMM YYYY, HH:mm');
}

/**
 * Formats a finished period (year + optional month) e.g. "marzec 2024" or "2024".
 */
export function formatFinishedPeriod(year: number | null | undefined, month: number | null | undefined): string {
  if (!year) return t.reading.unknownDate;
  if (!month) return String(year);
  return `${t.months[month - 1]} ${year}`;
}

export function formatAuthors(authors: string[]): string {
  return authors.join(', ');
}

export function formatRating(value: number | null | undefined): string {
  if (value == null) return '—';
  return value.toFixed(1);
}

export function todayIso(): string {
  return dayjs().format('YYYY-MM-DD');
}

export function currentYear(): number {
  return dayjs().year();
}
