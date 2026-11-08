import { ChangeEvent, FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import { api } from '../api';
import { DailySales, MenuItem, Order, OrderStatus, SalesReport, UserDto } from '../types';

type Section = 'overview' | 'orders' | 'menu' | 'reports' | 'staff';
type MenuForm = { name: string; description: string; price: string; imageUrl: string };
type KitchenUserForm = { username: string; fullName: string; password: string };
const emptyMenuForm: MenuForm = { name: '', description: '', price: '', imageUrl: '' };
const emptyKitchenUserForm: KitchenUserForm = { username: '', fullName: '', password: '' };
const statuses: OrderStatus[] = ['NEW', 'PREPARING', 'READY', 'COMPLETED'];

const getRoleFromPath = (workspace: string) => {
  switch (workspace) {
    case 'admin': return 'ADMIN';
    case 'waiter': return 'WAITER';
    case 'kitchen': return 'KITCHEN';
    case 'cashier': return 'CASHIER';
    default: return 'ADMIN';
  }
};

const formatPrice = (value: number) => new Intl.NumberFormat(undefined, {
  style: 'currency',
  currency: 'USD',
}).format(value);

const getErrorMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message || fallback;
  }
  return error instanceof Error ? error.message : fallback;
};

const formatDate = (date: string) => new Date(date).toLocaleString();
const emptyReport: SalesReport = {
  ordersToday: 0,
  revenueToday: 0,
  ordersThisMonth: 0,
  revenueThisMonth: 0,
  dailySales: [],
};

