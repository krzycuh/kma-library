import { t } from '../i18n';
import { currentYear } from '../utils/format';

interface YearMonthInputProps {
  year: number | null;
  month: number | null;
  onChange: (year: number | null, month: number | null) => void;
  idPrefix?: string;
}

/**
 * Picks a year (optional) and a month (optional, only when a year is set). No day precision.
 */
export function YearMonthInput({ year, month, onChange, idPrefix = 'finished' }: YearMonthInputProps) {
  const thisYear = currentYear();
  const years = Array.from({ length: 60 }, (_, i) => thisYear - i);
  const selectClasses =
    'rounded-md border border-gray-300 bg-white px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

  return (
    <div className="flex flex-wrap gap-2">
      <div className="flex flex-col">
        <label htmlFor={`${idPrefix}-year`} className="text-xs font-medium text-gray-600">
          {t.reading.year}
        </label>
        <select
          id={`${idPrefix}-year`}
          value={year ?? ''}
          onChange={(e) => {
            const y = e.target.value ? Number(e.target.value) : null;
            onChange(y, y ? month : null);
          }}
          className={selectClasses}
        >
          <option value="">{t.reading.unknownDate}</option>
          {years.map((y) => (
            <option key={y} value={y}>
              {y}
            </option>
          ))}
        </select>
      </div>
      <div className="flex flex-col">
        <label htmlFor={`${idPrefix}-month`} className="text-xs font-medium text-gray-600">
          {t.reading.month} <span className="text-gray-400">({t.common.optional})</span>
        </label>
        <select
          id={`${idPrefix}-month`}
          value={month ?? ''}
          disabled={!year}
          onChange={(e) => onChange(year, e.target.value ? Number(e.target.value) : null)}
          className={`${selectClasses} disabled:bg-gray-100 disabled:text-gray-400`}
        >
          <option value="">—</option>
          {t.months.map((name, index) => {
            const m = index + 1;
            const disabled = year === thisYear && m > new Date().getMonth() + 1;
            return (
              <option key={m} value={m} disabled={disabled}>
                {name}
              </option>
            );
          })}
        </select>
      </div>
    </div>
  );
}
