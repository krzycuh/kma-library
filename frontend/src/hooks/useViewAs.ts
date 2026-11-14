import { useCallback, useSyncExternalStore } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuth } from './useAuth';
import { UserRole } from '../types/auth';
import type { ViewedUser } from '../types/admin';
import { readViewedUser, subscribeViewedUser, writeViewedUser } from '../utils/viewAs';

/**
 * Admin "view as user": which account the pages show, and switching it.
 * Switching drops every cached query, so each page refetches as the (new) viewed user.
 */
export function useViewAs() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const stored = useSyncExternalStore(subscribeViewedUser, readViewedUser, () => null);
  const viewedUser = user?.role === UserRole.ADMIN ? stored : null;

  const switchTo = useCallback(
    (target: ViewedUser | null) => {
      writeViewedUser(target);
      queryClient.resetQueries();
    },
    [queryClient]
  );

  return {
    viewedUser,
    isViewing: viewedUser !== null,
    startViewing: (target: ViewedUser) => switchTo(target),
    stopViewing: () => switchTo(null),
  };
}
