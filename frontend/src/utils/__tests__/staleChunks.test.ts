import { describe, expect, it } from 'vitest';
import { isChunkLoadError, shouldReloadForStaleChunks } from '../staleChunks';

function memoryStorage() {
  const data = new Map<string, string>();
  return { getItem: (k: string) => data.get(k) ?? null, setItem: (k: string, v: string) => void data.set(k, v) };
}

describe('stale chunks', () => {
  it('recognizes failed dynamic imports in Safari, Chrome and Firefox', () => {
    expect(isChunkLoadError(new TypeError('Importing a module script failed.'))).toBe(true);
    expect(isChunkLoadError(new TypeError('Failed to fetch dynamically imported module: https://x/assets/Dashboard-abc.js'))).toBe(true);
    expect(isChunkLoadError(new TypeError('error loading dynamically imported module'))).toBe(true);
    expect(isChunkLoadError(new Error('Cannot read properties of undefined'))).toBe(false);
  });

  it('reloads once and then lets the error through within the retry window', () => {
    const storage = memoryStorage();
    expect(shouldReloadForStaleChunks(storage, 1_000)).toBe(true);
    expect(shouldReloadForStaleChunks(storage, 5_000)).toBe(false);
    expect(shouldReloadForStaleChunks(storage, 20_000)).toBe(true);
  });

  it('still reloads when storage is unavailable', () => {
    expect(shouldReloadForStaleChunks(null)).toBe(true);
  });
});
