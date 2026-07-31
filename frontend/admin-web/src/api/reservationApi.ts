import { get } from './httpClient';

export type Reservation = {
  id: string;
  hotelId: string;
  guestName: string;
  guestPhone: string;
  guestEmail: string | null;
  guestCount: number;
  checkInDate: string;
  checkOutDate: string;
  notes: string | null;
  status: ReservationStatus;
  roomIds: string[];
  totalPrice: number | null;
  currency: string | null;
  paymentMode: string;
  manualConfirmationRequired: boolean;
  holdUntil: string | null;
  discountCode: string | null;
  discountAmount: number | null;
};

export type ReservationStatus =
  | 'PENDING'
  | 'HELD'
  | 'CONFIRMED'
  | 'CANCELLED'
  | 'CHECKED_IN'
  | 'CHECKED_OUT'
  | 'NO_SHOW';

export function findReservations(
  accessToken: string,
  hotelId: string,
  from: string,
  to: string,
) {
  const parameters = new URLSearchParams({ hotelId, from, to });

  return get<Reservation[]>('/reservations', accessToken, parameters);
}
