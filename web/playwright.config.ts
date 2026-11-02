import { devices, defineConfig } from '@playwright/test';
if (!process.env.CI && !process.env.SFX_BROWSER_URL)
  throw new Error(
    'Set SFX_BROWSER_URL to an isolated disposable cluster; these tests write fixtures.',
  );
const baseURL = process.env.SFX_BROWSER_URL ?? 'http://127.0.0.1:8090';
if (!['127.0.0.1', 'localhost'].includes(new URL(baseURL).hostname))
  throw new Error('Browser fixture writes require a loopback test cluster.');
export default defineConfig({
  testDir: './tests/ci',
  workers: 1,
  retries: 0,
  timeout: 30000,
  globalTimeout: 600000,
  reporter: [['list'], ['html', { open: 'never' }]],
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
  use: { baseURL, trace: 'retain-on-failure', screenshot: 'only-on-failure' },
});
