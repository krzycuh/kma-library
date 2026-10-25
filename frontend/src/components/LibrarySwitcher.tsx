import { useCurrentLibrary } from '../hooks/useLibraries';
import { t } from '../i18n';

/**
 * Picks the library the shelf pages work on. Hidden when the user has only their own library.
 */
export function LibrarySwitcher() {
  const { libraries, library, setLibrary } = useCurrentLibrary();
  if (libraries.length < 2 || !library) return null;

  return (
    <div className="flex items-center gap-2">
      <label htmlFor="library-switcher" className="text-sm font-medium text-gray-700">
        {t.members.switcherLabel}
      </label>
      <select
        id="library-switcher"
        value={library.id}
        onChange={(e) => setLibrary(e.target.value)}
        className="min-w-0 flex-1 rounded-md border border-gray-300 bg-white px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:flex-none"
      >
        {libraries.map((item) => (
          <option key={item.id} value={item.id}>
            {item.name}
            {item.memberCount > 1 ? ` (${item.memberCount})` : ''}
          </option>
        ))}
      </select>
    </div>
  );
}
