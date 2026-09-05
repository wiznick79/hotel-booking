import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import type { Reservation, ReservationItem } from '../api/reservationApi';
import {
  assignReservationRoom,
  cancelReservation,
  checkInReservation,
  checkOutReservation,
  confirmReservation,
  findReservations,
  markReservationAsNoShow,
} from '../api/reservationApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';
import { findBookableRooms, findRooms, findRoomTypes } from '../api/roomApi';
import type { Room, RoomType } from '../api/roomApi';
import { readHashQuery, replaceHashQuery } from '../utils/hashQuery';

type ReservationsPageProps = {
  hotelId: string;
};

type AssignmentTarget = {
  reservation: Reservation;
  item: ReservationItem;
};

type ReservationSortKey = 'guestName' | 'checkInDate' | 'guestCount' | 'status' | 'totalPrice';

type SortDirection = 'ascending' | 'descending';

export function ReservationsPage({ hotelId }: ReservationsPageProps) {
  const { session } = useAuth();
  const [fromDate, setFromDate] = useState(() => readDateParameter('from', today()));
  const [toDate, setToDate] = useState(() => readDateParameter('to', addDays(30)));
  const [appliedDateRange, setAppliedDateRange] = useState(() => readDateRange());
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [assignmentTarget, setAssignmentTarget] = useState<AssignmentTarget | null>(null);
  const [selectedReservation, setSelectedReservation] = useState<Reservation | null>(null);
  const [candidateRooms, setCandidateRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isAssigning, setIsAssigning] = useState(false);
  const [sortKey, setSortKey] = useState<ReservationSortKey>('checkInDate');
  const [sortDirection, setSortDirection] = useState<SortDirection>('ascending');

  const loadReservations = useCallback(async () => {
    if (!hotelId || !session) {
      return;
    }

    setError('');
    setIsLoading(true);

    try {
      const [loadedReservations, loadedRooms, loadedRoomTypes] = await Promise.all([
        findReservations(
          session.accessToken,
          hotelId,
          appliedDateRange.fromDate,
          appliedDateRange.toDate,
        ),
        findRooms(session.accessToken),
        findRoomTypes(session.accessToken),
      ]);

      setReservations(loadedReservations);
      setRooms(loadedRooms.filter((room) => room.hotelId === hotelId));
      setRoomTypes(loadedRoomTypes.filter((roomType) => roomType.hotelId === hotelId));
    } catch (exception) {
      setReservations([]);
      setError(messageFor(exception));
    } finally {
      setIsLoading(false);
    }
  }, [appliedDateRange, hotelId, session]);

  useEffect(() => {
    if (!hotelId) {
      setReservations([]);
      return;
    }

    void loadReservations();
  }, [hotelId, loadReservations]);

  useEffect(() => {
    const synchronizeFilters = () => {
      const dateRange = readDateRange();
      setFromDate(dateRange.fromDate);
      setToDate(dateRange.toDate);
      setAppliedDateRange(dateRange);
    };

    window.addEventListener('hashchange', synchronizeFilters);
    return () => window.removeEventListener('hashchange', synchronizeFilters);
  }, []);

  const sortedReservations = useMemo(
    () => [...reservations].sort((first, second) => compareReservations(first, second, sortKey, sortDirection)),
    [reservations, sortDirection, sortKey],
  );

  function toggleSort(nextSortKey: ReservationSortKey) {
    if (nextSortKey === sortKey) {
      setSortDirection((currentDirection) => currentDirection === 'ascending' ? 'descending' : 'ascending');
      return;
    }

    setSortKey(nextSortKey);
    setSortDirection('ascending');
  }

  function updateFromDate(nextFromDate: string) {
    setFromDate(nextFromDate);
    applyDateRange(nextFromDate, toDate);
  }

  function updateToDate(nextToDate: string) {
    setToDate(nextToDate);
    applyDateRange(fromDate, nextToDate);
  }

  function applyDateRange(nextFromDate: string, nextToDate: string) {
    replaceHashQuery({ from: nextFromDate, to: nextToDate });
    setAppliedDateRange({ fromDate: nextFromDate, toDate: nextToDate });
  }

  async function confirm(id: string) {
    if (!session) {
      return;
    }

    try {
      await confirmReservation(session.accessToken, id);
      await loadReservations();
    } catch {
      setError('The reservation could not be confirmed.');
    }
  }

  async function cancel(id: string) {
    if (!session || !window.confirm('Cancel this reservation?')) {
      return;
    }

    try {
      await cancelReservation(session.accessToken, id);
      await loadReservations();
    } catch {
      setError('The reservation could not be cancelled.');
    }
  }

  async function updateStayStatus(
    id: string,
    request: (accessToken: string, reservationId: string) => Promise<Reservation>,
    failureMessage: string,
  ) {
    if (!session) {
      return;
    }

    try {
      const updatedReservation = await request(session.accessToken, id);
      setSelectedReservation(updatedReservation);
      await loadReservations();
    } catch {
      setError(failureMessage);
    }
  }

  async function openAssignmentForm(reservation: Reservation, item: ReservationItem) {
    if (!session || !item.roomTypeId) {
      return;
    }

    setError('');
    setAssignmentTarget({ reservation, item });
    setCandidateRooms([]);
    setSelectedRoomId(item.roomId ?? '');

    try {
      const [bookableRooms, overlappingReservations] = await Promise.all([
        findBookableRooms(
          session.accessToken,
          reservation.hotelId,
          item.roomTypeId,
          reservation.checkInDate,
          reservation.checkOutDate,
        ),
        findReservations(
          session.accessToken,
          reservation.hotelId,
          reservation.checkInDate,
          reservation.checkOutDate,
        ),
      ]);
      const unavailableRoomIds = new Set(
        overlappingReservations
          .filter((candidate) => candidate.id !== reservation.id && blocksInventory(candidate))
          .flatMap((candidate) => candidate.items.map((candidateItem) => candidateItem.roomId))
          .filter((roomId): roomId is string => roomId !== null),
      );
      const availableCandidates = bookableRooms.filter((room) => !unavailableRoomIds.has(room.id));

      setCandidateRooms(availableCandidates);
      setSelectedRoomId((currentRoomId) =>
        availableCandidates.some((room) => room.id === currentRoomId)
          ? currentRoomId
          : availableCandidates[0]?.id ?? '',
      );
    } catch {
      setError('Suitable rooms could not be loaded for this reservation.');
      setAssignmentTarget(null);
    }
  }

  async function saveRoomAssignment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !assignmentTarget || !selectedRoomId) {
      return;
    }

    setError('');
    setIsAssigning(true);

    try {
      await assignReservationRoom(
        session.accessToken,
        assignmentTarget.reservation.id,
        assignmentTarget.item.id,
        selectedRoomId,
      );
      setAssignmentTarget(null);
      await loadReservations();
    } catch (exception) {
      setError(exception instanceof ApiError
        ? exception.message
        : 'The room assignment could not be updated.');
    } finally {
      setIsAssigning(false);
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Reservations</p>
          <h1>Manage reservations</h1>
          <p>View stays, confirm requests, and adjust physical-room assignments.</p>
        </div>

        <button type="button" disabled>New reservation</button>
      </div>

      {!hotelId && (
        <div className="empty-state">
          <h2>No hotel is assigned to this account</h2>
          <p>Create a hotel and assign this staff account before managing its reservations.</p>
        </div>
      )}

      {hotelId && (
        <>
          <div className="filter-bar">
            <label>
              From
              <input type="date" value={fromDate} onChange={(event) => updateFromDate(event.target.value)} required />
            </label>
            <label>
              To
              <input type="date" value={toDate} onChange={(event) => updateToDate(event.target.value)} required />
            </label>
            {isLoading && <span className="filter-status">Loading...</span>}
          </div>

          {error && <p className="form-error" role="alert">{error}</p>}

          {!error && !isLoading && reservations.length === 0 && (
            <div className="empty-state">
              <h2>No reservations in this period</h2>
              <p>Try a different date range or create a reservation.</p>
            </div>
          )}

          {reservations.length > 0 && (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>{renderSortableHeader('Guest', 'guestName')}</th>
                    <th>{renderSortableHeader('Stay', 'checkInDate')}</th>
                    <th>Room assignment</th>
                    <th>{renderSortableHeader('Guests', 'guestCount')}</th>
                    <th>{renderSortableHeader('Status', 'status')}</th>
                    <th>{renderSortableHeader('Total', 'totalPrice')}</th>
                    <th><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                  <tbody>
                  {sortedReservations.map((reservation) => {
                    const isPastStay = reservation.checkOutDate < today();

  return (
                    <tr key={reservation.id}>
                      <td>
                        <strong>{reservation.guestName}</strong>
                        <span className="table-subtext">{reservation.guestEmail ?? reservation.guestPhone}</span>
                      </td>
                      <td>{reservation.checkInDate} – {reservation.checkOutDate}</td>
                      <td>
                        {reservation.items.map((item) => (
                          <div className="assignment-line" key={item.id}>
                            <span>{formatAssignment(item.roomTypeId, item.roomId, roomTypes, rooms)}</span>
                            {canChangeAssignment(reservation, isPastStay) && (
                              <button
                                type="button"
                                className="text-button"
                                onClick={() => void openAssignmentForm(reservation, item)}
                              >
                                Change
                              </button>
                            )}
                          </div>
                        ))}
                      </td>
                      <td>{reservation.guestCount}</td>
                      <td>
                        <StatusBadge label={formatStatus(reservation.status)} tone={statusTone(reservation.status)} />
                      </td>
                      <td>{formatPrice(reservation.totalPrice, reservation.currency)}</td>
                      <td className="reservation-actions">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => setSelectedReservation(reservation)}
                        >
                          View
                        </button>
                        {reservation.status === 'PENDING' && !isPastStay && (
                          <button type="button" className="secondary-button" onClick={() => void confirm(reservation.id)}>
                            Confirm
                          </button>
                        )}
                        {reservation.status === 'CONFIRMED' && canCheckIn(reservation) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => void updateStayStatus(
                              reservation.id,
                              checkInReservation,
                              'The guest could not be checked in.',
                            )}
                          >
                            Check in
                          </button>
                        )}
                        {reservation.status === 'CHECKED_IN' && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => void updateStayStatus(
                              reservation.id,
                              checkOutReservation,
                              'The guest could not be checked out.',
                            )}
                          >
                            Check out
                          </button>
                        )}
                        {canMarkNoShow(reservation) && (
                          <button
                            type="button"
                            className="secondary-button"
                            onClick={() => void updateStayStatus(
                              reservation.id,
                              markReservationAsNoShow,
                              'The reservation could not be marked as no-show.',
                            )}
                          >
                            No-show
                          </button>
                        )}
                        {!isPastStay && !['CANCELLED', 'CHECKED_OUT', 'NO_SHOW'].includes(reservation.status) && (
                          <button type="button" className="secondary-button" onClick={() => void cancel(reservation.id)}>
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {assignmentTarget && (
            <form className="setup-card assignment-form" onSubmit={saveRoomAssignment}>
              <h2>Change room assignment</h2>
              <p>
                {assignmentTarget.reservation.guestName}: {assignmentTarget.reservation.checkInDate} – {assignmentTarget.reservation.checkOutDate}
              </p>
              <label>
                Suitable room
                <select value={selectedRoomId} onChange={(event) => setSelectedRoomId(event.target.value)} required>
                  <option value="">Choose a room</option>
                  {candidateRooms.map((room) => (
                    <option key={room.id} value={room.id}>Room {room.roomNumber} · Floor {room.floor}</option>
                  ))}
                </select>
              </label>
              {candidateRooms.length === 0 && <p className="form-error">No suitable room is currently available.</p>}
              <div className="form-actions">
                <button type="button" className="secondary-button" onClick={() => setAssignmentTarget(null)} disabled={isAssigning}>
                  Cancel
                </button>
                <button type="submit" disabled={!selectedRoomId || isAssigning}>
                  {isAssigning ? 'Saving...' : 'Save assignment'}
                </button>
              </div>
            </form>
          )}

          {selectedReservation && (
            <section className="setup-card reservation-detail-card">
              <div className="page-heading compact-heading">
                <div>
                  <p className="eyebrow">Reservation details</p>
                  <h2>{selectedReservation.guestName}</h2>
                </div>
                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => setSelectedReservation(null)}
                >
                  Close
                </button>
              </div>
              <dl className="reservation-details">
                <div><dt>Booking ID</dt><dd>{selectedReservation.id}</dd></div>
                <div><dt>Status</dt><dd>{formatStatus(selectedReservation.status)}</dd></div>
                <div><dt>Stay</dt><dd>{selectedReservation.checkInDate} – {selectedReservation.checkOutDate}</dd></div>
                <div><dt>Guests</dt><dd>{selectedReservation.guestCount}</dd></div>
                <div><dt>Phone</dt><dd>{selectedReservation.guestPhone}</dd></div>
                <div><dt>Email</dt><dd>{selectedReservation.guestEmail ?? 'Not provided'}</dd></div>
                <div>
                  <dt>Payment</dt>
                  <dd>{formatPayment(selectedReservation)}</dd>
                </div>
                <div><dt>Total</dt><dd>{formatPrice(selectedReservation.totalPrice, selectedReservation.currency)}</dd></div>
                <div><dt>Discount</dt><dd>{selectedReservation.discountCode ?? 'None'}</dd></div>
                <div><dt>Confirmation</dt><dd>{selectedReservation.manualConfirmationRequired ? 'Manual confirmation required' : 'Not required'}</dd></div>
                <div className="full-width"><dt>Rooms</dt><dd>{selectedReservation.items.map((item) => formatAssignment(item.roomTypeId, item.roomId, roomTypes, rooms)).join(', ')}</dd></div>
                <div className="full-width"><dt>Guest notes</dt><dd>{selectedReservation.notes || 'No notes provided.'}</dd></div>
              </dl>
            </section>
          )}
        </>
      )}
    </section>
  );

  function renderSortableHeader(label: string, column: ReservationSortKey) {
    const isActive = sortKey === column;
    const directionLabel = sortDirection === 'ascending' ? 'ascending' : 'descending';

    return (
      <button
        type="button"
        className="sortable-header"
        onClick={() => toggleSort(column)}
        aria-label={`Sort by ${label}`}
        aria-sort={isActive ? directionLabel : 'none'}
      >
        {label}
        {isActive && <span aria-hidden="true">{sortDirection === 'ascending' ? ' ▲' : ' ▼'}</span>}
      </button>
    );
  }
}

function today() {
  return new Date().toISOString().slice(0, 10);
}

function addDays(days: number) {
  const date = new Date();
  date.setDate(date.getDate() + days);

  return date.toISOString().slice(0, 10);
}

function blocksInventory(reservation: Reservation) {
  if (['CANCELLED', 'CHECKED_OUT', 'NO_SHOW'].includes(reservation.status)) {
    return false;
  }

  return reservation.status !== 'HELD'
    || (reservation.holdUntil !== null && new Date(reservation.holdUntil) > new Date());
}

function canChangeAssignment(reservation: Reservation, isPastStay: boolean) {
  return !isPastStay && !['CANCELLED', 'CHECKED_OUT', 'NO_SHOW'].includes(reservation.status);
}

function readDateParameter(name: string, fallback: string) {
  const value = readHashQuery().get(name);

  return value && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : fallback;
}

function readDateRange() {
  return {
    fromDate: readDateParameter('from', today()),
    toDate: readDateParameter('to', addDays(30)),
  };
}

function canCheckIn(reservation: Reservation) {
  const currentDate = today();

  return reservation.checkInDate <= currentDate && reservation.checkOutDate > currentDate;
}

function canMarkNoShow(reservation: Reservation) {
  return ['PENDING', 'CONFIRMED'].includes(reservation.status)
    && reservation.checkInDate < today();
}

function statusTone(status: Reservation['status']) {
  if (status === 'PENDING' || status === 'HELD') {
    return 'warning';
  }

  return status === 'CANCELLED' || status === 'NO_SHOW' ? 'negative' : 'positive';
}

function formatStatus(status: string) {
  if (status === 'CHECKED_OUT') {
    return 'Completed';
  }

  if (status === 'NO_SHOW') {
    return 'No-show';
  }

  return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatPaymentMode(paymentMode: string) {
  return paymentMode.replace('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatPayment(reservation: Reservation) {
  if (reservation.paymentMode === 'PAY_AT_RECEPTION') {
    return 'Pay at reception';
  }

  const method = formatPaymentMethod(reservation.paymentMethod);
  const status = paymentStatusLabel(reservation.paymentStatus);

  return `${status} · ${method}${reservation.paymentReviewRequired ? ' · Review required (cancelled booking)' : ''}`;
}

function paymentStatusLabel(paymentStatus: string | null) {
  const labels: Record<string, string> = {
    PENDING: 'Payment pending',
    SUCCEEDED: 'Paid',
    FAILED: 'Payment failed',
    EXPIRED: 'Payment expired',
    REFUNDED: 'Refunded',
  };

  return paymentStatus ? labels[paymentStatus] ?? paymentStatus : 'Payment pending';
}

function formatPaymentMethod(paymentMethod: string) {
  const labels: Record<string, string> = {
    CARD: 'Card',
    PAYPAL: 'PayPal',
    MULTIBANCO: 'Multibanco',
    MB_WAY: 'MB WAY',
  };

  return labels[paymentMethod] ?? formatPaymentMode(paymentMethod);
}

function compareReservations(
  first: Reservation,
  second: Reservation,
  sortKey: ReservationSortKey,
  sortDirection: SortDirection,
) {
  const multiplier = sortDirection === 'ascending' ? 1 : -1;
  const comparison = reservationSortValue(first, sortKey).localeCompare(reservationSortValue(second, sortKey));

  return comparison === 0 ? first.id.localeCompare(second.id) : comparison * multiplier;
}

function reservationSortValue(reservation: Reservation, sortKey: ReservationSortKey) {
  if (sortKey === 'guestName') {
    return reservation.guestName.toLocaleLowerCase();
  }

  if (sortKey === 'guestCount') {
    return reservation.guestCount.toString().padStart(4, '0');
  }

  if (sortKey === 'status') {
    return reservation.status;
  }

  if (sortKey === 'totalPrice') {
    return (reservation.totalPrice ?? 0).toFixed(2).padStart(14, '0');
  }

  return reservation.checkInDate;
}

function formatPrice(value: number | null, currency: string | null) {
  if (value === null || !currency) {
    return '—';
  }

  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(value);
}

function formatAssignment(
  roomTypeId: string | null,
  roomId: string | null,
  roomTypes: RoomType[],
  rooms: Room[],
) {
  const roomType = roomTypes.find((candidate) => candidate.id === roomTypeId);
  const room = rooms.find((candidate) => candidate.id === roomId);
  const roomTypeName = roomType?.name ?? 'Room type';

  return room ? `${roomTypeName} · Room ${room.roomNumber}` : `${roomTypeName} · Not assigned`;
}

function messageFor(exception: unknown) {
  if (exception instanceof ApiError && exception.status === 403) {
    return 'You do not have access to reservations for this hotel.';
  }

  return 'Reservations could not be loaded. Please try again.';
}
