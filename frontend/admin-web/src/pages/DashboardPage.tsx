import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { CreateHotelRequest } from '../api/hotelApi';
import { createHotel } from '../api/hotelApi';
import { ApiError } from '../api/httpClient';
import { findCurrentUser, updateHotelAssignments } from '../api/userApi';
import { useAuth } from '../auth/useAuth';
import { findReservations } from '../api/reservationApi';

type DashboardPageProps = {
  hotelId: string;
};

const initialHotel: CreateHotelRequest = {
  name: '',
  description: '',
  address: '',
  city: '',
  country: 'Portugal',
  defaultLanguage: 'en',
};

export function DashboardPage({ hotelId }: DashboardPageProps) {
  const { refresh, session } = useAuth();
  const [hotel, setHotel] = useState(initialHotel);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [summary, setSummary] = useState({ pending: 0, arrivals: 0, departures: 0 });

  useEffect(() => {
    if (!session || !hotelId) {
      setSummary({ pending: 0, arrivals: 0, departures: 0 });
      return;
    }
    const today = new Date();
    const date = today.toISOString().slice(0, 10);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    void findReservations(session.accessToken, hotelId, date, tomorrow.toISOString().slice(0, 10))
      .then((reservations) => setSummary({
        pending: reservations.filter((reservation) => reservation.status === 'PENDING').length,
        arrivals: reservations.filter((reservation) => reservation.checkInDate === date).length,
        departures: reservations.filter((reservation) => reservation.checkOutDate === date).length,
      }))
      .catch(() => setSummary({ pending: 0, arrivals: 0, departures: 0 }));
  }, [hotelId, session]);

  async function handleHotelSetup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session) {
      return;
    }

    setError('');
    setSuccess('');
    setIsSubmitting(true);

    let createdHotelName: string | undefined;

    try {
      const createdHotel = await createHotel(session.accessToken, hotel);
      createdHotelName = createdHotel.name;
      const currentUser = await findCurrentUser(session.accessToken);
      const hotelIds = [...new Set([...currentUser.hotelIds, createdHotel.id])];

      await updateHotelAssignments(session.accessToken, currentUser.id, hotelIds);

      try {
        await refresh();
        setSuccess('Hotel created and assigned. Your access has been refreshed.');
      } catch {
        setSuccess('Hotel created and assigned. Refresh the page once to load the new hotel assignment.');
      }
    } catch (exception) {
      if (createdHotelName) {
        setError(`${createdHotelName} was created, but could not be assigned automatically. Do not submit the form again.`);
      } else {
        setError(messageFor(exception));
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateField(field: keyof CreateHotelRequest, value: string) {
    setHotel((currentHotel) => ({ ...currentHotel, [field]: value }));
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Overview</p>
          <h1>Welcome back, {session?.claims.sub}</h1>
          <p>Use the navigation to manage reservations and hotel settings.</p>
        </div>
      </div>

      {hotelId && <div className="dashboard-cards"><div className="info-card"><p className="eyebrow">Today</p><h2>{summary.arrivals}</h2><p>Arrivals</p></div><div className="info-card"><p className="eyebrow">Today</p><h2>{summary.departures}</h2><p>Departures</p></div><div className="info-card"><p className="eyebrow">Attention</p><h2>{summary.pending}</h2><p>Pending confirmations</p></div></div>}

      {!hotelId && (
        <section className="setup-card" aria-labelledby="setup-title">
          <div>
            <h2 id="setup-title">Set up your first hotel</h2>
            <p>Create the first hotel and assign it to your administrator account.</p>
          </div>

          <form className="hotel-form" onSubmit={handleHotelSetup}>
            <label>
              Hotel name
              <input value={hotel.name} onChange={(event) => updateField('name', event.target.value)} required />
            </label>
            <label>
              Address
              <input value={hotel.address} onChange={(event) => updateField('address', event.target.value)} required />
            </label>
            <label>
              City
              <input value={hotel.city} onChange={(event) => updateField('city', event.target.value)} required />
            </label>
            <label>
              Country
              <input value={hotel.country} onChange={(event) => updateField('country', event.target.value)} required />
            </label>
            <label>
              Default language
              <select value={hotel.defaultLanguage} onChange={(event) => updateField('defaultLanguage', event.target.value)}>
                <option value="en">English</option>
                <option value="pt">Portuguese</option>
                <option value="es">Spanish</option>
                <option value="fr">French</option>
                <option value="de">German</option>
              </select>
            </label>
            <label className="full-width">
              Description
              <textarea value={hotel.description} onChange={(event) => updateField('description', event.target.value)} rows={3} />
            </label>
            {error && <p className="form-error full-width" role="alert">{error}</p>}
            {success && <p className="form-success full-width" role="status">{success}</p>}
            <div className="full-width">
              <button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Creating hotel...' : 'Create hotel'}</button>
            </div>
          </form>
        </section>
      )}
    </section>
  );
}

function messageFor(exception: unknown) {
  if (exception instanceof ApiError && exception.status === 403) {
    return 'Your account does not have permission to set up a hotel.';
  }

  return 'The hotel could not be created. Please check the fields and try again.';
}
