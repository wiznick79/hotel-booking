import { get, post } from './httpClient';

export type BookingPolicy = {
  hotelId: string;
  payLaterAllowed: boolean;
  maxUnconfirmedBookings: number;
  holdDurationMinutes: number;
  cancellationDeadlineDays: number;
};

export function findBookingPolicy(accessToken: string, hotelId: string) {
  return get<BookingPolicy>('/booking-policies', accessToken, new URLSearchParams({ hotelId }));
}

export function saveBookingPolicy(accessToken: string, policy: BookingPolicy) {
  return post<BookingPolicy>('/booking-policies', policy, accessToken);
}
