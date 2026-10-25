import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { UserRole } from '../types/auth';
import logoCard from '../assets/logo-card.svg';
import logoCardNeon from '../assets/logo-card-neon.svg';
import { VersionInfo } from './VersionInfo';
import { ThemeSwitcher } from './ThemeSwitcher';
import { t } from '../i18n';

/**
 * Main navigation component with accessibility features
 */
export function Navigation() {
  const { user, logout } = useAuth();
  const location = useLocation();
  const isAdmin = user?.role === UserRole.ADMIN;
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const isActive = (path: string) => location.pathname === path || location.pathname.startsWith(`${path}/`);

  const linkClasses = (path: string) =>
    `flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
      isActive(path)
        ? 'bg-blue-100 text-blue-700'
        : 'text-gray-700 hover:bg-gray-100 hover:text-gray-900'
    }`;

  useEffect(() => {
    setIsMenuOpen(false);
  }, [location.pathname]);

  const sharedLinks = (
    <>
      <Link to="/read" className={linkClasses('/read')} aria-label={t.nav.read}>
        📖 <span>{t.nav.read}</span>
      </Link>
      <Link to="/shelf" className={linkClasses('/shelf')} aria-label={t.nav.shelf}>
        📚 <span>{t.nav.shelf}</span>
      </Link>
      <Link to="/loans" className={linkClasses('/loans')} aria-label={t.nav.loans}>
        🤝 <span>{t.nav.loans}</span>
      </Link>
      <Link to="/profile" className={linkClasses('/profile')} aria-label={t.nav.profile}>
        👤 <span>{t.nav.profile}</span>
      </Link>
      {isAdmin && (
        <Link to="/admin/users" className={linkClasses('/admin/users')} aria-label={t.nav.users}>
          👥 <span>{t.nav.users}</span>
        </Link>
      )}
    </>
  );

  return (
    <nav className="sticky top-0 z-40 bg-white shadow-sm" role="navigation" aria-label={t.nav.mainNavigation}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center">
            <Link
              to="/"
              className="rounded text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              aria-label={t.nav.home}
            >
              <span className="flex items-center gap-2">
                <img src={logoCard} alt="" className="h-8 w-auto dark:hidden" />
                <img src={logoCardNeon} alt="" className="hidden h-8 w-auto dark:block" />
                <span className="flex flex-col leading-tight">
                  <span className="logo-glow text-xl font-bold tracking-tight">{t.app.name}</span>
                  <VersionInfo className="text-xs font-medium" />
                </span>
              </span>
            </Link>
          </div>
          <div className="hidden items-center space-x-2 md:flex">
            {sharedLinks}
            <Link
              to="/read/new"
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              aria-label={t.nav.addRead}
            >
              {t.nav.addRead}
            </Link>
            <ThemeSwitcher variant="compact" />
            <button
              onClick={logout}
              className="rounded px-4 py-2 text-sm font-medium text-gray-700 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
              aria-label={t.nav.logout}
            >
              {t.nav.logout}
            </button>
          </div>
          <button
            type="button"
            className="inline-flex items-center rounded-md p-2 text-gray-700 hover:bg-gray-100 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-blue-500 md:hidden"
            onClick={() => setIsMenuOpen((prev) => !prev)}
            aria-controls="primary-navigation"
            aria-expanded={isMenuOpen}
          >
            <span className="sr-only">{t.nav.toggleMenu}</span>
            {isMenuOpen ? (
              <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-6 w-6" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            )}
          </button>
        </div>
      </div>

      <div
        id="primary-navigation"
        className={`md:hidden ${isMenuOpen ? 'block' : 'hidden'} border-t border-gray-200 bg-white shadow-inner`}
      >
        <div className="space-y-1 px-4 py-4">
          {sharedLinks}
          <Link
            to="/read/new"
            className="flex items-center justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white"
            aria-label={t.nav.addRead}
          >
            {t.nav.addRead}
          </Link>
          <div className="border-t border-gray-200 pt-3">
            <ThemeSwitcher />
          </div>
          <button
            onClick={logout}
            className="w-full rounded-md px-3 py-2 text-left text-sm font-medium text-gray-700 hover:bg-gray-100 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
            aria-label={t.nav.logout}
          >
            {t.nav.logout}
          </button>
          <p className="pt-2 text-xs font-medium">
            <VersionInfo variant="full" colorClassName="text-gray-400" />
          </p>
        </div>
      </div>
    </nav>
  );
}
