import { ApiError } from './httpClient';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

export type WebsiteMediaUsage = 'HERO' | 'HOTEL_GALLERY' | 'ROOM_TYPE_GALLERY';
export type WebsiteMedia = {
  id: string; hotelId: string; roomTypeId: string | null; usage: WebsiteMediaUsage;
  sortOrder: number; altText: string | null; originalFilename: string;
  contentType: string; sizeBytes: number; url: string;
};

export async function findWebsiteMedia(accessToken: string, hotelId: string) {
  const response = await fetch(`${API_BASE_URL}/media?hotelId=${encodeURIComponent(hotelId)}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new ApiError('Website images could not be loaded.', response.status);
  return response.json() as Promise<WebsiteMedia[]>;
}

export async function uploadWebsiteMedia(accessToken: string, values: {
  hotelId: string; roomTypeId?: string; usage: WebsiteMediaUsage;
  sortOrder: number; altText: string; file: File;
}) {
  const body = new FormData();
  body.set('hotelId', values.hotelId);
  if (values.roomTypeId) body.set('roomTypeId', values.roomTypeId);
  body.set('usage', values.usage);
  body.set('sortOrder', String(values.sortOrder));
  body.set('altText', values.altText);
  body.set('file', values.file);
  const response = await fetch(`${API_BASE_URL}/media`, {
    method: 'POST', headers: { Authorization: `Bearer ${accessToken}` }, body,
  });
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as { detail?: string };
    throw new ApiError(problem.detail ?? 'The image could not be uploaded.', response.status);
  }
  return response.json() as Promise<WebsiteMedia>;
}

export async function deleteWebsiteMedia(accessToken: string, id: string) {
  const response = await fetch(`${API_BASE_URL}/media/${id}`, {
    method: 'DELETE', headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!response.ok) throw new ApiError('The image could not be deleted.', response.status);
}
