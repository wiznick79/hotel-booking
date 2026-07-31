import { get, post, put } from './httpClient';

export type CreateHotelRequest = {
  name: string;
  description: string;
  address: string;
  city: string;
  country: string;
  defaultLanguage: string;
};

export type Hotel = CreateHotelRequest & {
  id: string;
  active: boolean;
};

export function createHotel(accessToken: string, request: CreateHotelRequest) {
  return post<Hotel>('/hotels', request, accessToken);
}

export function findHotels(accessToken: string) {
  return get<Hotel[]>('/hotels', accessToken);
}

export function updateHotel(accessToken: string, id: string, request: CreateHotelRequest) {
  return put<Hotel>(`/hotels/${id}`, request, accessToken);
}
