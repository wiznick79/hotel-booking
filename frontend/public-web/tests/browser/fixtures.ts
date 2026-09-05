import { test as base, expect } from '@playwright/test';

export const hotel = {
  id: 'hotel-test', name: 'Browser Test Hotel', description: 'A quiet place to stay.',
  address: 'Test street', city: 'Miranda do Douro', country: 'Portugal',
  notificationReplyToAddress: null,
};
export const room = {
  id: 'room-test', hotelId: hotel.id, name: 'Double room', description: 'A comfortable double.',
  maximumOccupancy: 2, basePrice: 80, active: true,
};
export const reservation = {
  id: 'booking-test', guestName: 'Browser Guest', guestCount: 1,
  checkInDate: '2030-06-10', checkOutDate: '2030-06-12', notes: null,
  status: 'CONFIRMED', items: [{ roomTypeId: room.id }], totalPrice: 160, currency: 'EUR',
  paymentMode: 'PAY_AT_RECEPTION', paymentMethod: 'PAY_AT_RECEPTION',
  manualConfirmationRequired: false, paymentAttempt: null, paymentStatus: null,
  paymentInstructions: null,
};

// All requests are intercepted before navigating. Unknown API calls fail the test,
// rather than accidentally reaching the developer's running application.
export const test = base.extend<{ isolatedApi: void }>({
  isolatedApi: [async ({ context }, use) => {
    const unexpected: string[] = [];
    await context.route('**/*', async route => {
      const url = new URL(route.request().url());
      if (url.origin !== 'http://127.0.0.1:14327') {
        await route.abort();
        return;
      }
      if (!url.pathname.startsWith('/api/')) {
        await route.continue();
        return;
      }
      const responses: Record<string, unknown> = {
        '/api/hotels': [hotel], '/api/room-types': [room], '/api/media': [],
        '/api/reservations/availability': [{ roomTypeId: room.id, name: room.name,
          maximumOccupancy: 2, totalPrice: 160, currency: 'EUR' }],
        '/api/payment-methods': [{ paymentMethod: 'PAY_AT_RECEPTION', paymentMode: 'PAY_AT_RECEPTION' }],
      };
      if (url.pathname === '/api/auth/refresh') {
        await route.fulfill({ status: 401, json: {} });
      } else if (route.request().method() === 'GET' && url.pathname in responses) {
        await route.fulfill({ json: responses[url.pathname] });
      } else {
        unexpected.push(`${route.request().method()} ${url.pathname}`);
        await route.fulfill({ status: 501, json: { message: 'Unmocked test endpoint' } });
      }
    });
    await use();
    expect(unexpected, 'Every API request must have an explicit fixture').toEqual([]);
  }, { auto: true }],
});
export { expect };
