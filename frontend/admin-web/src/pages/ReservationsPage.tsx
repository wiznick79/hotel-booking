import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import type { Reservation } from '../api/reservationApi';
import { cancelReservation, confirmReservation, findReservations } from '../api/reservationApi';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';

type ReservationsPageProps = {
  hotelId: string;
};

export function ReservationsPage({ hotelId }: ReservationsPageProps) {
  const { session } = useAuth();
  const [fromDate, setFromDate] = useState(today());
  const [toDate, setToDate] = useState(addDays(30));
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const loadReservations = useCallback(async () => {
    if (!hotelId || !session) {
      return;
    }

    setError('');
    setIsLoading(true);

    try {
      const loadedReservations = await findReservations(
        session.accessToken,
        hotelId,
        fromDate,
        toDate,
      );

      setReservations(loadedReservations);
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
    if (!session) return;
    try { await confirmReservation(session.accessToken, id); await loadReservations(); }
    catch { setError('The reservation could not be confirmed.'); }
  }

  async function cancel(id: string) {
    if (!session || !window.confirm('Cancel this reservation?')) return;
    try { await cancelReservation(session.accessToken, id); await loadReservations(); }
    catch { setError('The reservation could not be cancelled.'); }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Reservations</p>
          <h1>Manage reservations</h1>
          <p>View the stays that overlap the selected date range.</p>
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
                    <th>Rooms</th>
                    <th>Guests</th>
                    <th>Status</th>
                    <th>Total</th><th />
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
                      <td>{reservation.roomIds.length}</td>
                      <td>{reservation.guestCount}</td>
                      <td><StatusBadge label={formatStatus(reservation.status)} tone={reservation.status === 'PENDING' || reservation.status === 'HELD' ? 'warning' : reservation.status === 'CANCELLED' || reservation.status === 'NO_SHOW' ? 'negative' : 'positive'} /></td>
                      <td>{formatPrice(reservation.totalPrice, reservation.currency)}</td><td>{reservation.status === 'PENDING' && <button type="button" className="secondary-button" onClick={() => void confirm(reservation.id)}>Confirm</button>} {!['CANCELLED', 'CHECKED_OUT'].includes(reservation.status) && <button type="button" className="secondary-button" onClick={() => void cancel(reservation.id)}>Cancel</button>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
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

function formatStatus(status: string) {
  return status.replace('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function formatPrice(value: number | null, currency: string | null) {
  if (value === null || !currency) {
    return '—';
  }

  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(value);
}

function messageFor(exception: unknown) {
  if (exception instanceof ApiError && exception.status === 403) {
    return 'You do not have access to reservations for this hotel.';
  }

  return 'Reservations could not be loaded. Please try again.';
}
