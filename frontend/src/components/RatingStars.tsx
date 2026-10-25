import { t } from '../i18n';

interface RatingStarsProps {
  value: number | null | undefined;
  onChange?: (value: number | null) => void;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const sizeClasses = { sm: 'text-base', md: 'text-2xl', lg: 'text-3xl' };

/**
 * 1–5 star rating. Read-only when no onChange is given; clicking the current value clears it.
 */
export function RatingStars({ value, onChange, size = 'md', className = '' }: RatingStarsProps) {
  const interactive = typeof onChange === 'function';
  const stars = [1, 2, 3, 4, 5];

  if (!interactive) {
    if (value == null) {
      return <span className={`text-sm text-gray-400 ${className}`}>{t.reading.noRating}</span>;
    }
    return (
      <span className={`inline-flex ${sizeClasses[size]} leading-none ${className}`} aria-label={t.reading.ratingLabel(value)}>
        {stars.map((star) => (
          <span key={star} className={star <= value ? 'text-star' : 'text-gray-300'} aria-hidden="true">
            ★
          </span>
        ))}
      </span>
    );
  }

  return (
    <div className={`inline-flex items-center gap-1 ${className}`} role="radiogroup" aria-label={t.reading.rating}>
      {stars.map((star) => {
        const selected = value != null && star <= value;
        return (
          <button
            key={star}
            type="button"
            role="radio"
            aria-checked={value === star}
            aria-label={t.reading.ratingLabel(star)}
            onClick={() => onChange(value === star ? null : star)}
            className={`${sizeClasses[size]} leading-none transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-blue-500 rounded ${
              selected ? 'text-star' : 'text-gray-300'
            }`}
          >
            ★
          </button>
        );
      })}
      <button
        type="button"
        onClick={() => onChange(null)}
        className="ml-2 text-xs text-gray-500 hover:text-gray-700"
      >
        {t.reading.noRating}
      </button>
    </div>
  );
}
