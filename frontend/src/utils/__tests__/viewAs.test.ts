import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  parseViewedUser,
  readViewedUser,
  sendsViewAsHeader,
  subscribeViewedUser,
  VIEW_AS_STORAGE_KEY,
  writeViewedUser,
} from '../viewAs';

const anna = { id: 'u-1', name: 'Anna', email: 'anna@example.com' };

describe('parseViewedUser', () => {
  it('reads a stored account and drops anything extra', () => {
    expect(parseViewedUser(JSON.stringify({ ...anna, role: 'USER' }))).toEqual(anna);
  });

  it('treats nothing, garbage and incomplete records as "not viewing"', () => {
    expect(parseViewedUser(null)).toBeNull();
    expect(parseViewedUser('{not json')).toBeNull();
    expect(parseViewedUser(JSON.stringify({ id: 'u-1' }))).toBeNull();
    expect(parseViewedUser(JSON.stringify('u-1'))).toBeNull();
  });
});

describe('sendsViewAsHeader', () => {
  it('previews the data pages', () => {
    for (const url of ['/dashboard', '/reading-entries?year=2026', '/libraries/1/items', '/books/2', '/users/me-books']) {
      expect(sendsViewAsHeader(url)).toBe(true);
    }
  });

  it("keeps the session, own profile and admin panel the admin's own", () => {
    for (const url of ['/auth/refresh', '/auth/logout', '/users/me', '/users/me/password', '/admin/users', '/admin/users/1/view-as']) {
      expect(sendsViewAsHeader(url)).toBe(false);
    }
    expect(sendsViewAsHeader(undefined)).toBe(false);
  });

  it('understands absolute and /api-prefixed urls', () => {
    expect(sendsViewAsHeader('http://localhost:8080/api/users/me')).toBe(false);
    expect(sendsViewAsHeader('/api/dashboard')).toBe(true);
  });
});

describe('view-as store', () => {
  beforeEach(() => localStorage.clear());

  it('remembers the viewed account on the device and notifies subscribers', () => {
    const listener = vi.fn();
    const unsubscribe = subscribeViewedUser(listener);

    writeViewedUser(anna);
    expect(readViewedUser()).toEqual(anna);
    expect(JSON.parse(localStorage.getItem(VIEW_AS_STORAGE_KEY) ?? '')).toEqual(anna);
    expect(listener).toHaveBeenCalledTimes(1);

    writeViewedUser(null);
    expect(readViewedUser()).toBeNull();
    expect(localStorage.getItem(VIEW_AS_STORAGE_KEY)).toBeNull();
    expect(listener).toHaveBeenCalledTimes(2);

    unsubscribe();
    writeViewedUser(anna);
    expect(listener).toHaveBeenCalledTimes(2);
  });

  it('returns the same object while nothing changed (stable for useSyncExternalStore)', () => {
    writeViewedUser(anna);
    expect(readViewedUser()).toBe(readViewedUser());
  });
});
