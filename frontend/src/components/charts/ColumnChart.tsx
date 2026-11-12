export interface ColumnItem {
  key: string;
  /** Short label under the column */
  label: string;
  /** Spoken label for assistive technology and the hover title, e.g. the full month name */
  title?: string;
  value: number;
  /** Highlighted column; the others step back to a lighter shade */
  emphasis?: boolean;
  /** Clicking the column selects it */
  onSelect?: () => void;
}

interface ColumnChartProps {
  items: ColumnItem[];
  /** Describes a value in words for screen readers, e.g. "3 książki" */
  describeValue: (value: number) => string;
  /** Cramped charts (many columns on a phone) show every other label below the narrow breakpoint */
  denseLabels?: boolean;
  className?: string;
}

/**
 * Column chart drawn with plain elements, one hue: columns rise from a shared baseline,
 * the value sits above each column in text ink. Theme-aware through Tailwind classes.
 */
export function ColumnChart({ items, describeValue, denseLabels = false, className = '' }: ColumnChartProps) {
  const max = Math.max(0, ...items.map((i) => i.value));
  const anyEmphasis = items.some((i) => i.emphasis);

  return (
    <ul className={`flex items-end gap-0.5 sm:gap-1 ${className}`}>
      {items.map((item, index) => {
        const heightPct = max > 0 ? (item.value / max) * 100 : 0;
        const barClasses = anyEmphasis && !item.emphasis ? 'bg-blue-200' : 'bg-blue-600';
        const hideOnMobile = denseLabels && index % 2 === 1;
        const description = `${item.title ?? item.label}: ${describeValue(item.value)}`;
        const content = (
          <>
            <span className="h-4 text-[11px] leading-4 tabular-nums text-gray-600" aria-hidden="true">
              {item.value > 0 ? item.value : ''}
            </span>
            <span className="flex h-28 w-full items-end sm:h-36" aria-hidden="true">
              <span
                className={`block w-full rounded-t ${barClasses} transition-[height] duration-300`}
                style={{ height: item.value > 0 ? `max(${heightPct}%, 3px)` : '0' }}
              />
            </span>
            <span
              className={`mt-1 w-full truncate border-t border-gray-200 pt-1 text-center text-[11px] leading-4 text-gray-600 ${
                hideOnMobile ? 'invisible sm:visible' : ''
              } ${item.emphasis ? 'font-semibold text-gray-900' : ''}`}
              aria-hidden="true"
            >
              {item.label}
            </span>
            <span className="sr-only">{description}</span>
          </>
        );
        const shared = 'flex min-w-0 flex-1 flex-col items-center';
        return (
          <li key={item.key} className="flex min-w-0 flex-1" title={description}>
            {item.onSelect ? (
              <button
                type="button"
                onClick={item.onSelect}
                aria-pressed={item.emphasis ?? false}
                className={`${shared} min-h-0 rounded-md px-0 py-0 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500`}
              >
                {content}
              </button>
            ) : (
              <div className={shared}>{content}</div>
            )}
          </li>
        );
      })}
    </ul>
  );
}
