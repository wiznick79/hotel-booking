import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import {
  createUser,
  findUsers,
  resetUserPassword,
  updateHotelAssignments,
  updateUserEnabled,
  updateUserRole,
} from '../api/userApi';
import type { CurrentUser } from '../api/userApi';
import { findHotels } from '../api/hotelApi';
import type { Hotel } from '../api/hotelApi';
import { useAuth } from '../auth/useAuth';
import { StatusBadge } from '../components/StatusBadge';

type UsersPageProps = {
  selectedHotelId: string;
  category: 'staff' | 'customers';
};

export function UsersPage({ selectedHotelId, category }: UsersPageProps) {
  const { session } = useAuth();
  const [users, setUsers] = useState<CurrentUser[]>([]);
  const [hotels, setHotels] = useState<Hotel[]>([]);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<CurrentUser | null>(null);
  const [error, setError] = useState('');
  const isAdmin = session?.claims.roles.includes('ROLE_ADMIN') ?? false;
  const isCustomerView = category === 'customers';
  const roles = isAdmin ? ['MANAGER', 'STAFF'] : ['STAFF'];

  const load = useCallback(async () => {
    if (!session) return;
    try {
      const [loadedUsers, loadedHotels] = await Promise.all([
        findUsers(session.accessToken, isCustomerView ? 'CUSTOMERS' : 'STAFF'),
        findHotels(session.accessToken),
      ]);
      setUsers(loadedUsers);
      setHotels(loadedHotels);
      setError('');
    } catch {
      setError('Users could not be loaded.');
    }
  }, [isCustomerView, session]);

  useEffect(() => { void load(); }, [load]);

  function openCreateForm() {
    setEditingUser(null);
    setError('');
    setIsFormOpen(true);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!session) return;
    const form = new FormData(event.currentTarget);
    const hotelIds = hotels.filter((hotel) => form.get(`hotel-${hotel.id}`) === 'on').map((hotel) => hotel.id);
    const role = String(form.get('role'));
    try {
      if (editingUser) {
        await Promise.all([
          updateUserRole(session.accessToken, editingUser.id, [role]),
          updateHotelAssignments(session.accessToken, editingUser.id, hotelIds),
        ]);
      } else {
        await createUser(session.accessToken, {
          username: String(form.get('username')).trim(),
          password: String(form.get('password')),
          roles: [role],
          hotelIds,
        });
      }
      setIsFormOpen(false);
      setEditingUser(null);
      await load();
    } catch {
      setError('The user could not be saved. Use a unique username, a 10-character password, and at least one hotel.');
    }
  }

  async function toggleEnabled(user: CurrentUser) {
    if (!session) return;
    try { await updateUserEnabled(session.accessToken, user.id, !user.enabled); await load(); }
    catch { setError('The user status could not be updated.'); }
  }

  async function resetPassword(user: CurrentUser) {
    if (!session) return;
    const password = window.prompt(`New temporary password for ${user.username}:`);
    if (!password) return;
    if (password.length < 10) { setError('The password must have at least 10 characters.'); return; }
    try { await resetUserPassword(session.accessToken, user.id, password); }
    catch { setError('The password could not be reset.'); }
  }

  return <section>
    <div className="page-heading">
      <div>
        <p className="eyebrow">Administration</p>
        <h1>{isCustomerView ? 'Customers' : 'Staff'}</h1>
        <p>{isCustomerView
          ? 'View and support registered customer accounts.'
          : 'Create staff accounts and assign the hotels they can manage.'}</p>
      </div>
      {!isCustomerView && <button type="button" onClick={openCreateForm}>New staff member</button>}
    </div>
    {error && <p className="form-error">{error}</p>}
    {!isCustomerView && isFormOpen && <section className="setup-card room-type-form-card">
      <h2>{editingUser ? `Edit ${editingUser.username}` : 'New user'}</h2>
      <form className="hotel-form" onSubmit={submit}>
        {!editingUser && <label>Username<input name="username" autoComplete="username" required /></label>}
        {!editingUser && <label>Temporary password<input name="password" type="password" autoComplete="new-password" minLength={10} required /></label>}
        <label>Role<select name="role" defaultValue={editingUser?.roles[0] ?? roles[0]}>{roles.map((role) => <option key={role} value={role}>{role === 'MANAGER' ? 'Manager' : 'Staff'}</option>)}</select></label>
        <div className="full-width"><h3>Hotel assignments</h3>{hotels.map((hotel) => <label className="checkbox-label" key={hotel.id}><input name={`hotel-${hotel.id}`} type="checkbox" defaultChecked={editingUser ? editingUser.hotelIds.includes(hotel.id) : hotel.id === selectedHotelId} /> {hotel.name}</label>)}</div>
        <div className="form-actions full-width"><button type="button" className="secondary-button" onClick={() => setIsFormOpen(false)}>Cancel</button><button type="submit">{editingUser ? 'Save user' : 'Create user'}</button></div>
      </form>
    </section>}
    {!error && (isCustomerView
      ? <CustomerTable
          isAdmin={isAdmin}
          onResetPassword={resetPassword}
          onToggleEnabled={toggleEnabled}
          users={users}
        />
      : <StaffTable
          isAdmin={isAdmin}
          onEdit={(user) => { setEditingUser(user); setIsFormOpen(true); }}
          onResetPassword={resetPassword}
          onToggleEnabled={toggleEnabled}
          users={users}
        />)}
  </section>;
}

