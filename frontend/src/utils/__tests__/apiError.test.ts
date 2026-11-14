import { describe, expect, it } from 'vitest';
import { getApiErrorMessage } from '../apiError';
import { t } from '../../i18n';

const apiError = (data: unknown) => ({ isAxiosError: true, response: { status: 403, data } });

describe('getApiErrorMessage', () => {
  it('shows the backend message', () => {
    expect(getApiErrorMessage(apiError({ message: 'Nope' }))).toBe('Nope');
  });

  it('joins field errors', () => {
    expect(getApiErrorMessage(apiError({ message: 'Validation failed', errors: { a: 'x', b: 'y' } }))).toBe('x, y');
  });

  it('falls back when there is nothing usable', () => {
    expect(getApiErrorMessage(new Error('boom'))).toBe(t.common.errorGeneric);
    expect(getApiErrorMessage(undefined, 'custom')).toBe('custom');
  });

  it('explains a change refused because the admin is only previewing an account', () => {
    expect(getApiErrorMessage(apiError({ message: 'Viewing another user\'s account is read-only', code: 'VIEW_AS_READ_ONLY' }))).toBe(
      t.viewAs.readOnlyError
    );
  });
});
