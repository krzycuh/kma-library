import type { ShelfItemStatus } from '../types/shelf';
import { t } from '../i18n';

const styles: Record<ShelfItemStatus, string> = {
  ON_SHELF: 'bg-green-100 text-green-800',
  LENT: 'bg-amber-100 text-amber-800',
  LOST: 'bg-red-100 text-red-800',
  GIVEN_AWAY: 'bg-gray-100 text-gray-700',
};

export function ShelfItemStatusBadge({ status, overdue = false }: { status: ShelfItemStatus; overdue?: boolean }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${overdue ? 'bg-red-100 text-red-800' : styles[status]}`}>
      {t.shelf.statusLabel[status]}
      {overdue ? ` · ${t.shelf.overdue}` : ''}
    </span>
  );
}
