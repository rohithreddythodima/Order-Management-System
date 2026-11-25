import { FormEvent, useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, ChefHat, CircleDollarSign, ShieldCheck, UtensilsCrossed } from 'lucide-react';
import { api } from '../api';
import { AuthResponse } from '../types';

type LoginType = 'admin' | 'kitchen' | 'waiter' | 'cashier';
type AccountForm = { username: string; password: string; confirmPassword: string; fullName: string };

const roleNames = { admin: 'Administrator', kitchen: 'Kitchen staff', waiter: 'Waiter', cashier: 'Cashier' };
const blankAccountForm: AccountForm = { username: '', password: '', confirmPassword: '', fullName: '' };
const workspaces = [
  { type: 'admin', title: 'Administrator', description: 'Manage your restaurant workspace.', icon: ShieldCheck },
  { type: 'kitchen', title: 'Kitchen team', description: 'Manage incoming orders and preparation.', icon: ChefHat },
  { type: 'waiter', title: 'Waiter', description: 'Take and update table orders.', icon: UtensilsCrossed },
  { type: 'cashier', title: 'Cashier', description: 'Monitor orders and manage billing.', icon: CircleDollarSign },
] as const;
const signupLabel = (role: LoginType) =>
  `Sign up for ${role === 'admin' ? 'an' : 'a'} ${roleNames[role].toLowerCase()} account`;

const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
};

