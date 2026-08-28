import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/httpClient';
import { useAuth } from '../auth/useAuth';
import { adminLanguages, translateAdmin, type AdminLanguage } from '../i18n';

type LoginPageProps = {
  language: AdminLanguage;
  onLanguageChange: (language: AdminLanguage) => void;
};

export function LoginPage({ language, onLanguageChange }: LoginPageProps) {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const t = (key: Parameters<typeof translateAdmin>[1]) => translateAdmin(language, key);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setIsSubmitting(true);

    try {
      await login(username, password);
      window.location.hash = '#/';
    } catch (exception) {
      setError(exception instanceof ApiError && exception.status === 401 ? t('invalidCredentials') : t('signInFailed'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <select aria-label="Language" className="login-language" onChange={(event) => onLanguageChange(event.target.value as AdminLanguage)} value={language}>
          {adminLanguages.map(({ code, label }) => <option key={code} value={code}>{label}</option>)}
        </select>
        <span className="brand-mark">HB</span>
        <h1 id="login-title">Hotel Booking</h1>
        <p>{t('managementPortal')}</p>
        <form onSubmit={handleSubmit}>
          <label>
            {t('username')}
            <input autoComplete="username" onChange={(event) => setUsername(event.target.value)} required value={username} />
          </label>
          <label>
            {t('password')}
            <input autoComplete="current-password" onChange={(event) => setPassword(event.target.value)} required type="password" value={password} />
          </label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button disabled={isSubmitting} type="submit">{isSubmitting ? t('signingIn') : t('signIn')}</button>
        </form>
      </section>
    </main>
  );
}
