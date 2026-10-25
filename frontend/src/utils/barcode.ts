import { isValidIsbn13 } from './isbn';

/**
 * ISBN-13 from a scanned barcode, or null when the code is not a book's ISBN.
 * A book's EAN-13 starting with 978/979 is its ISBN-13. Some books add a 2- or 5-digit
 * add-on (price) that a scanner may report glued to the main code – it is dropped.
 */
export function isbnFromBarcode(rawValue: string): string | null {
  const digits = rawValue.replace(/\D/g, '');
  if (![13, 15, 18].includes(digits.length)) return null;
  const ean = digits.slice(0, 13);
  if (!/^97[89]/.test(ean)) return null;
  return isValidIsbn13(ean) ? ean : null;
}
