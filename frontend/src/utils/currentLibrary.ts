import type { Library } from '../types/shelf';

/**
 * The library the user last picked, or – when there is none or it is gone (left the library,
 * another account on this device) – the first one; the API lists the user's own library first.
 */
export function pickCurrentLibrary(libraries: Library[], storedId: string | null): Library | null {
  return libraries.find((library) => library.id === storedId) ?? libraries[0] ?? null;
}
