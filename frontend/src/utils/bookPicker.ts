import type { Book, BookReference } from '../types/book';
import { t } from '../i18n';
import { formatAuthors } from './format';
import { isValidIsbn } from './isbn';
import { coverUrlError } from './coverUrl';

export interface BookPickerValue {
  selected: Book | null;
  title: string;
  authors: string;
  isbn: string;
  coverUrl: string;
}

export const emptyBookPickerValue: BookPickerValue = { selected: null, title: '', authors: '', isbn: '', coverUrl: '' };

export function bookPickerValueFromBook(book: Book): BookPickerValue {
  return { selected: book, title: book.title, authors: formatAuthors(book.authors), isbn: book.isbn ?? '', coverUrl: book.coverUrl ?? '' };
}

/**
 * Converts the picker state into the API's BookReference.
 */
export function toBookReference(value: BookPickerValue): BookReference {
  if (value.selected) return { bookId: value.selected.id };
  return {
    title: value.title.trim(),
    authors: value.authors.trim(),
    isbn: value.isbn.trim() || undefined,
    coverUrl: value.coverUrl.trim() || undefined,
  };
}

export function validateBookPicker(value: BookPickerValue): string | null {
  if (value.selected) return null;
  if (!value.title.trim()) return t.book.titleRequired;
  if (!value.authors.trim()) return t.book.authorsRequired;
  if (value.isbn.trim() && !isValidIsbn(value.isbn)) return t.book.invalidIsbn;
  return coverUrlError(value.coverUrl);
}
