import { useEffect, useMemo, useState } from 'react';
import heroImage from './assets/hotel-hero.png';
import { publicLanguages, translate, type PublicLanguage } from './i18n';

type Hotel = {
  id: string;
  name: string;
  description: string;
  city: string;
  country: string;
};

type RoomType = {
  id: string;
  hotelId: string;
  name: string;
  maximumOccupancy: number;
  basePrice: number;
  active: boolean;
};

type AvailableRoomType = {
  roomTypeId: string;
  name: string;
  maximumOccupancy: number;
  totalPrice: number;
  currency: string;
};

type Reservation = {
  id: string;
  guestName: string;
  guestCount: number;
  checkInDate: string;
  checkOutDate: string;
  notes: string | null;
  status: string;
  items: ReservationItem[];
  totalPrice: number;
  currency: string;
  paymentMode: string;
  manualConfirmationRequired: boolean;
};

type ReservationItem = {
  roomTypeId: string;
};

const api = import.meta.env.VITE_API_BASE_URL ?? '/api';

export function App() {
  const [hotel, setHotel] = useState<Hotel | null>(null);
  const [types, setTypes] = useState<RoomType[]>([]);
  const [path, setPath] = useState(readPath());
  const [loadError, setLoadError] = useState('');
  const [createdReservation, setCreatedReservation] = useState<Reservation | null>(null);
  const [language, setLanguage] = useState<PublicLanguage>(readLanguage);
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);

  useEffect(() => {
    void loadHotelData(language);
  }, [language]);

  useEffect(() => {
    const handleHashChange = () => setPath(readPath());

    window.addEventListener('hashchange', handleHashChange);

    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  async function loadHotelData(selectedLanguage: PublicLanguage) {
    try {
      const [hotels, roomTypes] = await Promise.all([
        get<Hotel[]>('/hotels', selectedLanguage),
        get<RoomType[]>('/room-types', selectedLanguage),
      ]);
      const selectedHotel = hotels[0] ?? null;

      setHotel(selectedHotel);
      setTypes(roomTypes.filter((type) => type.hotelId === selectedHotel?.id && type.active));
    } catch {
      setLoadError('The hotel information could not be loaded. Please try again shortly.');
    }
  }

  function openBooking() {
    window.location.hash = '#/book';
  }

  function returnHome() {
    window.location.hash = '#/';
  }

  function showConfirmation(reservation: Reservation) {
    setCreatedReservation(reservation);
    window.history.replaceState(null, '', '#/confirmation');
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  }

  function changeLanguage(nextLanguage: PublicLanguage) {
    localStorage.setItem('hotel-booking.public-language', nextLanguage);
    setLanguage(nextLanguage);
  }

  const guestAccessToken = readGuestAccessToken(path);

  return (
    <>
      <header className="site-header">
        <button className="hotel-title" onClick={returnHome} type="button">
          {hotel?.name ?? 'Hotel Booking'}
        </button>
        <div className="header-actions">
          <select aria-label="Language" onChange={(event) => changeLanguage(event.target.value as PublicLanguage)} value={language}>
            {publicLanguages.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
          </select>
          <button onClick={openBooking} type="button">{t('bookNow')}</button>
        </div>
      </header>

      <main>
        {loadError && <p className="error">{loadError}</p>}
        {guestAccessToken ? (
          <GuestBooking
            hotel={hotel}
            roomTypes={types}
            token={guestAccessToken}
            onReturnHome={returnHome}
            language={language}
          />
        ) : path === '/confirmation' ? (
          <BookingConfirmation
            hotel={hotel}
            reservation={createdReservation}
            onReturnHome={returnHome}
            language={language}
          />
        ) : path === '/book' ? (
          <Booking hotel={hotel} onBack={returnHome} onSuccess={showConfirmation} language={language} />
        ) : path === '/privacy' ? (
          <PrivacyNotice onReturnHome={returnHome} language={language} />
        ) : (
          <Landing hotel={hotel} types={types} onBook={openBooking} language={language} />
        )}
      </main>
    </>
  );
}

type LandingProps = {
  hotel: Hotel | null;
  types: RoomType[];
  onBook: () => void;
  language: PublicLanguage;
};

function Landing({ hotel, types, onBook, language }: LandingProps) {
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);

  return (
    <>
      <section
        className="hero-photo"
        style={{
          backgroundImage: `linear-gradient(90deg, #15251eaa, #15251e11), url(${heroImage})`,
        }}
      >
        <div>
          <p className="eyebrow">{t('welcome')}</p>
          <h1>{hotel?.name ?? 'Your stay starts here'}</h1>
          <p>
            {hotel?.description
              || 'Peaceful hospitality, thoughtful comfort, and an easy stay.'}
          </p>
          <button onClick={onBook} type="button">{t('reserveStay')}</button>
        </div>
      </section>

      <section className="section">
        <p className="eyebrow">{t('stayYourWay')}</p>
        <h2>{t('roomsMadeForRest')}</h2>
        <div className="cards">
          {types.map((type) => (
            <article key={type.id}>
              <span>From €{type.basePrice.toFixed(0)} / night</span>
              <h3>{type.name}</h3>
              <p>{t('sleepsUpTo', { count: type.maximumOccupancy })}</p>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}

type BookingProps = {
  hotel: Hotel | null;
  onBack: () => void;
  onSuccess: (reservation: Reservation) => void;
  language: PublicLanguage;
};

function Booking({ hotel, onBack, onSuccess, language }: BookingProps) {
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);
  const [roomTypeId, setRoomTypeId] = useState('');
  const [checkInDate, setCheckInDate] = useState(dateAfter(1));
  const [checkOutDate, setCheckOutDate] = useState(dateAfter(2));
  const [guestCount, setGuestCount] = useState(1);
  const [availableRoomTypes, setAvailableRoomTypes] = useState<AvailableRoomType[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [notes, setNotes] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!hotel || checkOutDate <= checkInDate) {
      setAvailableRoomTypes([]);
      return;
    }

    const hotelId = hotel.id;
    let cancelled = false;

    async function searchAvailability() {
      setIsSearching(true);
      setSearchError('');

      try {
        const parameters = new URLSearchParams({
          hotelId,
          checkInDate,
          checkOutDate,
          guestCount: String(guestCount),
        });
        const response = await get<AvailableRoomType[]>(`/reservations/availability?${parameters}`, language);

        if (!cancelled) {
          setAvailableRoomTypes(response);
          setRoomTypeId((currentRoomTypeId) =>
            response.some((roomType) => roomType.roomTypeId === currentRoomTypeId)
              ? currentRoomTypeId
              : '',
          );
        }
      } catch {
        if (!cancelled) {
          setAvailableRoomTypes([]);
          setSearchError('Availability could not be loaded. Please try again.');
        }
      } finally {
        if (!cancelled) {
          setIsSearching(false);
        }
      }
    }

    void searchAvailability();

    return () => {
      cancelled = true;
    };
  }, [checkInDate, checkOutDate, guestCount, hotel, language]);

  const selectedRoomType = useMemo(
    () => availableRoomTypes.find((roomType) => roomType.roomTypeId === roomTypeId) ?? null,
    [availableRoomTypes, roomTypeId],
  );

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setMessage('');

    if (!hotel || !selectedRoomType) {
      setMessage('Choose an available room type before submitting your booking request.');
      return;
    }

    if (checkOutDate <= checkInDate) {
      setMessage('Check-out must be after check-in.');
      return;
    }

    setSubmitting(true);

    try {
      const response = await fetch(`${api}/reservations`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept-Language': language },
        body: JSON.stringify({
          hotelId: hotel.id,
          guestName: name,
          guestPhone: phone,
          guestEmail: email || undefined,
          guestCount,
          checkInDate,
          checkOutDate,
          notes: notes || undefined,
          roomTypeIds: [selectedRoomType.roomTypeId],
          paymentMode: 'PAY_AT_RECEPTION',
          privacyNoticeAccepted: true,
        }),
      });

      if (response.ok) {
        onSuccess(await response.json() as Reservation);
        return;
      }

      setMessage('That room type is no longer available for the selected dates. Please search again.');
    } catch {
      setMessage('We could not send your booking request. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="booking-card" onSubmit={submit}>
      <button className="link" onClick={onBack} type="button">{t('backToHotel')}</button>
      <h2>{t('bookYourStay')}</h2>
      <p className="booking-intro">
        {hotel
          ? t('bookingIntro', { hotelName: hotel.name })
          : t('bookingIntroGeneric')}
      </p>

      <div className="form-grid">
        <label>
          {t('checkIn')}
          <input
            min={dateAfter(1)}
            onChange={(event) => setCheckInDate(event.target.value)}
            required
            type="date"
            value={checkInDate}
          />
        </label>
        <label>
          {t('checkOut')}
          <input
            min={checkInDate || dateAfter(1)}
            onChange={(event) => setCheckOutDate(event.target.value)}
            required
            type="date"
            value={checkOutDate}
          />
        </label>
      </div>

      <label>
        {t('guests')}
        <select onChange={(event) => setGuestCount(Number(event.target.value))} value={guestCount}>
          {[1, 2, 3, 4, 5, 6].map((count) => (
            <option key={count} value={count}>
              {count} {count === 1 ? t('guest') : t('guestsPlural')}
            </option>
          ))}
        </select>
      </label>

      <label>
        {t('availableRoomType')}
        <select
          disabled={isSearching || availableRoomTypes.length === 0}
          onChange={(event) => setRoomTypeId(event.target.value)}
          required
          value={roomTypeId}
        >
          <option value="">
            {isSearching
              ? t('checkingAvailability')
              : availableRoomTypes.length
                ? t('chooseRoomType')
                : t('noRoomTypes')}
          </option>
          {availableRoomTypes.map((roomType) => (
            <option key={roomType.roomTypeId} value={roomType.roomTypeId}>
              {roomType.name} · up to {roomType.maximumOccupancy} guests · {formatCurrency(roomType.totalPrice, roomType.currency)} total
            </option>
          ))}
        </select>
      </label>

      {selectedRoomType && (
        <p className="availability-summary">
          {selectedRoomType.name}: <strong>{formatCurrency(selectedRoomType.totalPrice, selectedRoomType.currency)}</strong> for the full stay.
        </p>
      )}
      {searchError && <p className="error">{searchError}</p>}

      <label>
        {t('fullName')}
        <input autoComplete="name" onChange={(event) => setName(event.target.value)} required value={name} />
      </label>

      <label>
        {t('phone')}
        <input autoComplete="tel" onChange={(event) => setPhone(event.target.value)} required type="tel" value={phone} />
      </label>

      <label>
        {t('email')} <span className="optional">{t('recommended')}</span>
        <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} type="email" value={email} />
      </label>

      <label>
        {t('notes')} <span className="optional">{t('optional')}</span>
        <textarea
          onChange={(event) => setNotes(event.target.value)}
          placeholder="For example, your expected arrival time."
          rows={4}
          value={notes}
        />
      </label>

      <label className="consent">
        <input required type="checkbox" />
        <span>
          {t('privacyNotice')} <a href="#/privacy">{t('privacy')}</a>
        </span>
      </label>

      {message && <p className="error">{message}</p>}
      <button disabled={submitting || !selectedRoomType}>
        {submitting ? 'Sending request…' : 'Request booking'}
      </button>
    </form>
  );
}

type BookingConfirmationProps = {
  hotel: Hotel | null;
  reservation: Reservation | null;
  onReturnHome: () => void;
  language: PublicLanguage;
};

function BookingConfirmation({ hotel, reservation, onReturnHome, language }: BookingConfirmationProps) {
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);

  return (
    <section className="booking-confirmation">
      <p className="eyebrow">{t('requestReceived')}</p>
      <h1>{t('bookingThankYou')}</h1>
      <p>
        {hotel
          ? `${hotel.name} will review your request and contact you shortly.`
          : 'We will review your request and contact you shortly.'}
      </p>
      {reservation && (
        <dl className="booking-summary">
          <div>
            <dt>Stay</dt>
            <dd>{formatDateRange(reservation.checkInDate, reservation.checkOutDate)}</dd>
          </div>
          <div>
            <dt>Guests</dt>
            <dd>{reservation.guestCount}</dd>
          </div>
          <div>
            <dt>Total</dt>
            <dd>{formatCurrency(reservation.totalPrice, reservation.currency)}</dd>
          </div>
        </dl>
      )}
      <p>
        If you provided an email address, we will send your booking details and a secure link
        to view this reservation.
      </p>
      <button onClick={onReturnHome} type="button">{t('returnToHotel')}</button>
    </section>
  );
}

