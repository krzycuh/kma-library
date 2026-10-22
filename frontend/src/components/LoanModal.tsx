import { Dialog } from '@headlessui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { t } from '../i18n';
import { todayIso } from '../utils/format';
import type { LendShelfItemRequest } from '../types/shelf';

const loanSchema = z
  .object({
    borrowerFirstName: z.string().trim().min(1, t.shelf.firstNameRequired).max(100),
    borrowerLastName: z.string().trim().min(1, t.shelf.lastNameRequired).max(100),
    lentOn: z.string().min(1, t.shelf.lentOnRequired),
    dueOn: z.string().optional().or(z.literal('')),
    notes: z.string().max(2000).optional().or(z.literal('')),
  })
  .refine((data) => !data.dueOn || data.dueOn >= data.lentOn, {
    message: t.shelf.dueBeforeLent,
    path: ['dueOn'],
  });

type LoanFormData = z.infer<typeof loanSchema>;

interface LoanModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: LendShelfItemRequest) => void;
  isLoading?: boolean;
}

const inputClasses =
  'mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

/**
 * Modal form for lending a shelf item to someone (first name + last name + dates).
 */
export function LoanModal({ isOpen, onClose, onSubmit, isLoading = false }: LoanModalProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<LoanFormData>({
    resolver: zodResolver(loanSchema),
    defaultValues: { borrowerFirstName: '', borrowerLastName: '', lentOn: todayIso(), dueOn: '', notes: '' },
  });

  const close = () => {
    if (isLoading) return;
    reset({ borrowerFirstName: '', borrowerLastName: '', lentOn: todayIso(), dueOn: '', notes: '' });
    onClose();
  };

  const submit = (data: LoanFormData) => {
    onSubmit({
      borrowerFirstName: data.borrowerFirstName.trim(),
      borrowerLastName: data.borrowerLastName.trim(),
      lentOn: data.lentOn,
      dueOn: data.dueOn || null,
      notes: data.notes?.trim() || null,
    });
  };

  return (
    <Dialog open={isOpen} onClose={close} className="relative z-50">
      <div className="fixed inset-0 bg-black/30" aria-hidden="true" />
      <div className="fixed inset-0 flex items-end justify-center p-0 sm:items-center sm:p-4">
        <Dialog.Panel className="w-full max-w-md rounded-t-lg bg-white p-6 shadow-xl sm:rounded-lg">
          <Dialog.Title className="mb-4 text-xl font-semibold text-gray-900">{t.shelf.lendTitle}</Dialog.Title>
          <form onSubmit={handleSubmit(submit)} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="loan-first-name" className="block text-sm font-medium text-gray-700">
                  {t.shelf.borrowerFirstName}
                </label>
                <input
                  {...register('borrowerFirstName')}
                  id="loan-first-name"
                  type="text"
                  autoComplete="given-name"
                  autoFocus
                  className={inputClasses}
                />
                {errors.borrowerFirstName && (
                  <p className="mt-1 text-sm text-red-600">{errors.borrowerFirstName.message}</p>
                )}
              </div>
              <div>
                <label htmlFor="loan-last-name" className="block text-sm font-medium text-gray-700">
                  {t.shelf.borrowerLastName}
                </label>
                <input
                  {...register('borrowerLastName')}
                  id="loan-last-name"
                  type="text"
                  autoComplete="family-name"
                  className={inputClasses}
                />
                {errors.borrowerLastName && (
                  <p className="mt-1 text-sm text-red-600">{errors.borrowerLastName.message}</p>
                )}
              </div>
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="loan-lent-on" className="block text-sm font-medium text-gray-700">
                  {t.shelf.lentOn}
                </label>
                <input {...register('lentOn')} id="loan-lent-on" type="date" className={inputClasses} />
                {errors.lentOn && <p className="mt-1 text-sm text-red-600">{errors.lentOn.message}</p>}
              </div>
              <div>
                <label htmlFor="loan-due-on" className="block text-sm font-medium text-gray-700">
                  {t.shelf.dueOn} <span className="text-gray-400">({t.common.optional})</span>
                </label>
                <input {...register('dueOn')} id="loan-due-on" type="date" className={inputClasses} />
                {errors.dueOn && <p className="mt-1 text-sm text-red-600">{errors.dueOn.message}</p>}
              </div>
            </div>
            <div>
              <label htmlFor="loan-notes" className="block text-sm font-medium text-gray-700">
                {t.shelf.loanNotes} <span className="text-gray-400">({t.common.optional})</span>
              </label>
              <textarea {...register('notes')} id="loan-notes" rows={2} className={inputClasses} />
            </div>
            <div className="mt-6 flex gap-3">
              <button
                type="button"
                onClick={close}
                disabled={isLoading}
                className="flex-1 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-gray-500 focus:ring-offset-2"
              >
                {t.common.cancel}
              </button>
              <button
                type="submit"
                disabled={isLoading}
                className="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              >
                {isLoading ? t.common.saving : t.shelf.lendSubmit}
              </button>
            </div>
          </form>
        </Dialog.Panel>
      </div>
    </Dialog>
  );
}
