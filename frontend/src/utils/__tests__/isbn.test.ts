import { describe, expect, it } from 'vitest';
import { isValidIsbn, toIsbn13 } from '../isbn';

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

  it('normalizes ISBN-10 and formatted ISBN-13 to bare ISBN-13', () => {
    expect(toIsbn13('0-14-032872-6')).toBe('9780140328721');
    expect(toIsbn13('080442957X')).toBe('9780804429573');
    expect(toIsbn13('978 83 08 05514 4')).toBe('9788308055144');
  });

  it('returns null for invalid input', () => {
    expect(toIsbn13('9780306406158')).toBeNull();
    expect(toIsbn13('')).toBeNull();
    expect(toIsbn13('abc')).toBeNull();
  });
});
