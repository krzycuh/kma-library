export interface PagedResponse<T> {
  items: T[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  /** Stable identifier for the few errors the app reacts to, e.g. VIEW_AS_READ_ONLY. */
  code?: string;
  errors?: Record<string, string>;
}
