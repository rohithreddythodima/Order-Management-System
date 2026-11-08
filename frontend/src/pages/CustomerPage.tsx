import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { MenuItem, Order, TrackedOrder } from '../types';

type SavedTracking = { code: string; order: TrackedOrder };
const trackingStorageKey = 'restaurant-order-tracking';
const formatPrice = (value: number) => new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency: 'USD',
}).format(value);

export default function CustomerPage() {
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [customerName, setCustomerName] = useState('');
  const [trackingCode, setTrackingCode] = useState('');
  const [trackedOrders, setTrackedOrders] = useState<SavedTracking[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const refreshTrackedOrders = useCallback(async (entries: SavedTracking[]) => {
    const refreshed = await Promise.all(entries.map(async (entry) => {
      try {
        const response = await api.get<TrackedOrder>(`/orders/track/${encodeURIComponent(entry.code)}`);
        return { ...entry, order: response.data };
      } catch {
        return entry;
      }
    }));
    setTrackedOrders(refreshed);
  }, []);

  useEffect(() => {
    let saved: SavedTracking[] = [];
    try {
      const parsed = JSON.parse(localStorage.getItem(trackingStorageKey) || '[]') as SavedTracking[];
      saved = Array.isArray(parsed) ? parsed.filter((entry) => typeof entry?.code === 'string') : [];
    } catch {
      localStorage.removeItem(trackingStorageKey);
    }
    setTrackedOrders(saved);
    Promise.all([
      api.get<MenuItem[]>('/menu').then((response) => setMenu(response.data)),
      refreshTrackedOrders(saved),
    ])
      .catch(() => setError('Could not load the menu. Please try again.'))
      .finally(() => setLoading(false));
  }, [refreshTrackedOrders]);

  useEffect(() => {
    localStorage.setItem(trackingStorageKey, JSON.stringify(trackedOrders));
  }, [trackedOrders]);

  useEffect(() => {
    if (trackedOrders.length === 0) return undefined;
    const timer = window.setInterval(() => {
      void refreshTrackedOrders(trackedOrders);
    }, 15000);
    return () => window.clearInterval(timer);
  }, [refreshTrackedOrders, trackedOrders]);

  const total = useMemo(() => menu.reduce(
    (sum, item) => sum + item.price * (cart[item.id] ?? 0), 0,
  ), [cart, menu]);

  const changeQuantity = (id: number, change: number) => {
    setCart((current) => {
      const next = { ...current, [id]: Math.max(0, (current[id] ?? 0) + change) };
      if (next[id] === 0) delete next[id];
      return next;
    });
  };

  const submitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const items = Object.entries(cart)
      .filter(([, quantity]) => quantity > 0)
      .map(([menuItemId, quantity]) => ({ menuItemId: Number(menuItemId), quantity }));
    if (!items.length) {
      setError('Add at least one menu item to your order.');
      return;
    }

    setSaving(true);
    setError('');
    setNotice('');
    try {
      const response = await api.post<Order>('/orders', { customerName, items });
      if (!response.data.trackingCode) throw new Error('The order was placed but a tracking code was not returned.');
      const newEntry = {
        code: response.data.trackingCode,
        order: {
          id: response.data.id,
          customerName: response.data.customerName,
          status: response.data.status,
          total: response.data.total,
          createdAt: response.data.createdAt,
          items: response.data.items,
        },
      };
      setTrackedOrders((current) => [newEntry, ...current.filter((entry) => entry.code !== newEntry.code)]);
      setCart({});
      setCustomerName('');
      setNotice(`Order #${response.data.id} placed. Save your tracking code: ${response.data.trackingCode}`);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Could not place your order.');
    } finally {
      setSaving(false);
    }
  };

  const addTrackingCode = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const code = trackingCode.trim();
    if (!code) return;
    setError('');
    try {
      const response = await api.get<TrackedOrder>(`/orders/track/${encodeURIComponent(code)}`);
      const entry = { code, order: response.data };
      setTrackedOrders((current) => [entry, ...current.filter((saved) => saved.code !== code)]);
      setTrackingCode('');
    } catch {
      setError('Order not found. Check the tracking code and try again.');
    }
  };

  const refreshOrders = () => refreshTrackedOrders(trackedOrders);
  const availableMenu = menu.filter((item) => item.available);

  return (
    <div className="customer-shell">
      <header className="customer-header">
        <div className="brand-header"><div className="brand-mark">O</div><div><h1>Order Desk</h1><p className="muted">Browse the menu and order online</p></div></div>
        <Link to="/login" className="secondary-button">Staff sign in</Link>
      </header>
      <main className="customer-content">
        {error && <div className="alert-error" role="alert">{error}</div>}
        {notice && <div className="alert-success" role="status">{notice}</div>}
        {loading ? <p className="loading-screen">Loading menu…</p> : (
          <>
            <section className="panel customer-menu-panel">
              <div className="section-heading"><div><p className="eyebrow">Fresh from our kitchen</p><h2>Menu</h2></div><span className="muted">{availableMenu.length} items available</span></div>
              {availableMenu.length === 0 ? <p className="empty-state">The menu is being updated. Please check back soon.</p> : (
                <form onSubmit={submitOrder}>
                  <div className="menu-grid">
                    {availableMenu.map((item) => (
                      <article className="menu-card" key={item.id}>
                        {item.imageUrl && <img className="item-image" src={item.imageUrl} alt={item.name} />}
                        <div className="menu-card-content">
                          <div className="menu-item-heading"><h3>{item.name}</h3><strong>{formatPrice(item.price)}</strong></div>
                          {item.description && <p className="muted item-description">{item.description}</p>}
                          <div className="customer-item-controls">
                            <span>Quantity</span>
                            <div className="quantity-control">
                              <button type="button" aria-label={`Remove one ${item.name}`} onClick={() => changeQuantity(item.id, -1)}>−</button>
                              <span>{cart[item.id] ?? 0}</span>
                              <button type="button" aria-label={`Add one ${item.name}`} onClick={() => changeQuantity(item.id, 1)}>+</button>
                            </div>
                          </div>
                        </div>
                      </article>
                    ))}
                  </div>
                  <div className="customer-order-submit">
                    <label className="field-label" htmlFor="customer-name">Name for the order <span>(optional)</span></label>
                    <input id="customer-name" maxLength={120} value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="Walk-in customer" />
                    <div className="order-total"><span>Order total</span><strong>{formatPrice(total)}</strong></div>
                    <button className="primary-button full-width" type="submit" disabled={saving || total === 0}>{saving ? 'Placing order…' : 'Place order'}</button>
                  </div>
                </form>
              )}
            </section>

            <section className="panel tracking-panel">
              <div className="section-heading"><div><p className="eyebrow">Order updates</p><h2>Track an order</h2></div><button className="secondary-button" type="button" onClick={refreshOrders}>Refresh status</button></div>
              <form className="tracking-form" onSubmit={addTrackingCode}>
                <input aria-label="Order tracking code" value={trackingCode} onChange={(event) => setTrackingCode(event.target.value)} placeholder="Enter your order tracking code" />
                <button className="secondary-button" type="submit">Track order</button>
              </form>
              {trackedOrders.length === 0 ? <p className="empty-state">After placing an order, its tracking status will appear here. Keep your tracking code to check from another device.</p> : (
                <div className="order-list">
                  {trackedOrders.map(({ code, order }) => (
                    <article className="order-card" key={code}>
                      <div className="order-card-header"><div><strong>Order #{order.id}</strong><span>{order.customerName}</span></div><strong>{formatPrice(order.total)}</strong></div>
                      <p className="order-items-summary">{order.items.map((item) => `${item.quantity} × ${item.itemName}`).join(' · ')}</p>
                      <div className="order-card-footer"><span>{new Date(order.createdAt).toLocaleString()}</span><span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status.replace('_', ' ')}</span></div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