type UserTableProps = {
  users: CurrentUser[];
  isAdmin: boolean;
  onToggleEnabled: (user: CurrentUser) => Promise<void>;
  onResetPassword: (user: CurrentUser) => Promise<void>;
};

function StaffTable({ users, isAdmin, onToggleEnabled, onResetPassword, onEdit }: UserTableProps & {
  onEdit: (user: CurrentUser) => void;
}) {
  return (
    <div className="table-container">
      <table>
        <thead>
          <tr><th>Username</th><th>Role</th><th>Assigned hotels</th><th>Status</th><th /></tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td><strong>{user.username}</strong></td>
              <td>{user.roles.join(', ') || 'No role'}</td>
              <td>{user.hotelIds.length}</td>
              <td><UserStatus user={user} /></td>
              <td>{isAdmin && <UserActions
                onEdit={() => onEdit(user)}
                onResetPassword={() => void onResetPassword(user)}
                onToggleEnabled={() => void onToggleEnabled(user)}
                showEdit
                user={user}
              />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CustomerTable({ users, isAdmin, onToggleEnabled, onResetPassword }: UserTableProps) {
  return (
    <div className="table-container">
      <table>
        <thead>
          <tr><th>Name</th><th>Email address</th><th>Status</th><th /></tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td><strong>{user.fullName || 'Not provided'}</strong></td>
              <td>{user.username}</td>
              <td><UserStatus user={user} /></td>
              <td>{isAdmin && <UserActions
                onResetPassword={() => void onResetPassword(user)}
                onToggleEnabled={() => void onToggleEnabled(user)}
                user={user}
              />}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function UserStatus({ user }: { user: CurrentUser }) {
  return <StatusBadge
    label={user.enabled ? 'Enabled' : 'Disabled'}
    tone={user.enabled ? 'positive' : 'negative'}
  />;
}

type UserActionsProps = {
  user: CurrentUser;
  showEdit?: boolean;
  onEdit?: () => void;
  onToggleEnabled: () => void;
  onResetPassword: () => void;
};

function UserActions({ user, showEdit = false, onEdit, onToggleEnabled, onResetPassword }: UserActionsProps) {
  return (
    <div className="table-actions">
      {showEdit && <button type="button" className="secondary-button" onClick={onEdit}>Edit</button>}
      <button type="button" className="secondary-button" onClick={onToggleEnabled}>
        {user.enabled ? 'Disable' : 'Enable'}
      </button>
      <button type="button" className="secondary-button" onClick={onResetPassword}>Reset password</button>
    </div>
  );
}
