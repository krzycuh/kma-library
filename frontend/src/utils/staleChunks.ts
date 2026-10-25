/**
 * After a deploy, a page opened earlier (or a cached index.html) asks for code chunks of the previous build,
 * which no longer exist. Reloading once fetches the current index.html; a second failure within a short
 * window is a real error and goes to the error screen, so this never loops.
 */
const STORAGE_KEY = 'stale-chunk-reload-at';
const RETRY_WINDOW_MS = 10_000;

const CHUNK_ERROR_PATTERNS = [
  /Importing a module script failed/i, // Safari
  /Failed to fetch dynamically imported module/i, // Chrome
  /error loading dynamically imported module/i, // Firefox
  /Unable to preload CSS/i, // Vite
];

export function isChunkLoadError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error ?? '');
  return CHUNK_ERROR_PATTERNS.some((pattern) => pattern.test(message));
}

interface SimpleStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
}

/** True when a reload should be attempted now (and records the attempt). */
export function shouldReloadForStaleChunks(storage: SimpleStorage | null, now: number = Date.now()): boolean {
  try {
    const last = Number(storage?.getItem(STORAGE_KEY) ?? 0);
    if (last && now - last < RETRY_WINDOW_MS) return false;
    storage?.setItem(STORAGE_KEY, String(now));
    return true;
  } catch {
    return true;
  }
}

function sessionStore(): SimpleStorage | null {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

/** Reloads the page for a stale-chunk error; returns false when it should be shown instead. */
export function reloadForStaleChunks(error: unknown): boolean {
  if (!isChunkLoadError(error) || !shouldReloadForStaleChunks(sessionStore())) return false;
  window.location.reload();
  return true;
}

/** Vite reports failed dynamic imports as `vite:preloadError`; reload instead of failing when it is stale code. */
export function installStaleChunkReload() {
  window.addEventListener('vite:preloadError', (event) => {
    const payload = (event as Event & { payload?: unknown }).payload;
    if (shouldReloadForStaleChunks(sessionStore())) {
      event.preventDefault();
      console.warn('Stale code chunk after a deploy, reloading', payload);
      window.location.reload();
    }
  });
}
