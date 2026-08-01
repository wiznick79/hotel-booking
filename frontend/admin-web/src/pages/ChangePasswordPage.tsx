import { useState } from 'react';
import { changeOwnPassword } from '../api/userApi';
import { useAuth } from '../auth/useAuth';

export function ChangePasswordPage() {
  const { session, logout } = useAuth();
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!session) {
      return;
    }

    const form = new FormData(event.currentTarget);
    const currentPassword = String(form.get('currentPassword'));
    const newPassword = String(form.get('newPassword'));
    const confirmPassword = String(form.get('confirmPassword'));

    if (newPassword.length < 10) {
      setError('The new password must have at least 10 characters.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('The new password and confirmation do not match.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      await changeOwnPassword(session.accessToken, currentPassword, newPassword);
      setSuccess(true);
    } catch {
      setError('The password could not be changed. Check your current password and try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  if (success) {
    return (
      <section className="info-card narrow-card">
        <p className="eyebrow">Account security</p>
        <h1>Password changed</h1>
        <p>Your other signed-in sessions were revoked. Please sign in again with the new password.</p>
        <button type="button" onClick={logout}>Return to sign in</button>
      </section>
    );
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Account security</p>
          <h1>Change password</h1>
          <p>Use a unique password with at least 10 characters.</p>
        </div>
      </div>

      <form className="assignment-form narrow-card" onSubmit={handleSubmit}>
        <label>
          Current password
          <input name="currentPassword" type="password" autoComplete="current-password" required />
        </label>
        <label>
          New password
          <input name="newPassword" type="password" autoComplete="new-password" minLength={10} required />
        </label>
        <label>
          Confirm new password
          <input name="confirmPassword" type="password" autoComplete="new-password" minLength={10} required />
        </label>
        {error && <p className="form-error" role="alert">{error}</p>}
        <div className="form-actions">
          <button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Changing password...' : 'Change password'}
          </button>
        </div>
      </form>
    </section>
  );
}
