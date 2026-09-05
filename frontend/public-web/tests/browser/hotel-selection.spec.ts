import { test, expect, hotel } from './fixtures';
import { selectPublicHotel } from '../../src/hotelSelection';

test('hotel selection is explicit and independent of API ordering', () => {
  const other = { ...hotel, id: 'other-hotel', name: 'Other Hotel' };
  expect(selectPublicHotel([other, hotel], hotel.id)).toEqual(hotel);
  expect(selectPublicHotel([hotel, other], hotel.id)).toEqual(hotel);
  expect(selectPublicHotel([hotel], 'missing')).toBeNull();
  expect(selectPublicHotel([], hotel.id)).toBeNull();
  expect(selectPublicHotel([hotel], '  ')).toEqual(hotel);
  expect(selectPublicHotel([hotel, other])).toBeNull();
  expect(selectPublicHotel([])).toBeNull();
});

test('ambiguous hotels do not display the first property or allow booking', async ({ page }) => {
  await page.route('**/api/hotels', route => route.fulfill({ json: [hotel,
    { ...hotel, id: 'other-hotel', name: 'Other Hotel' }] }));
  await page.goto('/#/book');
  await expect(page.getByText('The hotel information could not be loaded. Please try again shortly.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Request booking', exact: true })).toBeDisabled();
});
