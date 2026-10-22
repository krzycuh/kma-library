import { describe, expect, it } from 'vitest';
import { isValidIsbn } from '../isbn';

describe('isbn utils', () => {
  it('accepts valid ISBN-13', () => {
    expect(isValidIsbn('978-83-08-05514-4')).toBe(true);
    expect(isValidIsbn('9780306406157')).toBe(true);
  });

  it('accepts valid ISBN-10', () => {
    expect(isValidIsbn('0-306-40615-2')).toBe(true);
    expect(isValidIsbn('080442957X')).toBe(true);
  });

  it('rejects invalid checksums and lengths', () => {
    expect(isValidIsbn('9780306406158')).toBe(false);
    expect(isValidIsbn('0306406153')).toBe(false);
    expect(isValidIsbn('12345')).toBe(false);
  });
});
