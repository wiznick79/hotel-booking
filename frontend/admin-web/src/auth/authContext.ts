import { createContext } from 'react';
import type { JwtClaims } from './jwt';

export type AuthSession = {
  accessToken: string;
  expiresAt: number;
  claims: JwtClaims;
};

export type AuthContextValue = {
  session: AuthSession | null;
  login: (username: string, password: string) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);
