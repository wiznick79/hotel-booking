import { del, get, patch } from './httpClient';

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
  items: ReservationItem[];
  totalPrice: number | null;
  currency: string | null;
  paymentMode: string;
  paymentMethod: string;
  paymentStatus: PaymentStatus | null;
  paymentReviewRequired: boolean;
  manualConfirmationRequired: boolean;
  holdUntil: string | null;
  discountCode: string | null;
  discountAmount: number | null;
};

export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'EXPIRED' | 'REFUNDED';

export type ReservationItem = {
  id: string;
  roomTypeId: string | null;
  roomId: string | null;
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

export function countPendingConfirmations(accessToken: string, hotelId: string) {
  return get<number>('/reservations/pending-count', accessToken, new URLSearchParams({ hotelId }));
}

export function findAffectedReservations(
  accessToken: string,
  hotelId: string,
  roomId: string,
  from: string,
  to: string,
) {
  return get<Reservation[]>(
    `/reservations/room/${roomId}/affected`,
    accessToken,
    new URLSearchParams({ hotelId, from, to }),
  );
}

export function confirmReservation(accessToken: string, id: string) {
  return patch<Reservation>(`/reservations/${id}/confirm`, {}, accessToken);
}

export function checkInReservation(accessToken: string, id: string) {
  return patch<Reservation>(`/reservations/${id}/check-in`, {}, accessToken);
}

export function checkOutReservation(accessToken: string, id: string) {
  return patch<Reservation>(`/reservations/${id}/check-out`, {}, accessToken);
}

export function markReservationAsNoShow(accessToken: string, id: string) {
  return patch<Reservation>(`/reservations/${id}/no-show`, {}, accessToken);
}

export function cancelReservation(accessToken: string, id: string) {
  return del(`/reservations/${id}`, accessToken);
}

export function assignReservationRoom(
  accessToken: string,
  reservationId: string,
  itemId: string,
  roomId: string,
) {
  return patch<Reservation>(
    `/reservations/${reservationId}/items/${itemId}/room`,
    { roomId },
    accessToken,
  );
}
