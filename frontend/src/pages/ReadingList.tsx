import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { readingEntriesApi } from '../api/readingEntries';
import type { ReadingEntry } from '../types/reading';
import { t } from '../i18n';
import { PageHeader } from '../components/PageHeader';
import { BookCover } from '../components/BookCover';
import { RatingStars } from '../components/RatingStars';
import { Pagination } from '../components/Pagination';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { useToast } from '../hooks/useToast';
import { formatAuthors, formatFinishedPeriod } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';

const PAGE_SIZE = 20;
const selectClasses =
  'rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

// Edit and delete share one box: a link and a button line up, and the mobile 44px button minimum (App.css)
// doesn't push "delete" below "edit" or stretch the card
const entryActionClasses = 'inline-flex min-h-0 min-w-0 items-center py-1 font-medium';

function excerpt(text: string | null, max = 140): string {
  if (!text) return '';
  return text.length > max ? `${text.slice(0, max).trimEnd()}…` : text;
}

/**
 * List of books the current user has read, with search, year and rating filters.
 */
export function ReadingList() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [year, setYear] = useState<number | undefined>(undefined);
  const [rating, setRating] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [toDelete, setToDelete] = useState<ReadingEntry | null>(null);

  useEffect(() => {
    const handle = setTimeout(() => {
      setSearch(searchInput.trim());
      setPage(0);
    }, 300);
    return () => clearTimeout(handle);
  }, [searchInput]);

  const params = { search: search || undefined, year, rating, page, pageSize: PAGE_SIZE };

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['reading-entries', params],
    queryFn: () => readingEntriesApi.list(params),
  });

  const { data: summary } = useQuery({
    queryKey: ['reading-summary', undefined],
    queryFn: () => readingEntriesApi.summary(),
    staleTime: 60_000,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => readingEntriesApi.delete(id),
    onSuccess: () => {
      toast.success(t.reading.deleted);
      setToDelete(null);
      queryClient.invalidateQueries({ queryKey: ['reading-entries'] });
      queryClient.invalidateQueries({ queryKey: ['reading-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const hasFilters = Boolean(search || year || rating);

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={t.reading.title}
        description={t.reading.subtitle}
        actions={
          <Link
            to="/read/new"
            className="inline-block rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            + {t.reading.add}
          </Link>
        }
      />

      <div className="mb-4 flex flex-col gap-2 sm:flex-row sm:items-end">
        <div className="flex-1">
          <label htmlFor="reading-search" className="sr-only">
            {t.common.search}
          </label>
          <input
            id="reading-search"
            type="search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            placeholder={t.common.searchPlaceholder}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>
        <div className="flex gap-2">
          <div className="flex flex-col">
            <label htmlFor="reading-year" className="text-xs font-medium text-gray-600">
              {t.reading.filterYear}
            </label>
            <select
              id="reading-year"
              value={year ?? ''}
              onChange={(e) => {
                setYear(e.target.value ? Number(e.target.value) : undefined);
                setPage(0);
              }}
              className={selectClasses}
            >
              <option value="">{t.reading.allYears}</option>
              {(summary?.years ?? []).map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col">
            <label htmlFor="reading-rating" className="text-xs font-medium text-gray-600">
              {t.reading.filterRating}
            </label>
            <select
              id="reading-rating"
              value={rating ?? ''}
              onChange={(e) => {
                setRating(e.target.value ? Number(e.target.value) : undefined);
                setPage(0);
              }}
              className={selectClasses}
            >
              <option value="">{t.reading.anyRating}</option>
              {[5, 4, 3, 2, 1].map((r) => (
                <option key={r} value={r}>
                  {'★'.repeat(r)}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {isLoading && <LoadingSkeleton variant="card" count={3} />}
      {isError && <ErrorMessage message={getApiErrorMessage(error)} retry={() => refetch()} />}

      {data && data.items.length === 0 && (
        <EmptyState
          icon="📖"
          title={hasFilters ? t.reading.emptyFiltered : t.reading.emptyTitle}
          description={hasFilters ? '' : t.reading.emptyText}
          action={hasFilters ? undefined : { label: t.reading.add, onClick: () => navigate('/read/new') }}
        />
      )}

      {data && data.items.length > 0 && (
        <>
          <ul className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {data.items.map((entry) => (
              <li key={entry.id} className="flex min-w-0 gap-3 rounded-lg bg-white p-4 pb-3 shadow-sm">
                <BookCover title={entry.book.title} coverUrl={entry.book.coverUrl} size="md" />
                <div className="min-w-0 flex-1">
                  <Link to={`/books/${entry.book.id}`} className="block truncate font-semibold text-gray-900 hover:text-blue-700">
                    {entry.book.title}
                  </Link>
                  <p className="truncate text-sm text-gray-600">{formatAuthors(entry.book.authors)}</p>
                  <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1">
                    <span className="text-xs text-gray-500">{formatFinishedPeriod(entry.finishedYear, entry.finishedMonth)}</span>
                    <RatingStars value={entry.rating} size="sm" />
                  </div>
                  {entry.review && <p className="mt-1 text-sm text-gray-700">{excerpt(entry.review)}</p>}
                  <div className="mt-1 flex items-center gap-5 text-sm">
                    <Link to={`/read/${entry.id}/edit`} className={`${entryActionClasses} text-blue-600 hover:text-blue-700`}>
                      {t.common.edit}
                    </Link>
                    <button
                      type="button"
                      onClick={() => setToDelete(entry)}
                      className={`${entryActionClasses} text-red-600 hover:text-red-700`}
                    >
                      {t.common.delete}
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}

      <ConfirmationModal
        isOpen={toDelete !== null}
        onClose={() => setToDelete(null)}
        onConfirm={() => toDelete && deleteMutation.mutate(toDelete.id)}
        title={t.reading.deleteConfirmTitle}
        description={t.reading.deleteConfirmText}
        confirmText={t.common.delete}
        cancelText={t.common.cancel}
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
