import { get, patch, post } from './httpClient';

export type CurrentUser = {
  id: number;
  username: string;
  enabled: boolean;
  roles: string[];
  hotelIds: string[];
};

export function findCurrentUser(accessToken: string) {
  return get<CurrentUser>('/users/me', accessToken);
}

export function findUsers(accessToken: string) {
  return get<CurrentUser[]>('/users', accessToken);
}

export function createUser(accessToken: string, request: { username: string; password: string; roles: string[]; hotelIds: string[] }) {
  return post<CurrentUser>('/users', request, accessToken);
}

export function updateHotelAssignments(
  accessToken: string,
  userId: number,
  hotelIds: string[],
) {
  return patch<void>(`/users/${userId}/hotels`, { hotelIds }, accessToken);
}

export function updateUserRole(accessToken: string, userId: number, roles: string[]) {
  return patch<void>(`/users/${userId}/roles`, { roles }, accessToken);
}

export function updateUserEnabled(accessToken: string, userId: number, enabled: boolean) {
  return patch<void>(`/users/${userId}/enabled?enabled=${enabled}`, {}, accessToken);
}

export function resetUserPassword(accessToken: string, userId: number, password: string) {
  return patch<void>(`/users/${userId}/password`, { password }, accessToken);
}

export function changeOwnPassword(
  accessToken: string,
  currentPassword: string,
  newPassword: string,
) {
  return patch<void>('/users/me/password', { currentPassword, newPassword }, accessToken);
}
