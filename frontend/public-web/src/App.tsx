import { useEffect, useMemo, useState } from 'react';
import heroImage from './assets/hotel-hero.png';
import exteriorImage from './assets/hotel-exterior-gallery.png';
import roomImage from './assets/hotel-room-gallery.png';
import { publicLanguages, translate, type PublicLanguage } from './i18n';
import {
  ContactPage,
  ExperiencePage,
  GalleryPage,
  RoomDetailPage,
  SiteFooter,
  type PublicHotel,
  type PublicRoomType,
} from './MarketingPages';

type Hotel = PublicHotel;

type RoomType = PublicRoomType;

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
  paymentMethod: string;
  manualConfirmationRequired: boolean;
  paymentAttempt: PaymentAttempt | null;
  paymentStatus: string | null;
  paymentInstructions: PaymentInstructions | null;
};

type PaymentAttempt = {
  id: string;
  paymentMethod: string;
  status: string;
  redirectUrl: string | null;
};

type PaymentMethodAvailability = {
  paymentMethod: string;
  paymentMode: string;
};

type PaymentInstructions = {
  entity: string | null;
  reference: string | null;
  hostedVoucherUrl: string | null;
  expiresAt: string | null;
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
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [preferredRoomTypeId, setPreferredRoomTypeId] = useState('');
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

  function openBooking(roomTypeId = '') {
    setPreferredRoomTypeId(roomTypeId);
    window.location.hash = '#/book';
  }

  function returnHome() {
    if (readPath() === '/') {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    window.location.hash = '#/';
  }

  function showConfirmation(reservation: Reservation) {
    setCreatedReservation(reservation);
    window.history.replaceState(null, '', '#/confirmation');
    window.dispatchEvent(new HashChangeEvent('hashchange'));
  }

  function openAccount() {
    window.location.hash = '#/account';
  }

  function navigateToPage(nextPath: string) {
    if (nextPath === '/#rooms') {
      navigateToSection('rooms');
      return;
    }

    window.location.hash = `#${nextPath}`;
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function navigateToSection(sectionId: string) {
    if (path !== '/') {
      window.location.hash = '#/';
      window.setTimeout(() => document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' }), 0);
      return;
    }

    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' });
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
        <nav aria-label="Main navigation" className="site-nav">
          <button onClick={returnHome} type="button">{t('home')}</button>
          <button onClick={() => navigateToSection('rooms')} type="button">{t('rooms')}</button>
          <button onClick={() => navigateToPage('/experience')} type="button">{t('experiences')}</button>
          <button onClick={() => navigateToPage('/gallery')} type="button">{t('gallery')}</button>
          <button onClick={() => navigateToPage('/contact')} type="button">{t('contact')}</button>
        </nav>
        <div className="header-actions">
          <select aria-label="Language" onChange={(event) => changeLanguage(event.target.value as PublicLanguage)} value={language}>
            {publicLanguages.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
          </select>
          <button className="secondary" onClick={openAccount} type="button">My bookings</button>
          <button onClick={() => openBooking()} type="button">{t('bookNow')}</button>
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
          <Booking
            hotel={hotel}
            onBack={returnHome}
            onSuccess={showConfirmation}
            language={language}
            accessToken={accessToken}
            preferredRoomTypeId={preferredRoomTypeId}
          />
        ) : path === '/account' ? (
          <AccountPage accessToken={accessToken} onAuthenticated={setAccessToken} onReturnHome={returnHome} />
        ) : path === '/verify-account' ? (
          <VerifyAccount onAuthenticated={setAccessToken} onReturnHome={returnHome} />
        ) : path === '/privacy' ? (
          <PrivacyNotice onReturnHome={returnHome} language={language} />
        ) : path === '/payment/stripe/success' ? (
          <StripePaymentReturn onReturnHome={returnHome} />
        ) : /^\/payment\/simulated\/[^/?#]+$/.test(path) ? (
          <SimulatedPayment path={path} onReturnHome={returnHome} />
        ) : /^\/rooms\/[^/?#]+$/.test(path) ? (
          <RoomDetailPage
            hotel={hotel}
            language={language}
            onBook={openBooking}
            onOpenRoom={(roomTypeId) => navigateToPage(`/rooms/${roomTypeId}`)}
            roomType={types.find((type) => type.id === path.split('/')[2]) ?? null}
            roomTypes={types}
          />
        ) : path === '/gallery' ? (
          <GalleryPage hotel={hotel} language={language} />
        ) : path === '/experience' ? (
          <ExperiencePage hotel={hotel} language={language} />
        ) : path === '/contact' ? (
          <ContactPage apiBaseUrl={api} hotel={hotel} language={language} />
        ) : (
          <Landing
            hotel={hotel}
            types={types}
            onBook={() => openBooking()}
            onOpenRoom={(roomTypeId) => navigateToPage(`/rooms/${roomTypeId}`)}
            language={language}
          />
        )}
      </main>
      <SiteFooter
        hotel={hotel}
        language={language}
        onBook={() => openBooking()}
        onNavigate={navigateToPage}
      />
    </>
  );
}

type LandingProps = {
  hotel: Hotel | null;
  types: RoomType[];
  onBook: () => void;
  onOpenRoom: (roomTypeId: string) => void;
  language: PublicLanguage;
};

function Landing({ hotel, types, onBook, onOpenRoom, language }: LandingProps) {
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

      <section className="section intro-section" id="about">
        <div className="intro-copy">
          <p className="eyebrow">{t('stayWithUs')}</p>
          <h2>{t('ourHome')}</h2>
          <p>{t('ourHomeBody')}</p>
          <button onClick={onBook} type="button">{t('exploreRooms')}</button>
        </div>
        <img alt="Traditional Portuguese hotel exterior surrounded by greenery" src={exteriorImage} />
      </section>

      <section className="section room-section" id="rooms">
        <div className="section-heading">
          <div>
            <p className="eyebrow">{t('stayYourWay')}</p>
            <h2>{t('roomsMadeForRest')}</h2>
          </div>
          <button className="secondary" onClick={onBook} type="button">{t('bookNow')}</button>
        </div>
        <div className="room-cards">
          {types.map((type) => (
            <article className="room-card" key={type.id}>
              <span>{t('from')} €{type.basePrice.toFixed(0)} {t('perNight')}</span>
              <h3>{type.name}</h3>
              <p>{t('sleepsUpTo', { count: type.maximumOccupancy })}</p>
              <button className="text-action" onClick={() => onOpenRoom(type.id)} type="button">
                {t('viewRoom')} →
              </button>
            </article>
          ))}
        </div>
      </section>

      <section className="section amenities-section">
        <div className="section-heading section-heading-centered">
          <div>
            <p className="eyebrow">{t('thoughtfulDetails')}</p>
            <h2>{t('thoughtfulDetailsBody')}</h2>
          </div>
        </div>
        <div className="amenities-grid">
          <Amenity icon="✦" title={t('comfortableRooms')} body={t('comfortableRoomsBody')} />
          <Amenity icon="◌" title={t('localBreakfast')} body={t('localBreakfastBody')} />
          <Amenity icon="⌁" title={t('peacefulSetting')} body={t('peacefulSettingBody')} />
          <Amenity icon="↗" title={t('flexibleArrival')} body={t('flexibleArrivalBody')} />
        </div>
      </section>

      <section className="section editorial-section">
        <img alt="Warm, comfortable hotel bedroom" src={roomImage} />
        <div className="editorial-copy">
          <p className="eyebrow">{t('stayWithUs')}</p>
          <h2>{t('yourRoomAwaits')}</h2>
          <p>{t('yourRoomAwaitsBody')}</p>
        </div>
      </section>

      <section className="section experience-section" id="experiences">
        <div>
          <p className="eyebrow">{t('discover')}</p>
          <h2>{t('discoverMiranda')}</h2>
          <p>{t('discoverMirandaBody')}</p>
          <button className="secondary" onClick={onBook} type="button">{t('planYourStay')}</button>
        </div>
        <aside>
          <span>“</span>
          <p>{t('bookDirect')}</p>
          <small>{t('bookDirectBody')}</small>
        </aside>
      </section>
    </>
  );
}

type AmenityProps = {
  icon: string;
  title: string;
  body: string;
};

function Amenity({ icon, title, body }: AmenityProps) {
  return (
    <article className="amenity-card">
      <span aria-hidden="true" className="amenity-icon">{icon}</span>
      <h3>{title}</h3>
      <p>{body}</p>
    </article>
  );
}

type BookingProps = {
  hotel: Hotel | null;
  onBack: () => void;
  onSuccess: (reservation: Reservation) => void;
  language: PublicLanguage;
  accessToken: string | null;
  preferredRoomTypeId: string;
};

function Booking({
  hotel,
  onBack,
  onSuccess,
  language,
  accessToken,
  preferredRoomTypeId,
}: BookingProps) {
  const t = (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);
  const [roomTypeId, setRoomTypeId] = useState('');
  const [checkInDate, setCheckInDate] = useState(dateAfter(1));
  const [checkOutDate, setCheckOutDate] = useState(dateAfter(2));
  const [guestCount, setGuestCount] = useState(1);
  const [availableRoomTypes, setAvailableRoomTypes] = useState<AvailableRoomType[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodAvailability[]>([]);
  const [paymentMethod, setPaymentMethod] = useState('PAY_AT_RECEPTION');
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
              : response.some((roomType) => roomType.roomTypeId === preferredRoomTypeId)
                ? preferredRoomTypeId
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
  }, [checkInDate, checkOutDate, guestCount, hotel, language, preferredRoomTypeId]);

  useEffect(() => {
    if (!hotel) {
      return;
    }

    const hotelId = hotel.id;
    let cancelled = false;

    async function loadPaymentMethods() {
      try {
        const methods = await get<PaymentMethodAvailability[]>(
          `/payment-methods?hotelId=${encodeURIComponent(hotelId)}`,
          language,
        );

        if (!cancelled) {
          setPaymentMethods(methods);
          setPaymentMethod((currentMethod) => methods.some((method) => method.paymentMethod === currentMethod)
            ? currentMethod
            : methods[0]?.paymentMethod ?? '');
        }
      } catch {
        if (!cancelled) {
          setPaymentMethods([]);
        }
      }
    }

    void loadPaymentMethods();

    return () => {
      cancelled = true;
    };
  }, [hotel, language]);

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
        headers: {
          'Content-Type': 'application/json',
          'Accept-Language': language,
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
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
          paymentMode: paymentMethod === 'PAY_AT_RECEPTION' ? 'PAY_AT_RECEPTION' : 'PAY_NOW',
          paymentMethod,
          privacyNoticeAccepted: true,
        }),
      });

      if (response.ok) {
        const reservation = await response.json() as Reservation;
        const guestAccessToken = response.headers.get('X-Guest-Access-Token');

        if (reservation.paymentMode === 'PAY_NOW') {
          if (!guestAccessToken) {
            throw new Error('Guest access token was not returned.');
          }

          const paymentResponse = await fetch(
            `${api}/reservations/guest/${encodeURIComponent(guestAccessToken)}/payment`,
            { method: 'POST' },
          );

          if (!paymentResponse.ok) {
            throw new Error('Payment initiation failed.');
          }

          const paymentAttempt = await paymentResponse.json() as PaymentAttempt;

          if (paymentAttempt.redirectUrl) {
            window.location.assign(paymentAttempt.redirectUrl);
            return;
          }

          throw new Error('Payment provider did not return a checkout URL.');
        }

        onSuccess(reservation);
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
        Payment method
        <select
          disabled={paymentMethods.length === 0}
          onChange={(event) => setPaymentMethod(event.target.value)}
          required
          value={paymentMethod}
        >
          {paymentMethods.map((method) => (
            <option key={method.paymentMethod} value={method.paymentMethod}>
              {formatPaymentMethod(method.paymentMethod)}
            </option>
          ))}
        </select>
      </label>
      {paymentMethod !== 'PAY_AT_RECEPTION' && (
        <p className="availability-summary">
          You will be redirected to a secure payment page to complete your booking.
        </p>
      )}

      <label>
        {t('fullName')}
        <input autoComplete="name" onChange={(event) => setName(event.target.value)} required value={name} />
      </label>

      <label>
        {t('phone')}
        <input autoComplete="tel" onChange={(event) => setPhone(event.target.value)} required type="tel" value={phone} />
      </label>

      <label>
        {t('email')} <span className="optional">({t('recommended')})</span>
        <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} type="email" value={email} />
      </label>

      <label>
        {t('notes')} <span className="optional">({t('optional')})</span>
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

