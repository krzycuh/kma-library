import type { UserRole } from './auth';

export interface UserSummary {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
}
