import { useSyncExternalStore } from 'react';
import {
  parseThemePreference,
  resolveTheme,
  THEME_COLORS,
  THEME_STORAGE_KEY,
  type ResolvedTheme,
  type ThemePreference,
} from './themePreference';

/**
 * Theme preference (system / light / dark) remembered per device. public/theme-init.js applies the same rules
 * before the first paint; this module keeps it in sync afterwards, including when the phone switches modes.
 */
const DARK_QUERY = '(prefers-color-scheme: dark)';
const listeners = new Set<() => void>();

function readPreference(): ThemePreference {
  try {
    return parseThemePreference(localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return 'system';
  }
}

function systemPrefersDark(): boolean {
  return typeof window.matchMedia === 'function' && window.matchMedia(DARK_QUERY).matches;
}

function apply() {
  const theme: ResolvedTheme = resolveTheme(readPreference(), systemPrefersDark());
  document.documentElement.classList.toggle('dark', theme === 'dark');
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme]);
  listeners.forEach((listener) => listener());
}

export function setThemePreference(preference: ThemePreference) {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    // Private mode: the choice lasts until the page is reloaded
  }
  apply();
}

let watching = false;
function subscribe(listener: () => void) {
  listeners.add(listener);
  if (!watching && typeof window.matchMedia === 'function') {
    watching = true;
    window.matchMedia(DARK_QUERY).addEventListener('change', apply);
    window.addEventListener('storage', (event) => event.key === THEME_STORAGE_KEY && apply());
  }
  return () => listeners.delete(listener);
}

export function useThemePreference() {
  const preference = useSyncExternalStore(subscribe, readPreference, () => 'system' as ThemePreference);
  const resolved = useSyncExternalStore(
    subscribe,
    () => (document.documentElement.classList.contains('dark') ? 'dark' : 'light') as ResolvedTheme,
    () => 'light' as ResolvedTheme,
  );
  return { preference, resolved, setPreference: setThemePreference };
}
