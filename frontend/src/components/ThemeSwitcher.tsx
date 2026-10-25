import type { ReactNode } from 'react';
import { useThemePreference } from '../theme/theme';
import type { ThemePreference } from '../theme/themePreference';
import { t } from '../i18n';

const icons: Record<ThemePreference, ReactNode> = {
  system: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <rect x="7" y="2" width="10" height="20" rx="2" />
      <path d="M11 18h2" />
    </svg>
  ),
  light: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  ),
  dark: (
    <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z" />
    </svg>
  ),
};

const OPTIONS: ThemePreference[] = ['system', 'light', 'dark'];

interface ThemeSwitcherProps {
  /** full: icons with labels and a hint (menu, profile); compact: icons only (desktop navigation bar) */
  variant?: 'full' | 'compact';
}

/**
 * System / light / dark (neon). "System" follows the phone's setting and is the default.
 */
export function ThemeSwitcher({ variant = 'full' }: ThemeSwitcherProps) {
  const { preference, resolved, setPreference } = useThemePreference();
  const compact = variant === 'compact';

  return (
    <div className={compact ? '' : 'space-y-2'}>
      {!compact && (
        <span id="theme-switcher-label" className="block text-xs font-semibold uppercase tracking-wide text-gray-500">
          {t.theme.label}
        </span>
      )}
      <div
        role="radiogroup"
        aria-labelledby={compact ? undefined : 'theme-switcher-label'}
        aria-label={compact ? t.theme.label : undefined}
        className={`grid grid-cols-3 gap-1 rounded-lg border border-gray-200 bg-gray-50 p-1 ${compact ? 'w-32' : ''}`}
      >
        {OPTIONS.map((option) => {
          const checked = preference === option;
          return (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={checked}
              aria-label={compact ? t.theme.options[option] : undefined}
              title={compact ? t.theme.options[option] : undefined}
              onClick={() => setPreference(option)}
              className={`flex items-center justify-center rounded-md text-sm transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                compact ? 'h-8' : 'min-h-[3.5rem] flex-col gap-1 px-1 py-2'
              } ${
                checked
                  ? 'border border-blue-500 bg-blue-100 font-semibold text-blue-700'
                  : 'border border-transparent text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              }`}
            >
              {icons[option]}
              {!compact && <span>{t.theme.options[option]}</span>}
            </button>
          );
        })}
      </div>
      {!compact && (
        <p className="text-xs text-gray-600">
          {preference === 'system' ? t.theme.systemHint(t.theme.resolved[resolved]) : t.theme.fixedHint[preference]}
        </p>
      )}
    </div>
  );
}
