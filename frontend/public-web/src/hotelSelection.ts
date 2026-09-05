// Never let API ordering decide which hotel's public website is displayed.
export function selectPublicHotel<T extends { id: string }>(hotels: T[], configuredId?: string): T | null {
  const id = configuredId?.trim();
  if (id) return hotels.find(hotel => hotel.id === id) ?? null;
  return hotels.length === 1 ? hotels[0] : null;
}
