import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { booksApi } from '../api/books';
import { librariesApi } from '../api/libraries';
import { PageHeader } from '../components/PageHeader';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { BookPicker } from '../components/BookPicker';
import {
  bookPickerValueFromBook,
  emptyBookPickerValue,
  toBookReference,
  validateBookPicker,
  type BookPickerValue,
} from '../utils/bookPicker';
import { useDefaultLibrary } from '../hooks/useLibraries';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import { getApiErrorMessage } from '../utils/apiError';

const inputClasses =
  'mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

/**
 * Add a new copy to the shelf. Supports ?bookId= prefill from the catalog.
 */
export function ShelfItemForm() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [searchParams] = useSearchParams();
  const prefillBookId = searchParams.get('bookId');
  const { library, isLoading: libraryLoading } = useDefaultLibrary();

  const [picker, setPicker] = useState<BookPickerValue>(emptyBookPickerValue);
  const [acquiredOn, setAcquiredOn] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const prefillQuery = useQuery({
    queryKey: ['book', prefillBookId],
    queryFn: () => booksApi.get(prefillBookId!),
    enabled: !!prefillBookId,
  });

  useEffect(() => {
    if (prefillQuery.data) setPicker(bookPickerValueFromBook(prefillQuery.data));
  }, [prefillQuery.data]);

  const mutation = useMutation({
    mutationFn: () =>
      librariesApi.addItem(library!.id, {
        book: toBookReference(picker),
        acquiredOn: acquiredOn || null,
        notes: notes.trim() || null,
      }),
    onSuccess: (item) => {
      queryClient.invalidateQueries({ queryKey: ['shelf-items'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['books'] });
      toast.success(t.shelf.added);
      navigate(`/shelf/${item.id}`);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const validation = validateBookPicker(picker);
    if (validation) {
      setError(validation);
      return;
    }
    setError(null);
    mutation.mutate();
  };

  if (libraryLoading || (prefillBookId && prefillQuery.isLoading)) {
    return <LoadingSpinner />;
  }

  if (!library) {
    return <EmptyState icon="📚" title={t.shelf.emptyTitle} description={t.shelf.noLibrary} />;
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t.shelf.addTitle} backTo="/shelf" backLabel={t.shelf.title} />
      <form onSubmit={submit} className="space-y-5 rounded-lg bg-white p-4 shadow-sm sm:p-6">
        <BookPicker value={picker} onChange={setPicker} />

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="acquired-on" className="block text-sm font-medium text-gray-700">
              {t.shelf.acquiredOn} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <input
              id="acquired-on"
              type="date"
              value={acquiredOn}
              onChange={(e) => setAcquiredOn(e.target.value)}
              className={inputClasses}
            />
          </div>
        </div>

        <div>
          <label htmlFor="notes" className="block text-sm font-medium text-gray-700">
            {t.shelf.notes} <span className="text-gray-400">({t.common.optional})</span>
          </label>
          <textarea
            id="notes"
            rows={3}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder={t.shelf.notesPlaceholder}
            className={inputClasses}
          />
        </div>

        {error && (
          <div className="rounded-md bg-red-50 p-3">
            <p className="text-sm font-medium text-red-800">{error}</p>
          </div>
        )}

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => navigate('/shelf')}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            {t.common.cancel}
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {mutation.isPending ? t.common.saving : t.shelf.add}
          </button>
        </div>
      </form>
    </div>
  );
}