type SimulatedPaymentProps = {
  path: string;
  onReturnHome: () => void;
};

function SimulatedPayment({ path, onReturnHome }: SimulatedPaymentProps) {
  const providerPaymentId = path.split('/').at(-1) ?? '';
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [error, setError] = useState('');
  const [completing, setCompleting] = useState(false);

  async function completePayment() {
    setCompleting(true);
    setError('');

    try {
      const response = await fetch(`${api}/payments/local/${encodeURIComponent(providerPaymentId)}/complete`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error('Payment completion failed.');
      }

      setReservation(await response.json() as Reservation);
    } catch {
      setError('The local payment simulation could not be completed. Please try again.');
    } finally {
      setCompleting(false);
    }
  }

  return (
    <section className="booking-confirmation">
      <p className="eyebrow">Development payment simulator</p>
      <h1>{reservation ? 'Payment complete' : 'Complete your payment'}</h1>
      {reservation ? (
        <>
          <p>Your reservation is confirmed. No real money was collected.</p>
          <dl className="booking-summary">
            <div>
              <dt>Stay</dt>
              <dd>{formatDateRange(reservation.checkInDate, reservation.checkOutDate)}</dd>
            </div>
            <div>
              <dt>Total</dt>
              <dd>{formatCurrency(reservation.totalPrice, reservation.currency)}</dd>
            </div>
          </dl>
          <button onClick={onReturnHome} type="button">Return to hotel</button>
        </>
      ) : (
        <>
          <p>This development-only page emulates a provider-hosted checkout and callback.</p>
          {error && <p className="error">{error}</p>}
          <button disabled={completing} onClick={completePayment} type="button">
            {completing ? 'Completing payment…' : 'Complete simulated payment'}
          </button>
        </>
      )}
    </section>
  );
}

