import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { librariesApi } from '../api/libraries';
import { shelfItemsApi } from '../api/shelfItems';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { BookCover } from '../components/BookCover';
import { useDefaultLibrary } from '../hooks/useLibraries';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { ShelfItem } from '../types/shelf';
import { formatAuthors, formatDate } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';

/**
 * Who currently has my books. Overdue loans are listed first.
 */
export function Loans() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { library, isLoading: libraryLoading, isError: libraryError } = useDefaultLibrary();
  const [returning, setReturning] = useState<ShelfItem | null>(null);

  const loansQuery = useQuery({
    queryKey: ['loans', library?.id],
    queryFn: () => librariesApi.activeLoans(library!.id),
    enabled: !!library,
  });

  const returnMutation = useMutation({
    mutationFn: (itemId: string) => shelfItemsApi.return(itemId, {}),
    onSuccess: (_, itemId) => {
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['shelf-items'] });
      queryClient.invalidateQueries({ queryKey: ['shelf-item', itemId] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setReturning(null);
      toast.success(t.shelf.returned);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const items = loansQuery.data ?? [];
  const sorted = [...items].sort((a, b) => {
    const ao = a.activeLoan?.overdue ? 0 : 1;
    const bo = b.activeLoan?.overdue ? 0 : 1;
    if (ao !== bo) return ao - bo;
    return (a.activeLoan?.lentOn ?? '').localeCompare(b.activeLoan?.lentOn ?? '');
  });
  const overdueCount = items.filter((i) => i.activeLoan?.overdue).length;

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title={t.loans.title}
        description={
          items.length > 0
            ? `${t.loans.count(items.length)}${overdueCount > 0 ? ` · ${t.loans.overdueCount(overdueCount)}` : ''}`
            : t.loans.subtitle
        }
      />

      {libraryLoading || (library && loansQuery.isLoading) ? (
        <LoadingSkeleton />
      ) : libraryError || loansQuery.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => loansQuery.refetch()} />
      ) : sorted.length === 0 ? (
        <EmptyState icon="✅" title={t.loans.emptyTitle} description={t.loans.emptyText} />
      ) : (
        <ul className="space-y-3">
          {sorted.map((item) => {
            const loan = item.activeLoan!;
            const overdue = loan.overdue;
            return (
              <li
                key={item.id}
                className={`rounded-lg bg-white p-4 shadow-sm ${overdue ? 'border-l-4 border-red-500' : ''}`}
              >
                <div className="flex gap-3">
                  <BookCover title={item.book.title} coverUrl={item.book.coverUrl} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold text-gray-900">
                      {loan.borrowerFirstName} {loan.borrowerLastName}
                      {overdue && (
                        <span className="ml-2 inline-flex items-center rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-800">
                          {t.shelf.overdue}
                        </span>
                      )}
                    </p>
                    <Link to={`/shelf/${item.id}`} className="block truncate text-gray-800 hover:text-blue-700">
                      {item.book.title}
                    </Link>
                    <p className="truncate text-sm text-gray-600">{formatAuthors(item.book.authors)}</p>
                    <p className="mt-1 text-sm text-gray-500">
                      {t.shelf.lentSince(formatDate(loan.lentOn))}
                      {loan.dueOn && ` · ${t.shelf.due(formatDate(loan.dueOn))}`}
                    </p>
                  </div>
                  <div className="flex items-start">
                    <button
                      type="button"
                      onClick={() => setReturning(item)}
                      className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
                    >
                      {t.shelf.returnItem}
                    </button>
                  </div>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      <ConfirmationModal
        isOpen={returning !== null}
        onClose={() => setReturning(null)}
        onConfirm={() => returning && returnMutation.mutate(returning.id)}
        title={t.shelf.returnConfirmTitle}
        description={
          returning?.activeLoan
            ? t.shelf.returnConfirmText(`${returning.activeLoan.borrowerFirstName} ${returning.activeLoan.borrowerLastName}`)
            : ''
        }
        confirmText={t.shelf.returnItem}
        cancelText={t.common.cancel}
        variant="info"
        isLoading={returnMutation.isPending}
      />
    </div>
  );
}
