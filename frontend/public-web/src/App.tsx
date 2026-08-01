import { useEffect, useMemo, useState } from 'react';
import heroImage from './assets/hotel-hero.png';

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

const api = import.meta.env.VITE_API_BASE_URL ?? '/api';

export function App() {
  const [hotel, setHotel] = useState<Hotel | null>(null);
  const [types, setTypes] = useState<RoomType[]>([]);
  const [path, setPath] = useState(readPath());
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    void loadHotelData();
  }, []);

  useEffect(() => {
    const handleHashChange = () => setPath(readPath());

    window.addEventListener('hashchange', handleHashChange);

    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  async function loadHotelData() {
    try {
      const [hotels, roomTypes] = await Promise.all([
        get<Hotel[]>('/hotels'),
        get<RoomType[]>('/room-types'),
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

  function showConfirmation() {
    window.history.replaceState(null, '', '#/confirmation');
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  }

  return (
    <>
      <header className="site-header">
        <button className="hotel-title" onClick={returnHome} type="button">
          {hotel?.name ?? 'Hotel Booking'}
        </button>
        <button onClick={openBooking} type="button">Book now</button>
      </header>

      <main>
        {loadError && <p className="error">{loadError}</p>}
        {path === '/confirmation' ? (
          <BookingConfirmation hotel={hotel} onReturnHome={returnHome} />
        ) : path === '/book' ? (
          <Booking hotel={hotel} onBack={returnHome} onSuccess={showConfirmation} />
        ) : (
          <Landing hotel={hotel} types={types} onBook={openBooking} />
        )}
      </main>
    </>
  );
}

type LandingProps = {
  hotel: Hotel | null;
  types: RoomType[];
  onBook: () => void;
};

function Landing({ hotel, types, onBook }: LandingProps) {
  return (
    <>
      <section
        className="hero-photo"
        style={{
          backgroundImage: `linear-gradient(90deg, #15251eaa, #15251e11), url(${heroImage})`,
        }}
      >
        <div>
          <p className="eyebrow">Welcome to Portugal</p>
          <h1>{hotel?.name ?? 'Your stay starts here'}</h1>
          <p>
            {hotel?.description
              || 'Peaceful hospitality, thoughtful comfort, and an easy stay.'}
          </p>
          <button onClick={onBook} type="button">Reserve your stay</button>
        </div>
      </section>

      <section className="section">
        <p className="eyebrow">Stay your way</p>
        <h2>Rooms made for rest</h2>
        <div className="cards">
          {types.map((type) => (
            <article key={type.id}>
              <span>From €{type.basePrice.toFixed(0)} / night</span>
              <h3>{type.name}</h3>
              <p>Comfortably sleeps up to {type.maximumOccupancy} guests.</p>
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
  onSuccess: () => void;
};

function Booking({ hotel, onBack, onSuccess }: BookingProps) {
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
        const response = await get<AvailableRoomType[]>(`/reservations/availability?${parameters}`);

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
  }, [checkInDate, checkOutDate, guestCount, hotel]);

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
        headers: { 'Content-Type': 'application/json' },
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
        }),
      });

      if (response.ok) {
        onSuccess();
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
      <button className="link" onClick={onBack} type="button">Back to hotel</button>
      <h2>Book your stay</h2>
      <p className="booking-intro">
        {hotel ? `Choose your dates for ${hotel.name} and see the available options.` : 'Choose your dates and see available options.'}
      </p>

      <div className="form-grid">
        <label>
          Check-in
          <input
            min={dateAfter(1)}
            onChange={(event) => setCheckInDate(event.target.value)}
            required
            type="date"
            value={checkInDate}
          />
        </label>
        <label>
          Check-out
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
        Number of guests
        <select onChange={(event) => setGuestCount(Number(event.target.value))} value={guestCount}>
          {[1, 2, 3, 4, 5, 6].map((count) => (
            <option key={count} value={count}>
              {count} {count === 1 ? 'guest' : 'guests'}
            </option>
          ))}
        </select>
      </label>

      <label>
        Available room type
        <select
          disabled={isSearching || availableRoomTypes.length === 0}
          onChange={(event) => setRoomTypeId(event.target.value)}
          required
          value={roomTypeId}
        >
          <option value="">
            {isSearching
              ? 'Checking availability...'
              : availableRoomTypes.length
                ? 'Choose a room type'
                : 'No available room type for these dates'}
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
        Full name
        <input autoComplete="name" onChange={(event) => setName(event.target.value)} required value={name} />
      </label>

      <label>
        Phone
        <input autoComplete="tel" onChange={(event) => setPhone(event.target.value)} required type="tel" value={phone} />
      </label>

      <label>
        Email <span className="optional">(recommended)</span>
        <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} type="email" value={email} />
      </label>

      <label>
        Notes <span className="optional">(optional)</span>
        <textarea
          onChange={(event) => setNotes(event.target.value)}
          placeholder="For example, your expected arrival time."
          rows={4}
          value={notes}
        />
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
  onReturnHome: () => void;
};

function BookingConfirmation({ hotel, onReturnHome }: BookingConfirmationProps) {
  return (
    <section className="booking-confirmation">
      <p className="eyebrow">Request received</p>
      <h1>Thank you for your booking request.</h1>
      <p>
        {hotel
          ? `${hotel.name} will review your request and contact you shortly.`
          : 'We will review your request and contact you shortly.'}
      </p>
      <p>If you provided an email address, we will send your booking details there.</p>
      <button onClick={onReturnHome} type="button">Return to hotel</button>
    </section>
  );
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${api}${path}`);

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}

function formatCurrency(value: number, currency: string) {
  return new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(value);
}

function dateAfter(days: number) {
  return new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
}

function readPath() {
  return window.location.hash.replace(/^#/, '') || '/';
}
