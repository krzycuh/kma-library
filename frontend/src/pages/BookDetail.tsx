import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { booksApi } from '../api/books';
import { readingEntriesApi } from '../api/readingEntries';
import { librariesApi } from '../api/libraries';
import { useAuth } from '../hooks/useAuth';
import { useDefaultLibrary } from '../hooks/useLibraries';
import { useToast } from '../hooks/useToast';
import { UserRole } from '../types/auth';
import type { Book } from '../types/book';
import { t } from '../i18n';
import { PageHeader } from '../components/PageHeader';
import { BookCover } from '../components/BookCover';
import { RatingStars } from '../components/RatingStars';
import { ShelfItemStatusBadge } from '../components/ShelfItemStatusBadge';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { formatAuthors, formatDate, formatFinishedPeriod } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';
import { isValidIsbn } from '../utils/isbn';

const inputClasses =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

function BookEditForm({ book }: { book: Book }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [title, setTitle] = useState(book.title);
  const [authors, setAuthors] = useState(book.authors.join('; '));
  const [isbn, setIsbn] = useState(book.isbn ?? '');
  const [publishedYear, setPublishedYear] = useState(book.publishedYear ? String(book.publishedYear) : '');
  const [coverUrl, setCoverUrl] = useState(book.coverUrl ?? '');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    setTitle(book.title);
    setAuthors(book.authors.join('; '));
    setIsbn(book.isbn ?? '');
    setPublishedYear(book.publishedYear ? String(book.publishedYear) : '');
    setCoverUrl(book.coverUrl ?? '');
  }, [book]);

  const mutation = useMutation({
    mutationFn: () =>
      booksApi.update(book.id, {
        title: title.trim(),
        authors: authors.trim(),
        isbn: isbn.trim() || null,
        publishedYear: publishedYear ? Number(publishedYear) : null,
        coverUrl: coverUrl.trim() || null,
      }),
    onSuccess: () => {
      toast.success(t.book.bookSaved);
      queryClient.invalidateQueries({ queryKey: ['book', book.id] });
      queryClient.invalidateQueries({ queryKey: ['reading-entries'] });
      queryClient.invalidateQueries({ queryKey: ['shelf-items'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['books'] });
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return setFormError(t.book.titleRequired);
    if (!authors.trim()) return setFormError(t.book.authorsRequired);
    if (isbn.trim() && !isValidIsbn(isbn)) return setFormError(t.book.invalidIsbn);
    setFormError(null);
    mutation.mutate();
  };

  return (
    <form onSubmit={onSubmit} className="space-y-3" noValidate>
      <div>
        <label htmlFor="edit-title" className="block text-sm font-medium text-gray-700">
          {t.book.title}
        </label>
        <input id="edit-title" type="text" value={title} onChange={(e) => setTitle(e.target.value)} className={inputClasses} />
      </div>
      <div>
        <label htmlFor="edit-authors" className="block text-sm font-medium text-gray-700">
          {t.book.authors}
        </label>
        <input id="edit-authors" type="text" value={authors} onChange={(e) => setAuthors(e.target.value)} className={inputClasses} />
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label htmlFor="edit-isbn" className="block text-sm font-medium text-gray-700">
            {t.book.isbn}
          </label>
          <input id="edit-isbn" type="text" inputMode="numeric" value={isbn} onChange={(e) => setIsbn(e.target.value)} className={inputClasses} />
        </div>
        <div>
          <label htmlFor="edit-year" className="block text-sm font-medium text-gray-700">
            {t.book.publishedYear}
          </label>
          <input
            id="edit-year"
            type="number"
            min={0}
            max={2200}
            value={publishedYear}
            onChange={(e) => setPublishedYear(e.target.value)}
            className={inputClasses}
          />
        </div>
      </div>
      <div>
        <label htmlFor="edit-cover" className="block text-sm font-medium text-gray-700">
          {t.book.coverUrl}
        </label>
        <input id="edit-cover" type="url" value={coverUrl} onChange={(e) => setCoverUrl(e.target.value)} className={inputClasses} />
      </div>
      {formError && (
        <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
          {formError}
        </p>
      )}
      <div className="flex justify-end">
        <button
          type="submit"
          disabled={mutation.isPending}
          className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
        >
          {mutation.isPending ? t.common.saving : t.common.save}
        </button>
      </div>
    </form>
  );
}

/**
 * Catalog book page: metadata, my reading entries, my copies on the shelf.
 */
export function BookDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const { library } = useDefaultLibrary();
  const [editOpen, setEditOpen] = useState(false);

  const bookQuery = useQuery({
    queryKey: ['book', id],
    queryFn: () => booksApi.get(id!),
    enabled: Boolean(id),
  });

  const readsQuery = useQuery({
    queryKey: ['reading-entries', { bookId: id, pageSize: 50 }],
    queryFn: () => readingEntriesApi.list({ bookId: id, pageSize: 50 }),
    enabled: Boolean(id),
  });

  const copiesQuery = useQuery({
    queryKey: ['shelf-items', library?.id, { bookId: id, pageSize: 50 }],
    queryFn: () => librariesApi.items(library!.id, { bookId: id, pageSize: 50 }),
    enabled: Boolean(id && library),
  });

  if (bookQuery.isLoading) return <LoadingSpinner text={t.common.loading} />;
  if (bookQuery.isError || !bookQuery.data) {
    return <ErrorMessage message={getApiErrorMessage(bookQuery.error)} retry={() => bookQuery.refetch()} />;
  }

  const book = bookQuery.data;
  const canEdit = user?.role === UserRole.ADMIN || (user?.id != null && user.id === book.createdByUserId);

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={book.title} description={formatAuthors(book.authors)} backTo={-1} backLabel={t.common.back} />

      <section className="rounded-lg bg-white p-4 shadow-sm sm:p-6">
        <div className="flex gap-4">
          <BookCover title={book.title} coverUrl={book.coverUrl} size="lg" />
          <dl className="flex-1 space-y-1 text-sm">
            <div>
              <dt className="inline font-medium text-gray-600">{t.book.authorsShort}: </dt>
              <dd className="inline text-gray-900">{formatAuthors(book.authors)}</dd>
            </div>
            <div>
              <dt className="inline font-medium text-gray-600">{t.book.isbn}: </dt>
              <dd className="inline text-gray-900">{book.isbn ?? '—'}</dd>
            </div>
            <div>
              <dt className="inline font-medium text-gray-600">{t.book.publishedYear}: </dt>
              <dd className="inline text-gray-900">{book.publishedYear ?? '—'}</dd>
            </div>
          </dl>
        </div>
        <div className="mt-4 flex flex-col gap-2 sm:flex-row">
          <Link
            to={`/read/new?bookId=${book.id}`}
            className="rounded-lg bg-blue-600 px-4 py-2 text-center text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.book.addAsRead}
          </Link>
          <Link
            to={`/shelf/new?bookId=${book.id}`}
            className="rounded-lg border border-blue-600 bg-white px-4 py-2 text-center text-sm font-semibold text-blue-700 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.book.addToShelf}
          </Link>
        </div>
        {canEdit && (
          <div className="mt-4 border-t border-gray-100 pt-4">
            <button
              type="button"
              onClick={() => setEditOpen((open) => !open)}
              aria-expanded={editOpen}
              className="text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              {editOpen ? '▾' : '▸'} {t.book.editBook}
            </button>
            {editOpen && (
              <div className="mt-3">
                <BookEditForm book={book} />
              </div>
            )}
          </div>
        )}
      </section>

      <section className="mt-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="my-reads-heading">
        <h2 id="my-reads-heading" className="mb-3 text-lg font-semibold text-gray-900">
          {t.book.myReads}
        </h2>
        {readsQuery.isLoading && <LoadingSpinner size="sm" />}
        {readsQuery.isError && <ErrorMessage message={getApiErrorMessage(readsQuery.error)} retry={() => readsQuery.refetch()} />}
        {readsQuery.data && readsQuery.data.items.length === 0 && (
          <p className="text-sm text-gray-500">{t.book.noReads}</p>
        )}
        {readsQuery.data && readsQuery.data.items.length > 0 && (
          <ul className="divide-y divide-gray-100">
            {readsQuery.data.items.map((entry) => (
              <li key={entry.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="text-sm text-gray-900">{formatFinishedPeriod(entry.finishedYear, entry.finishedMonth)}</p>
                  {entry.review && <p className="truncate text-xs text-gray-500">{entry.review}</p>}
                </div>
                <div className="flex items-center gap-3">
                  <RatingStars value={entry.rating} size="sm" />
                  <Link to={`/read/${entry.id}/edit`} className="text-sm font-medium text-blue-600 hover:text-blue-700">
                    {t.common.edit}
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mt-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="my-shelf-heading">
        <h2 id="my-shelf-heading" className="mb-3 text-lg font-semibold text-gray-900">
          {t.book.onMyShelf}
        </h2>
        {(copiesQuery.isLoading || (!library && !copiesQuery.data)) && <LoadingSpinner size="sm" />}
        {copiesQuery.isError && <ErrorMessage message={getApiErrorMessage(copiesQuery.error)} retry={() => copiesQuery.refetch()} />}
        {copiesQuery.data && copiesQuery.data.items.length === 0 && (
          <p className="text-sm text-gray-500">{t.book.noCopies}</p>
        )}
        {copiesQuery.data && copiesQuery.data.items.length > 0 && (
          <ul className="divide-y divide-gray-100">
            {copiesQuery.data.items.map((item) => (
              <li key={item.id}>
                <Link
                  to={`/shelf/${item.id}`}
                  className="flex items-center justify-between gap-3 py-3 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
                >
                  <div className="min-w-0 text-sm text-gray-700">
                    {item.acquiredOn ? `${t.shelf.acquiredOn}: ${formatDate(item.acquiredOn)}` : t.shelf.itemTitle}
                    {item.activeLoan && (
                      <span className="ml-2 text-gray-500">
                        {t.shelf.lentTo(`${item.activeLoan.borrowerFirstName} ${item.activeLoan.borrowerLastName}`)}
                      </span>
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
  );
}
