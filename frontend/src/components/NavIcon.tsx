export type NavIconName = 'read' | 'shelf' | 'loans' | 'profile' | 'users';

// Outline icons drawn on a 24px grid with a 1.75px stroke; the shelf echoes the three books of the logo
const paths: Record<NavIconName, string[]> = {
  read: [
    'M12 6.04A8.97 8.97 0 0 0 6 3.75c-1.05 0-2.06.18-3 .51v14.25A8.99 8.99 0 0 1 6 18c2.3 0 4.41.87 6 2.29',
    'M12 6.04a8.97 8.97 0 0 1 6-2.29c1.05 0 2.06.18 3 .51v14.25A8.99 8.99 0 0 0 18 18a8.97 8.97 0 0 0-6 2.29',
    'M12 6.04v14.25',
  ],
  shelf: ['M4 5h4v14H4z', 'M10 3h4v16h-4z', 'M16.4 6.2l3.6 1-3.2 11.8-3.6-1', 'M2.5 21h19'],
  loans: ['M7.5 21 3 16.5m0 0L7.5 12M3 16.5h13.5', 'M16.5 3 21 7.5m0 0L16.5 12M21 7.5H7.5'],
  profile: ['M15.75 7.5a3.75 3.75 0 1 1-7.5 0 3.75 3.75 0 0 1 7.5 0Z', 'M4.5 20.25a7.5 7.5 0 0 1 15 0'],
  users: [
    'M12 8.25a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
    'M3 19.5a6 6 0 0 1 12 0',
    'M15.75 5.4a2.75 2.75 0 1 1 0 5.2',
    'M17.25 13.6a5.25 5.25 0 0 1 3.75 5.15',
  ],
};

/**
 * Line icon for a main navigation entry. Strokes in the current text color; the neon theme adds a glow (theme.css).
 */
export function NavIcon({ name, className = 'h-5 w-5' }: { name: NavIconName; className?: string }) {
  return (
    <svg
      className={`nav-icon flex-shrink-0 ${className}`}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
