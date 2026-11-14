import type { AxiosError } from 'axios';
import { t } from '../i18n';
import type { ApiError } from '../types/common';
import { VIEW_AS_READ_ONLY } from './viewAs';

/**
 * Extracts a user-facing message from an API error.
 */
export function getApiErrorMessage(error: unknown, fallback: string = t.common.errorGeneric): string {
  const axiosError = error as AxiosError<ApiError>;
  const data = axiosError?.response?.data;
  if (data?.code === VIEW_AS_READ_ONLY) {
    return t.viewAs.readOnlyError;
  }
  if (data?.errors && Object.keys(data.errors).length > 0) {
    return Object.values(data.errors).join(', ');
  }
  return data?.message || fallback;
}
