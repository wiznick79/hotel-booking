import { post } from './httpClient';

type LoginResponse = {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
};

export function login(username: string, password: string) {
  return post<LoginResponse>('/auth/login', { username, password });
}

export function refresh() {
  return post<LoginResponse>('/auth/refresh', {});
}
