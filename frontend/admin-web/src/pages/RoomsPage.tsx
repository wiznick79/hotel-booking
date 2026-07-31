import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { CreateRoomRequest, Room, RoomType } from '../api/roomApi';
import { createRoom, findRooms, findRoomTypes, updateRoom } from '../api/roomApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

export function RoomsPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [rooms, setRooms] = useState<Room[]>([]);
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRoomId, setEditingRoomId] = useState<string | null>(null);
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

      setRooms(loadedRooms.filter((room) => room.hotelId === hotelId));
      setRoomTypes(selectedRoomTypes);
      setRoomTypeId((currentRoomTypeId) => currentRoomTypeId || selectedRoomTypes[0]?.id || '');
      setError('');
    } catch {
      setError('Rooms could not be loaded. Please try again.');
    }
  }, [hotelId, session]);

  useEffect(() => {
    void loadRooms();
  }, [loadRooms]);

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

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Hotel setup</p>
          <h1>Rooms</h1>
          <p>Manage the physical rooms available for reservation.</p>
        </div>
        <button type="button" disabled={!hotelId || roomTypes.length === 0} onClick={() => { setEditingRoomId(null); setStatus('AVAILABLE'); setIsFormOpen(true); }}>New room</button>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}
      {!hotelId && <div className="empty-state"><h2>No hotel selected</h2><p>Select a hotel before managing rooms.</p></div>}
      {hotelId && roomTypes.length === 0 && !error && <div className="empty-state"><h2>Create a room type first</h2><p>Every physical room must belong to a room type.</p></div>}
      {hotelId && roomTypes.length > 0 && rooms.length === 0 && !isFormOpen && !error && <div className="empty-state"><h2>No rooms yet</h2><p>Add the physical room numbers for this hotel.</p></div>}

      {rooms.length > 0 && (
        <div className="table-container"><table><thead><tr><th>Room</th><th>Type</th><th>Floor</th><th>Status</th><th /></tr></thead><tbody>{rooms.map((room) => <tr key={room.id}><td><strong>{room.roomNumber}</strong></td><td>{roomTypes.find((roomType) => roomType.id === room.roomTypeId)?.name ?? 'Unknown type'}</td><td>{room.floor}</td><td><span className={`status status-${room.status.toLowerCase()}`}>{formatStatus(room.status)}</span></td><td><button type="button" className="secondary-button" onClick={() => { setEditingRoomId(room.id); setRoomNumber(room.roomNumber); setRoomTypeId(room.roomTypeId); setFloor(String(room.floor)); setStatus(room.status); setIsFormOpen(true); }}>Edit</button></td></tr>)}</tbody></table></div>
      )}

      {isFormOpen && (
        <section className="setup-card room-type-form-card" aria-labelledby="new-room-title">
          <h2 id="new-room-title">{editingRoomId ? 'Edit room' : 'New room'}</h2>
          <form className="hotel-form" onSubmit={handleSubmit}>
            <label>Room number<input value={roomNumber} onChange={(event) => setRoomNumber(event.target.value)} required /></label>
            <label>Room type<select value={roomTypeId} onChange={(event) => setRoomTypeId(event.target.value)} required>{roomTypes.map((roomType) => <option key={roomType.id} value={roomType.id}>{roomType.name}</option>)}</select></label>
            <label>Floor<input type="number" min="0" value={floor} onChange={(event) => setFloor(event.target.value)} required /></label>
            {editingRoomId && <label>Status<select value={status} onChange={(event) => setStatus(event.target.value)}><option value="AVAILABLE">Available</option><option value="MAINTENANCE">Maintenance</option><option value="OUT_OF_SERVICE">Out of service</option></select></label>}
            <div className="form-actions full-width"><button type="button" className="secondary-button" onClick={() => { setIsFormOpen(false); setEditingRoomId(null); }}>Cancel</button><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Saving...' : editingRoomId ? 'Save room' : 'Create room'}</button></div>
          </form>
        </section>
      )}
    </section>
  );
}

function formatStatus(status: string) {
  return status.toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}
