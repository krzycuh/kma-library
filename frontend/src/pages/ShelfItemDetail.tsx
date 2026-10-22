import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { shelfItemsApi } from '../api/shelfItems';
import { PageHeader } from '../components/PageHeader';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { LoanModal } from '../components/LoanModal';
import { BookCover } from '../components/BookCover';
import { ShelfItemStatusBadge } from '../components/ShelfItemStatusBadge';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { LendShelfItemRequest, ShelfItem, ShelfItemStatus } from '../types/shelf';
import { formatAuthors, formatDate } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';

const inputClasses =
  'mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 disabled:bg-gray-100 disabled:text-gray-500';
const secondaryButton =
  'inline-flex items-center justify-center rounded-lg border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50';
const primaryButton =
  'inline-flex items-center justify-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50';

const editableStatuses: ShelfItemStatus[] = ['ON_SHELF', 'LOST', 'GIVEN_AWAY'];

function borrowerName(loan: { borrowerFirstName: string; borrowerLastName: string }) {
  return `${loan.borrowerFirstName} ${loan.borrowerLastName}`;
}

/**
 * Single copy: status, loan actions, edit, loan history.
 */
export function ShelfItemDetail() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [loanOpen, setLoanOpen] = useState(false);
  const [returnOpen, setReturnOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [acquiredOn, setAcquiredOn] = useState('');
  const [notes, setNotes] = useState('');
  const [status, setStatus] = useState<ShelfItemStatus>('ON_SHELF');

  const itemQuery = useQuery({
    queryKey: ['shelf-item', id],
    queryFn: () => shelfItemsApi.get(id),
    enabled: !!id,
  });

  useEffect(() => {
    if (itemQuery.data) {
      setAcquiredOn(itemQuery.data.acquiredOn ?? '');
      setNotes(itemQuery.data.notes ?? '');
      setStatus(itemQuery.data.status);
    }
  }, [itemQuery.data]);

  const invalidate = (updated?: ShelfItem) => {
    if (updated) queryClient.setQueryData(['shelf-item', id], updated);
    queryClient.invalidateQueries({ queryKey: ['shelf-item', id] });
    queryClient.invalidateQueries({ queryKey: ['shelf-items'] });
    queryClient.invalidateQueries({ queryKey: ['loans'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  };

  const lendMutation = useMutation({
    mutationFn: (data: LendShelfItemRequest) => shelfItemsApi.lend(id, data),
    onSuccess: (updated) => {
      invalidate(updated);
      setLoanOpen(false);
      toast.success(t.shelf.lent);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const returnMutation = useMutation({
    mutationFn: () => shelfItemsApi.return(id, {}),
    onSuccess: (updated) => {
      invalidate(updated);
      setReturnOpen(false);
      toast.success(t.shelf.returned);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const updateMutation = useMutation({
    mutationFn: () =>
      shelfItemsApi.update(id, {
        acquiredOn: acquiredOn || null,
        notes: notes.trim() || null,
        status: itemQuery.data?.status === 'LENT' ? null : status,
      }),
    onSuccess: (updated) => {
      invalidate(updated);
      setEditOpen(false);
      toast.success(t.shelf.updated);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => shelfItemsApi.delete(id),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['shelf-item', id] });
      invalidate();
      toast.success(t.shelf.removed);
      navigate('/shelf');
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  if (itemQuery.isLoading) return <LoadingSpinner />;
  if (itemQuery.isError || !itemQuery.data) {
    return <ErrorMessage message={t.shelf.bookNotFound} retry={() => itemQuery.refetch()} />;
  }

  const item = itemQuery.data;
  const activeLoan = item.activeLoan;
  const overdue = activeLoan?.overdue ?? false;
  const history = [...item.loans].sort((a, b) => (a.lentOn < b.lentOn ? 1 : a.lentOn > b.lentOn ? -1 : 0));

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t.shelf.itemTitle} backTo="/shelf" backLabel={t.shelf.title} />

      <div className="rounded-lg bg-white p-4 shadow-sm sm:p-6">
        <div className="flex gap-4">
          <BookCover title={item.book.title} coverUrl={item.book.coverUrl} size="lg" />
          <div className="min-w-0 flex-1">
            <Link to={`/books/${item.book.id}`} className="text-xl font-bold text-gray-900 hover:text-blue-700">
              {item.book.title}
            </Link>
            <p className="text-gray-600">{formatAuthors(item.book.authors)}</p>
            {item.book.publishedYear && <p className="text-sm text-gray-500">{item.book.publishedYear}</p>}
            <div className="mt-2">
              <ShelfItemStatusBadge status={item.status} overdue={overdue} />
            </div>
            <dl className="mt-3 space-y-1 text-sm text-gray-700">
              {item.acquiredOn && (
                <div className="flex gap-2">
                  <dt className="text-gray-500">{t.shelf.acquiredOn}:</dt>
                  <dd>{formatDate(item.acquiredOn)}</dd>
                </div>
              )}
              {item.notes && (
                <div className="flex gap-2">
                  <dt className="text-gray-500">{t.shelf.notes}:</dt>
                  <dd className="whitespace-pre-line">{item.notes}</dd>
                </div>
              )}
            </dl>
          </div>
        </div>

        {activeLoan && (
          <div className={`mt-4 rounded-lg border p-4 ${overdue ? 'border-red-200 bg-red-50' : 'border-amber-200 bg-amber-50'}`}>
            <h2 className="font-semibold text-gray-900">{t.shelf.activeLoanTitle}</h2>
            <p className="mt-1 text-gray-800">
              {t.shelf.borrower}: <span className="font-medium">{borrowerName(activeLoan)}</span>
            </p>
            <p className="text-sm text-gray-700">
              {t.shelf.lentSince(formatDate(activeLoan.lentOn))}
              {activeLoan.dueOn && ` · ${t.shelf.due(formatDate(activeLoan.dueOn))}`}
              {overdue && <span className="ml-2 font-semibold text-red-700">{t.shelf.overdue}</span>}
            </p>
            {activeLoan.notes && <p className="mt-1 text-sm text-gray-600">{activeLoan.notes}</p>}
            <button type="button" onClick={() => setReturnOpen(true)} className={`${primaryButton} mt-3`}>
              {t.shelf.returnItem}
            </button>
          </div>
        )}

        <div className="mt-4 flex flex-wrap gap-2">
          {item.status === 'ON_SHELF' && (
            <button type="button" onClick={() => setLoanOpen(true)} className={primaryButton}>
              {t.shelf.lend}
            </button>
          )}
          <Link to={`/read/new?bookId=${item.book.id}`} className={secondaryButton}>
            {t.shelf.markAsRead}
          </Link>
          <button type="button" onClick={() => setEditOpen((v) => !v)} className={secondaryButton} aria-expanded={editOpen}>
            {t.common.edit}
          </button>
          <button
            type="button"
            onClick={() => setDeleteOpen(true)}
            className="inline-flex items-center justify-center rounded-lg border border-red-200 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50"
          >
            {t.common.delete}
          </button>
        </div>

        {editOpen && (
          <form
            onSubmit={(e) => {
              e.preventDefault();
              updateMutation.mutate();
            }}
            className="mt-4 space-y-4 border-t border-gray-200 pt-4"
          >
            <h2 className="font-semibold text-gray-900">{t.shelf.editTitle}</h2>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="edit-acquired-on" className="block text-sm font-medium text-gray-700">
                  {t.shelf.acquiredOn}
                </label>
                <input
                  id="edit-acquired-on"
                  type="date"
                  value={acquiredOn}
                  onChange={(e) => setAcquiredOn(e.target.value)}
                  className={inputClasses}
                />
              </div>
              <div>
                <label htmlFor="edit-status" className="block text-sm font-medium text-gray-700">
                  {t.shelf.status}
                </label>
                <select
                  id="edit-status"
                  value={item.status === 'LENT' ? 'LENT' : status}
                  disabled={item.status === 'LENT'}
                  onChange={(e) => setStatus(e.target.value as ShelfItemStatus)}
                  className={inputClasses}
                >
                  {item.status === 'LENT' ? (
                    <option value="LENT">{t.shelf.statusLabel.LENT}</option>
                  ) : (
                    editableStatuses.map((s) => (
                      <option key={s} value={s}>
                        {t.shelf.statusLabel[s]}
                      </option>
                    ))
                  )}
                </select>
              </div>
            </div>
            <div>
              <label htmlFor="edit-notes" className="block text-sm font-medium text-gray-700">
                {t.shelf.notes}
              </label>
              <textarea
                id="edit-notes"
                rows={3}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder={t.shelf.notesPlaceholder}
                className={inputClasses}
              />
            </div>
            <div className="flex justify-end gap-2">
              <button type="button" onClick={() => setEditOpen(false)} className={secondaryButton}>
                {t.common.cancel}
              </button>
              <button type="submit" disabled={updateMutation.isPending} className={primaryButton}>
                {updateMutation.isPending ? t.common.saving : t.common.save}
              </button>
            </div>
          </form>
        )}
      </div>

      <section className="mt-6">
        <h2 className="mb-2 text-lg font-semibold text-gray-900">{t.shelf.loanHistory}</h2>
        {history.length === 0 ? (
          <p className="rounded-lg bg-white p-4 text-sm text-gray-500 shadow-sm">{t.shelf.noLoans}</p>
        ) : (
          <ul className="divide-y divide-gray-200 rounded-lg bg-white shadow-sm">
            {history.map((loan) => (
              <li key={loan.id} className="flex flex-col gap-1 p-4 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="font-medium text-gray-900">{borrowerName(loan)}</p>
                  <p className="text-sm text-gray-600">
                    {t.shelf.lentSince(formatDate(loan.lentOn))}
                    {loan.dueOn && ` · ${t.shelf.due(formatDate(loan.dueOn))}`}
                  </p>
                  {loan.notes && <p className="text-sm text-gray-500">{loan.notes}</p>}
                </div>
                <span
                  className={`text-sm font-medium ${
                    loan.active ? (loan.overdue ? 'text-red-700' : 'text-amber-700') : 'text-green-700'
                  }`}
                >
                  {loan.active
                    ? loan.overdue
                      ? t.shelf.overdue
                      : t.shelf.statusLabel.LENT
                    : t.shelf.returnedOn(formatDate(loan.returnedOn))}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <LoanModal
        isOpen={loanOpen}
        onClose={() => setLoanOpen(false)}
        onSubmit={(data) => lendMutation.mutate(data)}
        isLoading={lendMutation.isPending}
      />
      <ConfirmationModal
        isOpen={returnOpen}
        onClose={() => setReturnOpen(false)}
        onConfirm={() => returnMutation.mutate()}
        title={t.shelf.returnConfirmTitle}
        description={activeLoan ? t.shelf.returnConfirmText(borrowerName(activeLoan)) : ''}
        confirmText={t.shelf.returnItem}
        cancelText={t.common.cancel}
        variant="info"
        isLoading={returnMutation.isPending}
      />
      <ConfirmationModal
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => deleteMutation.mutate()}
        title={t.shelf.removeConfirmTitle}
        description={t.shelf.removeConfirmText}
        confirmText={t.common.delete}
        cancelText={t.common.cancel}
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
