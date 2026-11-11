import { useCallback, useEffect, useState } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { rememberListUrl } from '../utils/listUrl';

type ParamValue = string | number | undefined | null;

/**
 * List filters and pagination kept in the URL query string, so a refresh or coming back
 * from a detail page restores the same view. The page is 1-based in the URL ("?page=2")
 * and 0-based for the API; page 1 and empty filters are left out of the URL. The last query
 * string is also remembered per tab (see listUrl), for links back to the list from other pages.
 */
export function useListSearchParams() {
  const [searchParams, setSearchParams] = useSearchParams();
  const { pathname, search } = useLocation();

  useEffect(() => {
    rememberListUrl(pathname, search);
  }, [pathname, search]);

  const get = useCallback((key: string): string | undefined => searchParams.get(key) || undefined, [searchParams]);

  const getNumber = useCallback(
    (key: string): number | undefined => {
      const raw = searchParams.get(key);
      if (!raw) return undefined;
      const value = Number(raw);
      return Number.isInteger(value) ? value : undefined;
    },
    [searchParams],
  );

  const pageParam = Number(searchParams.get('page'));
  const page = Number.isInteger(pageParam) && pageParam > 1 ? pageParam - 1 : 0;

  /** Sets filters; any filter change goes back to the first page. */
  const setFilters = useCallback(
    (changes: Record<string, ParamValue>) => {
      setSearchParams(
        (prev) => {
          const next = new URLSearchParams(prev);
          for (const [key, value] of Object.entries(changes)) {
            if (value === undefined || value === null || value === '') next.delete(key);
            else next.set(key, String(value));
          }
          next.delete('page');
          return next;
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  const setPage = useCallback(
    (newPage: number, options?: { replace?: boolean }) => {
      setSearchParams((prev) => {
        const next = new URLSearchParams(prev);
        if (newPage > 0) next.set('page', String(newPage + 1));
        else next.delete('page');
        return next;
      }, options);
    },
    [setSearchParams],
  );

  /**
   * A remembered page past the end (items deleted since) moves to the last existing page
   * instead of showing an empty list. Pass the total from the loaded data.
   */
  const clampPage = useCallback(
    (totalPages: number | undefined) => {
      if (totalPages !== undefined && totalPages > 0 && page >= totalPages) setPage(totalPages - 1, { replace: true });
    },
    [page, setPage],
  );

  return { get, getNumber, page, setFilters, setPage, clampPage };
}

/**
 * Text input state for a search filter stored in the URL: the input updates immediately,
 * the URL after a short pause. If the URL changes from outside (back button, nav link),
 * the input follows it.
 */
export function useDebouncedSearchInput(urlValue: string, commit: (value: string) => void, delay = 300) {
  const [input, setInput] = useState(urlValue);

  useEffect(() => {
    setInput((current) => (current.trim() === urlValue ? current : urlValue));
  }, [urlValue]);

  useEffect(() => {
    const trimmed = input.trim();
    if (trimmed === urlValue) return;
    const handle = setTimeout(() => commit(trimmed), delay);
    return () => clearTimeout(handle);
    // commit is intentionally not a dependency: only typing should restart the timer.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [input, urlValue, delay]);

  return [input, setInput] as const;
}
