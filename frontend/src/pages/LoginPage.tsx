import { FormEvent, useState } from 'react';
import axios from 'axios';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { AuthResponse } from '../types';

type LoginType = 'admin' | 'kitchen';
type FormMode = 'login' | 'register';
type AccountForm = { username: string; password: string; confirmPassword: string; fullName: string };

const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
};

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const loginType = searchParams.get('type') as LoginType | null;
  const mode: FormMode = searchParams.get('mode') === 'register' ? 'register' : 'login';
  const [form, setForm] = useState<AccountForm>({ username: '', password: '', confirmPassword: '', fullName: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!loginType) return;
    setLoading(true);
    setError('');
    setNotice('');

    try {
      const response = await api.post<AuthResponse>('/auth/login', {
        username: form.username,
        password: form.password,
      });
      const user = response.data.user;
      if (!user) throw new Error('The server did not return an account.');
      const isAdmin = user.roles.includes('ADMIN');
      const isKitchen = user.roles.some((role) => role === 'KITCHEN' || role === 'CHEF');
      if ((loginType === 'admin' && !isAdmin) || (loginType === 'kitchen' && !isKitchen)) {
        await api.post('/auth/logout');
        setError(`This account is not registered as ${loginType === 'admin' ? 'an administrator' : 'kitchen staff'}.`);
        return;
      }

      localStorage.setItem('restaurant-user', JSON.stringify(user));
      navigate(isAdmin ? '/admin/dashboard' : '/kitchen/dashboard');
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, 'Invalid username or password'));
    } finally {
      setLoading(false);
    }
  };

  const registerAccount = async (event: FormEvent) => {
    event.preventDefault();
    if (!loginType) return;
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError('');
    setNotice('');
    try {
      if (loginType === 'admin') {
        await api.post('/auth/register-admin', {
          username: form.username,
          password: form.password,
          confirmPassword: form.confirmPassword,
          fullName: form.fullName,
        });
      } else {
        await api.post('/auth/register-kitchen', {
          username: form.username,
          password: form.password,
          fullName: form.fullName,
        });
      }
      setForm({ username: form.username, password: '', confirmPassword: '', fullName: '' });
      setNotice('Account created. Sign in with your new username and password.');
      navigate(`/login?type=${loginType}`, { replace: true });
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, 'Could not create the account.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="brand-header">
          <div className="brand-mark">O</div>
          <div>
            <h1>Order Desk</h1>
            <p className="muted">Order Management System</p>
          </div>
        </div>

        {loginType === 'admin' || loginType === 'kitchen' ? (
          <>
            <h2>
              {mode === 'register'
                ? `Create ${loginType === 'admin' ? 'administrator' : 'kitchen staff'} account`
                : `${loginType === 'admin' ? 'Administrator' : 'Kitchen staff'} sign in`}
            </h2>
            <p className="muted auth-description">
              {mode === 'register'
                ? 'Choose a username and a password of at least 12 characters.'
                : loginType === 'admin'
                  ? 'Sign in to manage menu items, orders, reports, and kitchen staff.'
                  : 'Sign in to view customer orders and update their status.'}
            </p>
            {notice && <div className="alert-success" role="status">{notice}</div>}
            <form onSubmit={mode === 'register' ? registerAccount : handleSubmit} className="stacked-form">
              {mode === 'register' && (
                <>
                  <input
                    autoComplete="name"
                    maxLength={150}
                    placeholder="Full name (optional)"
                    value={form.fullName}
                    onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                  />
                </>
              )}
              <input
                autoComplete="username"
                minLength={3}
                maxLength={100}
                required
                placeholder="Username"
                value={form.username}
                onChange={(event) => setForm({ ...form, username: event.target.value })}
              />
              <input
                type="password"
                autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                minLength={mode === 'register' ? 12 : undefined}
                maxLength={72}
                required
                placeholder={mode === 'register' ? 'Password (at least 12 characters)' : 'Password'}
                value={form.password}
                onChange={(event) => setForm({ ...form, password: event.target.value })}
              />
              {mode === 'register' && (
                <input
                  type="password"
                  autoComplete="new-password"
                  minLength={12}
                  maxLength={72}
                  required
                  placeholder="Confirm password"
                  value={form.confirmPassword}
                  onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
                />
              )}
              {error && <div className="alert-error" role="alert">{error}</div>}
              <button type="submit" className="primary-button" disabled={loading}>
                {loading
                  ? (mode === 'register' ? 'Creating account...' : 'Signing in...')
                  : (mode === 'register' ? 'Create account' : 'Sign in')}
              </button>
            </form>
            <p className="auth-links">
              <Link to={`/login?type=${loginType}${mode === 'register' ? '' : '&mode=register'}`}>
                {mode === 'register' ? 'Back to sign in' : `Create ${loginType === 'admin' ? 'administrator' : 'kitchen staff'} account`}
              </Link>
              <Link to="/login">Back to options</Link>
            </p>
          </>
        ) : (
          <>
            <h2>Choose how to continue</h2>
            <p className="muted auth-description">Customers can order without an account. Staff can sign in or create an account.</p>
            <div className="portal-grid">
              <Link className="portal-link portal-primary" to="/customer">
                <strong>Customer order</strong>
                <span>Browse the menu, place an order, and track its status.</span>
              </Link>
              <Link className="portal-link" to="/login?type=admin">
                <strong>Administrator sign in</strong>
                <span>Manage menu items, orders, and kitchen staff.</span>
              </Link>
              <Link className="portal-link" to="/login?type=kitchen">
                <strong>Kitchen staff sign in</strong>
                <span>View incoming orders and update their progress.</span>
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