export default function DashboardPage() {
  const navigate = useNavigate();
  const { workspace } = useParams();
  const [user, setUser] = useState<UserDto | null>(null);
  const [activeSection, setActiveSection] = useState<Section>('overview');
  const [menu, setMenu] = useState<MenuItem[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [report, setReport] = useState<SalesReport>(emptyReport);
  const [menuForm, setMenuForm] = useState<MenuForm>(emptyMenuForm);
  const [kitchenUserForm, setKitchenUserForm] = useState<KitchenUserForm>(emptyKitchenUserForm);
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [customerName, setCustomerName] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const isAdmin = user?.roles.includes('ADMIN') ?? false;
  const isKitchen = user?.roles.some((role) => role === 'KITCHEN' || role === 'CHEF') ?? false;

  const refreshData = useCallback(async () => {
    if (workspace === 'kitchen') {
      const orderResponse = await api.get<Order[]>('/orders');
      setOrders(orderResponse.data);
      return;
    }

    const [menuResponse, orderResponse] = await Promise.all([
      api.get<MenuItem[]>('/menu'),
      api.get<Order[]>('/orders'),
    ]);
    setMenu(menuResponse.data);
    setOrders(orderResponse.data);
    if (workspace === 'admin' || workspace === 'cashier') {
      const reportResponse = await api.get<SalesReport>('/reports/sales');
      setReport(reportResponse.data);
    }
  }, [workspace]);

  const handleExpiredSession = useCallback((requestError: unknown) => {
    if (axios.isAxiosError(requestError) && [401, 403].includes(requestError.response?.status ?? 0)) {
      localStorage.removeItem('restaurant-user');
      navigate('/login', { replace: true });
      return true;
    }
    return false;
  }, [navigate]);

  useEffect(() => {
    const stored = localStorage.getItem('restaurant-user');
    if (!stored) {
      navigate('/login');
      return;
    }

    try {
      const parsed = JSON.parse(stored) as UserDto;
      if (!parsed.roles?.length) {
        localStorage.removeItem('restaurant-user');
        navigate('/login');
        return;
      }
      const workspaceRoles = workspace === 'kitchen'
        ? ['KITCHEN', 'CHEF']
        : [getRoleFromPath(workspace || 'admin')];
      if (!workspaceRoles.some((role) => parsed.roles.includes(role as UserDto['roles'][number]))) {
        navigate('/access-denied');
        return;
      }

      setUser(parsed);
      if (workspace === 'kitchen') setActiveSection('orders');
      setLoading(true);
      refreshData()
        .catch((requestError: unknown) => {
          if (handleExpiredSession(requestError)) return;
          setError(getErrorMessage(requestError, 'Could not load your order data.'));
        })
        .finally(() => setLoading(false));
    } catch {
      localStorage.removeItem('restaurant-user');
      navigate('/login');
    }
  }, [handleExpiredSession, navigate, refreshData, workspace]);

  const title = useMemo(() => {
    switch (workspace) {
      case 'admin': return 'Order Management';
      case 'waiter': return 'Orders';
      case 'kitchen': return 'Kitchen Orders';
      case 'cashier': return 'Sales';
      default: return 'Order Management';
    }
  }, [workspace]);

  const submitMenuItem = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const payload = {
        ...menuForm,
        price: Number(menuForm.price),
        available: true,
      };
      if (editingItemId !== null) {
        await api.put(`/menu/${editingItemId}`, payload);
        setNotice('Menu item updated.');
      } else {
        await api.post('/menu', payload);
        setNotice('Menu item added.');
      }
      setMenuForm(emptyMenuForm);
      setEditingItemId(null);
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not save this menu item.'));
    } finally {
      setSaving(false);
    }
  };

  const createKitchenUser = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const response = await api.post<UserDto>('/admin/kitchen-users', kitchenUserForm);
      setKitchenUserForm(emptyKitchenUserForm);
      setNotice(`Kitchen staff account "${response.data.username}" was created. Share its username and password securely.`);
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not provision this kitchen staff account.'));
    } finally {
      setSaving(false);
    }
  };

  const chooseImage = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setError('Choose an image file.');
      event.target.value = '';
      return;
    }
    if (file.size > 2 * 1024 * 1024) {
      setError('Images must be 2 MB or smaller.');
      event.target.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setMenuForm((form) => ({ ...form, imageUrl: reader.result as string }));
        setError('');
      }
    };
    reader.onerror = () => setError('Could not read the selected image.');
    reader.readAsDataURL(file);
    event.target.value = '';
  };

  const deleteMenuItem = async (item: MenuItem) => {
    if (!window.confirm(`Delete "${item.name}" from the menu?`)) return;
    setError('');
    setNotice('');
    try {
      await api.delete(`/menu/${item.id}`);
      setNotice('Menu item deleted.');
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not delete this menu item.'));
    }
  };

  const submitOrder = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const items = Object.entries(cart)
      .filter(([, quantity]) => quantity > 0)
      .map(([menuItemId, quantity]) => ({ menuItemId: Number(menuItemId), quantity }));
    if (items.length === 0) {
      setError('Add at least one menu item to the order.');
      return;
    }

    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post('/orders', { customerName, items });
      setCart({});
      setCustomerName('');
      setNotice('Order created.');
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not create this order.'));
    } finally {
      setSaving(false);
    }
  };

  const updateOrderStatus = async (id: number, status: OrderStatus) => {
    setError('');
    try {
      await api.patch(`/orders/${id}/status`, { status });
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not update the order.'));
    }
  };

  const deleteOrder = async (order: Order) => {
    if (!window.confirm(`Delete order #${order.id}? This will also remove it from sales reports.`)) return;
    setError('');
    try {
      await api.delete(`/orders/${order.id}`);
      setNotice(`Order #${order.id} deleted.`);
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not delete this order.'));
    }
  };

  const logout = async () => {
    setError('');
    try {
      await api.post('/auth/logout');
      localStorage.removeItem('restaurant-user');
      navigate('/login');
    } catch (requestError: unknown) {
      setError(getErrorMessage(requestError, 'Could not log out. Please try again.'));
    }
  };

  const changeCartQuantity = (id: number, adjustment: number) => {
    setCart((current) => {
      const next = { ...current, [id]: Math.max(0, (current[id] ?? 0) + adjustment) };
      if (next[id] === 0) delete next[id];
      return next;
    });
  };

  const sectionName = activeSection === 'overview'
    ? 'Overview'
    : activeSection === 'menu' ? 'Menu' : activeSection === 'orders' ? (isKitchen ? 'Kitchen orders' : 'Orders')
      : activeSection === 'staff' ? 'Kitchen staff' : 'Reports';
  const hasSampleOrders = orders.some((order) => order.customerName.startsWith('Sample · '));
  const maxDailyRevenue = Math.max(...report.dailySales.map((day) => day.revenue), 1);
  const totalCart = menu.reduce((sum, item) => sum + item.price * (cart[item.id] ?? 0), 0);

  if (loading || !user) {
    return <main className="loading-screen">Loading your workspace…</main>;
  }

  const renderOrders = () => (
    <div className="orders-layout">
      <section className="panel order-builder">
        <div className="section-heading">
          <div><p className="eyebrow">New sale</p><h2>Create an order</h2></div>
          <span className="muted">{menu.filter((item) => item.available).length} items available</span>
        </div>
        <form onSubmit={submitOrder}>
          <label className="field-label" htmlFor="customer-name">Customer name <span>(optional)</span></label>
          <input id="customer-name" value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="Walk-in customer" maxLength={120} />
          {menu.filter((item) => item.available).length === 0 ? (
            <p className="empty-state">Add an available item in the Menu section to start an order.</p>
          ) : (
            <div className="order-menu-list">
              {menu.filter((item) => item.available).map((item) => (
                <div className="order-menu-item" key={item.id}>
                  <img className="item-image item-image-small" src={item.imageUrl || undefined} alt="" />
                  <div className="order-menu-copy"><strong>{item.name}</strong><span>{formatPrice(item.price)}</span></div>
                  <div className="quantity-control">
                    <button type="button" aria-label={`Remove one ${item.name}`} onClick={() => changeCartQuantity(item.id, -1)}>−</button>
                    <span>{cart[item.id] ?? 0}</span>
                    <button type="button" aria-label={`Add one ${item.name}`} onClick={() => changeCartQuantity(item.id, 1)}>+</button>
                  </div>
                </div>
              ))}
            </div>
          )}
          <div className="order-total"><span>Order total</span><strong>{formatPrice(totalCart)}</strong></div>
          <button className="primary-button full-width" type="submit" disabled={saving || totalCart === 0}>{saving ? 'Saving…' : 'Place order'}</button>
        </form>
      </section>

      <section className="panel">
        <div className="section-heading"><div><p className="eyebrow">Live tickets</p><h2>Recent orders</h2></div></div>
        {orders.length === 0 ? <p className="empty-state">Orders you place will appear here.</p> : (
          <div className="order-list">
            {orders.map((order) => (
              <article className="order-card" key={order.id}>
                <div className="order-card-header"><div><strong>Order #{order.id}</strong><span>{order.customerName}</span></div><strong>{formatPrice(order.total)}</strong></div>
                <p className="order-items-summary">{order.items.map((item) => `${item.quantity} × ${item.itemName}`).join(' · ')}</p>
                <div className="order-card-footer"><span>{formatDate(order.createdAt)}</span><div className="row-actions">
                  {(isAdmin || isKitchen)
                    ? <select aria-label={`Status for order ${order.id}`} value={order.status} onChange={(event) => updateOrderStatus(order.id, event.target.value as OrderStatus)}>
                      {statuses.map((status) => <option value={status} key={status}>{status.replace('_', ' ')}</option>)}
                    </select>
                    : <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status.replace('_', ' ')}</span>}
                  {isAdmin && <button className="text-button danger" type="button" onClick={() => deleteOrder(order)}>Delete</button>}
                </div></div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );

  const renderKitchenOrders = () => {
    const newOrders = orders.filter((order) => order.status === 'NEW');
    const preparingOrders = orders.filter((order) => order.status === 'PREPARING' || order.status === 'READY');
    const completedOrders = orders.filter((order) => order.status === 'COMPLETED');
    return (
      <>
        <section className="cards-grid kitchen-counts">
          <div className="metric-card"><div><span>Incoming</span><strong>{newOrders.length}</strong></div><span className="metric-icon">New</span></div>
          <div className="metric-card"><div><span>In progress</span><strong>{preparingOrders.length}</strong></div><span className="metric-icon">Preparing</span></div>
          <div className="metric-card"><div><span>Completed</span><strong>{completedOrders.length}</strong></div><span className="metric-icon">Done</span></div>
        </section>
        <section className="panel">
          <div className="section-heading"><div><p className="eyebrow">Incoming tickets</p><h2>Kitchen queue</h2></div><span className="muted">{orders.length} total orders</span></div>
          {orders.length === 0 ? <p className="empty-state">New customer orders will appear here.</p> : (
            <div className="order-list">
              {orders.map((order) => (
                <article className="order-card kitchen-order-card" key={order.id}>
                  <div className="order-card-header"><div><strong>Order #{order.id}</strong><span>{order.customerName} · {formatDate(order.createdAt)}</span></div><span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status.replace('_', ' ')}</span></div>
                  <ul className="kitchen-items">{order.items.map((item) => <li key={`${order.id}-${item.menuItemId}-${item.itemName}`}><strong>{item.quantity} ×</strong> {item.itemName}</li>)}</ul>
                  {order.status === 'NEW' && <button className="primary-button" type="button" onClick={() => updateOrderStatus(order.id, 'PREPARING')}>Start preparing</button>}
                  {(order.status === 'PREPARING' || order.status === 'READY') && <button className="primary-button" type="button" onClick={() => updateOrderStatus(order.id, 'COMPLETED')}>Mark completed</button>}
                </article>
              ))}
            </div>
          )}
        </section>
      </>
    );
  };

  const renderKitchenStaff = () => (
    <section className="panel staff-panel">
      <p className="eyebrow">Provision staff access</p>
      <h2>Create a kitchen staff account</h2>
      <p className="muted">Set a unique username and a strong password. Passwords are stored as BCrypt hashes.</p>
      <form className="staff-form stacked-form" onSubmit={createKitchenUser}>
        <label className="field-label" htmlFor="staff-full-name">Full name <span>(optional)</span></label>
        <input id="staff-full-name" maxLength={150} value={kitchenUserForm.fullName} onChange={(event) => setKitchenUserForm({ ...kitchenUserForm, fullName: event.target.value })} placeholder="Kitchen team member" />
        <label className="field-label" htmlFor="staff-username">Username</label>
        <input id="staff-username" required minLength={3} maxLength={100} autoComplete="off" value={kitchenUserForm.username} onChange={(event) => setKitchenUserForm({ ...kitchenUserForm, username: event.target.value })} />
        <label className="field-label" htmlFor="staff-password">Password <span>(at least 12 characters)</span></label>
        <input id="staff-password" type="password" required minLength={12} maxLength={72} autoComplete="new-password" value={kitchenUserForm.password} onChange={(event) => setKitchenUserForm({ ...kitchenUserForm, password: event.target.value })} />
        <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Creating account…' : 'Create kitchen account'}</button>
      </form>
    </section>
  );

  const renderMenu = () => (
    <div className="menu-layout">
      {isAdmin && (
        <section className="panel menu-editor">
          <p className="eyebrow">{editingItemId === null ? 'Add to your menu' : 'Edit menu item'}</p>
          <h2>{editingItemId === null ? 'New menu item' : 'Update menu item'}</h2>
          <form className="stacked-form" onSubmit={submitMenuItem}>
            <label className="field-label" htmlFor="item-name">Item name</label>
            <input id="item-name" required maxLength={120} value={menuForm.name} onChange={(event) => setMenuForm({ ...menuForm, name: event.target.value })} placeholder="e.g. Garden salad" />
            <label className="field-label" htmlFor="item-description">Description</label>
            <textarea id="item-description" maxLength={500} value={menuForm.description} onChange={(event) => setMenuForm({ ...menuForm, description: event.target.value })} placeholder="A short description" rows={3} />
            <label className="field-label" htmlFor="item-price">Price</label>
            <input id="item-price" type="number" min="0.01" step="0.01" required value={menuForm.price} onChange={(event) => setMenuForm({ ...menuForm, price: event.target.value })} placeholder="0.00" />
            <label className="field-label" htmlFor="item-photo">Photo <span>(optional, max 2 MB)</span></label>
            <input id="item-photo" type="file" accept="image/*" onChange={chooseImage} />
            {menuForm.imageUrl && <div className="photo-preview"><img src={menuForm.imageUrl} alt="Menu item preview" /><button className="text-button danger" type="button" onClick={() => setMenuForm({ ...menuForm, imageUrl: '' })}>Remove photo</button></div>}
            <div className="editor-actions">
              <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Saving…' : editingItemId === null ? 'Add item' : 'Save changes'}</button>
              {editingItemId !== null && <button className="secondary-button" type="button" onClick={() => { setEditingItemId(null); setMenuForm(emptyMenuForm); }}>Cancel</button>}
            </div>
          </form>
        </section>
      )}
      <section className="panel menu-catalog">
        <div className="section-heading"><div><p className="eyebrow">Available products</p><h2>Menu items</h2></div><span className="muted">{menu.length} total</span></div>
        {menu.length === 0 ? <p className="empty-state">No menu items yet{isAdmin ? ' — add the first one to get started.' : '.'}</p> : (
          <div className="menu-grid">
            {menu.map((item) => (
              <article className="menu-card" key={item.id}>
                <img
                  className="item-image"
                  src={item.imageUrl || undefined}
                  alt={item.name}
                  onLoad={(event) => { event.currentTarget.style.visibility = 'visible'; }}
                  onError={(event) => { event.currentTarget.style.visibility = 'hidden'; }}
                />
                <div className="menu-card-content"><div className="menu-item-heading"><h3>{item.name}</h3><span className={item.available ? 'availability available' : 'availability unavailable'}>{item.available ? 'Available' : 'Unavailable'}</span></div>
                  {item.description && <p className="muted item-description">{item.description}</p>}
                  <div className="menu-card-footer"><strong>{formatPrice(item.price)}</strong>
                    {isAdmin && <div className="row-actions"><button className="text-button" type="button" onClick={() => { setEditingItemId(item.id); setMenuForm({ name: item.name, description: item.description ?? '', price: String(item.price), imageUrl: item.imageUrl ?? '' }); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Edit</button><button className="text-button danger" type="button" onClick={() => deleteMenuItem(item)}>Delete</button></div>}
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );

  const renderReports = () => (
    <>
      <section className="cards-grid">
        <div className="metric-card"><div><span>Revenue today</span><strong>{formatPrice(report.revenueToday)}</strong></div><span className="metric-icon">Today</span></div>
        <div className="metric-card"><div><span>Orders today</span><strong>{report.ordersToday}</strong></div><span className="metric-icon">Orders</span></div>
        <div className="metric-card"><div><span>Revenue this month</span><strong>{formatPrice(report.revenueThisMonth)}</strong></div><span className="metric-icon">Month</span></div>
        <div className="metric-card"><div><span>Orders this month</span><strong>{report.ordersThisMonth}</strong></div><span className="metric-icon">Orders</span></div>
      </section>
      <section className="panel report-panel">
        {hasSampleOrders && <p className="demo-notice">Demo report: totals include example orders marked “Sample” in Orders. Delete those sample orders when you’re ready to use your own sales.</p>}
        <div className="section-heading"><div><p className="eyebrow">{hasSampleOrders ? 'Example sales data' : 'Saved sales records'}</p><h2>Daily sales · last 30 days</h2></div></div>
        {report.dailySales.length === 0 ? <p className="empty-state">Sales will be reported here as orders are placed.</p> : (
          <div className="sales-chart">
            {report.dailySales.map((day: DailySales) => (
              <div className="chart-column" key={day.date} title={`${day.date}: ${formatPrice(day.revenue)} (${day.orderCount} orders)`}>
                <span className="chart-value">{day.revenue > 0 ? formatPrice(day.revenue) : ''}</span>
                <div className="chart-track"><div className="chart-bar" style={{ height: `${day.revenue === 0 ? 2 : Math.max(8, day.revenue / maxDailyRevenue * 100)}%` }} /></div>
                <span className="chart-date">{day.date.slice(5)}</span>
              </div>
            ))}
          </div>
        )}
      </section>
    </>
  );

  const renderOverview = () => (
    <>
      <section className="cards-grid">
        <div className="metric-card"><div><span>Revenue today</span><strong>{formatPrice(report.revenueToday)}</strong></div><span className="metric-icon">Today</span></div>
        <div className="metric-card"><div><span>Orders today</span><strong>{report.ordersToday}</strong></div><span className="metric-icon">Orders</span></div>
        <div className="metric-card"><div><span>Revenue this month</span><strong>{formatPrice(report.revenueThisMonth)}</strong></div><span className="metric-icon">Month</span></div>
        <div className="metric-card"><div><span>Menu items</span><strong>{menu.length}</strong></div><span className="metric-icon">Menu</span></div>
      </section>
      {hasSampleOrders && <p className="demo-notice overview-demo-notice">Demo data is included in these sales totals. Sample orders are clearly marked in Orders and can be deleted.</p>}
      <section className="panel">
        <div className="section-heading"><div><p className="eyebrow">Latest activity</p><h2>Recent orders</h2></div><button className="secondary-button" type="button" onClick={() => setActiveSection('orders')}>View all orders</button></div>
        {orders.length === 0 ? <p className="empty-state">No orders yet. Create your first order to see sales data here.</p> : (
          <div className="table-scroll"><table className="data-table"><thead><tr><th>Order</th><th>Customer</th><th>Items</th><th>Status</th><th>Total</th><th>Time</th></tr></thead>
            <tbody>{orders.slice(0, 6).map((order) => <tr key={order.id}><td>#{order.id}</td><td>{order.customerName}</td><td>{order.items.map((item) => `${item.quantity} × ${item.itemName}`).join(', ')}</td><td><span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status.replace('_', ' ')}</span></td><td>{formatPrice(order.total)}</td><td>{formatDate(order.createdAt)}</td></tr>)}</tbody>
          </table></div>
        )}
      </section>
    </>
  );

  return (
    <div className="dashboard-shell">
      <aside className="sidebar">
        <div className="brand-header small-brand"><div className="brand-mark">O</div><div><h2>Order Desk</h2><p className="sidebar-subtitle">Management system</p></div></div>
        <p className="nav-caption">WORKSPACE</p>
        <nav className="nav-list" aria-label="Main navigation">
          {(isKitchen ? [
            ['orders', 'Kitchen orders'],
          ] as [Section, string][] : [
            ['overview', 'Overview'],
            ['orders', 'Orders'],
            ['menu', 'Menu'],
            ...(isAdmin ? [['staff', 'Kitchen staff'] as [Section, string]] : []),
            ...((isAdmin || user.roles.includes('CASHIER')) ? [['reports', 'Reports'] as [Section, string]] : []),
          ] as [Section, string][]).map(([section, label]) => (
            <button key={section} className={`nav-item ${activeSection === section ? 'active' : ''}`} onClick={() => { setActiveSection(section); setError(''); setNotice(''); }}>{label}</button>
          ))}
        </nav>
        <div className="sidebar-footer"><div className="sidebar-user"><div className="user-avatar">{(user.fullName || user.username).charAt(0).toUpperCase()}</div><div><strong>{user.fullName || user.username}</strong><span>{user.roles[0]?.toLowerCase()}</span></div></div><button className="logout-button" onClick={logout}>Log out</button></div>
      </aside>

      <main className="content-shell">
        <header className="topbar"><div><p className="eyebrow">{title}</p><h1>{sectionName}</h1></div><button className="secondary-button refresh-button" type="button" onClick={() => { setError(''); refreshData().catch((requestError: unknown) => { if (!handleExpiredSession(requestError)) setError(getErrorMessage(requestError, 'Could not refresh data.')); }); }}>Refresh data</button></header>
        {error && <div className="alert-error" role="alert">{error}</div>}
        {notice && <div className="alert-success" role="status">{notice}</div>}
        {!isKitchen && activeSection === 'overview' && renderOverview()}
        {activeSection === 'orders' && (isKitchen ? renderKitchenOrders() : renderOrders())}
        {!isKitchen && activeSection === 'menu' && renderMenu()}
        {!isKitchen && isAdmin && activeSection === 'staff' && renderKitchenStaff()}
        {!isKitchen && (isAdmin || user.roles.includes('CASHIER')) && activeSection === 'reports' && renderReports()}
      </main>
    </div>
  );
}
