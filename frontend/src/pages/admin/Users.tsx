import { useEffect, useState } from 'react';
import { Dialog } from '@headlessui/react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminUsersApi } from '../../api/adminUsers';
import { PageHeader } from '../../components/PageHeader';
import { EmptyState } from '../../components/EmptyState';
import { ErrorMessage } from '../../components/ErrorMessage';
import { LoadingSkeleton } from '../../components/LoadingSkeleton';
import { Pagination } from '../../components/Pagination';
import { ConfirmationModal } from '../../components/ConfirmationModal';
import { useAuth } from '../../hooks/useAuth';
import { useToast } from '../../hooks/useToast';
import { t } from '../../i18n';
import type { UserSummary } from '../../types/admin';
import { formatDateTime } from '../../utils/format';
import { getApiErrorMessage } from '../../utils/apiError';

const PAGE_SIZE = 20;

const smallButton =
  'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50';

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${
        active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
      }`}
    >
      {active ? t.admin.active : t.admin.blocked}
    </span>
  );
}

function roleLabel(role: string) {
  return role === 'ADMIN' ? t.admin.roleAdmin : t.admin.roleUser;
}

/**
 * Admin: list users, block/unblock, reset password.
 */
export function AdminUsers() {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [page, setPage] = useState(0);
  const [toggling, setToggling] = useState<UserSummary | null>(null);
  const [resetting, setResetting] = useState<UserSummary | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [passwordError, setPasswordError] = useState<string | null>(null);

  useEffect(() => {
    const handle = setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => clearTimeout(handle);
  }, [search]);

  useEffect(() => {
    setPage(0);
  }, [debouncedSearch]);

  const params = { searchTerm: debouncedSearch || undefined, page, pageSize: PAGE_SIZE };
  const usersQuery = useQuery({
    queryKey: ['admin-users', params],
    queryFn: () => adminUsersApi.list(params),
  });

  const toggleMutation = useMutation({
    mutationFn: (u: UserSummary) => adminUsersApi.setActive(u.id, !u.isActive),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setToggling(null);
      toast.success(updated.isActive ? t.admin.userUnblocked : t.admin.userBlocked);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const resetMutation = useMutation({
    mutationFn: ({ id, password }: { id: string; password: string }) => adminUsersApi.resetPassword(id, password),
    onSuccess: () => {
      closeReset();
      toast.success(t.admin.passwordReset);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const closeReset = () => {
    setResetting(null);
    setNewPassword('');
    setPasswordError(null);
  };

  const submitReset = (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 8) {
      setPasswordError(t.admin.passwordMin);
      return;
    }
    if (resetting) resetMutation.mutate({ id: resetting.id, password: newPassword });
  };

  const users = usersQuery.data?.items ?? [];

  const actions = (u: UserSummary) => {
    const isSelf = u.id === currentUser?.id;
    return (
      <div className="flex flex-wrap gap-2">
        <button type="button" className={smallButton} disabled={isSelf} onClick={() => setToggling(u)}>
          {u.isActive ? t.admin.block : t.admin.unblock}
        </button>
        <button type="button" className={smallButton} onClick={() => setResetting(u)}>
          {t.admin.resetPassword}
        </button>
      </div>
    );
  };

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader title={t.admin.usersTitle} description={t.admin.usersSubtitle} />

      <div className="mb-4">
        <label htmlFor="users-search" className="sr-only">
          {t.admin.search}
        </label>
        <input
          id="users-search"
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t.admin.search}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:w-80"
        />
      </div>

      {usersQuery.isLoading ? (
        <LoadingSkeleton />
      ) : usersQuery.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => usersQuery.refetch()} />
      ) : users.length === 0 ? (
        <EmptyState icon="👥" title={t.admin.usersTitle} description={t.admin.empty} />
      ) : (
        <>
          {/* Mobile cards */}
          <ul className="space-y-3 md:hidden">
            {users.map((u) => (
              <li key={u.id} className="rounded-lg bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="truncate font-semibold text-gray-900">
                      {u.name}
                      {u.id === currentUser?.id && <span className="ml-1 text-xs text-gray-500">({t.admin.you})</span>}
                    </p>
                    <p className="truncate text-sm text-gray-600">{u.email}</p>
                    <p className="text-xs text-gray-500">
                      {roleLabel(u.role)} · {formatDateTime(u.createdAt)}
                    </p>
                  </div>
                  <StatusBadge active={u.isActive} />
                </div>
                <div className="mt-3">{actions(u)}</div>
              </li>
            ))}
          </ul>

          {/* Desktop table */}
          <div className="hidden overflow-hidden rounded-lg bg-white shadow-sm md:block">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {[t.admin.name, t.admin.email, t.admin.role, t.admin.status, t.admin.createdAt, t.admin.actions].map((h) => (
                    <th key={h} scope="col" className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">
                      {u.name}
                      {u.id === currentUser?.id && <span className="ml-1 text-xs text-gray-500">({t.admin.you})</span>}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{u.email}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{roleLabel(u.role)}</td>
                    <td className="px-4 py-3 text-sm">
                      <StatusBadge active={u.isActive} />
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{formatDateTime(u.createdAt)}</td>
                    <td className="px-4 py-3 text-sm">{actions(u)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <Pagination page={page} totalPages={usersQuery.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}

      <ConfirmationModal
        isOpen={toggling !== null}
        onClose={() => setToggling(null)}
        onConfirm={() => toggling && toggleMutation.mutate(toggling)}
        title={toggling ? (toggling.isActive ? t.admin.blockConfirmTitle(toggling.name) : t.admin.unblockConfirmTitle(toggling.name)) : ''}
        description={toggling?.isActive ? t.admin.blockConfirmText : t.admin.unblockConfirmText}
        confirmText={toggling?.isActive ? t.admin.block : t.admin.unblock}
        cancelText={t.common.cancel}
        variant={toggling?.isActive ? 'danger' : 'info'}
        isLoading={toggleMutation.isPending}
      />

      <Dialog open={resetting !== null} onClose={closeReset} className="relative z-50">
        <div className="fixed inset-0 bg-black/30" aria-hidden="true" />
        <div className="fixed inset-0 flex items-center justify-center p-4">
          <Dialog.Panel className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <Dialog.Title className="mb-4 text-lg font-semibold text-gray-900">
              {resetting ? t.admin.resetPasswordTitle(resetting.name) : ''}
            </Dialog.Title>
            <form onSubmit={submitReset} className="space-y-4">
              <div>
                <label htmlFor="reset-password" className="block text-sm font-medium text-gray-700">
                  {t.admin.newPassword}
                </label>
                <input
                  id="reset-password"
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="mt-1 block w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
                />
                {passwordError && <p className="mt-1 text-sm text-red-600">{passwordError}</p>}
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={closeReset}
                  disabled={resetMutation.isPending}
                  className="flex-1 rounded-lg border border-gray-300 px-4 py-2 text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                >
                  {t.common.cancel}
                </button>
                <button
                  type="submit"
                  disabled={resetMutation.isPending}
                  className="flex-1 rounded-lg bg-blue-600 px-4 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {resetMutation.isPending ? t.common.saving : t.admin.resetPassword}
                </button>
              </div>
            </form>
          </Dialog.Panel>
        </div>
      </Dialog>
    </div>
  );
}
