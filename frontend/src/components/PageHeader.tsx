import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'

type PageHeaderProps = {
  title: string
  description?: string
  backTo?: string | number
  backLabel?: string
  actions?: ReactNode
  className?: string
}

export function PageHeader({
  title,
  description,
  backTo = '/',
  backLabel = 'Wróć',
  actions,
  className = '',
}: PageHeaderProps) {
  const navigate = useNavigate()

  const handleBack = () => {
    if (typeof backTo === 'number') {
      navigate(backTo)
      return
    }

    navigate(backTo)
  }

  return (
    <div className={`mb-4 sm:mb-8 ${className}`}>
      <button
        type="button"
        onClick={handleBack}
        className="text-blue-600 hover:text-blue-700 flex items-center gap-1.5 sm:gap-2 mb-1 sm:mb-2 text-sm sm:text-base"
      >
        <svg className="w-4 h-4 sm:w-5 sm:h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M10 19l-7-7m0 0l7-7m-7 7h18"
          />
        </svg>
        {backLabel}
      </button>
      <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">{title}</h1>
      {description && <p className="text-gray-600 mt-1 sm:mt-2 text-sm sm:text-base">{description}</p>}
      {actions && <div className="mt-3 sm:mt-6">{actions}</div>}
    </div>
  )
}
