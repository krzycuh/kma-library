import { describe, expect, it } from 'vitest';
import { coverUrlError } from '../coverUrl';
import { t } from '../../i18n';

describe('coverUrlError', () => {
  it('accepts blank and https addresses', () => {
    expect(coverUrlError('')).toBeNull();
    expect(coverUrlError('   ')).toBeNull();
    expect(coverUrlError(' https://www.storytel.com/images/cover.jpg ')).toBeNull();
  });

  it('rejects http, other schemes and spaces', () => {
    expect(coverUrlError('http://example.com/c.jpg')).toBe(t.cover.httpsOnly);
    expect(coverUrlError('javascript:alert(1)')).toBe(t.cover.httpsOnly);
    expect(coverUrlError('https://example.com/a b.jpg')).toBe(t.cover.httpsOnly);
  });

  it('rejects overlong addresses', () => {
    expect(coverUrlError(`https://example.com/${'a'.repeat(500)}`)).toBe(t.cover.tooLong);
  });
});
