export type PublicLanguage = 'en' | 'pt';

export const publicLanguages: { code: PublicLanguage; label: string }[] = [
  { code: 'en', label: 'English' },
  { code: 'pt', label: 'Português' },
];

const messages = {
  en: {
    bookNow: 'Book now',
    reserveStay: 'Reserve your stay',
    welcome: 'Welcome to Portugal',
    stayYourWay: 'Stay your way',
    roomsMadeForRest: 'Rooms made for rest',
    from: 'From',
    perNight: '/ night',
    sleepsUpTo: 'Comfortably sleeps up to {count} guests.',
    bookYourStay: 'Book your stay',
    backToHotel: 'Back to hotel',
    checkIn: 'Check-in',
    checkOut: 'Check-out',
    guests: 'Guests',
    availableRoomType: 'Available room type',
    fullName: 'Full name',
    phone: 'Phone',
    email: 'Email',
    recommended: '(recommended)',
    notes: 'Notes',
    optional: '(optional)',
    privacyNotice: 'I have read the privacy notice and agree to the processing of my data to manage this booking.',
    requestBooking: 'Request booking',
    sendingRequest: 'Sending request…',
    privacy: 'Privacy',
    privacyTitle: 'Privacy notice',
    privacyBody: 'We use the information you provide only to process your booking, communicate about your stay, and meet legal obligations. Contact the hotel to request access, correction, or deletion where applicable.',
    bookingIntro: 'Choose your dates for {hotelName} and see the available options.',
    bookingIntroGeneric: 'Choose your dates and see available options.',
    guest: 'guest',
    guestsPlural: 'guests',
    checkingAvailability: 'Checking availability...',
    chooseRoomType: 'Choose a room type',
    noRoomTypes: 'No available room type for these dates',
    requestReceived: 'Request received',
    bookingThankYou: 'Thank you for your booking request.',
    returnToHotel: 'Return to hotel',
  },
  pt: {
    bookNow: 'Reservar',
    reserveStay: 'Reserve a sua estadia',
    welcome: 'Bem-vindo a Portugal',
    stayYourWay: 'A sua estadia',
    roomsMadeForRest: 'Quartos para descansar',
    from: 'Desde',
    perNight: '/ noite',
    sleepsUpTo: 'Acomoda confortavelmente até {count} hóspedes.',
    bookYourStay: 'Reserve a sua estadia',
    backToHotel: 'Voltar ao hotel',
    checkIn: 'Check-in',
    checkOut: 'Check-out',
    guests: 'Hóspedes',
    availableRoomType: 'Tipo de quarto disponível',
    fullName: 'Nome completo',
    phone: 'Telefone',
    email: 'Email',
    recommended: '(recomendado)',
    notes: 'Notas',
    optional: '(opcional)',
    privacyNotice: 'Li o aviso de privacidade e concordo com o tratamento dos meus dados para gerir esta reserva.',
    requestBooking: 'Pedir reserva',
    sendingRequest: 'A enviar pedido…',
    privacy: 'Privacidade',
    privacyTitle: 'Aviso de privacidade',
    privacyBody: 'Utilizamos os dados fornecidos apenas para tratar a sua reserva, comunicar sobre a estadia e cumprir obrigações legais. Contacte o hotel para pedir acesso, correção ou eliminação, quando aplicável.',
    bookingIntro: 'Escolha as suas datas para {hotelName} e veja as opções disponíveis.',
    bookingIntroGeneric: 'Escolha as suas datas e veja as opções disponíveis.',
    guest: 'hóspede',
    guestsPlural: 'hóspedes',
    checkingAvailability: 'A verificar disponibilidade...',
    chooseRoomType: 'Escolha um tipo de quarto',
    noRoomTypes: 'Não há quartos disponíveis para estas datas',
    requestReceived: 'Pedido recebido',
    bookingThankYou: 'Obrigado pelo seu pedido de reserva.',
    returnToHotel: 'Voltar ao hotel',
  },
} as const;

export function translate(language: PublicLanguage, key: keyof typeof messages.en, values?: Record<string, string | number>) {
  let value = messages[language][key] as string;

  Object.entries(values ?? {}).forEach(([name, replacement]) => {
    value = value.replace(`{${name}}`, String(replacement));
  });

  return value;
}
