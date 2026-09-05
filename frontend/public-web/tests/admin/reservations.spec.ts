import { test as base, expect } from '@playwright/test';

const booking = (id: string, guestName: string, checkInDate: string, checkOutDate: string) => ({
  id, hotelId: 'hotel-test', guestName, guestPhone: '+351911111111', guestEmail: 'guest@example.test',
  guestCount: 1, checkInDate, checkOutDate, notes: null, status: 'CONFIRMED', items: [],
  totalPrice: 80, currency: 'EUR', paymentMode: 'PAY_NOW', paymentMethod: 'CARD',
  paymentStatus: 'SUCCEEDED', paymentReviewRequired: false, manualConfirmationRequired: false,
  holdUntil: null, discountCode: null, discountAmount: null,
});

const test = base.extend<{ isolated: void }>({
  isolated: [async ({ context }, use) => {
    await context.addInitScript(() => sessionStorage.setItem('hotel-booking.admin.session', JSON.stringify({
      accessToken: 'browser-fixture', expiresAt: Date.now() + 3600000,
      claims: { sub: 'Test staff', roles: ['ADMIN'], permissions: ['RESERVATION_READ'], hotelIds: ['hotel-test'] },
    })));
    const unexpected: string[] = [];
    await context.route('**/*', async route => {
      const url = new URL(route.request().url());
      if (url.origin !== 'http://127.0.0.1:14328') return route.abort();
      if (!url.pathname.startsWith('/api/')) return route.continue();
      if (url.pathname === '/api/hotels') return route.fulfill({ json: [{ id: 'hotel-test', name: 'Test Hotel' }] });
      if (['/api/rooms', '/api/room-types'].includes(url.pathname)) return route.fulfill({ json: [] });
      unexpected.push(url.pathname);
      return route.fulfill({ status: 501, json: {} });
    });
    await use();
    expect(unexpected).toEqual([]);
  }, { auto: true }],
});

test('no-show keeps date ordering even when backend response order changes', async ({ page }) => {
  const first = booking('first', 'Earlier Guest', '2020-01-01', '2020-01-03');
  const second = booking('second', 'Later Guest', '2020-01-05', '2020-01-07');
  let changed = false;
  await page.route('**/api/reservations?*', route => route.fulfill({ json: changed ? [second, first] : [first, second] }));
  await page.route('**/api/reservations/first/no-show', async route => {
    expect(route.request().method()).toBe('PATCH');
    first.status = 'NO_SHOW'; changed = true;
    await route.fulfill({ json: first });
  });
  await page.goto('/#/reservations?from=2020-01-01&to=2020-02-01');
  await page.getByRole('row').filter({ hasText: 'Earlier Guest' }).getByRole('button', { name: 'No-show', exact: true }).click();
  await expect(page.locator('tbody tr').first()).toContainText('Earlier Guest');
  await expect(page.locator('tbody tr').first()).toContainText('No-show');
  await expect(page.locator('tbody tr').first().getByRole('button', { name: 'Cancel', exact: true })).toHaveCount(0);
});

test('instant date filters survive refresh without extra history entries', async ({ page }) => {
  await page.route('**/api/reservations?*', route => route.fulfill({ json: [] }));
  await page.goto('/#/reservations');
  const historyLength = await page.evaluate(() => history.length);
  await page.getByLabel('From', { exact: true }).fill('2030-01-01');
  await page.getByLabel('To', { exact: true }).fill('2030-02-01');
  await expect(page).toHaveURL(/from=2030-01-01&to=2030-02-01/);
  expect(await page.evaluate(() => history.length)).toBe(historyLength);
  await page.reload();
  await expect(page.getByLabel('From', { exact: true })).toHaveValue('2030-01-01');
  await expect(page.getByLabel('To', { exact: true })).toHaveValue('2030-02-01');
});

test('cancelled paid booking displays staff payment follow-up', async ({ page }) => {
  const paid = { ...booking('paid', 'Paid Guest', '2020-01-01', '2020-01-03'), status: 'CANCELLED', paymentReviewRequired: true };
  await page.route('**/api/reservations?*', route => route.fulfill({ json: [paid] }));
  await page.goto('/#/reservations');
  await page.getByRole('button', { name: 'View', exact: true }).click();
  await expect(page.getByText('Paid · Card · Review required (cancelled booking)', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cancel', exact: true })).toHaveCount(0);
});

test('sorting by guests reorders the table in both directions', async ({ page }) => {
  const single = booking('one', 'Single Guest', '2020-01-01', '2020-01-03');
  const group = { ...booking('group', 'Group Guest', '2020-01-01', '2020-01-03'), guestCount: 3 };
  await page.route('**/api/reservations?*', route => route.fulfill({ json: [group, single] }));
  await page.goto('/#/reservations');
  await page.getByRole('button', { name: 'Sort by Guests', exact: true }).click();
  await expect(page.locator('tbody tr').first()).toContainText('Single Guest');
  await page.getByRole('button', { name: 'Sort by Guests', exact: true }).click();
  await expect(page.locator('tbody tr').first()).toContainText('Group Guest');
});
