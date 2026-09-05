import { defineConfig } from '@playwright/test';

if (!process.env.E2E_BASE_URL || !process.env.E2E_API_URL || !process.env.E2E_PRIVATE_KEY) {
  throw new Error('Start this suite through tests/full-stack/run.mjs to create an isolated stack.');
}
export default defineConfig({
  testDir: './tests/full-stack', testMatch: '*.spec.ts', workers: 1, timeout: 60000,
  use: { baseURL: process.env.E2E_BASE_URL },
  // Real test access tokens must not be captured in traces or screenshots.
  reporter: 'list',
});
