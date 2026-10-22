import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '../hooks/useAuth';
import { t } from '../i18n';
import { getApiErrorMessage } from '../utils/apiError';

/**
 * Registration page component
 */

const registerSchema = z
  .object({
    name: z.string().min(1, t.auth.nameRequired).max(255, t.auth.nameTooLong),
    email: z.string().email(t.auth.invalidEmail),
    password: z.string().min(8, t.auth.passwordMin),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: t.auth.passwordsMismatch,
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

type PasswordStrength = '' | 'weak' | 'medium' | 'strong';

export const Register: React.FC = () => {
  const navigate = useNavigate();
  const { register: registerUser } = useAuth();
  const [error, setError] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
    watch,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const password = watch('password', '');

  const getPasswordStrength = (pass: string): PasswordStrength => {
    if (pass.length === 0) return '';
    if (pass.length < 8) return 'weak';
    if (pass.length < 12) return 'medium';
    return 'strong';
  };

  const passwordStrength = getPasswordStrength(password);
  const strengthLabel: Record<Exclude<PasswordStrength, ''>, string> = {
    weak: t.auth.strengthWeak,
    medium: t.auth.strengthMedium,
    strong: t.auth.strengthStrong,
  };

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError('');

    try {
      await registerUser({
        email: data.email,
        password: data.password,
        name: data.name,
      });
      navigate('/');
    } catch (err) {
      setError(getApiErrorMessage(err, t.auth.registerFailed));
    } finally {
      setIsLoading(false);
    }
  };

  const inputClasses =
    'mt-1 appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm';

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            {t.auth.registerTitle}
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            {t.auth.registerSubtitle}{' '}
            <Link to="/login" className="font-medium text-blue-600 hover:text-blue-500">
              {t.auth.loginLink}
            </Link>
          </p>
        </div>

        <form className="mt-8 space-y-6" onSubmit={handleSubmit(onSubmit)}>
          {error && (
            <div className="rounded-md bg-red-50 p-4">
              <div className="flex">
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-red-800">{error}</h3>
                </div>
              </div>
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                {t.auth.name}
              </label>
              <input
                {...register('name')}
                id="name"
                type="text"
                autoComplete="name"
                className={inputClasses}
                placeholder={t.auth.namePlaceholder}
              />
              {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>}
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                {t.auth.email}
              </label>
              <input
                {...register('email')}
                id="email"
                type="email"
                autoComplete="email"
                className={inputClasses}
                placeholder="twoj@email.pl"
              />
              {errors.email && <p className="mt-1 text-sm text-red-600">{errors.email.message}</p>}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                {t.auth.password}
              </label>
              <input
                {...register('password')}
                id="password"
                type="password"
                autoComplete="new-password"
                className={inputClasses}
                placeholder={t.auth.passwordPlaceholder}
              />
              {passwordStrength && (
                <div className="mt-2">
                  <div className="flex items-center space-x-2">
                    <div className="flex-1 h-2 bg-gray-200 rounded">
                      <div
                        className={`h-2 rounded ${
                          passwordStrength === 'weak'
                            ? 'w-1/3 bg-red-500'
                            : passwordStrength === 'medium'
                              ? 'w-2/3 bg-yellow-500'
                              : 'w-full bg-green-500'
                        }`}
                      />
                    </div>
                    <span className="text-xs text-gray-600">{strengthLabel[passwordStrength]}</span>
                  </div>
                </div>
              )}
              {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                {t.auth.confirmPassword}
              </label>
              <input
                {...register('confirmPassword')}
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                className={inputClasses}
                placeholder={t.auth.confirmPasswordPlaceholder}
              />
              {errors.confirmPassword && (
                <p className="mt-1 text-sm text-red-600">{errors.confirmPassword.message}</p>
              )}
            </div>
          </div>

          <div>
            <button
              type="submit"
              disabled={isLoading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? t.auth.signingUp : t.auth.signUp}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
