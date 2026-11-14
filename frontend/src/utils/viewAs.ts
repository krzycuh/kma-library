import type { ViewedUser } from '../types/admin';

/**
 * Admin "view as user": the account whose data the app currently shows instead of the admin's own.
 * Remembered on the device (like the current library) so a reload keeps the preview; cleared on logout.
 * Plain store without React so that the API client can read it too; `useViewAs` wraps it for components.
 */

export const VIEW_AS_HEADER = 'X-View-As-User';
export const VIEW_AS_STORAGE_KEY = 'viewAsUser';

/** Error codes the backend returns for a refused view-as request (see ViewAsUserResolver). */
export const VIEW_AS_READ_ONLY = 'VIEW_AS_READ_ONLY';
export const VIEW_AS_ENDED_CODES = new Set(['VIEW_AS_USER_NOT_FOUND', 'VIEW_AS_FORBIDDEN']);

const listeners = new Set<() => void>();
let cachedRaw: string | null | undefined;
let cachedUser: ViewedUser | null = null;

export function parseViewedUser(raw: string | null): ViewedUser | null {
  if (!raw) return null;
  try {
    const value: unknown = JSON.parse(raw);
    if (
      typeof value === 'object' &&
      value !== null &&
      typeof (value as ViewedUser).id === 'string' &&
      typeof (value as ViewedUser).name === 'string' &&
      typeof (value as ViewedUser).email === 'string'
    ) {
      const { id, name, email } = value as ViewedUser;
      return { id, name, email };
    }
  } catch {
    // Garbage in storage: treat as "not viewing"
  }
  return null;
}

/**
 * Requests that must always be the admin's own: the session and profile (so the app still knows who is
 * logged in) and the admin panel (where the preview is started and stopped).
 */
export function sendsViewAsHeader(url: string | undefined): boolean {
  if (!url) return false;
  const path = url.replace(/^https?:\/\/[^/]+/, '').replace(/^\/api(?=\/)/, '').split('?')[0];
  return !(path.startsWith('/auth/') || path.startsWith('/admin/') || path === '/users/me' || path.startsWith('/users/me/'));
}

export function readViewedUser(): ViewedUser | null {
  let raw: string | null;
  try {
    raw = localStorage.getItem(VIEW_AS_STORAGE_KEY);
  } catch {
    raw = null;
  }
  // useSyncExternalStore needs a stable reference while the stored value is unchanged
  if (raw !== cachedRaw) {
    cachedRaw = raw;
    cachedUser = parseViewedUser(raw);
  }
  return cachedUser;
}

export function writeViewedUser(user: ViewedUser | null) {
  try {
    if (user) {
      localStorage.setItem(VIEW_AS_STORAGE_KEY, JSON.stringify({ id: user.id, name: user.name, email: user.email }));
    } else {
      localStorage.removeItem(VIEW_AS_STORAGE_KEY);
    }
  } catch {
    // Private mode etc.: the preview just won't survive a reload
  }
  listeners.forEach((listener) => listener());
}

export function subscribeViewedUser(listener: () => void) {
  listeners.add(listener);
  window.addEventListener('storage', listener);
  return () => {
    listeners.delete(listener);
    window.removeEventListener('storage', listener);
  };
}
