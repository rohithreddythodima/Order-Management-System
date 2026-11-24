import { FormEvent, useEffect, useState } from 'react';
import axios from 'axios';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, ChefHat, CircleDollarSign, ShieldCheck, UtensilsCrossed } from 'lucide-react';
import { api } from '../api';
import { AuthResponse } from '../types';

type LoginType = 'admin' | 'kitchen' | 'waiter' | 'cashier';
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
  const accessDenied = searchParams.get('denied') === '1';
  const mode: FormMode = searchParams.get('mode') === 'register' && loginType ? 'register' : 'login';
  const [form, setForm] = useState<AccountForm>({ username: '', password: '', confirmPassword: '', fullName: '' });
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const [setupRequired, setSetupRequired] = useState(false);
  const [setupChecked, setSetupChecked] = useState(false);

  useEffect(() => {
    api.get<{ required: boolean }>('/auth/setup-status')
      .then((response) => setSetupRequired(response.data.required))
      .catch(() => setSetupRequired(false))
      .finally(() => setSetupChecked(true));
  }, []);

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
    if (loginType === 'admin' && !setupRequired) {
      setError('Initial administrator setup is only available when no user profiles exist.');
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    setError('');
    setNotice('');
    try {
      const registrationPath = loginType === 'admin'
        ? '/auth/setup-admin'
        : `/auth/register-staff/${loginType.toUpperCase()}`;
      await api.post(registrationPath, {
        username: form.username,
        password: form.password,
        confirmPassword: form.confirmPassword,
        fullName: form.fullName,
      });
      setForm({ username: form.username, password: '', confirmPassword: '', fullName: '' });
      setNotice('Account created. Sign in to continue.');
      if (loginType === 'admin') setSetupRequired(false);
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

        {loginType === 'admin' || loginType === 'kitchen' || loginType === 'waiter' || loginType === 'cashier' ? (
          <>
            <h2>
              {mode === 'register'
                ? loginType === 'admin' ? 'Initial administrator setup' : `Create ${loginType} account`
                : `${loginType === 'admin' ? 'Administrator' : loginType === 'kitchen' ? 'Kitchen staff' : loginType === 'waiter' ? 'Waiter' : 'Cashier'} sign in`}
            </h2>
            <p className="muted auth-description">
              {mode === 'register'
                ? loginType === 'admin'
                  ? 'Create the first administrator profile. Setup is only available while no user profiles exist.'
                  : 'Create a staff account to access this workspace.'
                : loginType === 'admin'
                  ? 'Sign in to manage menu items, orders, reports, and kitchen staff.'
                  : loginType === 'kitchen'
                    ? 'Sign in to view orders and update their preparation status.'
                    : loginType === 'waiter' ? 'Sign in to enter restaurant orders.' : 'Sign in to monitor orders and handle billing.'}
            </p>
            {notice && <div className="alert-success" role="status">{notice}</div>}
            {mode === 'register' && loginType === 'admin' && setupChecked && !setupRequired ? (
              <div className="alert-error" role="alert">Initial setup is unavailable because user profiles already exist.</div>
            ) : (
            <form onSubmit={mode === 'register' ? registerAccount : handleSubmit} className="stacked-form">
              {mode === 'register' && (
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
                  minLength={3}
                  maxLength={100}
                  required
                  value={form.username}
                  onChange={(event) => setForm({ ...form, username: event.target.value })}
                />
              </div>
              <div className="auth-field">
                <label htmlFor="password">{mode === 'register' ? 'Password (at least 12 characters)' : 'Password'}</label>
                <input
                  id="password"
                  type="password"
                  autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                  minLength={mode === 'register' ? 12 : undefined}
                  maxLength={72}
                  required
                  value={form.password}
                  onChange={(event) => setForm({ ...form, password: event.target.value })}
                />
              </div>
              {mode === 'register' && (
                <div className="auth-field">
                  <label htmlFor="confirm-password">Confirm password</label>
                  <input
                    id="confirm-password"
                    type="password"
                    autoComplete="new-password"
                    minLength={12}
                    maxLength={72}
                    required
                    value={form.confirmPassword}
                    onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
                  />
                </div>
              )}
              {error && <div className="alert-error" role="alert">{error}</div>}
              <button type="submit" className="primary-button" disabled={loading || (mode === 'register' && !setupChecked)}>
                {loading
                  ? (mode === 'register' ? 'Creating account...' : 'Signing in...')
                  : (mode === 'register' ? 'Create account' : 'Sign in')}
              </button>
            </form>
            )}
            <p className="auth-links">
              {mode === 'register'
                ? <Link to={`/login?type=${loginType}`}>Back to sign in</Link>
                : loginType === 'admin' && setupRequired && setupChecked
                  ? <Link to="/setup">Set up the first administrator</Link>
                  : loginType !== 'admin' ? <Link to={`/login?type=${loginType}&mode=register`}>Sign up</Link> : null}
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
              <p className="muted portal-description">Customers can order without an account. Staff sign in with their assigned profile.</p>
            <div className="portal-grid">
              {setupChecked && setupRequired && (
                <Link className="portal-link portal-primary" to="/setup">
                  <span className="portal-icon"><ShieldCheck size={19} aria-hidden="true" /></span>
                  <span className="portal-link-copy"><strong>Sign up as administrator</strong><small>Create the first restaurant account.</small></span>
                  <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
                </Link>
              )}
              <Link className="portal-link portal-primary" to="/customer">
                <span className="portal-icon"><UtensilsCrossed size={19} aria-hidden="true" /></span>
                <span className="portal-link-copy"><strong>Customer</strong><small>Explore the menu, order, and track your meal.</small></span>
                <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
              </Link>
              <Link className="portal-link" to="/login?type=admin">
                <span className="portal-icon"><ShieldCheck size={19} aria-hidden="true" /></span>
                <span className="portal-link-copy"><strong>Administrator</strong><small>Manage your restaurant workspace.</small></span>
                <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
              </Link>
              <Link className="portal-link" to="/login?type=kitchen">
                <span className="portal-icon"><ChefHat size={19} aria-hidden="true" /></span>
                <span className="portal-link-copy"><strong>Kitchen team</strong><small>Manage incoming orders and preparation.</small></span>
                <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
              </Link>
              <Link className="portal-link" to="/login?type=waiter">
                <span className="portal-icon"><UtensilsCrossed size={19} aria-hidden="true" /></span>
                <span className="portal-link-copy"><strong>Waiter</strong><small>Take and update table orders.</small></span>
                <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
              </Link>
              <Link className="portal-link" to="/login?type=cashier">
                <span className="portal-icon"><CircleDollarSign size={19} aria-hidden="true" /></span>
                <span className="portal-link-copy"><strong>Cashier</strong><small>Monitor orders and manage billing.</small></span>
                <ArrowRight className="portal-arrow" size={18} aria-hidden="true" />
              </Link>
            </div>
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
