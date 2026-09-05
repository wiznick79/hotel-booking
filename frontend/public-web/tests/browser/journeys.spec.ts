import { test, expect, reservation, room } from './fixtures';

test('guest booking sends the chosen room and consent, then shows confirmation', async ({ page }) => {
  let submitted: Record<string, unknown> | undefined;
  await page.route('**/api/reservations', async route => {
    submitted = route.request().postDataJSON();
    await route.fulfill({ status: 201, json: reservation });
  });
  await page.goto('/#/book');
  await page.getByLabel('Available room type').selectOption(room.id);
  await page.getByLabel('Full name').fill('Browser Guest');
  await page.getByLabel('Phone', { exact: true }).fill('+351911111111');
  await page.getByLabel('Email').fill('browser@example.test');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Request booking', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Thank you for your booking request.' })).toBeVisible();
  expect(submitted).toMatchObject({ guestName: 'Browser Guest', roomTypeIds: [room.id],
    privacyNoticeAccepted: true, paymentMethod: 'PAY_AT_RECEPTION' });
  await expect(page).toHaveURL(/#\/confirmation$/);
});

test('availability failure prevents booking', async ({ page }) => {
  await page.route('**/api/reservations/availability?*', route => route.fulfill({ status: 503, json: {} }));
  await page.goto('/#/book');
  await expect(page.getByText('Availability could not be loaded. Please try again.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Request booking', exact: true })).toBeDisabled();
});

test('registration rejects mismatched passwords before sending, then asks for verification', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/auth/register', async route => {
    requests++;
    expect(route.request().postDataJSON()).toMatchObject({ email: 'browser@example.test', fullName: 'Browser Guest' });
    await route.fulfill({ status: 202, json: {} });
  });
  await page.goto('/#/account');
  await page.getByRole('button', { name: 'Create an optional account' }).click();
  await page.getByLabel('Full name').fill('Browser Guest');
  await page.getByLabel('Email address').fill('browser@example.test');
  await page.getByLabel('Password', { exact: true }).fill('OnlyForTests123!');
  await page.getByLabel('Confirm password').fill('DifferentTests123!');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  await expect(page.getByText('The passwords do not match. Please enter them again.')).toBeVisible();
  expect(requests).toBe(0);
  await page.getByLabel('Confirm password').fill('OnlyForTests123!');
  await page.getByRole('button', { name: 'Create account', exact: true }).click();
  await expect(page.getByText('Check your email for the verification link before signing in.')).toBeVisible();
  expect(requests).toBe(1);
});

test('verification explicitly confirms success before account navigation', async ({ page }) => {
  await page.route('**/api/auth/verify-email', route => route.fulfill({ json: {
    accessToken: 'browser-test-token', tokenType: 'Bearer', expiresIn: 900,
  } }));
  await page.goto('/#/verify-account?token=fixture-token');
  await expect(page.getByRole('heading', { name: 'Your account is ready' })).toBeVisible();
  await expect(page.getByText('Your email address is verified and your account is ready to use.')).toBeVisible();
  await expect(page).toHaveURL(/verify-account/);
});

test('expired verification link gives an actionable error', async ({ page }) => {
  await page.route('**/api/auth/verify-email', route => route.fulfill({ status: 400, json: {} }));
  await page.goto('/#/verify-account?token=expired-fixture');
  await expect(page.getByText('This verification link is invalid or has expired. Please create the account again.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'View my reservations' })).toHaveCount(0);
});

test('contact form accepts a normal email and blocks a malformed phone', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/contact-messages', async route => {
    requests++;
    expect(route.request().postDataJSON()).toMatchObject({ email: 'browser@example.test', privacyNoticeAccepted: true });
    await route.fulfill({ status: 202, json: {} });
  });
  await page.goto('/#/contact');
  await page.getByLabel('Full name').fill('Browser Guest');
  await page.getByLabel('Email', { exact: true }).fill('browser@example.test');
  await page.getByLabel('Phone').fill('bad-number');
  await page.getByLabel('Message', { exact: true }).fill('Could you tell me about breakfast?');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Send message', exact: true }).click();
  expect(await page.getByLabel('Phone').evaluate((input: HTMLInputElement) => input.validity.valid)).toBe(false);
  expect(requests).toBe(0);
  await page.getByLabel('Phone').fill('+351911111111');
  await page.getByRole('button', { name: 'Send message', exact: true }).click();
  await expect(page.locator('.success')).toBeVisible();
  expect(requests).toBe(1);
});

test('paid booking does not offer another payment', async ({ page }) => {
  await page.route('**/api/reservations/guest/fixture-token', route => route.fulfill({ json: {
    ...reservation, paymentMode: 'PAY_NOW', paymentMethod: 'CARD', paymentStatus: 'SUCCEEDED',
  } }));
  await page.goto('/#/booking/fixture-token');
  await expect(page.getByRole('heading', { name: 'Hello, Browser Guest' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Complete your payment' })).toHaveCount(0);
  await expect(page.getByText('Paid · Credit or debit card', { exact: true })).toBeVisible();
});

for (const state of ['HELD', 'CONFIRMED', 'CANCELLED']) {
  test(`Multibanco voucher actions match ${state} booking state`, async ({ page }) => {
    await page.route('**/api/reservations/guest/fixture-token', route => route.fulfill({ json: {
      ...reservation, status: state, paymentMode: 'PAY_NOW', paymentMethod: 'MULTIBANCO',
      paymentStatus: state === 'HELD' ? 'PENDING' : 'SUCCEEDED',
      paymentInstructions: { entity: '12345', reference: '123456789',
        hostedVoucherUrl: 'https://example.test/voucher', expiresAt: '2030-06-10T10:00:00Z' },
    } }));
    await page.goto('/#/booking/fixture-token');
    await expect(page.getByText('123456789', { exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Open printable voucher' })).toHaveCount(state === 'HELD' ? 1 : 0);
    await expect(page.getByRole('heading', { name: 'Complete your payment' })).toHaveCount(state === 'HELD' ? 1 : 0);
  });
}

test('account survives refresh and profile edits update the greeting', async ({ page }) => {
  await page.route('**/api/auth/refresh', route => route.fulfill({ json: { accessToken: 'test-session' } }));
  await page.route('**/api/reservations/my', route => route.fulfill({ json: [] }));
  await page.route('**/api/users/me', route => route.fulfill({ json: {
    username: 'browser@example.test', fullName: 'Browser Guest', roles: ['CUSTOMER'], enabled: true,
  } }));
  await page.route('**/api/users/me/profile', route => route.fulfill({ json: {
    username: 'browser@example.test', fullName: route.request().postDataJSON().fullName,
    roles: ['CUSTOMER'], enabled: true,
  } }));
  await page.goto('/#/account');
  await expect(page.getByRole('heading', { name: 'Welcome back, Browser' })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Welcome back, Browser' })).toBeVisible();
  await page.getByRole('tab', { name: 'Profile', exact: true }).click();
  await page.getByLabel('Full name').fill('Updated Guest');
  await page.getByRole('button', { name: 'Save profile' }).click();
  await expect(page.getByRole('heading', { name: 'Welcome back, Updated' })).toBeVisible();
  await page.getByRole('tab', { name: 'My reservations' }).click();
  await expect(page.getByRole('heading', { name: 'No reservations yet' })).toBeVisible();
});
