import { useMemo, type ReactNode } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { readingEntriesApi } from '../api/readingEntries';
import type { ReadingPeriodStats, ReadingStatistics as ReadingStatisticsData } from '../types/statistics';
import { t } from '../i18n';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ColumnChart } from '../components/charts/ColumnChart';
import { BarList } from '../components/charts/BarList';
import { getApiErrorMessage } from '../utils/apiError';
import {
  averagePerYear,
  bestYear,
  formatAverage,
  maxCounted,
  parseYearParam,
  previousYearBooks,
  selectableYears,
  statsForYear,
  yearSeries,
} from '../utils/statistics';

const books = (n: number) => t.reading.booksCount(n);

interface StatTileProps {
  label: string;
  value: string | number;
  hint?: string;
}

function StatTile({ label, value, hint }: StatTileProps) {
  return (
    <div className="rounded-lg bg-white p-4 shadow-sm sm:p-5">
      <p className="text-sm font-medium text-gray-600">{label}</p>
      <p className="mt-1 truncate text-3xl font-bold text-gray-900">{value}</p>
      {hint && <p className="mt-1 truncate text-xs text-gray-500">{hint}</p>}
    </div>
  );
}

function ChartCard({ title, hint, children }: { title: string; hint?: string; children: ReactNode }) {
  return (
    <section className="min-w-0 rounded-lg bg-white p-4 shadow-sm sm:p-5" aria-label={title}>
      <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      {hint && <p className="mt-0.5 text-xs text-gray-500">{hint}</p>}
      <div className="mt-4">{children}</div>
    </section>
  );
}

/** Pills choosing the period: all years, or one year. The choice lives in the URL (?year=). */
function YearPicker({ years, selected, onSelect }: { years: number[]; selected: number | null; onSelect: (year: number | null) => void }) {
  const pill = (active: boolean) =>
    `shrink-0 rounded-full px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
      active ? 'bg-blue-600 text-white' : 'bg-white text-gray-700 shadow-sm hover:bg-blue-50'
    }`;
  return (
    <div role="group" aria-label={t.stats.periodLabel} className="-mx-4 mb-4 flex gap-2 overflow-x-auto px-4 pb-1 sm:mx-0 sm:flex-wrap sm:px-0">
      <button type="button" onClick={() => onSelect(null)} aria-pressed={selected === null} className={pill(selected === null)}>
        {t.stats.allYears}
      </button>
      {years.map((year) => (
        <button key={year} type="button" onClick={() => onSelect(year)} aria-pressed={selected === year} className={pill(selected === year)}>
          {year}
        </button>
      ))}
    </div>
  );
}

function Highlights({ stats, period, year }: { stats: ReadingStatisticsData; period: ReadingPeriodStats; year: number | null }) {
  const rereads = period.books - period.uniqueBooks;
  const best = bestYear(stats);
  const bestMonth = maxCounted(period.months);
  const season = maxCounted(period.seasons);

  if (year === null) {
    return (
      <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
        <StatTile label={t.stats.booksTotal} value={period.books} hint={t.stats.uniqueBooks(period.uniqueBooks, rereads)} />
        <StatTile
          label={t.stats.averagePerYear}
          value={formatAverage(averagePerYear(stats))}
          hint={stats.years.length > 0 ? t.stats.yearsCount(stats.years.length) : undefined}
        />
        <StatTile label={t.stats.bestYear} value={best?.year ?? t.stats.noData} hint={best ? books(best.books) : undefined} />
        <StatTile label={t.stats.favouriteSeason} value={season ? t.seasons[season.season] : t.stats.noData} hint={season ? books(season.count) : undefined} />
      </div>
    );
  }

  const previous = previousYearBooks(stats, year);
  const yearHint =
    previous !== null ? t.stats.previousYear(year - 1, previous) : year === stats.currentYear ? t.stats.yearInProgress : undefined;
  return (
    <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
      <StatTile label={t.stats.booksInYear(year)} value={period.books} hint={rereads > 0 ? t.stats.uniqueBooks(period.uniqueBooks, rereads) : yearHint} />
      <StatTile
        label={t.stats.averageRating}
        value={formatAverage(period.averageRating)}
        hint={period.ratedBooks > 0 ? t.stats.ratedCount(period.ratedBooks) : undefined}
      />
      <StatTile label={t.stats.bestMonth} value={bestMonth ? t.months[bestMonth.month - 1] : t.stats.noData} hint={bestMonth ? books(bestMonth.count) : undefined} />
      <StatTile label={t.stats.favouriteSeason} value={season ? t.seasons[season.season] : t.stats.noData} hint={season ? books(season.count) : undefined} />
    </div>
  );
}

