import type { Book, BookReference } from './book';

export type LibraryRole = 'OWNER' | 'MEMBER';

export interface Library {
  id: string;
  name: string;
  ownerId: string;
  createdAt: string;
  /** Role of the current user in this library */
  myRole: LibraryRole;
  memberCount: number;
}

export interface LibraryMember {
  userId: string;
  name: string;
  email: string;
  role: LibraryRole;
}

export type ShelfItemStatus = 'ON_SHELF' | 'LENT' | 'LOST' | 'GIVEN_AWAY';

export interface Loan {
  id: string;
  borrowerFirstName: string;
  borrowerLastName: string;
  lentOn: string;
  dueOn: string | null;
  returnedOn: string | null;
  notes: string | null;
  active: boolean;
  overdue: boolean;
}

export interface ShelfItem {
  id: string;
  libraryId: string;
  book: Book;
  status: ShelfItemStatus;
  acquiredOn: string | null;
  notes: string | null;
  activeLoan: Loan | null;
  loans: Loan[];
  createdAt: string;
  updatedAt: string;
}

export interface ShelfItemListParams {
  status?: ShelfItemStatus;
  search?: string;
  bookId?: string;
  page?: number;
  pageSize?: number;
}

export interface AddShelfItemRequest {
  book: BookReference;
  acquiredOn?: string | null;
  notes?: string | null;
}

export interface UpdateShelfItemRequest {
  acquiredOn?: string | null;
  notes?: string | null;
  status?: ShelfItemStatus | null;
}

export interface LendShelfItemRequest {
  borrowerFirstName: string;
  borrowerLastName: string;
  lentOn: string;
  dueOn?: string | null;
  notes?: string | null;
}

export interface ReturnLoanRequest {
  returnedOn?: string | null;
}