export default function LoginPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const type = searchParams.get('type');
  const loginType = type === 'admin' || type === 'kitchen' || type === 'waiter' || type === 'cashier' ? type : null;
  const accessDenied = searchParams.get('denied') === '1';
  const isRegister = searchParams.get('mode') === 'register' && loginType !== null;
  const [form, setForm] = useState<AccountForm>(blankAccountForm);
  const [error, setError] = useState('');
  const [createdAccountRole, setCreatedAccountRole] = useState<LoginType | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setError('');
  }, [loginType, isRegister]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!loginType) return;
    setLoading(true);
    setError('');
    setCreatedAccountRole(null);

    try {
      const response = await api.post<AuthResponse>('/auth/login', {
        username: form.username.trim(),
        password: form.password,
      });
      const user = response.data.user;
      if (!user) throw new Error('The server did not return an account.');
      const hasRequestedRole = {
        admin: user.roles.includes('ADMIN'),
        kitchen: user.roles.some((role) => role === 'KITCHEN' || role === 'CHEF'),
        waiter: user.roles.includes('WAITER'),
        cashier: user.roles.includes('CASHIER'),
      }[loginType];
      if (!hasRequestedRole) {
        await api.post('/auth/logout');
        setError(`This account is not registered as ${loginType === 'admin' ? 'an administrator' : loginType === 'kitchen' ? 'kitchen staff' : `${loginType} staff`}.`);
        return;
      }

      localStorage.setItem('restaurant-user', JSON.stringify(user));
      navigate(`/${loginType}/dashboard`);
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, 'Invalid username or password'));
    } finally {
      setLoading(false);
    }
  };

  const registerAccount = async (event: FormEvent) => {
    event.preventDefault();
    if (!loginType) return;

    const username = form.username.trim();
    if (username.length < 3) {
      setError('Username must be at least 3 characters.');
      return;
    }
    if (new TextEncoder().encode(form.password).length > 72) {
      setError('Password must be at most 72 UTF-8 bytes. Use a shorter password if it contains emojis or other multibyte characters.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError('');
    setCreatedAccountRole(null);
    try {
      await api.post(`/auth/register-staff/${loginType.toUpperCase()}`, {
        username,
        password: form.password,
        confirmPassword: form.confirmPassword,
        fullName: form.fullName.trim() || undefined,
      });
      setForm({ ...blankAccountForm, username });
      setCreatedAccountRole(loginType);
      navigate(`/login?type=${loginType}`, { replace: true });
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, 'Could not create the account.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className={`auth-card ${loginType ? 'sign-in-card' : 'portal-auth-card'}`}>
        <div className="brand-header">
          <div className="brand-mark" aria-hidden="true">O</div>
          <div>
            <h1>Order Desk</h1>
            <p className="muted">Restaurant operations</p>
          </div>
        </div>
        {accessDenied && <div className="alert-error" role="alert">Access denied. You were returned to the home screen because your account cannot open that workspace.</div>}

        {loginType ? (
          <>
            <h2>
              {isRegister
                ? `Create ${roleNames[loginType].toLowerCase()} account`
                : `${roleNames[loginType]} sign in`}
            </h2>
            <p className="muted auth-description">
              {isRegister
                ? 'Create an account to access this workspace.'
                : loginType === 'admin'
                  ? 'Sign in to manage menu items, orders, reports, and kitchen staff.'
                  : loginType === 'kitchen'
                    ? 'Sign in to view orders and update their preparation status.'
                    : loginType === 'waiter' ? 'Sign in to enter restaurant orders.' : 'Sign in to monitor orders and handle billing.'}
            </p>
            {!isRegister && createdAccountRole === loginType && (
              <div className="alert-success" role="status">Account created. Sign in to continue.</div>
            )}
            <form onSubmit={isRegister ? registerAccount : handleSubmit} className="stacked-form">
              {isRegister && (
                <div className="auth-field">
                  <label htmlFor="full-name">Full name <span className="muted">(optional)</span></label>
                  <input
                    id="full-name"
                    autoComplete="name"
                    maxLength={150}
                    value={form.fullName}
                    onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                  />
                </div>
              )}
              <div className="auth-field">
                <label htmlFor="username">Username</label>
                <input
                  id="username"
                  autoComplete="username"
                  minLength={isRegister ? 3 : undefined}
                  maxLength={100}
                  required
                  value={form.username}
                  onChange={(event) => setForm({ ...form, username: event.target.value })}
                />
              </div>
              <div className="auth-field">
                <label htmlFor="password">{isRegister ? 'Password (at least 12 characters)' : 'Password'}</label>
                <input
                  id="password"
                  type="password"
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                  minLength={isRegister ? 12 : undefined}
                  maxLength={isRegister ? undefined : 72}
                  required
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                />
              </div>
              {isRegister && (
                <div className="auth-field">
                  <label htmlFor="confirm-password">Confirm password</label>
                  <input
                    id="confirm-password"
                    type="password"
                    autoComplete="new-password"
                    minLength={12}
                    required
                    value={form.confirmPassword}
                    onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
                  />
                </div>
              )}
              {error && <div className="alert-error" role="alert">{error}</div>}
              <button type="submit" className="primary-button" disabled={loading}>
                {loading
                  ? (isRegister ? 'Creating account…' : 'Signing in…')
                  : (isRegister ? `Create ${roleNames[loginType].toLowerCase()} account` : 'Sign in')}
              </button>
            </form>
            {!isRegister && (
              <Link className="secondary-button auth-signup" to={`/login?type=${loginType}&mode=register`}>
                {signupLabel(loginType)}
              </Link>
            )}
            <p className="auth-links">
              {isRegister && (
                <Link to={`/login?type=${loginType}`}>Back to sign in</Link>
              )}
              <Link to="/login">Back to options</Link>
            </p>
          </>
        ) : (
          <div className="portal-layout">
            <section className="portal-intro">
              <span className="portal-kicker">SERVICE, IN SYNC</span>
              <h2>Make every shift run smoothly.</h2>
              <p>One simple workspace for the front of house, kitchen, and the people who keep it all moving.</p>
              <div className="portal-promise">
                <span className="portal-promise-icon"><ShieldCheck size={19} aria-hidden="true" /></span>
                <span><strong>Role-based workspaces</strong><small>Sign in only to the tools your role needs.</small></span>
              </div>
              <div className="portal-decoration" aria-hidden="true">
                <span className="decoration-ring decoration-ring-one" />
                <span className="decoration-ring decoration-ring-two" />
                <UtensilsCrossed className="decoration-icon" size={76} strokeWidth={1} />
              </div>
            </section>
            <section className="portal-options" aria-labelledby="portal-title">
              <p className="eyebrow">GET STARTED</p>
              <h2 id="portal-title">Choose your workspace</h2>
              <p className="muted portal-description">Customers can order without an account. Staff can sign in or sign up for their workspace.</p>
              <div className="portal-grid">
                <Link className="portal-link portal-primary" to="/customer">
                  <span className="portal-icon"><UtensilsCrossed size={19} aria-hidden="true" /></span>
                  <span className="portal-link-copy"><strong>Customer</strong><small>Explore the menu, order, and track your meal.</small></span>
                  <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
                </Link>
                {workspaces.map(({ type, title, description, icon: Icon }) => (
                  <div className="portal-workspace" key={type}>
                    <Link className="portal-link" to={`/login?type=${type}`}>
                      <span className="portal-icon"><Icon size={19} aria-hidden="true" /></span>
                      <span className="portal-link-copy"><strong>{title}</strong><small>{description}</small></span>
                    </Link>
                    <Link
                      className="secondary-button portal-signup"
                      to={`/login?type=${type}&mode=register`}
                      aria-label={signupLabel(type)}
                    >
                      Sign up
                    </Link>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
