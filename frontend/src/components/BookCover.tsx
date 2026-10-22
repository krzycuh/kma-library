interface BookCoverProps {
  title: string;
  coverUrl?: string | null;
  size?: 'sm' | 'md' | 'lg';
}

const sizes = { sm: 'h-14 w-10 text-lg', md: 'h-20 w-14 text-2xl', lg: 'h-40 w-28 text-4xl' };

/**
 * Cover image with a placeholder (first letter of the title) when no cover is known.
 */
export function BookCover({ title, coverUrl, size = 'md' }: BookCoverProps) {
  if (coverUrl) {
    return <img src={coverUrl} alt="" className={`${sizes[size]} flex-shrink-0 rounded object-cover shadow-sm`} />;
  }
  const letter = title.trim().charAt(0).toUpperCase() || '?';
  return (
    <div
      className={`${sizes[size]} flex flex-shrink-0 items-center justify-center rounded bg-gradient-to-br from-blue-100 to-blue-200 font-bold text-blue-700 shadow-sm`}
      aria-hidden="true"
    >
      {letter}
    </div>
  );
}
