import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { findHotels, updateHotel } from '../api/hotelApi';
import type { CreateHotelRequest, Hotel } from '../api/hotelApi';
import { findBookingPolicy, saveBookingPolicy } from '../api/bookingPolicyApi';
import type { BookingPolicy } from '../api/bookingPolicyApi';
import { useAuth } from '../auth/useAuth';

export function HotelSettingsPage({ hotelId }: { hotelId: string }) {
  const { session } = useAuth();
  const [hotel, setHotel] = useState<Hotel | null>(null);
  const [policy, setPolicy] = useState<BookingPolicy>(defaultPolicy(''));
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const loadSettings = useCallback(async () => {
    if (!session || !hotelId) {
      return;
    }

    try {
      const selectedHotel = (await findHotels(session.accessToken))
        .find((candidate) => candidate.id === hotelId) ?? null;
      setHotel(selectedHotel);

      try {
        setPolicy(await findBookingPolicy(session.accessToken, hotelId));
      } catch {
        setPolicy(defaultPolicy(hotelId));
      }

      setError('');
    } catch {
      setError('Hotel settings could not be loaded.');
    }
  }, [hotelId, session]);

  useEffect(() => {
    void loadSettings();
  }, [loadSettings]);

  async function saveHotel(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session || !hotel) {
      return;
    }

    const form = new FormData(event.currentTarget);
    const request: CreateHotelRequest = {
      name: String(form.get('name')).trim(),
      description: String(form.get('description')).trim(),
      address: String(form.get('address')).trim(),
      city: String(form.get('city')).trim(),
      country: String(form.get('country')).trim(),
      defaultLanguage: String(form.get('defaultLanguage')),
      notificationDisplayName: optionalText(form, 'notificationDisplayName'),
      notificationFromAddress: optionalText(form, 'notificationFromAddress'),
      notificationReplyToAddress: optionalText(form, 'notificationReplyToAddress'),
    };

    try {
      setHotel(await updateHotel(session.accessToken, hotel.id, request));
      setMessage('Hotel details saved.');
      setError('');
    } catch {
      setError('Hotel details could not be saved.');
    }
  }

  async function savePolicy(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session) {
      return;
    }

    try {
      setPolicy(await saveBookingPolicy(session.accessToken, policy));
      setMessage('Booking policy saved.');
      setError('');
    } catch {
      setError('Booking policy could not be saved.');
    }
  }

  if (!hotelId) {
    return <section className="empty-state"><h2>Select a hotel first</h2></section>;
  }

  if (!hotel) {
    return <section className="empty-state"><h2>Loading hotel settings…</h2>{error && <p className="form-error">{error}</p>}</section>;
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Configuration</p>
          <h1>Hotel settings</h1>
          <p>Control the hotel profile, guest email identity, and booking rules.</p>
        </div>
      </div>

      {message && <p className="form-success">{message}</p>}
      {error && <p className="form-error">{error}</p>}

      <section className="setup-card">
        <h2>Hotel profile</h2>
        <form className="hotel-form" onSubmit={saveHotel}>
          <label>Name<input name="name" defaultValue={hotel.name} required /></label>
          <label>Default language<select name="defaultLanguage" defaultValue={hotel.defaultLanguage}><option value="en">English</option><option value="pt">Portuguese</option><option value="es">Spanish</option><option value="fr">French</option><option value="de">German</option></select></label>
          <label>Address<input name="address" defaultValue={hotel.address} required /></label>
          <label>City<input name="city" defaultValue={hotel.city} required /></label>
          <label>Country<input name="country" defaultValue={hotel.country} required /></label>
          <label className="full-width">Description<textarea name="description" defaultValue={hotel.description ?? ''} /></label>

          <div className="full-width form-section-heading">
            <h3>Guest email identity</h3>
            <p>Optional. Blank values use the global staging sender.</p>
          </div>
          <label>Display name<input name="notificationDisplayName" defaultValue={hotel.notificationDisplayName ?? ''} placeholder="Hotel Morgadinha" /></label>
          <label>From address<input name="notificationFromAddress" type="email" defaultValue={hotel.notificationFromAddress ?? ''} placeholder="morgadinha@wiznick.net" /></label>
          <label>Reply-to address<input name="notificationReplyToAddress" type="email" defaultValue={hotel.notificationReplyToAddress ?? ''} placeholder="morgadinha@wiznick.net" /></label>

          <div className="form-actions full-width"><button type="submit">Save hotel details</button></div>
        </form>
      </section>

      <section className="setup-card room-type-form-card">
        <h2>Booking policy</h2>
        <form className="hotel-form" onSubmit={savePolicy}>
          <label className="full-width checkbox-label"><input type="checkbox" checked={policy.payLaterAllowed} onChange={(event) => setPolicy({ ...policy, payLaterAllowed: event.target.checked })} /> Allow pay at reception</label>
          <fieldset className="full-width payment-methods">
            <legend>Online payment methods</legend>
            <p>These methods appear on the public booking form. Real provider credentials are configured separately.</p>
            {[
              ['CARD', 'Credit or debit card'],
              ['PAYPAL', 'PayPal'],
              ['MULTIBANCO', 'Multibanco'],
              ['MB_WAY', 'MB WAY'],
            ].map(([method, label]) => (
              <label className="checkbox-label" key={method}>
                <input
                  checked={policy.enabledOnlinePaymentMethods.includes(method)}
                  onChange={(event) => setPolicy({
                    ...policy,
                    enabledOnlinePaymentMethods: event.target.checked
                      ? [...policy.enabledOnlinePaymentMethods, method]
                      : policy.enabledOnlinePaymentMethods.filter((currentMethod) => currentMethod !== method),
                  })}
                  type="checkbox"
                />
                {label}
              </label>
            ))}
          </fieldset>
          <label>Maximum unconfirmed bookings<input type="number" min="0" value={policy.maxUnconfirmedBookings} onChange={(event) => setPolicy({ ...policy, maxUnconfirmedBookings: Number(event.target.value) })} required /></label>
          <label>Temporary hold (minutes)<input type="number" min="1" value={policy.holdDurationMinutes} onChange={(event) => setPolicy({ ...policy, holdDurationMinutes: Number(event.target.value) })} required /></label>
          <label>Cancellation deadline (days)<input type="number" min="0" value={policy.cancellationDeadlineDays} onChange={(event) => setPolicy({ ...policy, cancellationDeadlineDays: Number(event.target.value) })} required /></label>
          <div className="form-actions full-width"><button type="submit">Save booking policy</button></div>
        </form>
      </section>
    </section>
  );
}

function optionalText(form: FormData, name: string) {
  const value = String(form.get(name) ?? '').trim();
  return value === '' ? undefined : value;
}

function defaultPolicy(hotelId: string): BookingPolicy {
  return {
    hotelId,
    payLaterAllowed: false,
    maxUnconfirmedBookings: 0,
    holdDurationMinutes: 30,
    cancellationDeadlineDays: 2,
    enabledOnlinePaymentMethods: [],
  };
}
