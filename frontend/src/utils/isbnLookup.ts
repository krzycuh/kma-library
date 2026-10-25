import type { AxiosError } from 'axios';
import { t } from '../i18n';
import { getApiErrorMessage } from './apiError';

/**
 * Maps errors of /books/lookup and /books/import to messages for the ISBN flow.
 */
export function isbnLookupErrorMessage(error: unknown): string {
  const status = (error as AxiosError)?.response?.status;
  if (status === 404) return t.isbn.notFound;
  if (status === 503) return t.isbn.unavailable;
  if (status === 400) return t.isbn.incomplete;
  return getApiErrorMessage(error);
}
