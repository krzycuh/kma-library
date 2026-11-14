import type { ReadingFormat } from '../types/reading';
import { t } from '../i18n';

/**
 * Small pill naming the edition a reading entry was read in; renders nothing when the entry doesn't say.
 * One neutral style for every format: the word tells them apart, and gray follows both themes (src/theme.css).
 */
export function ReadingFormatBadge({ format }: { format: ReadingFormat | null | undefined }) {
  if (!format) return null;
  return (
    <span className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-700">
      {t.reading.formatLabel[format]}
    </span>
  );
}
