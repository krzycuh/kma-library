import { defineConfig, devices } from '@playwright/test';

/**
 * E2E tests run against a full stack: backend on :8080 (dev profile + PostgreSQL) and Vite dev server on :5173.
 * Start the backend yourself (./gradlew bootRun) or point BASE_URL at a deployed instance.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'retain-on-failure',
    locale: 'pl-PL',
  },
  projects: [
    { name: 'mobile-chromium', use: { ...devices['Pixel 5'] } },
  ],
  webServer: process.env.BASE_URL
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://localhost:5173',
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
