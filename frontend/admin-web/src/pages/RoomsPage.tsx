import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import type {
  CreateRoomRequest,
  Room,
  RoomType,
  RoomUnavailability,
} from '../api/roomApi';
import {
  createRoom,
  createRoomUnavailability,
  deleteRoomUnavailability,
  findRoomUnavailabilities,
  findRooms,
  findRoomTypes,
  updateRoom,
} from '../api/roomApi';
import type { Reservation } from '../api/reservationApi';
import { findAffectedReservations, findReservations } from '../api/reservationApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';
import { readHashQuery, replaceHashQuery } from '../utils/hashQuery';

export function RoomsPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [unavailabilities, setUnavailabilities] = useState<RoomUnavailability[]>([]);
  const [view, setView] = useState<'list' | 'calendar'>(() => readViewParameter());
  const [weekStart, setWeekStart] = useState(() => readWeekParameter());
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [isBlockFormOpen, setIsBlockFormOpen] = useState(false);
  const [editingRoomId, setEditingRoomId] = useState<string | null>(null);
  const [blockedRoomId, setBlockedRoomId] = useState('');
  const [blockFromDate, setBlockFromDate] = useState('');
  const [blockToDate, setBlockToDate] = useState('');
  const [blockReason, setBlockReason] = useState('');
  const [emergencyBlock, setEmergencyBlock] = useState(false);
  const [selectedBlockId, setSelectedBlockId] = useState<string | null>(null);
  const [status, setStatus] = useState('AVAILABLE');
  const [roomNumber, setRoomNumber] = useState('');
  const [roomTypeId, setRoomTypeId] = useState('');
  const [floor, setFloor] = useState('0');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadRooms = useCallback(async () => {
    if (!hotelId || !session) {
      setRooms([]);
      setRoomTypes([]);
      return;
    }

    try {
      const [loadedRooms, loadedRoomTypes] = await Promise.all([
        findRooms(session.accessToken),
        findRoomTypes(session.accessToken),
      ]);

      const selectedRoomTypes = loadedRoomTypes.filter((roomType) => roomType.hotelId === hotelId);
      const selectedRooms = loadedRooms.filter((room) => room.hotelId === hotelId);
      const calendarStart = toDateInputValue(weekStart);
      const calendarEnd = toDateInputValue(addDays(weekStart, 7));

      const [loadedReservations, unavailableByRoom] = await Promise.all([
        findReservations(session.accessToken, hotelId, calendarStart, calendarEnd),
        Promise.all(selectedRooms.map((room) => findRoomUnavailabilities(session.accessToken, room.id))),
      ]);

      setRooms(selectedRooms);
      setRoomTypes(selectedRoomTypes);
      setReservations(loadedReservations);
      setUnavailabilities(unavailableByRoom.flat());
      setRoomTypeId((currentRoomTypeId) => currentRoomTypeId || selectedRoomTypes[0]?.id || '');
      setError('');
    } catch {
      setError('Rooms could not be loaded. Please try again.');
    }
  }, [hotelId, session, weekStart]);

  useEffect(() => {
    void loadRooms();
  }, [loadRooms]);

  useEffect(() => {
    const synchronizeView = () => {
      setView(readViewParameter());
      setWeekStart(readWeekParameter());
    };

    window.addEventListener('hashchange', synchronizeView);
    return () => window.removeEventListener('hashchange', synchronizeView);
  }, []);

  const calendarDays = useMemo(
    () => Array.from({ length: 7 }, (_, index) => addDays(weekStart, index)),
    [weekStart],
  );

  function changeView(nextView: 'list' | 'calendar') {
    setView(nextView);
    replaceHashQuery({ view: nextView, week: toDateInputValue(weekStart) });
  }

  function changeWeek(nextWeekStart: Date) {
    setWeekStart(nextWeekStart);
    replaceHashQuery({ view, week: toDateInputValue(nextWeekStart) });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !hotelId || !roomTypeId) {
      return;
    }

    setError('');
    setIsSubmitting(true);

    const request: CreateRoomRequest = {
      hotelId,
      roomTypeId,
      roomNumber: roomNumber.trim(),
      floor: Number(floor),
    };

    try {
      if (editingRoomId) {
        await updateRoom(session.accessToken, editingRoomId, { ...request, status });
      } else {
        await createRoom(session.accessToken, request);
      }
      setRoomNumber('');
      setFloor('0');
      setStatus('AVAILABLE');
      setEditingRoomId(null);
      setIsFormOpen(false);
      await loadRooms();
    } catch (exception) {
      setError(exception instanceof ApiError && exception.status === 403
        ? 'You do not have permission to create rooms for this hotel.'
        : 'The room could not be created. Check that the room number is unique and try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function openEditForm(room: Room) {
    setEditingRoomId(room.id);
    setRoomNumber(room.roomNumber);
    setRoomTypeId(room.roomTypeId);
    setFloor(String(room.floor));
    setStatus(room.status);
    setIsFormOpen(true);
  }

  function openBlockForm(roomId: string, date: Date) {
    const dateValue = toDateInputValue(date);
    const existingBlock = unavailabilities.find((unavailability) =>
      unavailability.roomId === roomId
        && unavailability.fromDate <= dateValue
        && unavailability.toDate > dateValue,
    );

    setBlockedRoomId(roomId);
    setBlockFromDate(existingBlock?.fromDate ?? dateValue);
    setBlockToDate(existingBlock?.toDate ?? toDateInputValue(addDays(date, 1)));
    setBlockReason(existingBlock?.reason ?? '');
    setEmergencyBlock(existingBlock?.emergency ?? false);
    setSelectedBlockId(existingBlock?.id ?? null);
    setIsBlockFormOpen(true);
  }

  async function saveRoomBlock(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !blockedRoomId || !blockFromDate || !blockToDate) {
      return;
    }

    if (blockToDate <= blockFromDate) {
      setError('The block end date must be after the start date.');
      return;
    }

    if (emergencyBlock && !blockReason.trim()) {
      setError('An emergency safety block requires a reason.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const affectedReservations = await findAffectedReservations(
        session.accessToken,
        hotelId,
        blockedRoomId,
        blockFromDate,
        blockToDate,
      );

      if (affectedReservations.length > 0 && !window.confirm(
        `${affectedReservations.length} future reservation(s) use this room during the selected period. `
          + 'They will remain active and must be reassigned. Create the block anyway?',
      )) {
        return;
      }

      await createRoomUnavailability(session.accessToken, {
        roomId: blockedRoomId,
        fromDate: blockFromDate,
        toDate: blockToDate,
        reason: blockReason.trim() || undefined,
        emergency: emergencyBlock,
      });
      closeBlockForm();
      await loadRooms();
    } catch (exception) {
      setError(exception instanceof ApiError
        ? exception.message
        : 'The room could not be blocked for the selected dates.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function removeRoomBlock() {
    if (!session || !selectedBlockId || !window.confirm('Remove this room unavailability block?')) {
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      await deleteRoomUnavailability(session.accessToken, selectedBlockId);
      closeBlockForm();
      await loadRooms();
    } catch {
      setError('The room unavailability block could not be removed.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function closeBlockForm() {
    setIsBlockFormOpen(false);
    setBlockedRoomId('');
    setBlockFromDate('');
    setBlockToDate('');
    setBlockReason('');
    setEmergencyBlock(false);
    setSelectedBlockId(null);
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Hotel setup</p>
          <h1>Rooms</h1>
          <p>Manage physical rooms and inspect their date-by-date availability.</p>
        </div>
        <button
          type="button"
          disabled={!hotelId || roomTypes.length === 0}
          onClick={() => {
            setEditingRoomId(null);
            setStatus('AVAILABLE');
            setIsFormOpen(true);
          }}
        >
          New room
        </button>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}
      {!hotelId && (
        <div className="empty-state">
          <h2>No hotel selected</h2>
          <p>Select a hotel before managing rooms.</p>
        </div>
      )}
      {hotelId && roomTypes.length === 0 && !error && (
        <div className="empty-state">
          <h2>Create a room type first</h2>
          <p>Every physical room must belong to a room type.</p>
        </div>
      )}
      {hotelId && roomTypes.length > 0 && rooms.length === 0 && !isFormOpen && !error && (
        <div className="empty-state">
          <h2>No rooms yet</h2>
          <p>Add the physical room numbers for this hotel.</p>
        </div>
      )}

      {rooms.length > 0 && (
        <>
          <div className="view-switcher" role="group" aria-label="Room view">
            <button
              type="button"
              className={view === 'list' ? '' : 'secondary-button'}
              onClick={() => changeView('list')}
            >
              Room list
            </button>
            <button
              type="button"
              className={view === 'calendar' ? '' : 'secondary-button'}
              onClick={() => changeView('calendar')}
            >
              Weekly availability
            </button>
          </div>

          {view === 'list' ? (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Room</th>
                    <th>Type</th>
                    <th>Floor</th>
                    <th>Status</th>
                    <th><span className="visually-hidden">Actions</span></th>
                  </tr>
                </thead>
                <tbody>
                  {rooms.map((room) => (
                    <tr key={room.id}>
                      <td><strong>{room.roomNumber}</strong></td>
                      <td>{roomTypes.find((roomType) => roomType.id === room.roomTypeId)?.name ?? 'Unknown type'}</td>
                      <td>{room.floor}</td>
                      <td>
                        <StatusBadge
                          label={formatStatus(room.status)}
                          tone={room.status === 'AVAILABLE' ? 'positive' : room.status === 'MAINTENANCE' ? 'warning' : 'negative'}
                        />
                      </td>
                      <td>
                        <button type="button" className="secondary-button" onClick={() => openEditForm(room)}>
                          Edit
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <section className="availability-calendar" aria-labelledby="availability-title">
              <div className="calendar-toolbar">
                <div>
                  <h2 id="availability-title">Weekly availability</h2>
                  <p>Select a day to add or manage a maintenance block. Checkout dates are free.</p>
                </div>
                <div className="calendar-navigation">
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => changeWeek(addDays(weekStart, -7))}
                  >
                    Previous week
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => changeWeek(getMonday(new Date()))}
                  >
                    This week
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={() => changeWeek(addDays(weekStart, 7))}
                  >
                    Next week
                  </button>
                </div>
              </div>

              <div className="availability-grid" role="table" aria-label="Room availability by date">
                <div className="availability-grid-row availability-grid-heading" role="row">
                  <div role="columnheader">Room</div>
                  {calendarDays.map((day) => (
                    <div key={day.toISOString()} role="columnheader">
                      <span>{formatWeekday(day)}</span>
                      <strong>{formatDate(day)}</strong>
                    </div>
                  ))}
                </div>
                {rooms.map((room) => (
                  <div className="availability-grid-row" key={room.id} role="row">
                    <div className="calendar-room" role="rowheader">
                      <strong>{room.roomNumber}</strong>
                      <span>{roomTypes.find((roomType) => roomType.id === room.roomTypeId)?.name ?? 'Unknown type'}</span>
                    </div>
                    {calendarDays.map((day) => {
                      const state = getAvailabilityState(room, day, reservations, unavailabilities);

                      return (
                        <div
                          key={day.toISOString()}
                          className={`availability-cell availability-${state}`}
                          role="cell"
                          title={`${availabilityLabel(state)}. Select to manage availability.`}
                          onClick={() => openBlockForm(room.id, day)}
                        >
                          <span className="visually-hidden">{availabilityLabel(state)}</span>
                        </div>
                      );
                    })}
                  </div>
                ))}
              </div>

              <div className="calendar-legend" aria-label="Availability legend">
                <span><i className="availability-cell availability-free" />Free</span>
                <span><i className="availability-cell availability-booked" />Reserved</span>
                <span><i className="availability-cell availability-maintenance" />Maintenance block</span>
                <span><i className="availability-cell availability-unavailable" />Out of service</span>
              </div>

              {isBlockFormOpen && (
                <form className="hotel-form calendar-block-form" onSubmit={saveRoomBlock}>
                  <div className="full-width">
                    <h3>{selectedBlockId ? 'Manage room block' : 'Block room availability'}</h3>
                    <p className="field-help">
                      {rooms.find((room) => room.id === blockedRoomId)?.roomNumber ?? 'Selected room'} is unavailable from the start date up to, but not including, the end date.
                    </p>
                  </div>
                  <label>
                    Start date
                    <input
                      type="date"
                      value={blockFromDate}
                      onChange={(event) => setBlockFromDate(event.target.value)}
                      required
                      disabled={selectedBlockId !== null}
                    />
                  </label>
                  <label>
                    End date
                    <input
                      type="date"
                      min={blockFromDate}
                      value={blockToDate}
                      onChange={(event) => setBlockToDate(event.target.value)}
                      required
                      disabled={selectedBlockId !== null}
                    />
                  </label>
                  <label className="full-width">
                    Reason
                    <textarea
                      value={blockReason}
                      onChange={(event) => setBlockReason(event.target.value)}
                      maxLength={500}
                      rows={3}
                      disabled={selectedBlockId !== null}
                    />
                  </label>
                  <label className="checkbox-label">
                    <input
                      type="checkbox"
                      checked={emergencyBlock}
                      onChange={(event) => setEmergencyBlock(event.target.checked)}
                      disabled={selectedBlockId !== null}
                    />
                    Emergency safety block
                  </label>
                  <div className="form-actions full-width">
                    {selectedBlockId && (
                      <button type="button" className="danger-button" onClick={() => void removeRoomBlock()} disabled={isSubmitting}>
                        Remove block
                      </button>
                    )}
                    <button type="button" className="secondary-button" onClick={closeBlockForm} disabled={isSubmitting}>
                      Cancel
                    </button>
                    {!selectedBlockId && (
                      <button type="submit" disabled={isSubmitting}>
                        {isSubmitting ? 'Saving...' : 'Block room'}
                      </button>
                    )}
                  </div>
                </form>
              )}
            </section>
          )}
        </>
      )}

      {isFormOpen && (
        <section className="setup-card room-type-form-card" aria-labelledby="new-room-title">
          <h2 id="new-room-title">{editingRoomId ? 'Edit room' : 'New room'}</h2>
          <form className="hotel-form" onSubmit={handleSubmit}>
            <label>
              Room number
              <input
                value={roomNumber}
                onChange={(event) => setRoomNumber(event.target.value)}
                required
              />
            </label>
            <label>
              Room type
              <select
                value={roomTypeId}
                onChange={(event) => setRoomTypeId(event.target.value)}
                required
              >
                {roomTypes.map((roomType) => (
                  <option key={roomType.id} value={roomType.id}>{roomType.name}</option>
                ))}
              </select>
            </label>
            <label>
              Floor
              <input
                type="number"
                min="0"
                value={floor}
                onChange={(event) => setFloor(event.target.value)}
                required
              />
            </label>
            {editingRoomId && (
              <label>
                Status
                <select value={status} onChange={(event) => setStatus(event.target.value)}>
                  <option value="AVAILABLE">Available</option>
                  <option value="MAINTENANCE">Maintenance</option>
                  <option value="OUT_OF_SERVICE">Out of service</option>
                </select>
              </label>
            )}
            <div className="form-actions full-width">
              <button
                type="button"
                className="secondary-button"
                onClick={() => {
                  setIsFormOpen(false);
                  setEditingRoomId(null);
                }}
              >
                Cancel
              </button>
              <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Saving...' : editingRoomId ? 'Save room' : 'Create room'}
              </button>
            </div>
          </form>
        </section>
      )}
    </section>
  );
}

function getAvailabilityState(
  room: Room,
  day: Date,
  reservations: Reservation[],
  unavailabilities: RoomUnavailability[],
) {
  if (room.status === 'OUT_OF_SERVICE') {
    return 'unavailable';
  }

  const date = toDateInputValue(day);
  const hasMaintenanceBlock = unavailabilities.some((unavailability) =>
    unavailability.roomId === room.id
      && unavailability.fromDate <= date
      && unavailability.toDate > date,
  );

  if (room.status === 'MAINTENANCE' || hasMaintenanceBlock) {
    return 'maintenance';
  }

  const isReserved = reservations.some((reservation) =>
    reservation.status !== 'CANCELLED'
      && reservation.status !== 'CHECKED_OUT'
      && reservation.status !== 'NO_SHOW'
      && (
        reservation.status !== 'HELD'
        || (reservation.holdUntil !== null && new Date(reservation.holdUntil) > new Date())
      )
      && reservation.checkInDate <= date
      && reservation.checkOutDate > date
      && reservation.items.some((item) => item.roomId === room.id),
  );

  return isReserved ? 'booked' : 'free';
}

function availabilityLabel(state: string) {
  return {
    free: 'Free',
    booked: 'Reserved',
    maintenance: 'Maintenance block',
    unavailable: 'Out of service',
  }[state] ?? 'Unknown';
}

function addDays(date: Date, days: number) {
  const result = new Date(date);
  result.setDate(result.getDate() + days);
  return result;
}

function getMonday(date: Date) {
  const result = new Date(date);
  const day = result.getDay();
  const difference = day === 0 ? -6 : 1 - day;
  result.setDate(result.getDate() + difference);
  result.setHours(0, 0, 0, 0);
  return result;
}

function toDateInputValue(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function readViewParameter(): 'list' | 'calendar' {
  return readHashQuery().get('view') === 'calendar' ? 'calendar' : 'list';
}

function readWeekParameter() {
  const value = readHashQuery().get('week');
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return getMonday(new Date());
  }

  return getMonday(new Date(`${value}T00:00:00`));
}

function formatWeekday(date: Date) {
  return new Intl.DateTimeFormat('en', { weekday: 'short' }).format(date);
}

function formatDate(date: Date) {
  return new Intl.DateTimeFormat('en', { day: 'numeric', month: 'short' }).format(date);
}

function formatStatus(status: string) {
  return status.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}
