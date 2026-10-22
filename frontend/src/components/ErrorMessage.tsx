import type { ReactNode } from 'react';

interface ErrorMessageProps {
  title?: string;
  message: string;
  retry?: () => void;
  children?: ReactNode;
}

/**
 * Consistent error message component
 */
export function ErrorMessage({ title = 'Błąd', message, retry, children }: ErrorMessageProps) {
  return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-6" role="alert">
      <div className="flex items-start">
        <div className="flex-shrink-0">
          <span className="text-3xl" role="img" aria-label="Błąd">
            ⚠️
          </span>
        </div>
        <div className="ml-3 flex-1">
          <h3 className="text-lg font-medium text-red-900">{title}</h3>
          <p className="mt-2 text-sm text-red-800">{message}</p>
          {retry && (
            <button
              onClick={retry}
              className="mt-4 px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
            >
              Spróbuj ponownie
            </button>
          )}
          {children}
        </div>
      </div>
    </div>
  );
}
