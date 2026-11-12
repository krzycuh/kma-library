import type { ReactNode } from 'react';
import { chartBarClasses, type ChartColor } from './chartColors';

export interface BarItem {
  key: string;
  label: string;
  value: number;
  /** Rendered in place of the plain label (e.g. stars); the plain label stays for assistive technology */
  labelNode?: ReactNode;
}

interface BarListProps {
  items: BarItem[];
  describeValue: (value: number) => string;
  /** Width of the label column */
  labelClassName?: string;
  /** Hue of the bars; every chart on a page gets its own */
  color?: ChartColor;
  className?: string;
}

/**
 * Horizontal bars, one per named row, all on the same left baseline: label, bar, figure.
 * One hue; the figure beside the bar is the value, so no tooltip is needed.
 */
export function BarList({ items, describeValue, labelClassName = 'w-20 sm:w-28', color = 1, className = '' }: BarListProps) {
  const max = Math.max(0, ...items.map((i) => i.value));
  return (
    <ul className={`space-y-2 ${className}`}>
      {items.map((item) => {
        const widthPct = max > 0 ? (item.value / max) * 100 : 0;
        return (
          <li key={item.key} className="flex items-center gap-3 text-sm">
            <span className={`${labelClassName} shrink-0 truncate text-gray-700`} title={item.label}>
              {item.labelNode ?? item.label}
              {item.labelNode && <span className="sr-only">{item.label}</span>}
            </span>
            <span className="flex h-4 min-w-0 flex-1 items-center" aria-hidden="true">
              <span
                className={`block h-3 rounded-r ${chartBarClasses[color]} transition-[width] duration-300`}
                style={{ width: item.value > 0 ? `max(${widthPct}%, 3px)` : '0' }}
              />
            </span>
            <span className="w-8 shrink-0 text-right tabular-nums text-gray-900" aria-hidden="true">
              {item.value}
            </span>
            <span className="sr-only">{describeValue(item.value)}</span>
          </li>
        );
      })}
    </ul>
  );
}