type StripePaymentReturnProps = {
  onReturnHome: () => void;
};

function StripePaymentReturn({ onReturnHome }: StripePaymentReturnProps) {
  return (
    <section className="booking-confirmation">
      <p className="eyebrow">Payment received</p>
      <h1>Thank you</h1>
      <p>
        Stripe has received your payment. We are confirming your reservation now; this can take a moment for some
        payment methods.
      </p>
      <p className="muted">
        If you provided an email address, we will send your booking details and secure access link shortly.
      </p>
      <button onClick={onReturnHome} type="button">Return to hotel</button>
    </section>
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
  const [paymentError, setPaymentError] = useState('');
  const [initiatingPayment, setInitiatingPayment] = useState(false);

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

  async function initiatePayment() {
    setInitiatingPayment(true);
    setPaymentError('');

    try {
      const response = await fetch(`${api}/reservations/guest/${encodeURIComponent(token)}/payment`, {
        method: 'POST',
      });

      if (!response.ok) {
        throw new Error('Payment initiation failed.');
      }

      const paymentAttempt = await response.json() as PaymentAttempt;

      if (!paymentAttempt.redirectUrl) {
        throw new Error('Payment provider did not return a checkout URL.');
      }

      window.location.assign(paymentAttempt.redirectUrl);
    } catch {
      setPaymentError('We could not open the payment page. Please try again or contact the hotel.');
      setInitiatingPayment(false);
    }
  }

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
          <dd>{reservation.paymentStatus === 'SUCCEEDED' || reservation.paymentAttempt?.status === 'SUCCEEDED'
            ? `Paid · ${formatPaymentMethod(reservation.paymentMethod)}`
            : formatPaymentMode(reservation.paymentMode)}</dd>
        </div>
      </dl>
      {reservation.manualConfirmationRequired && (
        <p>Your booking request requires confirmation from the hotel.</p>
      )}
      {reservation.paymentMode === 'PAY_NOW' && reservation.status === 'HELD'
        && reservation.paymentStatus !== 'SUCCEEDED' && reservation.paymentAttempt?.status !== 'SUCCEEDED'
        && !reservation.paymentInstructions && (
          <section className="payment-instructions">
            <p className="eyebrow">Payment required</p>
            <h2>Complete your payment</h2>
            <p>Your reservation is held while payment is completed securely with the selected provider.</p>
            {paymentError && <p className="error">{paymentError}</p>}
            <button disabled={initiatingPayment} onClick={initiatePayment} type="button">
              {initiatingPayment ? 'Opening payment pageâ€¦' : 'Complete payment'}
            </button>
          </section>
        )}
      {reservation.paymentInstructions && (
        <section className="payment-instructions">
          <p className="eyebrow">Multibanco payment details</p>
          <h2>{reservation.status === 'HELD' && reservation.paymentStatus !== 'SUCCEEDED'
            && reservation.paymentAttempt?.status !== 'SUCCEEDED' ? 'Complete your payment' : 'Payment details'}</h2>
          <dl className="booking-summary">
            <div>
              <dt>Entity</dt>
              <dd>{reservation.paymentInstructions.entity}</dd>
            </div>
            <div>
              <dt>Reference</dt>
              <dd>{reservation.paymentInstructions.reference}</dd>
            </div>
            {reservation.paymentInstructions.expiresAt && (
              <div>
                <dt>Expires</dt>
                <dd>{formatDateTime(reservation.paymentInstructions.expiresAt)}</dd>
              </div>
            )}
          </dl>
          {reservation.status === 'HELD' && reservation.paymentStatus !== 'SUCCEEDED'
            && reservation.paymentAttempt?.status !== 'SUCCEEDED' && reservation.paymentInstructions.hostedVoucherUrl && (
            <a
              className="button-link"
              href={reservation.paymentInstructions.hostedVoucherUrl}
              rel="noreferrer"
              target="_blank"
            >
              Open printable voucher
            </a>
          )}
        </section>
      )}
      <p className="muted">To change or cancel this booking, please contact the hotel directly.</p>
      <button onClick={onReturnHome} type="button">{t('returnToHotel')}</button>
    </section>
  );
}

