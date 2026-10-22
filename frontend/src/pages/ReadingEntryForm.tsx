import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { readingEntriesApi } from '../api/readingEntries';
import { booksApi } from '../api/books';
import { t } from '../i18n';
import { PageHeader } from '../components/PageHeader';
import { BookPicker } from '../components/BookPicker';
import {
  bookPickerValueFromBook,
  emptyBookPickerValue,
  toBookReference,
  validateBookPicker,
  type BookPickerValue,
} from '../utils/bookPicker';
import { YearMonthInput } from '../components/YearMonthInput';
import { RatingStars } from '../components/RatingStars';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { useToast } from '../hooks/useToast';
import { currentYear } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';

/**
 * Create (/read/new, optional ?bookId=) or edit (/read/:id/edit) a reading entry.
 */
export function ReadingEntryForm() {
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const prefillBookId = searchParams.get('bookId');
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [picker, setPicker] = useState<BookPickerValue>(emptyBookPickerValue);
  const [year, setYear] = useState<number | null>(currentYear());
  const [month, setMonth] = useState<number | null>(null);
  const [rating, setRating] = useState<number | null>(null);
  const [review, setReview] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const entryQuery = useQuery({
    queryKey: ['reading-entry', id],
    queryFn: () => readingEntriesApi.get(id!),
    enabled: isEdit,
  });

  const prefillQuery = useQuery({
    queryKey: ['book', prefillBookId],
    queryFn: () => booksApi.get(prefillBookId!),
    enabled: !isEdit && Boolean(prefillBookId),
  });

  useEffect(() => {
    if (entryQuery.data) {
      const entry = entryQuery.data;
      setPicker(bookPickerValueFromBook(entry.book));
      setYear(entry.finishedYear);
      setMonth(entry.finishedMonth);
      setRating(entry.rating);
      setReview(entry.review ?? '');
    }
  }, [entryQuery.data]);

  useEffect(() => {
    if (prefillQuery.data) setPicker(bookPickerValueFromBook(prefillQuery.data));
  }, [prefillQuery.data]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['reading-entries'] });
    queryClient.invalidateQueries({ queryKey: ['reading-summary'] });
    queryClient.invalidateQueries({ queryKey: ['reading-entry', id] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    queryClient.invalidateQueries({ queryKey: ['books'] });
  };

  const saveMutation = useMutation({
    mutationFn: () => {
      const payload = {
        finishedYear: year,
        finishedMonth: year ? month : null,
        rating,
        review: review.trim() || null,
      };
      return isEdit
        ? readingEntriesApi.update(id!, payload)
        : readingEntriesApi.create({ book: toBookReference(picker), ...payload });
    },
    onSuccess: () => {
      invalidate();
      toast.success(isEdit ? t.reading.updated : t.reading.created);
      navigate('/read');
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!isEdit) {
      const error = validateBookPicker(picker);
      if (error) {
        setFormError(error);
        return;
      }
    }
    setFormError(null);
    saveMutation.mutate();
  };

  if (isEdit && entryQuery.isLoading) return <LoadingSpinner text={t.common.loading} />;
  if (isEdit && entryQuery.isError) {
    return <ErrorMessage message={getApiErrorMessage(entryQuery.error)} retry={() => entryQuery.refetch()} />;
  }

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={isEdit ? t.reading.editTitle : t.reading.addTitle} backTo="/read" backLabel={t.nav.read} />

      <form onSubmit={onSubmit} className="space-y-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" noValidate>
        <BookPicker value={picker} onChange={setPicker} disabled={isEdit} />

        <fieldset>
          <legend className="mb-1 block text-sm font-medium text-gray-700">{t.reading.finishedOn}</legend>
          <YearMonthInput
            year={year}
            month={month}
            onChange={(y, m) => {
              setYear(y);
              setMonth(m);
            }}
          />
        </fieldset>

        <div>
          <span className="mb-1 block text-sm font-medium text-gray-700">{t.reading.rating}</span>
          <RatingStars value={rating} onChange={setRating} size="lg" />
        </div>

        <div>
          <label htmlFor="reading-review" className="block text-sm font-medium text-gray-700">
            {t.reading.review} <span className="text-gray-400">({t.common.optional})</span>
          </label>
          <textarea
            id="reading-review"
            value={review}
            onChange={(e) => setReview(e.target.value)}
            rows={4}
            maxLength={5000}
            placeholder={t.reading.reviewPlaceholder}
            className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
        </div>

        {formError && (
          <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        )}

        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-gray-400"
          >
            {t.common.cancel}
          </button>
          <button
            type="submit"
            disabled={saveMutation.isPending}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {saveMutation.isPending ? t.common.saving : t.common.save}
          </button>
        </div>
      </form>
    </div>
  );
}
