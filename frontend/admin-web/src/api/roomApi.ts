import { del, get, post, put } from './httpClient';

export type RoomType = {
  id: string;
  hotelId: string;
  maximumOccupancy: number;
  basePrice: number;
  language: string;
  name: string;
  description: string | null;
  active: boolean;
};

export type Room = {
  id: string;
  hotelId: string;
  roomTypeId: string;
  roomNumber: string;
  floor: number;
  status: string;
  active: boolean;
};

export type CreateRoomTypeRequest = {
  hotelId: string;
  maximumOccupancy: number;
  basePrice: number;
  translations: Record<string, { name: string; description: string }>;
};

export type CreateRoomRequest = {
  hotelId: string;
  roomTypeId: string;
  roomNumber: string;
  floor: number;
};

export function findRoomTypes(accessToken: string) {
  return get<RoomType[]>('/room-types', accessToken);
}

export function findRooms(accessToken: string) {
  return get<Room[]>('/rooms', accessToken);
}

export function createRoomType(accessToken: string, request: CreateRoomTypeRequest) {
  return post<RoomType>('/room-types', request, accessToken);
}

export function createRoom(accessToken: string, request: CreateRoomRequest) {
  return post<Room>('/rooms', request, accessToken);
}

export function updateRoom(accessToken: string, id: string, request: Omit<CreateRoomRequest, 'hotelId'> & { status: string }) {
  return put<Room>(`/rooms/${id}`, request, accessToken);
}

export function updateRoomType(accessToken: string, id: string, request: CreateRoomTypeRequest) {
  return put<RoomType>(`/room-types/${id}`, request, accessToken);
}

export function deactivateRoomType(accessToken: string, id: string) {
  return del(`/room-types/${id}`, accessToken);
}
