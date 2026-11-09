interface BookCoverProps {
  title: string;
  coverUrl?: string | null;
  /** `fill` stretches to the parent's height and bleeds to its edges (the parent clips the corners). */
  size?: 'sm' | 'md' | 'lg' | 'fill';
}

const sizes = { sm: 'h-14 w-10 text-lg', md: 'h-20 w-14 text-2xl', lg: 'h-40 w-28 text-4xl' };

const placeholderClasses = 'flex items-center justify-center bg-gradient-to-br from-blue-100 to-blue-200 font-bold text-blue-700';

/**
 * Cover image with a placeholder (first letter of the title) when no cover is known.
 */
export function BookCover({ title, coverUrl, size = 'md' }: BookCoverProps) {
  const letter = title.trim().charAt(0).toUpperCase() || '?';

  if (size === 'fill') {
    // The wrapper has no content height of its own, so the text next to it decides how tall the card is
    // and the cover follows (2:3 at the usual card height; taller cards crop it a little)
    return (
      <div className="relative w-[4.5rem] flex-shrink-0 self-stretch">
        {coverUrl ? (
          <img src={coverUrl} alt="" className="absolute inset-0 h-full w-full object-cover" />
        ) : (
          <div className={`absolute inset-0 ${placeholderClasses} text-3xl`} aria-hidden="true">
            {letter}
          </div>
        )}
      </div>
    );
  }

  if (coverUrl) {
    return <img src={coverUrl} alt="" className={`${sizes[size]} flex-shrink-0 rounded object-cover shadow-sm`} />;
  }
  return (
    <div className={`${sizes[size]} ${placeholderClasses} flex-shrink-0 rounded shadow-sm`} aria-hidden="true">
      {letter}
    </div>
  );
}
