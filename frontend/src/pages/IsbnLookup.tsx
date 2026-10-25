import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { booksApi } from '../api/books';
import { PageHeader } from '../components/PageHeader';
import { BookCover } from '../components/BookCover';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { BarcodeScanner } from '../components/BarcodeScanner';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import { formatAuthors } from '../utils/format';
import { toIsbn13 } from '../utils/isbn';
import { isbnLookupErrorMessage } from '../utils/isbnLookup';

type Destination = 'shelf' | 'read';

const destinationPath: Record<Destination, string> = { shelf: '/shelf/new', read: '/read/new' };

/**
 * /isbn (optional ?isbn=, ?scan=1 opens the scanner): scan or type an ISBN → preview from our catalog
 * or the external one → add to the shelf or as read, with the book prefilled.
 */
export function IsbnLookup() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const isbn = toIsbn13(searchParams.get('isbn') ?? '');
  const [input, setInput] = useState(searchParams.get('isbn') ?? '');
  const [inputError, setInputError] = useState<string | null>(null);
  const [scanOpen, setScanOpen] = useState(searchParams.get('scan') === '1');

  useEffect(() => {
    setInput(searchParams.get('isbn') ?? '');
  }, [searchParams]);

  const lookupQuery = useQuery({
    queryKey: ['book-lookup', isbn],
    queryFn: () => booksApi.lookup(isbn!),
    enabled: isbn !== null,
    retry: false,
    staleTime: 5 * 60_000,
  });

  const addMutation = useMutation({
    mutationFn: async (destination: Destination) => {
      const book = lookupQuery.data?.book ?? (await booksApi.importByIsbn(isbn!));
      return { book, destination };
    },
    onSuccess: ({ book, destination }) => {
      queryClient.invalidateQueries({ queryKey: ['books'] });
      navigate(`${destinationPath[destination]}?bookId=${book.id}`);
    },
    onError: (err) => toast.error(isbnLookupErrorMessage(err)),
  });

  const onScanned = (scanned: string) => {
    setScanOpen(false);
    setInputError(null);
    setSearchParams({ isbn: scanned }, { replace: true });
  };

  const closeScanner = () => {
    setScanOpen(false);
    if (searchParams.has('scan')) {
      const next = new URLSearchParams(searchParams);
      next.delete('scan');
      setSearchParams(next, { replace: true });
    }
  };

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    const normalized = toIsbn13(input);
    if (!normalized) {
      setInputError(t.isbn.invalid);
      return;
    }
    setInputError(null);
    setSearchParams({ isbn: normalized });
  };

  const result = lookupQuery.data;
  const preview = result?.book ?? result?.metadata ?? null;

  return (
    <div className="mx-auto max-w-2xl">
      <BarcodeScanner open={scanOpen} onClose={closeScanner} onDetected={onScanned} />
      <PageHeader title={t.isbn.title} description={t.isbn.subtitle} backTo="/" backLabel={t.nav.home} />

      <form onSubmit={onSubmit} className="rounded-lg bg-white p-4 shadow-sm sm:p-6" noValidate>
        <label htmlFor="isbn-input" className="block text-sm font-medium text-gray-700">
          {t.isbn.label}
        </label>
        <div className="mt-1 flex gap-2">
          <input
            id="isbn-input"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            autoFocus
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={t.isbn.placeholder}
            aria-invalid={inputError !== null}
            aria-describedby={inputError ? 'isbn-input-error' : undefined}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <button
            type="submit"
            disabled={lookupQuery.isFetching}
            className="flex-shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
          >
            {lookupQuery.isFetching ? t.isbn.searching : t.isbn.search}
          </button>
        </div>
        <button
          type="button"
          onClick={() => setScanOpen(true)}
          aria-label={t.scanner.openLabel}
          className="mt-3 flex w-full items-center justify-center gap-2 rounded-lg border border-blue-600 bg-white px-4 py-3 text-sm font-semibold text-blue-700 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
        >
          <span aria-hidden="true">📷</span> {t.scanner.open}
        </button>
        {inputError && (
          <p id="isbn-input-error" className="mt-2 text-sm text-red-700" role="alert">
            {inputError}
          </p>
        )}
      </form>

      {lookupQuery.isFetching && !result && <LoadingSpinner text={t.isbn.searching} />}

      {lookupQuery.isError && (
        <div className="mt-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" role="alert">
          <p className="text-sm text-gray-700">{isbnLookupErrorMessage(lookupQuery.error)}</p>
          <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:gap-4">
            <Link to="/read/new" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              {t.isbn.addManuallyRead}
            </Link>
            <Link to="/shelf/new" className="text-sm font-medium text-blue-600 hover:text-blue-700">
              {t.isbn.addManuallyShelf}
            </Link>
          </div>
        </div>
      )}

      {result && preview && (
        <section className="mt-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="isbn-preview-title">
          <p className="mb-3 text-xs font-medium uppercase tracking-wide text-gray-500">
            {result.book ? t.isbn.inCatalog : t.isbn.foundIn(t.isbn.sourceName[preview.source] ?? preview.source)}
          </p>
          <div className="flex gap-4">
            <BookCover title={preview.title} coverUrl={preview.coverUrl} size="lg" />
            <div className="min-w-0 flex-1 space-y-1">
              <h2 id="isbn-preview-title" className="text-lg font-semibold text-gray-900">
                {preview.title}
              </h2>
              <p className="text-sm text-gray-700">{formatAuthors(preview.authors)}</p>
              <p className="text-sm text-gray-500">
                ISBN {result.isbn}
                {preview.publishedYear ? ` · ${preview.publishedYear}` : ''}
              </p>
              {result.book && (
                <Link to={`/books/${result.book.id}`} className="inline-block pt-1 text-sm font-medium text-blue-600 hover:text-blue-700">
                  {t.isbn.showBook}
                </Link>
              )}
            </div>
          </div>
          <div className="mt-4 flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              onClick={() => addMutation.mutate('shelf')}
              disabled={addMutation.isPending}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
            >
              {addMutation.isPending && addMutation.variables === 'shelf' ? t.isbn.adding : t.isbn.addToShelf}
            </button>
            <button
              type="button"
              onClick={() => addMutation.mutate('read')}
              disabled={addMutation.isPending}
              className="rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
            >
              {addMutation.isPending && addMutation.variables === 'read' ? t.isbn.adding : t.isbn.addAsRead}
            </button>
          </div>
        </section>
      )}
    </div>
  );
}
