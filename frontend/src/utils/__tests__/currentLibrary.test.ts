import { describe, expect, it } from 'vitest';
import { pickCurrentLibrary } from '../currentLibrary';
import type { Library } from '../../types/shelf';

const library = (id: string, myRole: Library['myRole']): Library => ({
  id,
  name: id,
  ownerId: 'o',
  createdAt: '2026-01-01T00:00:00',
  myRole,
  memberCount: 1,
});

describe('pickCurrentLibrary', () => {
  const own = library('own', 'OWNER');
  const family = library('family', 'MEMBER');

  it('keeps the library the user picked', () => {
    expect(pickCurrentLibrary([own, family], 'family')).toBe(family);
  });

  it('falls back to the first (own) library when nothing or an unknown library is stored', () => {
    expect(pickCurrentLibrary([own, family], null)).toBe(own);
    expect(pickCurrentLibrary([own, family], 'left-long-ago')).toBe(own);
  });

  it('returns null without libraries', () => {
    expect(pickCurrentLibrary([], 'own')).toBeNull();
  });
});
