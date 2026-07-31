import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { Hotel } from './api/hotelApi';
import { findHotels } from './api/hotelApi';
import { useAuth } from './auth/useAuth';
import { AppLayout } from './components/AppLayout';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { ReservationsPage } from './pages/ReservationsPage';
import { RoomsPage } from './pages/RoomsPage';
import { RoomTypesPage } from './pages/RoomTypesPage';
import { RatesPage } from './pages/RatesPage';
import { DiscountCodesPage } from './pages/DiscountCodesPage';
import { HotelSettingsPage } from './pages/HotelSettingsPage';
import { UsersPage } from './pages/UsersPage';

function App() {
  const { session } = useAuth();
  const [path, setPath] = useState(readPath);
  const [selectedHotelId, setSelectedHotelId] = useState('');
  const [hotels, setHotels] = useState<Hotel[]>([]);

  useEffect(() => {
    setSelectedHotelId(session?.claims.hotelIds[0] ?? '');
  }, [session]);

  useEffect(() => {
    if (!session) {
      setHotels([]);
      return;
    }

    void findHotels(session.accessToken)
      .then(setHotels)
      .catch(() => setHotels([]));
  }, [session]);

  useEffect(() => {
    const onHashChange = () => setPath(readPath());

    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  if (!session) {
    return <LoginPage />;
  }

  const pages: Record<string, ReactNode> = {
    '/': <DashboardPage hasAssignedHotel={Boolean(selectedHotelId)} />,
    '/reservations': <ReservationsPage hotelId={selectedHotelId} />,
    '/rooms': <RoomsPage hotelId={selectedHotelId} />,
    '/room-types': <RoomTypesPage hotelId={selectedHotelId} />,
    '/rates': <RatesPage hotelId={selectedHotelId} />,
    '/discount-codes': <DiscountCodesPage hotelId={selectedHotelId} />,
    '/hotel-settings': <HotelSettingsPage hotelId={selectedHotelId} />,
    '/users': <UsersPage selectedHotelId={selectedHotelId} />,
  };

  return (
    <AppLayout
      selectedHotelId={selectedHotelId}
      onHotelChange={setSelectedHotelId}
      hotels={hotels}
    >
      {pages[path] ?? <DashboardPage hasAssignedHotel={Boolean(selectedHotelId)} />}
    </AppLayout>
  );
}

function readPath() {
  const path = window.location.hash.replace(/^#/, '');

  return path || '/';
}

export default App;
