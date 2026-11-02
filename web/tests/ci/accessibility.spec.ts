import { test, expect } from '@playwright/test';
import { createRequire } from 'node:module';
import { readFileSync } from 'node:fs';
const require = createRequire(import.meta.url);

async function audit(page: import('@playwright/test').Page) {
  // DevTools evaluation leaves the application's strict script CSP intact.
  await page.evaluate(
    readFileSync(require.resolve('axe-core/axe.min.js'), 'utf-8'),
  );
  const violations = await page.evaluate(async () => {
    const axe = (window as any).axe;
    return (
      await axe.run(document, {
        runOnly: {
          type: 'tag',
          values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'],
        },
      })
    ).violations;
  });
  expect(violations, JSON.stringify(violations, null, 2)).toEqual([]);
}

test.beforeAll(async ({ request }) => {
  const response = await request.post('/ingest', {
    data: {
      upserts: [
        {
          id: 11000,
          text: 'accessibilityfixture',
          metadata: { title: 'Accessible result' },
        },
      ],
      deletes: [],
    },
  });
  expect(response.ok()).toBeTruthy();
  expect((await response.json()).status).toBe('COMPLETE');
});

for (const width of [1440, 390]) {
  test(`WCAG automated checks: search, results, studio and validation at ${width}px`, async ({
    page,
  }) => {
    test.setTimeout(60000);
    await page.setViewportSize({ width, height: 1000 });
    await page.goto('/');
    await expect(
      page.getByLabel('Search query', { exact: true }),
    ).toBeVisible();
    await audit(page);
    await page
      .getByLabel('Search query', { exact: true })
      .fill('accessibilityfixture');
    await page.getByRole('button', { name: 'Run search', exact: true }).click();
    await expect(page.locator('.result-card').first()).toBeVisible();
    await audit(page);
    await page
      .getByRole('button', { name: 'Document studio', exact: true })
      .click();
    await audit(page);
    await page
      .getByRole('button', { name: 'Index document', exact: true })
      .click();
    await audit(page);
  });
}

test('keyboard navigation and reduced motion', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/');
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', { name: /skip/i })).toBeFocused();
  await page.keyboard.press('Enter');
  await page.getByLabel('Search query', { exact: true }).focus();
  await page.keyboard.type('accessibilityfixture');
  await page.keyboard.press('Enter');
  await expect(page.locator('.result-card').first()).toBeVisible();
  expect(
    await page
      .locator('.pulse-dot')
      .evaluate((element) => getComputedStyle(element).animationName),
  ).toBe('none');
  expect(
    await page.evaluate(
      () => matchMedia('(prefers-reduced-motion: reduce)').matches,
    ),
  ).toBe(true);
});

test('search survives WebGL context loss or unavailable graphics', async ({
  page,
}) => {
  await page.goto('/');
  await expect(page.locator('.scene-canvas')).toBeVisible();
  await expect(
    page.locator('.scene-canvas canvas, .scene-fallback'),
  ).toHaveCount(1);
  if (await page.locator('.scene-canvas canvas').count()) {
    await page.locator('.scene-canvas canvas').evaluate((canvas) => {
      canvas.dispatchEvent(new Event('webglcontextlost', { cancelable: true }));
    });
  }
  await expect(
    page.getByText('3D rendering unavailable', { exact: true }),
  ).toBeVisible();
  await page
    .getByLabel('Search query', { exact: true })
    .fill('accessibilityfixture');
  await page.getByRole('button', { name: 'Run search', exact: true }).click();
  await expect(page.locator('.result-card').first()).toBeVisible();
});
