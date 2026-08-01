import { useMemo } from 'react';
import type { ReactNode } from 'react';
import type { Hotel } from '../api/hotelApi';
import { useAuth } from '../auth/useAuth';

type AppLayoutProps = {
  children: ReactNode;
  selectedHotelId: string;
  onHotelChange: (hotelId: string) => void;
  hotels: Hotel[];
};

type NavigationItem = {
  label: string;
  to: string;
  permission?: string;
};

const navigationItems: NavigationItem[] = [
  { label: 'Dashboard', to: '/' },
  { label: 'Reservations', to: '/reservations', permission: 'RESERVATION_READ' },
  { label: 'Rooms', to: '/rooms', permission: 'ROOM_READ' },
  { label: 'Room types', to: '/room-types', permission: 'ROOM_TYPE_MANAGE' },
  { label: 'Rates', to: '/rates', permission: 'RATE_PERIOD_MANAGE' },
  { label: 'Discount codes', to: '/discount-codes', permission: 'DISCOUNT_CODE_MANAGE' },
  { label: 'Hotel settings', to: '/hotel-settings', permission: 'HOTEL_MANAGE' },
  { label: 'Users', to: '/users', permission: 'STAFF_MANAGE' },
];

export function AppLayout({ children, selectedHotelId, onHotelChange, hotels }: AppLayoutProps) {
  const { logout, session } = useAuth();

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
            <a key={item.to} href={`#${item.to}`}>{item.label}</a>
          ))}
        </nav>
      </aside>

      <div className="main-content">
        <header className="topbar">
          <label>
            <span className="visually-hidden">Selected hotel</span>
            <select value={selectedHotelId} onChange={(event) => onHotelChange(event.target.value)}>
              {session?.claims.hotelIds.length === 0 && <option value="">No hotel assigned</option>}
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
            <span>{session?.claims.sub}</span>
            <a className="text-button" href="#/change-password">Change password</a>
            <button type="button" className="text-button" onClick={handleLogout}>Log out</button>
          </div>
        </header>

        <main data-selected-hotel-id={selectedHotelId}>{children}</main>
      </div>
    </div>
  );
}
