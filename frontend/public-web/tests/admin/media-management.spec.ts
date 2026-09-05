import { test as base, expect } from '@playwright/test';

const hotel = {
  id: 'hotel-test', name: 'Test Hotel', description: 'Test description', address: 'Test street',
  city: 'Lisbon', country: 'Portugal', defaultLanguage: 'en', active: true,
  notificationDisplayName: null, notificationFromAddress: null, notificationReplyToAddress: null,
};

const test = base.extend<{ session: void }>({
  session: [async ({ context }, use) => {
    await context.addInitScript(() => sessionStorage.setItem('hotel-booking.admin.session', JSON.stringify({
      accessToken: 'browser-fixture', expiresAt: Date.now() + 3600000,
      claims: { sub: 'Manager', roles: ['MANAGER'], permissions: ['HOTEL_MANAGE', 'ROOM_TYPE_MANAGE'], hotelIds: ['hotel-test'] },
    })));
    await use();
  }, { auto: true }],
});

test('manager uploads and previews a homepage hero', async ({ page }) => {
  let uploaded = false;
  let multipartRequest = false;
  const media = {
    id: 'media-test', hotelId: hotel.id, roomTypeId: null, usage: 'HERO', sortOrder: 0,
    altText: 'Hotel at sunset', originalFilename: 'hero.png', contentType: 'image/png',
    sizeBytes: 68, url: '/api/media/media-test/content',
  };
  await page.route('**/*', async route => {
    const url = new URL(route.request().url());
    if (!url.pathname.startsWith('/api/')) return route.continue();
    if (url.pathname === '/api/hotels') return route.fulfill({ json: [hotel] });
    if (url.pathname === '/api/booking-policies/hotel-test') return route.fulfill({ status: 404, json: {} });
    if (url.pathname === '/api/media' && route.request().method() === 'GET')
      return route.fulfill({ json: uploaded ? [media] : [] });
    if (url.pathname === '/api/media' && route.request().method() === 'POST') {
      multipartRequest = route.request().headers()['content-type']?.startsWith('multipart/form-data') ?? false;
      uploaded = true;
      return route.fulfill({ status: 201, json: media });
    }
    if (url.pathname === media.url) return route.fulfill({ body: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=', 'base64'), contentType: 'image/png' });
    return route.fulfill({ status: 501, json: {} });
  });

  await page.goto('/#/hotel-settings');
  const hero = page.locator('.media-manager').filter({ hasText: 'Homepage hero' });
  await hero.getByLabel('Alternative text').fill('Hotel at sunset');
  await hero.getByLabel('Photo').setInputFiles({ name: 'hero.png', mimeType: 'image/png', buffer: Buffer.from('image') });
  await hero.getByRole('button', { name: 'Upload photo' }).click();

  await expect(hero.getByRole('img', { name: 'Hotel at sunset' })).toBeVisible();
  expect(multipartRequest).toBe(true);
});
