import { useNavigate } from 'react-router-dom';
import { useViewAs } from '../hooks/useViewAs';
import { t } from '../i18n';

/**
 * Shown under the navigation while an admin previews another account; the only way out of the preview.
 */
export function ViewAsBanner() {
  const { viewedUser, stopViewing } = useViewAs();
  const navigate = useNavigate();
  if (!viewedUser) return null;

  const stop = () => {
    stopViewing();
    navigate('/admin/users');
  };

  return (
    <div
      role="status"
      className="sticky top-16 z-30 border-b border-amber-300 bg-amber-50 px-4 py-2 text-sm text-amber-900 sm:px-6 lg:px-8"
    >
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2">
        <p className="min-w-0">
          <span className="font-semibold">{t.viewAs.banner(viewedUser.name)}</span>{' '}
          <span className="text-amber-800">({viewedUser.email})</span>
          <span className="mx-2 text-amber-400" aria-hidden="true">
            ·
          </span>
          <span>{t.viewAs.readOnly}</span>
        </p>
        <button
          type="button"
          onClick={stop}
          className="rounded-md border border-amber-400 bg-white px-3 py-1 text-sm font-medium text-amber-900 hover:bg-amber-100 focus:outline-none focus:ring-2 focus:ring-amber-500 focus:ring-offset-2"
        >
          {t.viewAs.stop}
        </button>
      </div>
    </div>
  );
}
