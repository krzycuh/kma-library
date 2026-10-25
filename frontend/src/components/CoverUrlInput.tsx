import { useEffect, useState } from 'react';
import { t } from '../i18n';
import { coverUrlError } from '../utils/coverUrl';

interface CoverUrlInputProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  inputClassName: string;
}

type PreviewState = 'empty' | 'loading' | 'ok' | 'error';

/**
 * Cover address field with a live preview: paste an image address (e.g. "Copy image address" on a bookstore
 * or Storytel page) and see right away whether it works.
 */
export function CoverUrlInput({ id, value, onChange, disabled = false, inputClassName }: CoverUrlInputProps) {
  const [previewUrl, setPreviewUrl] = useState('');
  const [state, setState] = useState<PreviewState>('empty');
  const validationError = coverUrlError(value);

  useEffect(() => {
    const url = value.trim();
    const handle = setTimeout(() => {
      if (!url || coverUrlError(url)) {
        setPreviewUrl('');
        setState('empty');
      } else {
        setPreviewUrl(url);
        setState('loading');
      }
    }, 300);
    return () => clearTimeout(handle);
  }, [value]);

  const message =
    validationError ?? (state === 'error' ? t.cover.loadFailed : state === 'loading' ? t.cover.loading : null);

  return (
    <div>
      <label htmlFor={id} className="block text-sm font-medium text-gray-700">
        {t.book.coverUrl} <span className="text-gray-400">({t.common.optional})</span>
      </label>
      <div className="mt-1 flex items-start gap-3">
        <div
          className="flex h-20 w-14 flex-shrink-0 items-center justify-center overflow-hidden rounded border border-gray-200 bg-gray-50"
          aria-hidden="true"
        >
          {previewUrl && state !== 'error' ? (
            <img
              src={previewUrl}
              alt=""
              className={`h-full w-full object-cover ${state === 'ok' ? '' : 'opacity-0'}`}
              onLoad={() => setState('ok')}
              onError={() => setState('error')}
            />
          ) : (
            <svg className="h-6 w-6 text-gray-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round">
              <rect x="4" y="3" width="16" height="18" rx="2" />
              <path d="M8 15l3-3 2 2 3-3" />
            </svg>
          )}
        </div>
        <div className="min-w-0 flex-1">
          <input
            id={id}
            type="url"
            inputMode="url"
            autoComplete="off"
            placeholder="https://…"
            value={value}
            disabled={disabled}
            onChange={(e) => onChange(e.target.value)}
            aria-invalid={validationError !== null || state === 'error'}
            aria-describedby={`${id}-hint`}
            className={inputClassName}
          />
          <p
            id={`${id}-hint`}
            className={`mt-1 text-xs ${validationError || state === 'error' ? 'text-red-700' : 'text-gray-500'}`}
            aria-live="polite"
          >
            {message ?? t.cover.hint}
          </p>
        </div>
      </div>
    </div>
  );
}
