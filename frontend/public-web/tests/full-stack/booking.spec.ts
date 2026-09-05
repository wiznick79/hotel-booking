import { test, expect } from '@playwright/test';
import { createPrivateKey, sign } from 'node:crypto';

test('browser booking is persisted and delivered through Kafka to the test mailbox', async ({ page, request }) => {
  const api = process.env.E2E_API_URL!;
  const key = createPrivateKey({ key: Buffer.from(process.env.E2E_PRIVATE_KEY!, 'base64'), type: 'pkcs8', format: 'der' });
  function token(hotelIds: string[] = []) {
    const encode = (value: unknown) => Buffer.from(JSON.stringify(value)).toString('base64url');
    const payload = `${encode({ alg: 'RS256' })}.${encode({ sub: 'e2e-staff', exp: Math.floor(Date.now() / 1000) + 600,
      roles: ['ADMIN'], permissions: ['HOTEL_MANAGE', 'ROOM_TYPE_MANAGE', 'ROOM_MANAGE', 'RESERVATION_READ'], hotelIds })}`;
    return `${payload}.${sign('RSA-SHA256', Buffer.from(payload), key).toString('base64url')}`;
  }
  async function post(path: string, data: unknown, hotelIds: string[] = []) {
    const response = await request.post(`${api}/api${path}`, { data, headers: { Authorization: `Bearer ${token(hotelIds)}` } });
    expect(response.ok(), `${path}: ${response.status()}`).toBeTruthy();
    return response.json();
  }
  const hotel = await post('/hotels', { name: 'Disposable Browser Hotel', address: 'Test street', city: 'Lisbon', country: 'Portugal', defaultLanguage: 'en' });
  const roomType = await post('/room-types', { hotelId: hotel.id, maximumOccupancy: 2, basePrice: 80,
    translations: { en: { name: 'Test double', description: 'Test room' } } }, [hotel.id]);
  await post('/rooms', { hotelId: hotel.id, roomTypeId: roomType.id, roomNumber: '101', floor: 1 }, [hotel.id]);
  await page.goto('/#/book');
  await page.getByLabel('Available room type').selectOption(roomType.id);
  await page.getByLabel('Full name').fill('Disposable Guest');
  await page.getByLabel('Phone', { exact: true }).fill('+351911111111');
  await page.getByLabel('Email').fill('disposable@example.test');
  await page.getByRole('checkbox').check();
  const created = page.waitForResponse(response => response.url().endsWith('/api/reservations') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Request booking', exact: true }).click();
  const response = await created;
  expect(response.ok()).toBeTruthy();
  const reservation = await response.json();
  await expect(page.getByRole('heading', { name: 'Thank you for your booking request.' })).toBeVisible();
  const saved = await request.get(`${api}/api/reservations/${reservation.id}`, { headers: { Authorization: `Bearer ${token([hotel.id])}` } });
  expect(saved.ok()).toBeTruthy();
  expect(await saved.json()).toMatchObject({ guestName: 'Disposable Guest', hotelId: hotel.id });
  await expect.poll(async () => {
    const mailbox = await request.get(`${process.env.E2E_MAIL_URL}/api/v1/messages`);
    return (await mailbox.json()).total;
  }, { timeout: 30000 }).toBeGreaterThan(0);
});
