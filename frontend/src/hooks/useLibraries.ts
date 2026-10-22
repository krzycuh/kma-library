import { useQuery } from '@tanstack/react-query';
import { librariesApi } from '../api/libraries';

/**
 * Libraries of the current user. MVP: exactly one (the default library).
 */
export function useLibraries() {
  return useQuery({
    queryKey: ['libraries'],
    queryFn: librariesApi.list,
    staleTime: 5 * 60_000,
  });
}

export function useDefaultLibrary() {
  const query = useLibraries();
  return { ...query, library: query.data?.[0] ?? null };
}
