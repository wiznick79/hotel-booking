import { useMemo } from 'react';
import type { ReactNode } from 'react';
import type { Hotel } from '../api/hotelApi';
import { useAuth } from '../auth/useAuth';
import { adminLanguages, translateAdmin, type AdminLanguage } from '../i18n';

type AppLayoutProps = {
  children: ReactNode;
  selectedHotelId: string;
  onHotelChange: (hotelId: string) => void;
  hotels: Hotel[];
  language: AdminLanguage;
  onLanguageChange: (language: AdminLanguage) => void;
};

type NavigationItem = {
  labelKey: Parameters<typeof translateAdmin>[1];
  to: string;
  permission?: string;
};

const navigationItems: NavigationItem[] = [
  { labelKey: 'dashboard', to: '/' },
  { labelKey: 'reservations', to: '/reservations', permission: 'RESERVATION_READ' },
  { labelKey: 'contactMessages', to: '/contact-messages', permission: 'NOTIFICATION_MANAGE' },
  { labelKey: 'rooms', to: '/rooms', permission: 'ROOM_READ' },
  { labelKey: 'roomTypes', to: '/room-types', permission: 'ROOM_TYPE_MANAGE' },
  { labelKey: 'rates', to: '/rates', permission: 'RATE_PERIOD_MANAGE' },
  { labelKey: 'discountCodes', to: '/discount-codes', permission: 'DISCOUNT_CODE_MANAGE' },
  { labelKey: 'hotelSettings', to: '/hotel-settings', permission: 'HOTEL_MANAGE' },
  { labelKey: 'staff', to: '/staff', permission: 'STAFF_MANAGE' },
  { labelKey: 'customers', to: '/customers', permission: 'USER_MANAGE' },
];

export function AppLayout({ children, selectedHotelId, onHotelChange, hotels, language, onLanguageChange }: AppLayoutProps) {
  const { logout, session } = useAuth();
  const t = (key: Parameters<typeof translateAdmin>[1]) => translateAdmin(language, key);

  const visibleItems = useMemo(
    () => navigationItems.filter((item) => !item.permission || session?.claims.permissions.includes(item.permission)),
    [session],
  );

  function handleLogout() {
    logout();
    window.location.hash = '#/';
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">HB</span>
          <span>Hotel Booking</span>
        </div>

        <nav aria-label="Main navigation">
          {visibleItems.map((item) => (
            <a key={item.to} href={`#${item.to}`}>{t(item.labelKey)}</a>
          ))}
        </nav>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <label>
            <span className="visually-hidden">{t('selectedHotel')}</span>
            <select value={selectedHotelId} onChange={(event) => onHotelChange(event.target.value)}>
              {session?.claims.hotelIds.length === 0 && <option value="">{t('noHotelAssigned')}</option>}
              {session?.claims.hotelIds.map((hotelId) => {
                const hotel = hotels.find((candidate) => candidate.id === hotelId);

                return (
                  <option key={hotelId} value={hotelId}>
                    {hotel?.name ?? `Hotel ${hotelId.slice(0, 8)}`}
                  </option>
                );
              })}
            </select>
          </label>

          <div className="user-menu">
            <select aria-label="Language" onChange={(event) => onLanguageChange(event.target.value as AdminLanguage)} value={language}>
              {adminLanguages.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
            </select>
            <span>{session?.claims.sub}</span>
            <a className="text-button" href="#/change-password">{t('changePassword')}</a>
            <button type="button" className="text-button" onClick={handleLogout}>{t('logOut')}</button>
          </div>
        </header>

        <main data-selected-hotel-id={selectedHotelId}>{children}</main>
      </div>
    </div>
  );
}
