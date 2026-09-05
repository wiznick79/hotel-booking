import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/admin', fullyParallel: true, forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0, workers: 2,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report/admin', open: 'never' }]],
  use: { baseURL: 'http://127.0.0.1:14328', trace: 'retain-on-failure', screenshot: 'only-on-failure' },
  projects: [{ name: 'admin-chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm --prefix ../admin-web run dev -- --host 127.0.0.1 --port 14328 --strictPort',
    url: 'http://127.0.0.1:14328', reuseExistingServer: false,
  },
});