type AccountPageProps = {
  accessToken: string | null;
  onAuthenticated: (accessToken: string | null) => void;
  onReturnHome: () => void;
};

function AccountPage({ accessToken, onAuthenticated, onReturnHome }: AccountPageProps) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirmation, setPasswordConfirmation] = useState('');
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [message, setMessage] = useState('');
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [section, setSection] = useState<'overview' | 'reservations' | 'profile' | 'security'>('overview');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirmation, setNewPasswordConfirmation] = useState('');
  const [profileName, setProfileName] = useState('');

  useEffect(() => {
    if (accessToken) {
      return;
    }

    let cancelled = false;

    async function refreshSession() {
      try {
        const response = await fetch(`${api}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
        });

        if (!response.ok || cancelled) {
          return;
        }

        const tokens = await response.json() as TokenResponse;
        onAuthenticated(tokens.accessToken);
      } catch {
        // A missing or expired refresh cookie simply leaves the visitor signed out.
      }
    }

    void refreshSession();

    return () => {
      cancelled = true;
    };
  }, [accessToken, onAuthenticated]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadAccountData(accessToken);
  }, [accessToken]);

  async function loadAccountData(token: string) {
    await Promise.all([loadReservations(token), loadCurrentUser(token)]);
  }

  async function loadReservations(token: string) {
    try {
      const response = await fetch(`${api}/reservations/my`, {
        headers: { Authorization: `Bearer ${token}` },
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error('Could not load reservations.');
      }

      setReservations(await response.json() as Reservation[]);
    } catch {
      setMessage('We could not load your reservations. Please sign in again.');
    }
  }

  async function loadCurrentUser(token: string) {
    try {
      const response = await fetch(`${api}/users/me`, {
        headers: { Authorization: `Bearer ${token}` },
        credentials: 'include',
      });

      if (!response.ok) {
        throw new Error('Could not load account details.');
      }

      const user = await response.json() as CurrentUser;
      setCurrentUser(user);
      setProfileName(user.fullName ?? '');
    } catch {
      setMessage('We could not load your account details. Please sign in again.');
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setMessage('');

    if (mode === 'register' && password !== passwordConfirmation) {
      setMessage('The passwords do not match. Please enter them again.');
      return;
    }

    const endpoint = mode === 'login' ? '/auth/login' : '/auth/register';
    const response = await fetch(`${api}${endpoint}`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(mode === 'login'
        ? { username: email, password }
        : { fullName, email, password }),
    });

    if (!response.ok) {
      setMessage(mode === 'login'
        ? 'Email address or password is incorrect.'
        : 'We could not create the account. Please review the details and try again.');
      return;
    }

    if (mode === 'register') {
      setMessage('Check your email for the verification link before signing in.');
      return;
    }

    const tokens = await response.json() as TokenResponse;
    onAuthenticated(tokens.accessToken);
  }

  async function changePassword(event: React.FormEvent) {
    event.preventDefault();
    setMessage('');

    if (!accessToken) {
      return;
    }

    if (newPassword !== newPasswordConfirmation) {
      setMessage('The new passwords do not match. Please enter them again.');
      return;
    }

    const response = await fetch(`${api}/users/me/password`, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ currentPassword, newPassword }),
    });

    if (!response.ok) {
      setMessage('Your password could not be changed. Check your current password and try again.');
      return;
    }

    await logout('Your password was updated. Please sign in again.');
  }

  async function updateProfile(event: React.FormEvent) {
    event.preventDefault();
    setMessage('');

    if (!accessToken) {
      return;
    }

    const response = await fetch(`${api}/users/me/profile`, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ fullName: profileName }),
    });

    if (!response.ok) {
      setMessage('Your profile could not be updated. Please check your name and try again.');
      return;
    }

    const user = await response.json() as CurrentUser;
    setCurrentUser(user);
    setProfileName(user.fullName ?? '');
    setMessage('Your profile has been updated.');
  }

  async function logout(nextMessage = 'You have been signed out.') {
    try {
      await fetch(`${api}/auth/logout`, {
        method: 'POST',
        credentials: 'include',
      });
    } finally {
      onAuthenticated(null);
      setReservations([]);
      setCurrentUser(null);
      setSection('overview');
      setMessage(nextMessage);
    }
  }

  if (accessToken) {
    return (
      <section className="account-page">
        <header className="account-header">
          <div>
            <p className="eyebrow">Your account</p>
            <h1>Welcome back{currentUser?.fullName ? `, ${firstName(currentUser.fullName)}` : ''}</h1>
            <p className="account-identity">{currentUser?.username ?? 'Loading your account…'}</p>
          </div>
          <span aria-hidden="true" className="account-avatar">
            {(currentUser?.fullName ?? currentUser?.username ?? 'A').charAt(0).toUpperCase()}
          </span>
        </header>

        <nav aria-label="Account navigation" className="account-menu" role="tablist">
          <button aria-selected={section === 'overview'} className={section === 'overview' ? 'selected' : ''} onClick={() => setSection('overview')} role="tab" type="button">
            Overview
          </button>
          <button aria-selected={section === 'reservations'} className={section === 'reservations' ? 'selected' : ''} onClick={() => setSection('reservations')} role="tab" type="button">
            My reservations
          </button>
          <button aria-selected={section === 'profile'} className={section === 'profile' ? 'selected' : ''} onClick={() => setSection('profile')} role="tab" type="button">
            Profile
          </button>
          <button aria-selected={section === 'security'} className={section === 'security' ? 'selected' : ''} onClick={() => setSection('security')} role="tab" type="button">
            Password and security
          </button>
        </nav>

        <div className="account-workspace">
          {section === 'overview' && (
            <div className="account-section">
              <p className="eyebrow">At a glance</p>
              <h2>Account overview</h2>
              <p className="account-section-intro">View your stays and keep your account details secure.</p>
              <dl className="account-summary account-summary-overview">
                <div>
                  <dt>Reservations</dt>
                  <dd className="account-stat">{reservations.length}</dd>
                  <small>Linked to this account</small>
                </div>
                <div>
                  <dt>Account status</dt>
                  <dd><span className="account-status">{currentUser?.enabled ? 'Verified' : 'Pending'}</span></dd>
                  <small>{currentUser?.enabled ? 'Your email is verified' : 'Email verification required'}</small>
                </div>
                <div className="account-summary-wide">
                  <dt>Email address</dt>
                  <dd>{currentUser?.username ?? 'Loading…'}</dd>
                </div>
              </dl>
            </div>
          )}

          {section === 'reservations' && (
            <div className="account-section">
              <p className="eyebrow">Your stays</p>
              <h2>My reservations</h2>
              {reservations.length === 0 ? (
                <div className="account-empty-state">
                  <h3>No reservations yet</h3>
                  <p>Bookings made while signed in will appear here.</p>
                  <button onClick={onReturnHome} type="button">Explore the hotel</button>
                </div>
              ) : (
                <div className="account-reservations">
                  {reservations.map((reservation) => (
                    <article key={reservation.id}>
                      <strong>{formatDateRange(reservation.checkInDate, reservation.checkOutDate)}</strong>
                      <span>{formatReservationStatus(reservation.status)}</span>
                      <span>{formatCurrency(reservation.totalPrice, reservation.currency)}</span>
                    </article>
                  ))}
                </div>
              )}
            </div>
          )}

          {section === 'profile' && (
            <form className="account-section account-form" onSubmit={updateProfile}>
              <p className="eyebrow">Personal details</p>
              <h2>Profile</h2>
              <p className="account-section-intro">Keep the personal details associated with your account up to date.</p>
              <label>
                Full name
                <input autoComplete="name" maxLength={120} onChange={(event) => setProfileName(event.target.value)} required value={profileName} />
              </label>
              <dl className="account-profile-details">
                <div>
                  <dt>Email address</dt>
                  <dd>{currentUser?.username ?? 'Loading…'}</dd>
                </div>
              </dl>
              <button>Save profile</button>
            </form>
          )}

          {section === 'security' && (
            <form className="account-section account-form" onSubmit={changePassword}>
              <p className="eyebrow">Sign-in security</p>
              <h2>Change password</h2>
              <p className="account-section-intro">Changing your password signs you out on this device and any other active sessions.</p>
            <label>
              Current password
              <input autoComplete="current-password" onChange={(event) => setCurrentPassword(event.target.value)} required type="password" value={currentPassword} />
            </label>
            <label>
              New password
              <input autoComplete="new-password" minLength={10} onChange={(event) => setNewPassword(event.target.value)} required type="password" value={newPassword} />
            </label>
            <label>
              Confirm new password
              <input autoComplete="new-password" minLength={10} onChange={(event) => setNewPasswordConfirmation(event.target.value)} required type="password" value={newPasswordConfirmation} />
            </label>
              <button>Update password</button>
            </form>
          )}

          {message && <p className="error">{message}</p>}
        </div>

        <footer className="account-actions">
          <button className="secondary" onClick={onReturnHome} type="button">Return to hotel</button>
          <button className="account-sign-out" onClick={() => { void logout(); }} type="button">Sign out</button>
        </footer>
      </section>
    );
  }

  return (
    <form className="booking-card" onSubmit={submit}>
      <button className="link" onClick={onReturnHome} type="button">Back to hotel</button>
      <p className="eyebrow">Your account</p>
      <h2>{mode === 'login' ? 'Sign in' : 'Create an account'}</h2>
      <p>
        {mode === 'login'
          ? 'Sign in to see and manage reservations made while you were signed in.'
          : 'Creating an account is optional. We will send an email to verify it before it becomes active.'}
      </p>
      {mode === 'register' && (
        <label>
          Full name
          <input autoComplete="name" maxLength={120} onChange={(event) => setFullName(event.target.value)} required value={fullName} />
        </label>
      )}
      <label>
        Email address
        <input autoComplete="email" onChange={(event) => setEmail(event.target.value)} required type="email" value={email} />
      </label>
      <label>
        Password
        <input autoComplete={mode === 'login' ? 'current-password' : 'new-password'} minLength={10}
          onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
      </label>
      {mode === 'register' && (
        <label>
          Confirm password
          <input
            autoComplete="new-password"
            minLength={10}
            onChange={(event) => setPasswordConfirmation(event.target.value)}
            required
            type="password"
            value={passwordConfirmation}
          />
        </label>
      )}
      {message && <p className={message.startsWith('Check') || message.startsWith('Your password') || message.startsWith('You have') ? 'success' : 'error'}>{message}</p>}
      <button>{mode === 'login' ? 'Sign in' : 'Create account'}</button>
      <button
        className="link"
        onClick={() => {
          setMode(mode === 'login' ? 'register' : 'login');
          setPasswordConfirmation('');
          setMessage('');
        }}
        type="button"
      >
        {mode === 'login' ? 'Create an optional account' : 'I already have an account'}
      </button>
    </form>
  );
}

type VerifyAccountProps = {
  onAuthenticated: (accessToken: string | null) => void;
  onReturnHome: () => void;
};

function VerifyAccount({ onAuthenticated, onReturnHome }: VerifyAccountProps) {
  const [message, setMessage] = useState('Verifying your email address…');
  const [verified, setVerified] = useState(false);

  useEffect(() => {
    const token = new URLSearchParams(window.location.hash.split('?')[1] ?? '').get('token');

    if (!token) {
      setMessage('This verification link is incomplete.');
      return;
    }

    async function verify() {
      try {
        const response = await fetch(`${api}/auth/verify-email`, {
          method: 'POST',
          credentials: 'include',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ token }),
        });

        if (!response.ok) {
          throw new Error('Verification failed.');
        }

        const tokens = await response.json() as TokenResponse;
        onAuthenticated(tokens.accessToken);
        setVerified(true);
        setMessage('Your email address is verified and your account is ready to use.');
      } catch {
        setMessage('This verification link is invalid or has expired. Please create the account again.');
      }
    }

    void verify();
  }, [onAuthenticated]);

  return (
    <section className="booking-confirmation">
      <p className="eyebrow">Account verification</p>
      <h1>{verified ? 'Your account is ready' : 'Verify your account'}</h1>
      <p>{message}</p>
      {verified && (
        <button onClick={() => { window.location.hash = '#/account'; }} type="button">
          View my reservations
        </button>
      )}
      <button onClick={onReturnHome} type="button">Return to hotel</button>
    </section>
  );
}

type TokenResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

type CurrentUser = {
  username: string;
  fullName: string | null;
  enabled: boolean;
  roles: string[];
};

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

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value));
}

function formatReservationStatus(status: string) {
  return status.toLowerCase().replaceAll('_', ' ');
}

function firstName(fullName: string) {
  return fullName.trim().split(/\s+/)[0];
}

function formatPaymentMode(paymentMode: string) {
  return paymentMode.toLowerCase().replaceAll('_', ' ');
}

function formatPaymentMethod(paymentMethod: string) {
  const labels: Record<string, string> = {
    PAY_AT_RECEPTION: 'Pay at reception',
    CARD: 'Credit or debit card',
    PAYPAL: 'PayPal',
    MULTIBANCO: 'Multibanco',
    MB_WAY: 'MB WAY',
  };

  return labels[paymentMethod] ?? formatPaymentMode(paymentMethod);
}

function dateAfter(days: number) {
  return new Date(Date.now() + days * 86_400_000).toISOString().slice(0, 10);
}

function readPath() {
  const hash = window.location.hash.replace(/^#/, '');
  return hash.split('?')[0] || '/';
}

function readLanguage(): PublicLanguage {
  return localStorage.getItem('hotel-booking.public-language') === 'pt' ? 'pt' : 'en';
}

function readGuestAccessToken(path: string) {
  const match = /^\/booking\/([^/?#]+)$/.exec(path);

  return match ? decodeURIComponent(match[1]) : null;
}
