import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { CreateRoomTypeRequest, RoomType } from '../api/roomApi';
import { createRoomType, deactivateRoomType, findRoomTypes, updateRoomType } from '../api/roomApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';

const languages = [
  { code: 'en', label: 'English' },
  { code: 'pt', label: 'Portuguese' },
  { code: 'es', label: 'Spanish' },
  { code: 'fr', label: 'French' },
];

function emptyTranslations() {
  return Object.fromEntries(languages.map(({ code }) => [code, { name: '', description: '' }]));
}

export function RoomTypesPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [roomTypes, setRoomTypes] = useState<RoomType[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingRoomTypeId, setEditingRoomTypeId] = useState<string | null>(null);
  const [maximumOccupancy, setMaximumOccupancy] = useState(2);
  const [basePrice, setBasePrice] = useState('');
  const [translations, setTranslations] = useState(emptyTranslations);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const loadRoomTypes = useCallback(async () => {
    if (!hotelId || !session) {
      setRoomTypes([]);
      return;
    }

    try {
      const loadedRoomTypes = await findRoomTypes(session.accessToken);
      setRoomTypes(loadedRoomTypes.filter((roomType) => roomType.hotelId === hotelId));
      setError('');
    } catch {
      setError('Room types could not be loaded. Please try again.');
    }
  }, [hotelId, session]);

  useEffect(() => {
    void loadRoomTypes();
  }, [loadRoomTypes]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !hotelId) {
      return;
    }

    setError('');
    setIsSubmitting(true);

    const populatedTranslations = Object.fromEntries(
      Object.entries(translations)
        .filter(([, translation]) => translation.name.trim().length > 0)
        .map(([language, translation]) => [
          language,
          { name: translation.name.trim(), description: translation.description.trim() },
        ]),
    );

    if (Object.keys(populatedTranslations).length === 0) {
      setError('Enter a name in at least one language.');
      setIsSubmitting(false);
      return;
    }

    const request: CreateRoomTypeRequest = {
      hotelId,
      maximumOccupancy,
      basePrice: Number(basePrice),
      translations: populatedTranslations,
    };

    try {
      if (editingRoomTypeId) await updateRoomType(session.accessToken, editingRoomTypeId, request);
      else await createRoomType(session.accessToken, request);
      setIsFormOpen(false);
      setMaximumOccupancy(2);
      setBasePrice('');
      setTranslations(emptyTranslations());
      setEditingRoomTypeId(null);
      await loadRoomTypes();
    } catch (exception) {
      setError(exception instanceof ApiError && exception.status === 403
        ? 'You do not have permission to create room types for this hotel.'
        : 'The room type could not be created. Please check the fields and try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateTranslation(language: string, field: 'name' | 'description', value: string) {
    setTranslations((currentTranslations) => ({
      ...currentTranslations,
      [language]: { ...currentTranslations[language], [field]: value },
    }));
  }

  async function deleteRoomType(roomType: RoomType) {
    if (!session || !window.confirm(`Delete ${roomType.name}? It will disappear from normal management screens. Active rooms must be reassigned or deactivated first.`)) {
      return;
    }
    try {
      await deactivateRoomType(session.accessToken, roomType.id);
      await loadRoomTypes();
    } catch (exception) {
      setError(exception instanceof ApiError
        ? 'The room type could not be deleted. It may still have active rooms.'
        : 'The room type could not be deleted. Please try again.');
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Hotel setup</p>
          <h1>Room types</h1>
          <p>Define the bookable categories used by this hotel.</p>
        </div>
        <button type="button" disabled={!hotelId} onClick={() => { setEditingRoomTypeId(null); setIsFormOpen(true); }}>New room type</button>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}

      {!hotelId && <div className="empty-state"><h2>No hotel selected</h2><p>Select a hotel before creating room types.</p></div>}

      {hotelId && roomTypes.length === 0 && !isFormOpen && !error && (
        <div className="empty-state"><h2>No room types yet</h2><p>Create types such as Double Room or Suite before adding rooms.</p></div>
      )}

      {roomTypes.length > 0 && (
        <div className="table-container">
          <table>
            <thead><tr><th>Name</th><th>Language</th><th>Maximum guests</th><th>Base price</th><th>Status</th><th /></tr></thead>
            <tbody>{roomTypes.map((roomType) => <tr key={roomType.id}><td><strong>{roomType.name}</strong><span className="table-subtext">{roomType.description}</span></td><td>{roomType.language.toUpperCase()}</td><td>{roomType.maximumOccupancy}</td><td>{formatPrice(roomType.basePrice)}</td><td><StatusBadge label={roomType.active ? 'Active' : 'Inactive'} tone={roomType.active ? 'positive' : 'negative'} /></td><td><button type="button" className="secondary-button" onClick={() => { setEditingRoomTypeId(roomType.id); setMaximumOccupancy(roomType.maximumOccupancy); setBasePrice(String(roomType.basePrice)); setTranslations({ ...emptyTranslations(), [roomType.language]: { name: roomType.name, description: roomType.description ?? '' } }); setIsFormOpen(true); }}>Edit</button> <button type="button" className="secondary-button" onClick={() => void deleteRoomType(roomType)}>Delete</button></td></tr>)}</tbody>
          </table>
        </div>
      )}

      {isFormOpen && (
        <section className="setup-card room-type-form-card" aria-labelledby="new-room-type-title">
          <h2 id="new-room-type-title">{editingRoomTypeId ? 'Edit room type' : 'New room type'}</h2>
          <form className="hotel-form" onSubmit={handleSubmit}>
            <label>Maximum occupancy<input type="number" min="1" value={maximumOccupancy} onChange={(event) => setMaximumOccupancy(Number(event.target.value))} required /></label>
            <label>Base price per night (€)<input type="number" min="0" step="0.01" value={basePrice} onChange={(event) => setBasePrice(event.target.value)} required /></label>
            {languages.map(({ code, label }) => <fieldset className="translation-fieldset full-width" key={code}><legend>{label}</legend><label>Name<input value={translations[code].name} onChange={(event) => updateTranslation(code, 'name', event.target.value)} /></label><label>Description<textarea rows={2} value={translations[code].description} onChange={(event) => updateTranslation(code, 'description', event.target.value)} /></label></fieldset>)}
            <div className="form-actions full-width"><button type="button" className="secondary-button" onClick={() => setIsFormOpen(false)}>Cancel</button><button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Saving...' : editingRoomTypeId ? 'Save room type' : 'Create room type'}</button></div>
          </form>
        </section>
      )}
    </section>
  );
}

function formatPrice(value: number) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'EUR' }).format(value);
}
