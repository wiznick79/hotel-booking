import { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { login as requestLogin, refresh as requestRefresh } from '../api/authApi';
import { readJwtClaims } from './jwt';
import type { JwtClaims } from './jwt';

const SESSION_STORAGE_KEY = 'hotel-booking.admin.session';

export type AuthSession = {
  accessToken: string;
  expiresAt: number;
  claims: JwtClaims;
};

type AuthContextValue = {
  session: AuthSession | null;
  login: (username: string, password: string) => Promise<void>;
  refresh: () => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function readStoredSession(): AuthSession | null {
  const value = sessionStorage.getItem(SESSION_STORAGE_KEY);

  if (!value) {
    return null;
  }

  try {
    const session = JSON.parse(value) as AuthSession;

    if (session.expiresAt <= Date.now()) {
      sessionStorage.removeItem(SESSION_STORAGE_KEY);
      return null;
    }

    return session;
  } catch {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(readStoredSession);

  const storeSession = useCallback((response: { accessToken: string; expiresIn: number }) => {
    const nextSession: AuthSession = {
      accessToken: response.accessToken,
      expiresAt: Date.now() + response.expiresIn * 1000,
      claims: readJwtClaims(response.accessToken),
    };

    sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(nextSession));
    setSession(nextSession);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const response = await requestLogin(username, password);
    storeSession(response);
  }, [storeSession]);

  const logout = useCallback(() => {
    sessionStorage.removeItem(SESSION_STORAGE_KEY);
    setSession(null);
  }, []);

  const refresh = useCallback(async () => {
    const response = await requestRefresh();
    storeSession(response);
  }, [storeSession]);

  useEffect(() => {
    if (!session) {
      return undefined;
    }

    const refreshDelay = Math.max(session.expiresAt - Date.now() - 60_000, 1_000);
    const refreshTimer = window.setTimeout(() => {
      void refresh().catch(logout);
    }, refreshDelay);

    return () => window.clearTimeout(refreshTimer);
  }, [logout, refresh, session]);

  const value = useMemo(() => ({ session, login, refresh, logout }), [login, logout, refresh, session]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export { AuthContext };
