import { t } from '../i18n';

const MAX_LENGTH = 500;

/**
 * Mirrors the backend CoverUrl rules: blank = no cover, otherwise an https:// address without spaces
 * (browsers block plain-http images on an https page). Returns an error message or null.
 */
export function coverUrlError(raw: string): string | null {
  const value = raw.trim();
  if (!value) return null;
  if (value.length > MAX_LENGTH) return t.cover.tooLong;
  if (!/^https:\/\/\S+$/i.test(value)) return t.cover.httpsOnly;
  return null;
}
