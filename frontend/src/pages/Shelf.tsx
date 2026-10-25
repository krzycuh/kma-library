import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { librariesApi } from '../api/libraries';
import { PageHeader } from '../components/PageHeader';
import { LibrarySwitcher } from '../components/LibrarySwitcher';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { Pagination } from '../components/Pagination';
import { BookCover } from '../components/BookCover';
import { ShelfItemStatusBadge } from '../components/ShelfItemStatusBadge';
import { useCurrentLibrary } from '../hooks/useLibraries';
import { t } from '../i18n';
import type { ShelfItemStatus } from '../types/shelf';
import { formatAuthors, formatDate } from '../utils/format';

type Tab = 'ALL' | 'ON_SHELF' | 'LENT';

const tabs: { key: Tab; label: string }[] = [
  { key: 'ALL', label: t.shelf.tabAll },
  { key: 'ON_SHELF', label: t.shelf.tabOnShelf },
  { key: 'LENT', label: t.shelf.tabLent },
];

const PAGE_SIZE = 20;

/**
 * Bookshelf: all copies in the user's library with status tabs and search.
 */
export function Shelf() {
  const navigate = useNavigate();
  const { library, isLoading: libraryLoading, isError: libraryError } = useCurrentLibrary();
  const [tab, setTab] = useState<Tab>('ALL');
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => clearTimeout(handle);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [tab, debouncedSearch]);

  const status: ShelfItemStatus | undefined = tab === 'ALL' ? undefined : tab;
  const params = { status, search: debouncedSearch || undefined, page, pageSize: PAGE_SIZE };

  const itemsQuery = useQuery({
    queryKey: ['shelf-items', library?.id, params],
    queryFn: () => librariesApi.items(library!.id, params),
    enabled: !!library,
  });

  const hasFilters = tab !== 'ALL' || debouncedSearch.length > 0;

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={t.shelf.title}
        description={t.shelf.subtitle}
        actions={
          <div className="flex gap-2">
            <Link
              to="/shelf/members"
              className="inline-flex items-center rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              <span aria-hidden="true">👥</span>&nbsp;{t.members.link}
            </Link>
            <Link
              to="/shelf/new"
              className="inline-flex items-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              + {t.shelf.add}
            </Link>
          </div>
        }
      />

      <div className="mb-4">
        <LibrarySwitcher />
      </div>

      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="inline-flex rounded-lg bg-gray-100 p-1" role="tablist">
          {tabs.map((item) => (
            <button
              key={item.key}
              type="button"
              role="tab"
              aria-selected={tab === item.key}
              onClick={() => setTab(item.key)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                tab === item.key ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>
        <label className="sr-only" htmlFor="shelf-search">
          {t.common.search}
        </label>
        <input
          id="shelf-search"
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t.common.searchPlaceholder}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:w-64"
        />
      </div>

      {libraryLoading || (library && itemsQuery.isLoading) ? (
        <LoadingSkeleton />
      ) : libraryError || itemsQuery.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => itemsQuery.refetch()} />
      ) : !library ? (
        <EmptyState icon="📚" title={t.shelf.emptyTitle} description={t.shelf.noLibrary} />
      ) : itemsQuery.data && itemsQuery.data.items.length === 0 ? (
        hasFilters ? (
          <EmptyState icon="🔍" title={t.common.notFound} description={t.shelf.emptyFiltered} />
        ) : (
          <EmptyState
            icon="📚"
            title={t.shelf.emptyTitle}
            description={t.shelf.emptyText}
            action={{ label: t.shelf.add, onClick: () => navigate('/shelf/new') }}
          />
        )
      ) : (
        <>
          <p className="mb-2 text-sm text-gray-500">{t.shelf.itemsCount(itemsQuery.data?.totalItems ?? 0)}</p>
          <ul className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {itemsQuery.data?.items.map((item) => {
              const overdue = item.activeLoan?.overdue ?? false;
              return (
                <li key={item.id} className="rounded-lg bg-white p-4 shadow-sm">
                  <div className="flex gap-3">
                    <BookCover title={item.book.title} coverUrl={item.book.coverUrl} />
                    <div className="min-w-0 flex-1">
                      <Link
                        to={`/shelf/${item.id}`}
                        className="block truncate font-semibold text-gray-900 hover:text-blue-700"
                      >
                        {item.book.title}
                      </Link>
                      <p className="truncate text-sm text-gray-600">{formatAuthors(item.book.authors)}</p>
                      <div className="mt-2">
                        <ShelfItemStatusBadge status={item.status} overdue={overdue} />
                      </div>
                      {item.activeLoan && (
                        <p className="mt-1 text-sm text-gray-700">
                          {t.shelf.lentTo(`${item.activeLoan.borrowerFirstName} ${item.activeLoan.borrowerLastName}`)}{' '}
                          <span className="text-gray-500">{t.shelf.lentSince(formatDate(item.activeLoan.lentOn))}</span>
                        </p>
                      )}
                    </div>
                  </div>
                </li>
              );
            })}
          </ul>
          <Pagination page={page} totalPages={itemsQuery.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </div>
  );
}
