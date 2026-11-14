import type { UserRole } from './auth';

export interface UserSummary {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
}

/** The account an admin is previewing (see utils/viewAs.ts). */
export type ViewedUser = Pick<UserSummary, 'id' | 'name' | 'email'>;
