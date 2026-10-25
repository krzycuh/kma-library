import { describe, expect, it } from 'vitest';
import { isbnLookupErrorMessage } from '../isbnLookup';
import { t } from '../../i18n';

const httpError = (status: number, message?: string) => ({ response: { status, data: message ? { message } : {} } });

describe('isbnLookupErrorMessage', () => {
  it('maps catalog statuses to friendly messages', () => {
    expect(isbnLookupErrorMessage(httpError(404))).toBe(t.isbn.notFound);
    expect(isbnLookupErrorMessage(httpError(503))).toBe(t.isbn.unavailable);
    expect(isbnLookupErrorMessage(httpError(400))).toBe(t.isbn.incomplete);
  });

  it('falls back to the API message or a generic one', () => {
    expect(isbnLookupErrorMessage(httpError(500, 'boom'))).toBe('boom');
    expect(isbnLookupErrorMessage(new Error('network'))).toBe(t.common.errorGeneric);
  });
});
