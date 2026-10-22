import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { usersApi } from '../api/users';
import { PageHeader } from '../components/PageHeader';
import { t } from '../i18n';
import { getApiErrorMessage } from '../utils/apiError';

/**
 * Profile page component
 */

const profileSchema = z.object({
  name: z.string().min(1, t.profile.nameRequired).max(255, t.profile.nameTooLong),
  avatarUrl: z.string().url(t.profile.invalidUrl).optional().or(z.literal('')),
});

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, t.profile.currentPasswordRequired),
    newPassword: z.string().min(8, t.auth.passwordMin),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: t.auth.passwordsMismatch,
    path: ['confirmPassword'],
  });

type ProfileFormData = z.infer<typeof profileSchema>;
type PasswordFormData = z.infer<typeof passwordSchema>;

const inputClasses =
  'mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm';
const buttonClasses =
  'w-full sm:w-auto px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed';

export const Profile: React.FC = () => {
  const { user, refreshUser } = useAuth();
  const [successMessage, setSuccessMessage] = useState<string>('');
  const [errorMessage, setErrorMessage] = useState<string>('');
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  const {
    register: registerProfile,
    handleSubmit: handleProfileSubmit,
    formState: { errors: profileErrors },
  } = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      name: user?.name || '',
      avatarUrl: user?.avatarUrl || '',
    },
  });

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    formState: { errors: passwordErrors },
    reset: resetPasswordForm,
  } = useForm<PasswordFormData>({
    resolver: zodResolver(passwordSchema),
  });

  const onProfileSubmit = async (data: ProfileFormData) => {
    setIsUpdatingProfile(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      await usersApi.updateProfile({
        name: data.name,
        avatarUrl: data.avatarUrl || undefined,
      });
      await refreshUser();
      setSuccessMessage(t.profile.profileSaved);
    } catch (err) {
      setErrorMessage(getApiErrorMessage(err));
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  const onPasswordSubmit = async (data: PasswordFormData) => {
    setIsChangingPassword(true);
    setSuccessMessage('');
    setErrorMessage('');

    try {
      await usersApi.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      setSuccessMessage(t.profile.passwordChanged);
      resetPasswordForm();
    } catch (err) {
      setErrorMessage(getApiErrorMessage(err));
    } finally {
      setIsChangingPassword(false);
    }
  };

  if (!user) return null;

  return (
    <div className="max-w-3xl mx-auto">
      <PageHeader title={t.profile.title} description={t.profile.subtitle} />
      <div className="bg-white shadow rounded-lg">
        <div className="px-4 py-5 sm:p-6">
          {successMessage && (
            <div className="mb-4 rounded-md bg-green-50 p-4">
              <p className="text-sm font-medium text-green-800">{successMessage}</p>
            </div>
          )}

          {errorMessage && (
            <div className="mb-4 rounded-md bg-red-50 p-4">
              <p className="text-sm font-medium text-red-800">{errorMessage}</p>
            </div>
          )}

          <div className="mb-8">
            <h2 className="text-lg font-medium text-gray-900 mb-4">{t.profile.profileSection}</h2>
            <form onSubmit={handleProfileSubmit(onProfileSubmit)} className="space-y-4">
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  {t.profile.email}
                </label>
                <input
                  id="email"
                  type="email"
                  value={user.email}
                  disabled
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm bg-gray-50 text-gray-500 sm:text-sm"
                />
                <p className="mt-1 text-xs text-gray-500">{t.profile.emailReadOnly}</p>
              </div>

              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                  {t.profile.name}
                </label>
                <input {...registerProfile('name')} id="name" type="text" className={inputClasses} />
                {profileErrors.name && <p className="mt-1 text-sm text-red-600">{profileErrors.name.message}</p>}
              </div>

              <div>
                <label htmlFor="avatarUrl" className="block text-sm font-medium text-gray-700">
                  {t.profile.avatarUrl}
                </label>
                <input
                  {...registerProfile('avatarUrl')}
                  id="avatarUrl"
                  type="url"
                  className={inputClasses}
                  placeholder="https://example.com/avatar.jpg"
                />
                {profileErrors.avatarUrl && (
                  <p className="mt-1 text-sm text-red-600">{profileErrors.avatarUrl.message}</p>
                )}
              </div>

              <div>
                <span className="block text-sm font-medium text-gray-700 mb-2">{t.profile.role}</span>
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
                  {user.role === 'ADMIN' ? t.profile.roleAdmin : t.profile.roleUser}
                </span>
              </div>

              <div className="pt-4">
                <button type="submit" disabled={isUpdatingProfile} className={buttonClasses}>
                  {isUpdatingProfile ? t.common.saving : t.profile.saveProfile}
                </button>
              </div>
            </form>
          </div>

          <div className="border-t border-gray-200 pt-8">
            <h2 className="text-lg font-medium text-gray-900 mb-4">{t.profile.passwordSection}</h2>
            <form onSubmit={handlePasswordSubmit(onPasswordSubmit)} className="space-y-4">
              <div>
                <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700">
                  {t.profile.currentPassword}
                </label>
                <input
                  {...registerPassword('currentPassword')}
                  id="currentPassword"
                  type="password"
                  autoComplete="current-password"
                  className={inputClasses}
                />
                {passwordErrors.currentPassword && (
                  <p className="mt-1 text-sm text-red-600">{passwordErrors.currentPassword.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700">
                  {t.profile.newPassword}
                </label>
                <input
                  {...registerPassword('newPassword')}
                  id="newPassword"
                  type="password"
                  autoComplete="new-password"
                  className={inputClasses}
                />
                {passwordErrors.newPassword && (
                  <p className="mt-1 text-sm text-red-600">{passwordErrors.newPassword.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                  {t.profile.confirmPassword}
                </label>
                <input
                  {...registerPassword('confirmPassword')}
                  id="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  className={inputClasses}
                />
                {passwordErrors.confirmPassword && (
                  <p className="mt-1 text-sm text-red-600">{passwordErrors.confirmPassword.message}</p>
                )}
              </div>

              <div className="pt-4">
                <button type="submit" disabled={isChangingPassword} className={buttonClasses}>
                  {isChangingPassword ? t.common.saving : t.profile.changePassword}
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};
