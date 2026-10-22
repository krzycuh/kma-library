import { test, expect, type Page } from '@playwright/test';

/**
 * Critical path: register → add a read book → add a copy to the shelf → lend it → return it.
 */
const unique = Date.now();
const user = {
  name: `E2E Tester ${unique}`,
  email: `e2e.${unique}@example.com`,
  password: 'Password123!',
};

async function register(page: Page) {
  await page.goto('/register');
  await page.getByLabel('Imię i nazwisko').fill(user.name);
  await page.getByLabel('Adres e-mail').fill(user.email);
  await page.getByLabel('Hasło', { exact: true }).fill(user.password);
  await page.getByLabel('Powtórz hasło').fill(user.password);
  await page.getByRole('button', { name: 'Zarejestruj' }).click();
  await expect(page).toHaveURL(/\/$/);
}

async function login(page: Page) {
  await page.goto('/login');
  await page.getByLabel('Adres e-mail').fill(user.email);
  await page.getByLabel('Hasło', { exact: true }).fill(user.password);
  await page.getByRole('button', { name: 'Zaloguj' }).click();
  await expect(page).toHaveURL(/\/$/);
}

test.describe.serial('library flow', () => {
  test('register and see empty dashboard', async ({ page }) => {
    await register(page);
    await expect(page.getByText(/Cześć, E2E/)).toBeVisible();
  });

  test('add a read book', async ({ page }) => {
    await login(page);

    await page.goto('/read/new');
    await page.getByLabel('Tytuł').fill(`Solaris ${unique}`);
    await page.getByLabel(/Autor/).fill('Stanisław Lem');
    await page.getByRole('radio', { name: '4 z 5 gwiazdek' }).click();
    await page.getByRole('button', { name: 'Zapisz' }).click();
    await expect(page).toHaveURL(/\/read$/);
    await expect(page.getByText(`Solaris ${unique}`)).toBeVisible();
  });

  test('add a copy, lend and return it', async ({ page }) => {
    await login(page);

    await page.goto('/shelf/new');
    await page.getByLabel('Tytuł').fill(`Lalka ${unique}`);
    await page.getByLabel(/Autor/).fill('Bolesław Prus');
    await page.getByRole('button', { name: 'Dodaj na półkę' }).click();
    await expect(page).toHaveURL(/\/shelf\/[0-9a-f-]+$/);

    await page.getByRole('button', { name: 'Pożycz' }).first().click();
    await page.getByLabel('Imię').fill('Jan');
    await page.getByLabel('Nazwisko').fill('Kowalski');
    await page.getByRole('dialog').getByRole('button', { name: 'Pożycz' }).click();
    await expect(page.getByText(/Jan Kowalski/).first()).toBeVisible();

    await page.goto('/loans');
    await expect(page.getByText(`Lalka ${unique}`).first()).toBeVisible();

    await page.goto('/shelf');
    await page.getByRole('link', { name: `Lalka ${unique}` }).first().click();
    await page.getByRole('button', { name: 'Zwrócono' }).first().click();
    await page.getByRole('dialog').getByRole('button', { name: 'Zwrócono' }).click();
    await expect(page.getByText('Na półce').first()).toBeVisible();
  });
});
