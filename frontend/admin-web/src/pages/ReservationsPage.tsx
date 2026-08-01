import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { Reservation, ReservationItem } from '../api/reservationApi';
import {
  assignReservationRoom,
  cancelReservation,
  confirmReservation,
  findReservations,
} from '../api/reservationApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';
import { findBookableRooms, findRooms, findRoomTypes } from '../api/roomApi';
import type { Room, RoomType } from '../api/roomApi';

type ReservationsPageProps = {
  hotelId: string;
};

type AssignmentTarget = {
  reservation: Reservation;
  item: ReservationItem;
};

export function ReservationsPage({ hotelId }: ReservationsPageProps) {
  const { session } = useAuth();
  const [fromDate, setFromDate] = useState(today());
  const [toDate, setToDate] = useState(addDays(30));
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [assignmentTarget, setAssignmentTarget] = useState<AssignmentTarget | null>(null);
  const [candidateRooms, setCandidateRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState('');
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isAssigning, setIsAssigning] = useState(false);

  const loadReservations = useCallback(async () => {
    if (!hotelId || !session) {
      return;
    }

    setError('');
    setIsLoading(true);

    try {
      const [loadedReservations, loadedRooms, loadedRoomTypes] = await Promise.all([
        findReservations(session.accessToken, hotelId, fromDate, toDate),
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
  }, [fromDate, hotelId, session, toDate]);

  useEffect(() => {
    if (!hotelId) {
      setReservations([]);
      return;
    }

    void loadReservations();
  }, [hotelId, loadReservations]);

  function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void loadReservations();
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
          <form className="filter-bar" onSubmit={handleFilterSubmit}>
            <label>
              From
              <input type="date" value={fromDate} onChange={(event) => setFromDate(event.target.value)} required />
            </label>
            <label>
              To
              <input type="date" value={toDate} onChange={(event) => setToDate(event.target.value)} required />
            </label>
            <button type="submit" disabled={isLoading}>{isLoading ? 'Loading...' : 'Apply filters'}</button>
          </form>

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
                    <th>Guest</th>
                    <th>Stay</th>
                    <th>Room assignment</th>
                    <th>Guests</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {reservations.map((reservation) => (
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
                            {canChangeAssignment(reservation) && (
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
                        {reservation.status === 'PENDING' && (
                          <button type="button" className="secondary-button" onClick={() => void confirm(reservation.id)}>
                            Confirm
                          </button>
                        )}
                        {!['CANCELLED', 'CHECKED_OUT'].includes(reservation.status) && (
                          <button type="button" className="secondary-button" onClick={() => void cancel(reservation.id)}>
                            Cancel
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
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
        </>
      )}
    </section>
  );
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

function canChangeAssignment(reservation: Reservation) {
  return !['CANCELLED', 'CHECKED_OUT', 'NO_SHOW'].includes(reservation.status);
}

function statusTone(status: Reservation['status']) {
  if (status === 'PENDING' || status === 'HELD') {
    return 'warning';
  }

  return status === 'CANCELLED' || status === 'NO_SHOW' ? 'negative' : 'positive';
}

function formatStatus(status: string) {
  return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
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
