export type AdminLanguage = 'en' | 'pt';

export const adminLanguages = [
  { code: 'en', label: 'English' },
  { code: 'pt', label: 'Português' },
] as const;

const messages = {
  en: {
    dashboard: 'Dashboard',
    reservations: 'Reservations',
    rooms: 'Rooms',
    roomTypes: 'Room types',
    rates: 'Rates',
    discountCodes: 'Discount codes',
    hotelSettings: 'Hotel settings',
    users: 'Users',
    selectedHotel: 'Selected hotel',
    noHotelAssigned: 'No hotel assigned',
    changePassword: 'Change password',
    logOut: 'Log out',
    managementPortal: 'Management portal',
    username: 'Username',
    password: 'Password',
    signingIn: 'Signing in...',
    signIn: 'Sign in',
    invalidCredentials: 'Invalid username or password.',
    signInFailed: 'We could not sign you in. Please try again.',
  },
  pt: {
    dashboard: 'Painel',
    reservations: 'Reservas',
    rooms: 'Quartos',
    roomTypes: 'Tipos de quarto',
    rates: 'Tarifas',
    discountCodes: 'Códigos de desconto',
    hotelSettings: 'Definições do hotel',
    users: 'Utilizadores',
    selectedHotel: 'Hotel selecionado',
    noHotelAssigned: 'Nenhum hotel atribuído',
    changePassword: 'Alterar palavra-passe',
    logOut: 'Terminar sessão',
    managementPortal: 'Portal de gestão',
    username: 'Nome de utilizador',
    password: 'Palavra-passe',
    signingIn: 'A iniciar sessão...',
    signIn: 'Iniciar sessão',
    invalidCredentials: 'Nome de utilizador ou palavra-passe inválidos.',
    signInFailed: 'Não foi possível iniciar sessão. Tente novamente.',
  },
} as const;

export function translateAdmin(language: AdminLanguage, key: keyof typeof messages.en) {
  return messages[language][key];
}

export function readAdminLanguage(): AdminLanguage {
  return localStorage.getItem('hotel-booking.admin-language') === 'pt' ? 'pt' : 'en';
}
