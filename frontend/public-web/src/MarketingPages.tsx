import { useMemo, useState } from 'react';
import breakfastImage from './assets/hotel-breakfast-gallery.png';
import exteriorImage from './assets/hotel-exterior-gallery.png';
import heroImage from './assets/hotel-hero.png';
import roomImage from './assets/hotel-room-gallery.png';
import suiteImage from './assets/hotel-suite-gallery.png';
import landscapeImage from './assets/miranda-douro-gallery.png';
import { translate, type PublicLanguage } from './i18n';

export type PublicHotel = {
  id: string;
  name: string;
  description: string;
  address: string;
  city: string;
  country: string;
  notificationReplyToAddress: string | null;
};

export type PublicRoomType = {
  id: string;
  hotelId: string;
  name: string;
  description?: string | null;
  maximumOccupancy: number;
  basePrice: number;
  active: boolean;
};

type PageProps = {
  hotel: PublicHotel | null;
  language: PublicLanguage;
};

type RoomDetailPageProps = PageProps & {
  roomType: PublicRoomType | null;
  roomTypes: PublicRoomType[];
  onBook: (roomTypeId: string) => void;
  onOpenRoom: (roomTypeId: string) => void;
};

export function RoomDetailPage({
  hotel,
  language,
  roomType,
  roomTypes,
  onBook,
  onOpenRoom,
}: RoomDetailPageProps) {
  const t = translator(language);
  const roomIndex = Math.max(0, roomTypes.findIndex((candidate) => candidate.id === roomType?.id));
  const image = roomImageFor(roomIndex);

  if (!roomType) {
    return (
      <section className="page-shell empty-page">
        <p className="eyebrow">{t('rooms')}</p>
        <h1>{t('noRoomTypes')}</h1>
      </section>
    );
  }

  const alternatives = roomTypes.filter((candidate) => candidate.id !== roomType.id).slice(0, 3);

  return (
    <div className="marketing-page">
      <section className="room-detail-hero">
        <img alt={roomType.name} src={image} />
        <div>
          <p className="eyebrow">{hotel?.name} · {t('roomDetails')}</p>
          <h1>{roomType.name}</h1>
          <p className="room-lead">
            {roomType.description || t('yourRoomAwaitsBody')}
          </p>
          <div className="room-facts">
            <span>{t('sleepsUpTo', { count: roomType.maximumOccupancy })}</span>
            <strong>{t('from')} €{roomType.basePrice.toFixed(0)} {t('perNight')}</strong>
          </div>
          <button onClick={() => onBook(roomType.id)} type="button">{t('bookThisRoom')}</button>
        </div>
      </section>

      <section className="section room-highlight-section">
        <div className="section-heading">
          <div>
            <p className="eyebrow">{t('roomHighlights')}</p>
            <h2>{t('thoughtfulDetails')}</h2>
          </div>
        </div>
        <div className="room-highlight-grid">
          <Feature number="01" title={t('restfulSleep')} body={t('restfulSleepBody')} />
          <Feature number="02" title={t('privateComfort')} body={t('privateComfortBody')} />
          <Feature number="03" title={t('countrysideView')} body={t('countrysideViewBody')} />
        </div>
      </section>

      <section className="section room-gallery-band">
        <img alt="Hotel room detail" src={roomImage} />
        <img alt="Hotel suite detail" src={suiteImage} />
      </section>

      {alternatives.length > 0 && (
        <section className="section alternative-rooms">
          <p className="eyebrow">{t('otherRooms')}</p>
          <div className="room-cards room-cards-visual">
            {alternatives.map((candidate, index) => (
              <article className="room-card" key={candidate.id}>
                <img alt={candidate.name} src={roomImageFor(index + 1)} />
                <div>
                  <span>{t('from')} €{candidate.basePrice.toFixed(0)} {t('perNight')}</span>
                  <h3>{candidate.name}</h3>
                  <button className="text-action" onClick={() => onOpenRoom(candidate.id)} type="button">
                    {t('viewRoom')} →
                  </button>
                </div>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}

export function GalleryPage({ language }: PageProps) {
  const t = translator(language);

  return (
    <div className="marketing-page">
      <section className="page-intro">
        <p className="eyebrow">{t('gallery')}</p>
        <h1>{t('photoGallery')}</h1>
        <p>{t('photoGalleryBody')}</p>
      </section>
      <section className="editorial-gallery" aria-label={t('gallery')}>
        <figure className="gallery-wide">
          <img alt="Hotel exterior" src={exteriorImage} />
        </figure>
        <figure><img alt="Warm hotel bedroom" src={roomImage} /></figure>
        <figure><img alt="Hotel suite" src={suiteImage} /></figure>
        <figure><img alt="Regional Portuguese breakfast" src={breakfastImage} /></figure>
        <figure className="gallery-wide">
          <img alt="Miranda do Douro landscape" src={landscapeImage} />
        </figure>
      </section>
    </div>
  );
}

export function ExperiencePage({ language }: PageProps) {
  const t = translator(language);

  return (
    <div className="marketing-page">
      <section
        className="experience-hero"
        style={{ backgroundImage: `linear-gradient(90deg, #11241ab5, #11241a25), url(${landscapeImage})` }}
      >
        <div>
          <p className="eyebrow">{t('explore')}</p>
          <h1>{t('localExperiences')}</h1>
          <p>{t('localExperiencesBody')}</p>
        </div>
      </section>
      <section className="section story-grid">
        <Story image={exteriorImage} label="01" title={t('historicMiranda')} body={t('historicMirandaBody')} />
        <Story image={landscapeImage} label="02" title={t('douroLandscape')} body={t('douroLandscapeBody')} />
        <Story image={breakfastImage} label="03" title={t('regionalFlavours')} body={t('regionalFlavoursBody')} />
      </section>
    </div>
  );
}

type ContactPageProps = PageProps & {
  apiBaseUrl: string;
};

export function ContactPage({ hotel, language, apiBaseUrl }: ContactPageProps) {
  const t = translator(language);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [subject, setSubject] = useState('GENERAL');
  const [message, setMessage] = useState('');
  const [consent, setConsent] = useState(false);
  const [website, setWebsite] = useState('');
  const [status, setStatus] = useState<'idle' | 'sending' | 'sent' | 'failed'>('idle');
  const mapQuery = useMemo(
    () => [hotel?.address, hotel?.city, hotel?.country].filter(Boolean).join(', '),
    [hotel],
  );

  async function submit(event: React.FormEvent) {
    event.preventDefault();

    if (!hotel) {
      setStatus('failed');
      return;
    }

    setStatus('sending');

    try {
      const response = await fetch(`${apiBaseUrl}/contact-messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept-Language': language,
        },
        body: JSON.stringify({
          hotelId: hotel.id,
          name,
          email,
          phone: phone || null,
          subject,
          message,
          privacyNoticeAccepted: consent,
          website,
        }),
      });

      if (!response.ok) {
        throw new Error('Contact request failed.');
      }

      setStatus('sent');
      setName('');
      setEmail('');
      setPhone('');
      setMessage('');
      setConsent(false);
    } catch {
      setStatus('failed');
    }
  }

  return (
    <div className="marketing-page">
      <section className="page-intro contact-intro">
        <p className="eyebrow">{t('contact')}</p>
        <h1>{t('contactUs')}</h1>
        <p>{t('contactIntro')}</p>
      </section>

      <section className="contact-grid">
        <div className="contact-map-card">
          <iframe
            allowFullScreen
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            src={`https://www.google.com/maps?q=${encodeURIComponent(mapQuery)}&output=embed`}
            title={t('findUs')}
          />
          <div>
            <p className="eyebrow">{t('findUs')}</p>
            <h2>{hotel?.name}</h2>
            <address>
              {hotel?.address}<br />
              {hotel?.city}, {hotel?.country}
            </address>
            {hotel?.notificationReplyToAddress && (
              <a href={`mailto:${hotel.notificationReplyToAddress}`}>{hotel.notificationReplyToAddress}</a>
            )}
          </div>
        </div>

        <form className="contact-form" onSubmit={submit}>
          <p className="eyebrow">{t('sendMessage')}</p>
          <div className="form-grid">
            <label>{t('fullName')}<input maxLength={120} onChange={(event) => setName(event.target.value)} required value={name} /></label>
            <label>
              {t('email')}
              <input
                maxLength={180}
                onChange={(event) => setEmail(event.target.value)}
                pattern="[^ @]+@[^ @]+[.][^ @]{2,}"
                required
                type="email"
                value={email}
              />
            </label>
            <label>
              {t('phone')} <span>({t('optional')})</span>
              <input
                inputMode="tel"
                maxLength={40}
                onChange={(event) => setPhone(event.target.value)}
                pattern={'[+0-9. \\(\\)\\-]*'}
                value={phone}
              />
            </label>
            <label>{t('subject')}
              <select onChange={(event) => setSubject(event.target.value)} value={subject}>
                <option value="GENERAL">{t('generalEnquiry')}</option>
                <option value="BOOKING">{t('bookingQuestion')}</option>
                <option value="ACCESSIBILITY">{t('accessibilityQuestion')}</option>
              </select>
            </label>
          </div>
          <label>{t('message')}<textarea maxLength={3000} minLength={10} onChange={(event) => setMessage(event.target.value)} required rows={7} value={message} /></label>
          <label className="honeypot" aria-hidden="true">Website<input autoComplete="off" onChange={(event) => setWebsite(event.target.value)} tabIndex={-1} value={website} /></label>
          <label className="consent-row"><input checked={consent} onChange={(event) => setConsent(event.target.checked)} required type="checkbox" />{t('contactConsent')}</label>
          {status === 'sent' && <p className="success">{t('messageSent')}</p>}
          {status === 'failed' && <p className="error">{t('messageFailed')}</p>}
          <button disabled={status === 'sending'} type="submit">{status === 'sending' ? t('sending') : t('send')}</button>
        </form>
      </section>
    </div>
  );
}

type SiteFooterProps = PageProps & {
  onNavigate: (path: string) => void;
  onBook: () => void;
};

export function SiteFooter({ hotel, language, onNavigate, onBook }: SiteFooterProps) {
  const t = translator(language);

  return (
    <footer className="site-footer">
      <div className="footer-brand">
        <button onClick={() => onNavigate('/')} type="button">{hotel?.name ?? 'Hotel Booking'}</button>
        <p>{t('footerCopy')}</p>
        <address>{hotel?.address}<br />{hotel?.city}, {hotel?.country}</address>
      </div>
      <div>
        <h2>{t('stay')}</h2>
        <button onClick={() => onNavigate('/#rooms')} type="button">{t('rooms')}</button>
        <button onClick={onBook} type="button">{t('bookNow')}</button>
      </div>
      <div>
        <h2>{t('explore')}</h2>
        <button onClick={() => onNavigate('/gallery')} type="button">{t('gallery')}</button>
        <button onClick={() => onNavigate('/experience')} type="button">{t('experiences')}</button>
        <button onClick={() => onNavigate('/contact')} type="button">{t('contact')}</button>
      </div>
      <div>
        <h2>{t('legal')}</h2>
        <button onClick={() => onNavigate('/privacy')} type="button">{t('privacy')}</button>
        {hotel?.notificationReplyToAddress && (
          <a href={`mailto:${hotel.notificationReplyToAddress}`}>{hotel.notificationReplyToAddress}</a>
        )}
      </div>
      <p className="footer-bottom">© {new Date().getFullYear()} {hotel?.name}. {t('rightsReserved')}</p>
    </footer>
  );
}

function Feature({ number, title, body }: { number: string; title: string; body: string }) {
  return <article><span>{number}</span><h3>{title}</h3><p>{body}</p></article>;
}

function Story({ image, label, title, body }: { image: string; label: string; title: string; body: string }) {
  return <article><img alt={title} src={image} /><div><span>{label}</span><h2>{title}</h2><p>{body}</p></div></article>;
}

function roomImageFor(index: number) {
  return [roomImage, suiteImage, heroImage][index % 3];
}

function translator(language: PublicLanguage) {
  return (key: Parameters<typeof translate>[1], values?: Record<string, string | number>) =>
    translate(language, key, values);
}
