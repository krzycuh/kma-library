import { useQuery } from '@tanstack/react-query';
import { healthApi } from '../api/health';
import { APP_RELEASE_TAG, APP_VERSION, shortVersion, versionsDiffer } from '../utils/appInfo';
import { t } from '../i18n';

interface VersionInfoProps {
  /** compact: "fe a1b2c3d · be a1b2c3d" under the logo; full: the same with words, for the mobile menu */
  variant?: 'compact' | 'full';
  className?: string;
  /** Text color when both sides match; a mismatch is always amber */
  colorClassName?: string;
}

/**
 * Commits the frontend bundle and the backend were built from. Highlighted when they differ
 * (backend not restarted after an update, or an old bundle in the browser cache).
 */
export function VersionInfo({ variant = 'compact', className = '', colorClassName = 'text-gray-500' }: VersionInfoProps) {
  const backendQuery = useQuery({
    queryKey: ['backend-version'],
    queryFn: healthApi.get,
    staleTime: Infinity,
    retry: false,
  });

  const frontend = shortVersion(APP_VERSION);
  const backend = backendQuery.data ? shortVersion(backendQuery.data.version) : backendQuery.isError ? '?' : '…';
  const differ = versionsDiffer(APP_VERSION, backendQuery.data?.version);
  const release = APP_RELEASE_TAG ?? backendQuery.data?.releaseTag ?? null;

  const tooltip = [
    `${t.version.frontend}: ${APP_VERSION}`,
    `${t.version.backend}: ${backendQuery.data?.version ?? backend}`,
    backendQuery.data?.buildTime ? `${t.version.backendBuilt}: ${backendQuery.data.buildTime}` : null,
    differ ? t.version.differ : null,
  ]
    .filter(Boolean)
    .join('\n');

  const text =
    variant === 'compact'
      ? `fe ${frontend} · be ${backend}`
      : `${t.version.frontend} ${frontend} · ${t.version.backend} ${backend}${release ? ` · ${release}` : ''}`;

  return (
    <span title={tooltip} className={`${differ ? 'font-semibold text-amber-600' : colorClassName} ${className}`}>
      {text}
      {differ && <span className="sr-only">. {t.version.differ}</span>}
    </span>
  );
}
