/**
 * Build metadata of the frontend, baked in by Vite: CI sets VITE_APP_VERSION to the commit SHA
 * (and VITE_RELEASE_TAG for releases); local builds say "dev".
 */
function envValue(value: unknown): string {
  return typeof value === 'string' ? value.trim().replace(/"/g, '') : '';
}

/** Short form of a commit SHA ("a1b2c3d"); "dev" stays as is. */
export function shortVersion(version: string | null | undefined): string {
  const value = version?.trim() ?? '';
  if (!value || value === 'dev') return 'dev';
  return /^[0-9a-f]{8,40}$/i.test(value) ? value.slice(0, 7) : value;
}

/** Both sides were built by CI and come from different commits (e.g. a stale container or cached bundle). */
export function versionsDiffer(frontend: string, backend: string | null | undefined): boolean {
  if (!backend || frontend === 'dev' || backend === 'dev') return false;
  return frontend !== backend;
}

export const APP_VERSION = envValue(import.meta.env.VITE_APP_VERSION) || 'dev';
export const APP_RELEASE_TAG = envValue(import.meta.env.VITE_RELEASE_TAG) || null;
