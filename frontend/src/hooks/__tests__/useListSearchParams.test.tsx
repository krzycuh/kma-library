import { beforeEach, describe, it, expect } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useListSearchParams } from '../useListSearchParams';
import { listUrl } from '../../utils/listUrl';

function setup(initialUrl: string) {
  const wrapper = ({ children }: { children: ReactNode }) => (
    <MemoryRouter initialEntries={[initialUrl]}>{children}</MemoryRouter>
  );
  return renderHook(() => ({ list: useListSearchParams(), location: useLocation() }), { wrapper });
}

describe('useListSearchParams', () => {
  beforeEach(() => sessionStorage.clear());

  it('reads filters and a 1-based page from the URL', () => {
    const { result } = setup('/read?q=lem&year=2024&page=3');
    expect(result.current.list.get('q')).toBe('lem');
    expect(result.current.list.getNumber('year')).toBe(2024);
    expect(result.current.list.page).toBe(2);
  });

  it('falls back to the first page for missing or invalid values', () => {
    expect(setup('/read').result.current.list.page).toBe(0);
    expect(setup('/read?page=abc').result.current.list.page).toBe(0);
    expect(setup('/read?page=-4').result.current.list.page).toBe(0);
  });

  it('resets the page when a filter changes and drops empty filters', () => {
    const { result } = setup('/shelf?status=LENT&q=x&page=2');
    act(() => result.current.list.setFilters({ q: '', status: 'ON_SHELF' }));
    expect(result.current.location.search).toBe('?status=ON_SHELF');
  });

  it('writes the page 1-based and omits the first page', () => {
    const { result } = setup('/shelf?status=LENT');
    act(() => result.current.list.setPage(1));
    expect(result.current.location.search).toBe('?status=LENT&page=2');
    act(() => result.current.list.setPage(0));
    expect(result.current.location.search).toBe('?status=LENT');
  });

  it('remembers the list URL for links back from other pages', () => {
    const { result } = setup('/read?year=2024');
    expect(listUrl('/read')).toBe('/read?year=2024');
    act(() => result.current.list.setPage(2));
    expect(listUrl('/read')).toBe('/read?year=2024&page=3');
    act(() => result.current.list.setFilters({ year: undefined }));
    expect(listUrl('/read')).toBe('/read');
    expect(listUrl('/shelf')).toBe('/shelf');
  });

  it('moves a page past the end to the last existing page', () => {
    const { result } = setup('/shelf?page=5');
    act(() => result.current.list.clampPage(2));
    expect(result.current.location.search).toBe('?page=2');
    act(() => result.current.list.clampPage(0));
    expect(result.current.location.search).toBe('?page=2');
  });
});
