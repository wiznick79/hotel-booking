import { get, patchWithoutBody } from './httpClient';

export type ContactMessage = {
  id: string;
  referenceId: string;
  hotelId: string;
  name: string;
  email: string;
  phone: string | null;
  subject: string;
  message: string;
  deliveryStatus: 'PENDING' | 'SENT' | 'FAILED';
  receivedAt: string;
  readAt: string | null;
};

export type ContactMessagePage = {
  content: ContactMessage[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export function findContactMessages(
  accessToken: string,
  hotelId: string,
  page: number,
  size = 20,
) {
  return get<ContactMessagePage>(
    '/contact-messages/admin',
    accessToken,
    new URLSearchParams({
      hotelId,
      page: String(page),
      size: String(size),
    }),
  );
}

export function markContactMessageRead(accessToken: string, messageId: string) {
  return patchWithoutBody(`/contact-messages/admin/${messageId}/read`, accessToken);
}
