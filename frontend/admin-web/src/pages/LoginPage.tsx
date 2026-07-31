import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';

export function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      await login(username, password);
      window.location.hash = '#/';
    } catch (exception) {
      setError(exception instanceof ApiError && exception.status === 401 ? 'Invalid username or password.' : 'We could not sign you in. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  return <main className="login-page"><section className="login-card" aria-labelledby="login-title"><span className="brand-mark">HB</span><h1 id="login-title">Hotel Booking</h1><p>Management portal</p><form onSubmit={handleSubmit}><label>Username<input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required /></label><label>Password<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required /></label>{error && <p className="form-error" role="alert">{error}</p>}<button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Signing in...' : 'Sign in'}</button></form></section></main>;
}
