import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { booksApi } from '../api/books';
import type { Book } from '../types/book';
import { t } from '../i18n';
import { bookPickerValueFromBook, emptyBookPickerValue, type BookPickerValue } from '../utils/bookPicker';
import { formatAuthors } from '../utils/format';

interface BookPickerProps {
  value: BookPickerValue;
  onChange: (value: BookPickerValue) => void;
  disabled?: boolean;
  showIsbn?: boolean;
}

const inputClasses =
  'w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100';

/**
 * Title + author fields with catalog autocomplete. Selecting a suggestion locks the fields
 * to the catalog entry; typing a new title creates a new catalog entry on save.
 */
export function BookPicker({ value, onChange, disabled = false, showIsbn = true }: BookPickerProps) {
  const [open, setOpen] = useState(false);
  const [debounced, setDebounced] = useState('');
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(value.title.trim()), 250);
    return () => clearTimeout(handle);
  }, [value.title]);

  const { data: suggestions = [] } = useQuery({
    queryKey: ['books', 'search', debounced],
    queryFn: () => booksApi.search(debounced, 8),
    enabled: !value.selected && debounced.length >= 2 && open,
    staleTime: 30_000,
  });

  useEffect(() => {
    const onClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClickOutside);
    return () => document.removeEventListener('mousedown', onClickOutside);
  }, []);

  const select = (book: Book) => {
    onChange(bookPickerValueFromBook(book));
    setOpen(false);
  };

  const clear = () => onChange({ ...emptyBookPickerValue, title: value.title });

  const locked = disabled || value.selected !== null;

  return (
    <div ref={containerRef} className="space-y-3">
      <div className="relative">
        <label htmlFor="book-title" className="block text-sm font-medium text-gray-700">
          {t.book.title}
        </label>
        <input
          id="book-title"
          type="text"
          autoComplete="off"
          value={value.title}
          disabled={locked}
          placeholder={t.book.pickExisting}
          onChange={(e) => {
            onChange({ ...value, selected: null, title: e.target.value });
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          className={inputClasses}
          aria-autocomplete="list"
          aria-expanded={open && suggestions.length > 0}
        />
        {open && !value.selected && debounced.length >= 2 && (
          <ul
            role="listbox"
            className="absolute z-20 mt-1 max-h-64 w-full overflow-auto rounded-md border border-gray-200 bg-white shadow-lg"
          >
            {suggestions.length === 0 ? (
              <li className="px-3 py-2 text-sm text-gray-500">{t.book.noSuggestions}</li>
            ) : (
              suggestions.map((book) => (
                <li key={book.id} role="option" aria-selected={false}>
                  <button
                    type="button"
                    onClick={() => select(book)}
                    className="flex w-full flex-col items-start px-3 py-2 text-left hover:bg-blue-50 focus:bg-blue-50 focus:outline-none"
                  >
                    <span className="font-medium text-gray-900">{book.title}</span>
                    <span className="text-sm text-gray-600">
                      {formatAuthors(book.authors)}
                      {book.publishedYear ? ` · ${book.publishedYear}` : ''}
                    </span>
                  </button>
                </li>
              ))
            )}
          </ul>
        )}
      </div>

      {value.selected ? (
        <div className="flex items-center justify-between rounded-md bg-blue-50 px-3 py-2 text-sm">
          <span className="text-blue-900">
            <span className="font-medium">{t.book.fromCatalog}:</span> {formatAuthors(value.selected.authors)}
            {value.selected.isbn ? ` · ISBN ${value.selected.isbn}` : ''}
          </span>
          {!disabled && (
            <button type="button" onClick={clear} className="ml-3 text-blue-700 underline hover:text-blue-900">
              {t.book.clearSelection}
            </button>
          )}
        </div>
      ) : (
        <>
          <div>
            <label htmlFor="book-authors" className="block text-sm font-medium text-gray-700">
              {t.book.authors}
            </label>
            <input
              id="book-authors"
              type="text"
              value={value.authors}
              disabled={disabled}
              onChange={(e) => onChange({ ...value, authors: e.target.value })}
              className={inputClasses}
            />
          </div>
          {showIsbn && (
            <div>
              <label htmlFor="book-isbn" className="block text-sm font-medium text-gray-700">
                {t.book.isbn} <span className="text-gray-400">({t.book.isbnHint})</span>
              </label>
              <input
                id="book-isbn"
                type="text"
                inputMode="numeric"
                value={value.isbn}
                disabled={disabled}
                onChange={(e) => onChange({ ...value, isbn: e.target.value })}
                className={inputClasses}
              />
            </div>
          )}
        </>
      )}
    </div>
  );
}
