import { test, expect, type Page } from '@playwright/test';

/**
 * ISBN path with a typed number (the camera scanner is tested by hand on iOS and Android).
 * Self-contained: the API is mocked, because external catalogs (Open Library, BN, Google) are not reliable in tests.
 */
const user = { id: 'u1', email: 'e2e@example.com', name: 'E2E', role: 'USER', avatarUrl: null, isActive: true, createdAt: '2026-01-01T00:00:00' };
const book = {
  id: 'b1',
  title: 'Nawiedzony dom: opowiadania zebrane',
  authors: ['Virginia Woolf'],
  isbn: '9788308049402',
  publishedYear: 2012,
  coverUrl: null,
  source: 'NATIONAL_LIBRARY',
  createdByUserId: 'u1',
  createdAt: '2026-01-01T00:00:00',
};

async function mockApi(page: Page) {
  await page.route('**/api/**', (route) => {
    const url = new URL(route.request().url());
    const json = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
    switch (url.pathname) {
      case '/api/users/me':
        return json(user);
      case '/api/books/lookup':
        return url.searchParams.get('isbn') === book.isbn
          ? json({ isbn: book.isbn, book: null, metadata: { ...book, id: undefined } })
          : json({ message: 'not found' }, 404);
      case '/api/books/import':
        return json(book);
      case `/api/books/${book.id}`:
        return json(book);
      case '/api/libraries':
        return json([{ id: 'l1', name: 'Biblioteczka', ownerId: 'u1', role: 'OWNER' }]);
      default:
        return json({});
    }
  });
  await page.goto('/login');
  await page.evaluate(() => localStorage.setItem('accessToken', 'e2e'));
}

test('typed ISBN → preview → add to shelf with the book selected', async ({ page }) => {
  await mockApi(page);
  await page.goto('/isbn');

  await page.getByLabel('Numer ISBN').fill('83-08-04940-');
  await page.getByRole('button', { name: 'Szukaj' }).click();
  await expect(page.getByText('Nieprawidłowy ISBN')).toBeVisible();

  await page.getByLabel('Numer ISBN').fill('978-83-08-04940-2');
  await page.getByRole('button', { name: 'Szukaj' }).click();
  await expect(page.getByText('Znaleziono w: Biblioteka Narodowa')).toBeVisible();
  await expect(page.getByRole('heading', { name: book.title })).toBeVisible();

  await page.getByRole('button', { name: 'Dodaj na półkę' }).click();
  await expect(page).toHaveURL(/\/shelf\/new\?bookId=b1$/);
  await expect(page.getByText('z katalogu:')).toBeVisible();
});

test('unknown ISBN offers manual entry', async ({ page }) => {
  await mockApi(page);
  await page.goto('/isbn?isbn=9780306406157');

  await expect(page.getByText('Nie znaleziono książki o tym numerze ISBN.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Dodaj ręcznie na półkę' })).toBeVisible();
});
