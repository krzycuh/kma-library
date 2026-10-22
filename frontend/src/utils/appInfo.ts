/**
 * Shared helpers for exposing build metadata to the UI.
 */
const rawVersion =
  (typeof import.meta.env.VITE_APP_VERSION === 'string' && import.meta.env.VITE_APP_VERSION.trim()) ||
  (typeof import.meta.env.VITE_GIT_SHA === 'string' && import.meta.env.VITE_GIT_SHA.trim()) ||
  ''

const normalizedVersion = rawVersion.replace(/"/g, '')

export const APP_VERSION = normalizedVersion.length > 0 ? normalizedVersion : 'dev'
export const APP_VERSION_SHORT =
  normalizedVersion.length > 0 ? normalizedVersion.slice(0, Math.min(7, normalizedVersion.length)) : 'dev'

export const APP_VERSION_LABEL = `v${APP_VERSION_SHORT}`
