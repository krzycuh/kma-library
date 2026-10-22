/**
 * ISBN helpers mirroring the backend Isbn value object.
 */
export function normalizeIsbn(raw: string): string {
  return raw.toUpperCase().replace(/[-\s]/g, '');
}

export function isValidIsbn10(s: string): boolean {
  if (!/^\d{9}[\dX]$/.test(s)) return false;
  let sum = 0;
  for (let i = 0; i < 9; i++) sum += Number(s[i]) * (10 - i);
  sum += s[9] === 'X' ? 10 : Number(s[9]);
  return sum % 11 === 0;
}

export function isValidIsbn13(s: string): boolean {
  if (!/^\d{13}$/.test(s)) return false;
  let sum = 0;
  for (let i = 0; i < 12; i++) sum += Number(s[i]) * (i % 2 === 0 ? 1 : 3);
  const check = (10 - (sum % 10)) % 10;
  return check === Number(s[12]);
}

export function isValidIsbn(raw: string): boolean {
  const s = normalizeIsbn(raw);
  return s.length === 10 ? isValidIsbn10(s) : s.length === 13 ? isValidIsbn13(s) : false;
}
