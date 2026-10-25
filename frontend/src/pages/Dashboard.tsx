import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '../api/dashboard';
import { useAuth } from '../hooks/useAuth';
import { t } from '../i18n';
import { BookCover } from '../components/BookCover';
import { RatingStars } from '../components/RatingStars';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ErrorMessage } from '../components/ErrorMessage';
import { ShelfItemStatusBadge } from '../components/ShelfItemStatusBadge';
import { formatAuthors, formatDate, formatFinishedPeriod, formatRating } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';

interface StatTileProps {
  label: string;
  value: string | number;
  hint?: string;
  hintClassName?: string;
  to?: string;
}

function StatTile({ label, value, hint, hintClassName = 'text-gray-500', to }: StatTileProps) {
  const content = (
    <>
      <p className="text-sm font-medium text-gray-600">{label}</p>
      <p className="mt-1 text-3xl font-bold text-gray-900">{value}</p>
      {hint && <p className={`mt-1 text-xs ${hintClassName}`}>{hint}</p>}
    </>
  );
  const classes = 'block rounded-lg bg-white p-4 shadow-sm sm:p-5';
  if (to) {
    return (
      <Link to={to} className={`${classes} transition-colors hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500`}>
        {content}
      </Link>
    );
  }
  return <div className={classes}>{content}</div>;
}

/**
 * Home dashboard: reading stats, shelf counts, recent reads and active loans.
 */
export function Dashboard() {
  const { user } = useAuth();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard'],
    queryFn: dashboardApi.get,
  });

  const firstName = user?.name?.trim().split(/\s+/)[0] || '';

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-4 flex flex-col gap-3 sm:mb-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">{t.dashboard.greeting(firstName)}</h1>
          <p className="mt-1 text-sm text-gray-600">{t.app.tagline}</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            to="/read/new"
            className="rounded-lg bg-blue-600 px-4 py-2 text-center text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.dashboard.addRead}
          </Link>
          <Link
            to="/shelf/new"
            className="rounded-lg border border-blue-600 bg-white px-4 py-2 text-center text-sm font-semibold text-blue-700 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.dashboard.addShelf}
          </Link>
          <Link
            to="/isbn?scan=1"
            className="rounded-lg border border-gray-300 bg-white px-4 py-2 text-center text-sm font-semibold text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.dashboard.addByIsbn}
          </Link>
        </div>
      </div>

      {isLoading && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
        </div>
      )}

      {isError && <ErrorMessage message={getApiErrorMessage(error)} retry={() => refetch()} />}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatTile
              label={t.dashboard.readThisYear(data.year)}
              value={data.booksReadThisYear}
              hint={`${t.reading.total}: ${data.booksReadTotal}`}
              to="/read"
            />
            <StatTile label={t.dashboard.averageRating} value={formatRating(data.averageRatingThisYear)} hint="★ 1–5" />
            <StatTile label={t.dashboard.onShelf} value={data.onShelfCount} to="/shelf" />
            <StatTile
              label={t.dashboard.lent}
              value={data.lentCount}
              hint={data.overdueCount > 0 ? t.dashboard.overdue(data.overdueCount) : undefined}
              hintClassName="text-red-600 font-medium"
              to="/loans"
            />
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-2">
            <section className="rounded-lg bg-white p-4 shadow-sm sm:p-5" aria-labelledby="recent-reads-heading">
              <div className="mb-3 flex items-center justify-between">
                <h2 id="recent-reads-heading" className="text-lg font-semibold text-gray-900">
                  {t.dashboard.recentReads}
                </h2>
                <Link to="/read" className="text-sm font-medium text-blue-600 hover:text-blue-700">
                  {t.dashboard.seeAllRead} →
                </Link>
              </div>
              {data.recentReadingEntries.length === 0 ? (
                <p className="py-6 text-center text-sm text-gray-500">{t.dashboard.emptyReads}</p>
              ) : (
                <ul className="divide-y divide-gray-100">
                  {data.recentReadingEntries.map((entry) => (
                    <li key={entry.id}>
                      <Link
                        to={`/books/${entry.book.id}`}
                        className="flex items-center gap-3 py-3 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
                      >
                        <BookCover title={entry.book.title} coverUrl={entry.book.coverUrl} size="sm" />
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium text-gray-900">{entry.book.title}</p>
                          <p className="truncate text-sm text-gray-600">{formatAuthors(entry.book.authors)}</p>
                          <p className="text-xs text-gray-500">{formatFinishedPeriod(entry.finishedYear, entry.finishedMonth)}</p>
                        </div>
                        <RatingStars value={entry.rating} size="sm" />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="rounded-lg bg-white p-4 shadow-sm sm:p-5" aria-labelledby="active-loans-heading">
              <div className="mb-3 flex items-center justify-between">
                <h2 id="active-loans-heading" className="text-lg font-semibold text-gray-900">
                  {t.dashboard.activeLoans}
                </h2>
                <Link to="/loans" className="text-sm font-medium text-blue-600 hover:text-blue-700">
                  {t.dashboard.seeAllLoans} →
                </Link>
              </div>
              {data.activeLoans.length === 0 ? (
                <p className="py-6 text-center text-sm text-gray-500">{t.dashboard.emptyLoans}</p>
              ) : (
                <ul className="divide-y divide-gray-100">
                  {data.activeLoans.map((item) => (
                    <li key={item.id}>
                      <Link
                        to={`/shelf/${item.id}`}
                        className="flex items-center gap-3 py-3 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
                      >
                        <BookCover title={item.book.title} coverUrl={item.book.coverUrl} size="sm" />
                        <div className="min-w-0 flex-1">
                          <p className="truncate font-medium text-gray-900">{item.book.title}</p>
                          {item.activeLoan && (
                            <>
                              <p className="truncate text-sm text-gray-700">
                                {item.activeLoan.borrowerFirstName} {item.activeLoan.borrowerLastName}
                              </p>
                              <p className="text-xs text-gray-500">
                                {t.shelf.lentSince(formatDate(item.activeLoan.lentOn))}
                                {item.activeLoan.dueOn ? ` · ${t.shelf.due(formatDate(item.activeLoan.dueOn))}` : ''}
                              </p>
                            </>
                          )}
                        </div>
                        <ShelfItemStatusBadge status={item.status} overdue={item.activeLoan?.overdue ?? false} />
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        </>
      )}
    </div>
  );
}
