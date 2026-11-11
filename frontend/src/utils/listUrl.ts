const keyFor = (path: string) => `kma.listUrl:${path}`;

/** Remembers the list's current query string, so "back to the list" links can restore it. */
export function rememberListUrl(path: string, search: string): void {
  try {
    if (search && search !== '?') sessionStorage.setItem(keyFor(path), search);
    else sessionStorage.removeItem(keyFor(path));
  } catch {
    // Storage unavailable (private mode quota etc.): links just go to the plain list.
  }
}

/** The list path with the filters and page it was last shown with in this tab, e.g. "/read?year=2024&page=2". */
export function listUrl(path: string): string {
  try {
    return path + (sessionStorage.getItem(keyFor(path)) ?? '');
  } catch {
    return path;
  }
}
