import { describe, expect, it } from 'vitest';
import { isbnFromBarcode } from '../barcode';

describe('isbnFromBarcode', () => {
  it('accepts EAN-13 codes of books (978/979)', () => {
    expect(isbnFromBarcode('9788308049402')).toBe('9788308049402');
    expect(isbnFromBarcode('9791032305690')).toBe('9791032305690');
  });

  it('drops a 2- or 5-digit add-on reported together with the code', () => {
    expect(isbnFromBarcode('978830804940251299')).toBe('9788308049402');
    expect(isbnFromBarcode('978830804940212')).toBe('9788308049402');
  });

  it('rejects other products, wrong checksums and partial reads', () => {
    expect(isbnFromBarcode('5901234123457')).toBeNull(); // grocery EAN-13
    expect(isbnFromBarcode('9788308049403')).toBeNull();
    expect(isbnFromBarcode('978830804940')).toBeNull();
    expect(isbnFromBarcode('')).toBeNull();
  });
});
