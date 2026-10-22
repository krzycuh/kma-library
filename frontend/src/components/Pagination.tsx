import { t } from '../i18n';

interface PaginationProps {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) return null;
  const buttonClasses =
    'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50';
  return (
    <nav className="mt-4 flex items-center justify-between" aria-label="Paginacja">
      <button type="button" className={buttonClasses} disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ← {t.common.previous}
      </button>
      <span className="text-sm text-gray-600">
        {t.common.page} {page + 1} {t.common.of} {totalPages}
      </span>
      <button type="button" className={buttonClasses} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        {t.common.next} →
      </button>
    </nav>
  );
}
