import { describe, expect, it } from 'vitest';
import { parseThemePreference, resolveTheme } from '../themePreference';

describe('theme preference', () => {
  it('reads stored values and falls back to system', () => {
    expect(parseThemePreference('dark')).toBe('dark');
    expect(parseThemePreference('light')).toBe('light');
    expect(parseThemePreference('system')).toBe('system');
    expect(parseThemePreference(null)).toBe('system');
    expect(parseThemePreference('neon')).toBe('system');
  });

  it('follows the system only for the system preference', () => {
    expect(resolveTheme('system', true)).toBe('dark');
    expect(resolveTheme('system', false)).toBe('light');
    expect(resolveTheme('light', true)).toBe('light');
    expect(resolveTheme('dark', false)).toBe('dark');
  });
});
