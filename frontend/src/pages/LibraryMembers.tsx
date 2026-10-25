import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { librariesApi } from '../api/libraries';
import { PageHeader } from '../components/PageHeader';
import { LibrarySwitcher } from '../components/LibrarySwitcher';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { EmptyState } from '../components/EmptyState';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { useAuth } from '../hooks/useAuth';
import { useCurrentLibrary } from '../hooks/useLibraries';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { LibraryMember } from '../types/shelf';
import { getApiErrorMessage } from '../utils/apiError';

const inputClasses =
  'mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;

function inviteErrorMessage(error: unknown): string {
  const status = (error as AxiosError)?.response?.status;
  if (status === 404) return t.members.inviteNotFound;
  if (status === 409) return t.members.alreadyMember;
  return getApiErrorMessage(error);
}

/**
 * /shelf/members: who uses the current library. The owner renames it, adds members by e-mail and removes them;
 * a member can leave.
 */
export function LibraryMembers() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const toast = useToast();
  const { user } = useAuth();
  const { library, isLoading: libraryLoading } = useCurrentLibrary();
  const isOwner = library?.myRole === 'OWNER';

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [removing, setRemoving] = useState<LibraryMember | null>(null);

  useEffect(() => {
    setName(library?.name ?? '');
  }, [library?.id, library?.name]);

  const membersQuery = useQuery({
    queryKey: ['library-members', library?.id],
    queryFn: () => librariesApi.members(library!.id),
    enabled: !!library,
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['library-members', library?.id] });
    queryClient.invalidateQueries({ queryKey: ['libraries'] });
  };

  const renameMutation = useMutation({
    mutationFn: () => librariesApi.rename(library!.id, name.trim()),
    onSuccess: () => {
      refresh();
      toast.success(t.members.renamed);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const inviteMutation = useMutation({
    mutationFn: () => librariesApi.invite(library!.id, email.trim()),
    onSuccess: (member) => {
      refresh();
      setEmail('');
      toast.success(t.members.invited(member.name));
    },
    onError: (err) => setInviteError(inviteErrorMessage(err)),
  });

  const removeMutation = useMutation({
    mutationFn: (member: LibraryMember) => librariesApi.removeMember(library!.id, member.userId),
    onSuccess: (_, member) => {
      setRemoving(null);
      if (member.userId === user?.id) {
        // Left the library: drop its data and go back to the remaining (own) shelf
        queryClient.invalidateQueries({ queryKey: ['libraries'] });
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        toast.success(t.members.left);
        navigate('/shelf', { replace: true });
        return;
      }
      refresh();
      toast.success(t.members.removed);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const onRename = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return toast.error(t.members.nameRequired);
    renameMutation.mutate();
  };

  const onInvite = (e: FormEvent) => {
    e.preventDefault();
    if (!EMAIL_PATTERN.test(email.trim())) return setInviteError(t.members.invalidEmail);
    setInviteError(null);
    inviteMutation.mutate();
  };

  if (libraryLoading) return <LoadingSpinner text={t.common.loading} />;
  if (!library) return <EmptyState icon="📚" title={t.shelf.emptyTitle} description={t.shelf.noLibrary} />;

  const leaving = removing?.userId === user?.id;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t.members.title} description={t.members.subtitle(library.name)} backTo="/shelf" backLabel={t.shelf.title} />

      <div className="mb-4">
        <LibrarySwitcher />
      </div>

      {isOwner && (
        <form onSubmit={onRename} className="mb-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" noValidate>
          <h2 className="text-lg font-semibold text-gray-900">{t.members.nameSection}</h2>
          <label htmlFor="library-name" className="sr-only">
            {t.members.nameLabel}
          </label>
          <div className="mt-2 flex gap-2">
            <input
              id="library-name"
              type="text"
              maxLength={255}
              value={name}
              onChange={(e) => setName(e.target.value)}
              className={`${inputClasses} mt-0`}
            />
            <button
              type="submit"
              disabled={renameMutation.isPending || name.trim() === library.name}
              className="flex-shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
            >
              {renameMutation.isPending ? t.common.saving : t.common.save}
            </button>
          </div>
        </form>
      )}

      {isOwner && (
        <form onSubmit={onInvite} className="mb-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" noValidate>
          <h2 className="text-lg font-semibold text-gray-900">{t.members.inviteSection}</h2>
          <p className="mt-1 text-sm text-gray-600">{t.members.inviteHint}</p>
          <label htmlFor="invite-email" className="mt-3 block text-sm font-medium text-gray-700">
            {t.members.inviteEmail}
          </label>
          <div className="flex gap-2">
            <input
              id="invite-email"
              type="email"
              inputMode="email"
              autoComplete="off"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                setInviteError(null);
              }}
              aria-invalid={inviteError !== null}
              aria-describedby={inviteError ? 'invite-error' : undefined}
              className={inputClasses}
            />
            <button
              type="submit"
              disabled={inviteMutation.isPending || !email.trim()}
              className="mt-1 flex-shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
            >
              {inviteMutation.isPending ? t.members.inviting : t.members.invite}
            </button>
          </div>
          {inviteError && (
            <p id="invite-error" className="mt-2 text-sm text-red-700" role="alert">
              {inviteError}
            </p>
          )}
        </form>
      )}

      <section className="rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="members-heading">
        <h2 id="members-heading" className="mb-3 text-lg font-semibold text-gray-900">
          {t.members.listSection}
        </h2>
        {!isOwner && <p className="mb-3 text-sm text-gray-600">{t.members.ownerManages}</p>}
        {membersQuery.isLoading && <LoadingSpinner size="sm" />}
        {membersQuery.isError && (
          <ErrorMessage message={getApiErrorMessage(membersQuery.error)} retry={() => membersQuery.refetch()} />
        )}
        {membersQuery.data && (
          <ul className="divide-y divide-gray-100">
            {membersQuery.data.map((member) => {
              const isMe = member.userId === user?.id;
              const canRemove = member.role !== 'OWNER' && (isOwner || isMe);
              return (
                <li key={member.userId} className="flex items-center justify-between gap-3 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-gray-900">
                      {member.name}
                      {isMe && <span className="ml-1 font-normal text-gray-500">({t.members.you})</span>}
                    </p>
                    <p className="truncate text-sm text-gray-600">{member.email}</p>
                  </div>
                  <div className="flex flex-shrink-0 items-center gap-2">
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${
                        member.role === 'OWNER' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {member.role === 'OWNER' ? t.members.roleOwner : t.members.roleMember}
                    </span>
                    {canRemove && (
                      <button
                        type="button"
                        onClick={() => setRemoving(member)}
                        aria-label={isMe ? t.members.leaveFull : `${t.members.remove}: ${member.name}`}
                        className="rounded-md border border-red-300 bg-white px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500"
                      >
                        {isMe ? t.members.leave : t.members.remove}
                      </button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <ConfirmationModal
        isOpen={removing !== null}
        onClose={() => setRemoving(null)}
        onConfirm={() => removing && removeMutation.mutate(removing)}
        title={leaving ? t.members.leaveConfirmTitle : t.members.removeConfirmTitle(removing?.name ?? '')}
        description={leaving ? t.members.leaveConfirmText(library.name) : t.members.removeConfirmText}
        confirmText={leaving ? t.members.leaveFull : t.members.remove}
        cancelText={t.common.cancel}
        variant="danger"
        isLoading={removeMutation.isPending}
      />
    </div>
  );
}