type PrivacyNoticeProps = {
  onReturnHome: () => void;
  language: PublicLanguage;
};

function PrivacyNotice({ onReturnHome, language }: PrivacyNoticeProps) {
  const t = (key: Parameters<typeof translate>[1]) => translate(language, key);

  return (
    <section className="booking-confirmation">
      <p className="eyebrow">{t('privacy')}</p>
      <h1>{t('privacyTitle')}</h1>
      <p>{t('privacyBody')}</p>
      <p className="muted">
        This project provides a practical privacy baseline. The final notice, retention periods,
        and contact details must be reviewed for the hotel&apos;s real operating context.
      </p>
      <button onClick={onReturnHome} type="button">{t('returnToHotel')}</button>
    </section>
  );
}

type GuestBookingProps = {
  hotel: Hotel | null;
  roomTypes: RoomType[];
  token: string;
  onReturnHome: () => void;
  language: PublicLanguage;
};

function GuestBooking({ hotel, roomTypes, token, onReturnHome, language }: GuestBookingProps) {
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    let cancelled = false;

    async function loadReservation() {
      try {
        const response = await get<Reservation>(`/reservations/guest/${encodeURIComponent(token)}`);

        if (!cancelled) {
          setReservation(response);
        }
      } catch {
        if (!cancelled) {
          setLoadError('This booking link is invalid, expired, or no longer available.');
        }
      }
    }

    void loadReservation();

    return () => {
      cancelled = true;
    };
  }, [token]);

  if (loadError) {
    return (
      <section className="booking-confirmation">
        <p className="eyebrow">Booking access</p>
        <h1>We could not open this booking.</h1>
        <p className="error">{loadError}</p>
        <p>Please contact the hotel if you need help with an existing reservation.</p>
        <button onClick={onReturnHome} type="button">{t('returnToHotel')}</button>
      </section>
    );
  }

  if (!reservation) {
    return (
      <section className="booking-confirmation">
        <p>Loading your booking details…</p>
      </section>
    );
  }

  const bookedRoomTypes = reservation.items
    .map((item) => roomTypes.find((roomType) => roomType.id === item.roomTypeId)?.name ?? 'Room type')
    .join(', ');

  return (
    <section className="booking-confirmation">
      <p className="eyebrow">Your booking</p>
      <h1>Hello, {reservation.guestName}</h1>
      <p>
        Your reservation at {hotel?.name ?? 'the hotel'} is currently{' '}
        <strong>{formatReservationStatus(reservation.status)}</strong>.
      </p>
      <dl className="booking-summary">
        <div>
          <dt>Stay</dt>
          <dd>{formatDateRange(reservation.checkInDate, reservation.checkOutDate)}</dd>
        </div>
        <div>
          <dt>Guests</dt>
          <dd>{reservation.guestCount}</dd>
        </div>
        <div>
          <dt>Room type</dt>
          <dd>{bookedRoomTypes}</dd>
        </div>
        <div>
          <dt>Total</dt>
          <dd>{formatCurrency(reservation.totalPrice, reservation.currency)}</dd>
        </div>
        <div>
          <dt>Payment</dt>
          <dd>{formatPaymentMode(reservation.paymentMode)}</dd>
        </div>
      </dl>
      {reservation.manualConfirmationRequired && (
        <p>Your booking request requires confirmation from the hotel.</p>
      )}
      <p className="muted">To change or cancel this booking, please contact the hotel directly.</p>
      <button onClick={onReturnHome} type="button">{t('returnToHotel')}</button>
    </section>
  );
}

async function get<T>(path: string, language?: PublicLanguage): Promise<T> {
  const response = await fetch(`${api}${path}`, {
    headers: language ? { 'Accept-Language': language } : undefined,
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(value);
}

function formatDateRange(checkInDate: string, checkOutDate: string) {
  const formatter = new Intl.DateTimeFormat('en-GB', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });

  return `${formatter.format(new Date(`${checkInDate}T00:00:00`))} – ${formatter.format(
    new Date(`${checkOutDate}T00:00:00`),
  )}`;
}

function formatReservationStatus(status: string) {
  return status.toLowerCase().replaceAll('_', ' ');
}

function formatPaymentMode(paymentMode: string) {
  return paymentMode.toLowerCase().replaceAll('_', ' ');
}

function dateAfter(days: number) {
  return new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
}

function readPath() {
  return window.location.hash.replace(/^#/, '') || '/';
}

function readLanguage(): PublicLanguage {
  return localStorage.getItem('hotel-booking.public-language') === 'pt' ? 'pt' : 'en';
}

function readGuestAccessToken(path: string) {
  const match = /^\/booking\/([^/?#]+)$/.exec(path);

  return match ? decodeURIComponent(match[1]) : null;
}