/**
 * Reading statistics: books per year, per month and season, rating distribution and most read authors,
 * for all years or one chosen year.
 */
export function Statistics() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['reading-statistics'],
    queryFn: readingEntriesApi.statistics,
    staleTime: 60_000,
  });

  const years = useMemo(() => (data ? selectableYears(data) : []), [data]);
  const year = parseYearParam(searchParams.get('year'), years);
  const period = data ? (year === null ? data.total : statsForYear(data, year)) : null;

  const selectYear = (next: number | null) => {
    setSearchParams(next === null ? {} : { year: String(next) }, { replace: true });
  };

  const yearColumns = useMemo(() => {
    if (!data) return [];
    return yearSeries(data).map((y) => ({
      key: String(y.year),
      label: String(y.year),
      title: t.stats.showYear(y.year),
      value: y.books,
      emphasis: year === y.year,
      onSelect: () => selectYear(year === y.year ? null : y.year),
    }));
    // selectYear is stable enough: it only closes over setSearchParams and the current year
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, year]);

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={t.stats.title}
        description={t.stats.subtitle}
        actions={
          <Link to="/read" className="text-sm font-medium text-blue-600 hover:text-blue-700">
            {t.stats.seeList} →
          </Link>
        }
      />

      {isLoading && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
        </div>
      )}

      {isError && <ErrorMessage message={getApiErrorMessage(error)} retry={() => refetch()} />}

      {data && data.total.books === 0 && (
        <EmptyState
          icon="📊"
          title={t.stats.emptyTitle}
          description={t.stats.emptyText}
          action={{ label: t.stats.addRead, onClick: () => navigate('/read/new') }}
        />
      )}

      {data && period && data.total.books > 0 && (
        <>
          <YearPicker years={years} selected={year} onSelect={selectYear} />

          <Highlights stats={data} period={period} year={year} />

          <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
            {yearColumns.length > 0 && (
              <ChartCard title={t.stats.chartYears} hint={data.withoutYear > 0 ? t.stats.withoutYear(data.withoutYear) : undefined}>
                <ColumnChart items={yearColumns} describeValue={books} denseLabels={yearColumns.length > 8} color={1} />
              </ChartCard>
            )}

            <ChartCard
              title={year === null ? t.stats.chartMonthsAll : t.stats.chartMonths}
              hint={period.withoutMonth > 0 ? t.stats.withoutMonth(period.withoutMonth) : undefined}
            >
              {period.months.some((m) => m.count > 0) ? (
                <ColumnChart
                  items={period.months.map((m) => ({
                    key: String(m.month),
                    label: t.monthsShort[m.month - 1],
                    title: t.months[m.month - 1],
                    value: m.count,
                  }))}
                  describeValue={books}
                  denseLabels
                  color={2}
                />
              ) : (
                <p className="py-6 text-center text-sm text-gray-500">{t.stats.noMonths}</p>
              )}
            </ChartCard>

            <ChartCard title={t.stats.chartSeasons} hint={period.withoutMonth > 0 ? t.stats.withoutMonth(period.withoutMonth) : undefined}>
              <BarList
                items={period.seasons.map((s) => ({ key: s.season, label: t.seasons[s.season], value: s.count }))}
                describeValue={books}
                color={3}
              />
            </ChartCard>

            <ChartCard
              title={t.stats.chartRatings}
              hint={period.books - period.ratedBooks > 0 ? t.stats.unrated(period.books - period.ratedBooks) : undefined}
            >
              {period.ratedBooks > 0 ? (
                <BarList
                  items={[...period.ratings].reverse().map((r) => ({
                    key: String(r.rating),
                    label: t.reading.ratingLabel(r.rating),
                    labelNode: (
                      <span className="text-star" aria-hidden="true">
                        {'★'.repeat(r.rating)}
                      </span>
                    ),
                    value: r.count,
                  }))}
                  describeValue={books}
                  color={4}
                />
              ) : (
                <p className="py-6 text-center text-sm text-gray-500">{t.stats.noRatings}</p>
              )}
            </ChartCard>

            <ChartCard title={t.stats.chartAuthors}>
              {period.topAuthors.length > 0 ? (
                <BarList
                  items={period.topAuthors.map((a) => ({ key: a.author, label: a.author, value: a.count }))}
                  describeValue={books}
                  labelClassName="w-32 sm:w-44"
                  color={5}
                />
              ) : (
                <p className="py-6 text-center text-sm text-gray-500">{t.stats.noAuthors}</p>
              )}
            </ChartCard>
          </div>
        </>
      )}
    </div>
  );
}
