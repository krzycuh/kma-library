import { useSyncExternalStore } from 'react';
import { useQuery } from '@tanstack/react-query';
import { librariesApi } from '../api/libraries';
import { pickCurrentLibrary } from '../utils/currentLibrary';

/**
 * Libraries of the current user: their own one first, then family libraries they were invited to.
 */
export function useLibraries() {
  return useQuery({
    queryKey: ['libraries'],
    queryFn: librariesApi.list,
    staleTime: 5 * 60_000,
  });
}

const STORAGE_KEY = 'currentLibraryId';
const listeners = new Set<() => void>();

function readStoredId(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  window.addEventListener('storage', listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('storage', listener);
  };
}

export function setCurrentLibraryId(id: string) {
  try {
    localStorage.setItem(STORAGE_KEY, id);
  } catch {
    // Private mode etc.: the choice just won't survive a reload
  }
  listeners.forEach((listener) => listener());
}

/**
 * The library the shelf, loans and "add to shelf" work on. Remembered per device; shared by all pages.
 */
export function useCurrentLibrary() {
  const query = useLibraries();
  const storedId = useSyncExternalStore(subscribe, readStoredId, () => null);
  const libraries = query.data ?? [];
  return { ...query, libraries, library: pickCurrentLibrary(libraries, storedId), setLibrary: setCurrentLibraryId };
}
