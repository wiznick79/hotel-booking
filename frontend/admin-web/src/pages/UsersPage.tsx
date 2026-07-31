import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { createUser, findUsers } from '../api/userApi';
import type { CurrentUser } from '../api/userApi';
import { findHotels } from '../api/hotelApi';
import type { Hotel } from '../api/hotelApi';
import { useAuth } from '../auth/useAuth';

export function UsersPage({ selectedHotelId }: { selectedHotelId: string }) {
  const { session } = useAuth();
  const [users, setUsers] = useState<CurrentUser[]>([]);
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [error, setError] = useState('');
  const isAdmin = session?.claims.roles.includes('ROLE_ADMIN') ?? false;
  const roles = isAdmin ? ['MANAGER', 'STAFF'] : ['STAFF'];

  const load = useCallback(async () => {
    if (!session) return;
    try { const [loadedUsers, loadedHotels] = await Promise.all([findUsers(session.accessToken), findHotels(session.accessToken)]); setUsers(loadedUsers); setHotels(loadedHotels); setError(''); } catch { setError('Users could not be loaded.'); }
  }, [session]);

  useEffect(() => { void load(); }, [load]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    const form = new FormData(event.currentTarget);
    const hotelIds = hotels.filter((hotel) => form.get(`hotel-${hotel.id}`) === 'on').map((hotel) => hotel.id);
    try { await createUser(session.accessToken, { username: String(form.get('username')).trim(), password: String(form.get('password')), roles: [String(form.get('role'))], hotelIds }); setIsFormOpen(false); await load(); } catch { setError('The user could not be created. Use a unique username, a 10-character password, and at least one hotel.'); }
  }

  return <section><div className="page-heading"><div><p className="eyebrow">Administration</p><h1>Users</h1><p>Create staff and assign the hotels they can manage.</p></div><button type="button" onClick={() => setIsFormOpen(true)}>New user</button></div>{error && <p className="form-error">{error}</p>}{isFormOpen && <section className="setup-card room-type-form-card"><h2>New user</h2><form className="hotel-form" onSubmit={submit}><label>Username<input name="username" autoComplete="username" required /></label><label>Temporary password<input name="password" type="password" autoComplete="new-password" minLength={10} required /></label><label>Role<select name="role">{roles.map((role) => <option key={role} value={role}>{role === 'MANAGER' ? 'Manager' : 'Staff'}</option>)}</select></label><div className="full-width"><h3>Hotel assignments</h3>{hotels.map((hotel) => <label className="checkbox-label" key={hotel.id}><input name={`hotel-${hotel.id}`} type="checkbox" defaultChecked={hotel.id === selectedHotelId} /> {hotel.name}</label>)}</div><div className="form-actions full-width"><button type="button" className="secondary-button" onClick={() => setIsFormOpen(false)}>Cancel</button><button type="submit">Create user</button></div></form></section>}{!error && <div className="table-container"><table><thead><tr><th>Username</th><th>Roles</th><th>Assigned hotels</th><th>Status</th></tr></thead><tbody>{users.map((user) => <tr key={user.id}><td><strong>{user.username}</strong></td><td>{user.roles.join(', ') || 'No role'}</td><td>{user.hotelIds.length}</td><td><span className={`status ${user.enabled ? 'status-confirmed' : 'status-cancelled'}`}>{user.enabled ? 'Enabled' : 'Disabled'}</span></td></tr>)}</tbody></table></div>}</section>;
}
