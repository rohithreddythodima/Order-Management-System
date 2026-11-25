import { ChangeEvent, FormEvent, ReactNode, useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import { Armchair, BarChart3, ClipboardList, LayoutDashboard, LogOut, RefreshCw, Users, UtensilsCrossed } from 'lucide-react';
import { api } from '../api';
import { DailySales, MenuItem, Order, OrderStatus, RestaurantTable, Role, SalesReport, TableStatus, UserDto } from '../types';

type Section = 'overview' | 'orders' | 'menu' | 'reports' | 'staff' | 'tables';
type MenuForm = { name: string; description: string; category: string; price: string; imageUrl: string };
type StaffForm = { id: number | null; username: string; fullName: string; password: string; roles: Role[] };
const emptyMenuForm: MenuForm = { name: '', description: '', category: '', price: '', imageUrl: '' };
const emptyStaffForm: StaffForm = { id: null, username: '', fullName: '', password: '', roles: ['KITCHEN'] };
const sectionIcons: Record<Section, ReactNode> = {
  overview: <LayoutDashboard size={18} aria-hidden="true" />,
  orders: <ClipboardList size={18} aria-hidden="true" />,
  menu: <UtensilsCrossed size={18} aria-hidden="true" />,
  reports: <BarChart3 size={18} aria-hidden="true" />,
  staff: <Users size={18} aria-hidden="true" />,
  tables: <Armchair size={18} aria-hidden="true" />,
};

const getRoleFromPath = (workspace: string) => {
  switch (workspace) {
    case 'admin': return 'ADMIN';
    case 'waiter': return 'WAITER';
    case 'kitchen': return 'KITCHEN';
    case 'cashier': return 'CASHIER';
    default: return undefined;
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
  const [tables, setTables] = useState<RestaurantTable[]>([]);
  const [staff, setStaff] = useState<UserDto[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [report, setReport] = useState<SalesReport>(emptyReport);
  const [menuForm, setMenuForm] = useState<MenuForm>(emptyMenuForm);
  const [menuSearch, setMenuSearch] = useState('');
  const [staffForm, setStaffForm] = useState<StaffForm>(emptyStaffForm);
  const [tableNumberInput, setTableNumberInput] = useState('');
  const [tableStatusInput, setTableStatusInput] = useState<TableStatus>('AVAILABLE');
  const [selectedTableId, setSelectedTableId] = useState('');
  const [progressTable, setProgressTable] = useState('');
  const [editingItemId, setEditingItemId] = useState<number | null>(null);
  const [cart, setCart] = useState<Record<number, number>>({});
  const [customerName, setCustomerName] = useState('');
  const [billOrder, setBillOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const isAdmin = workspace === 'admin' && (user?.roles.includes('ADMIN') ?? false);
  const isKitchen = workspace === 'kitchen' && (user?.roles.some((role) => role === 'KITCHEN' || role === 'CHEF') ?? false);
  const isWaiter = workspace === 'waiter' && (user?.roles.includes('WAITER') ?? false);
  const isCashier = workspace === 'cashier' && (user?.roles.includes('CASHIER') ?? false);

  const refreshOrders = useCallback(async () => {
    const response = await api.get<Order[]>('/orders', {
      params: workspace === 'waiter' ? { assignedTables: true } : undefined,
    });
    setOrders(response.data);
  }, [workspace]);

  const refreshData = useCallback(async () => {
    const menuRequest = api.get<MenuItem[]>('/menu').then(({ data }) => setMenu(data));
    const tableRequest = workspace !== 'kitchen'
      ? api.get<RestaurantTable[]>('/tables').then(({ data }) => setTables(data))
      : undefined;
    await Promise.all([menuRequest, tableRequest, refreshOrders()]);
    if (workspace === 'admin') {
      const staffResponse = await api.get<UserDto[]>('/admin/staff');
      setStaff(staffResponse.data);
    }
    if (workspace === 'admin' || workspace === 'cashier') {
      const reportResponse = await api.get<SalesReport>('/reports/sales');
      setReport(reportResponse.data);
    }
  }, [refreshOrders, workspace]);

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
        : [getRoleFromPath(workspace ?? '')];
      if (!workspaceRoles.some((role) => parsed.roles.includes(role as UserDto['roles'][number]))) {
        navigate('/login?denied=1', { replace: true });
        return;
      }

      setUser(parsed);
      if (workspace === 'kitchen' || workspace === 'cashier' || workspace === 'waiter') setActiveSection('orders');
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

  useEffect(() => {
    if (!user || activeSection !== 'orders') return;
    const timer = window.setInterval(() => {
      refreshOrders().catch((requestError: unknown) => {
        if (!handleExpiredSession(requestError)) setError(getErrorMessage(requestError, 'Could not refresh orders.'));
      });
    }, 5000);
    return () => window.clearInterval(timer);
  }, [activeSection, handleExpiredSession, refreshOrders, user]);

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

  const saveStaff = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (staffForm.roles.length === 0) {
      setError('Select at least one role for this staff account.');
      return;
    }
    setSaving(true);
    setError('');
    setNotice('');
    try {
      const { id, ...staffFields } = staffForm;
      const payload = { ...staffFields, password: staffForm.password || null };
      const response = id === null
        ? await api.post<UserDto>('/admin/staff', payload)
        : await api.put<UserDto>(`/admin/staff/${id}`, payload);
      setStaffForm(emptyStaffForm);
      setNotice(`Staff account "${response.data.username}" ${staffForm.id === null ? 'created' : 'updated'}.`);
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not save this staff account.'));
    } finally {
      setSaving(false);
    }
  };

  const deleteStaff = async (member: UserDto) => {
    if (!window.confirm(`Delete staff account "${member.username}"?`)) return;
    setError('');
    setNotice('');
    try {
      await api.delete(`/admin/staff/${member.id}`);
      setNotice(`Staff account "${member.username}" deleted.`);
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not delete this staff account.'));
    }
  };

  const createTable = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await api.post('/tables', { id: Number(tableNumberInput), status: tableStatusInput });
      setTableNumberInput('');
      setTableStatusInput('AVAILABLE');
      setNotice('Table added.');
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not create this table.'));
    } finally {
      setSaving(false);
    }
  };

  const updateTableStatus = async (table: RestaurantTable, status: TableStatus) => {
    try {
      await api.put(`/tables/${table.id}`, { id: table.id, status });
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not update this table.'));
    }
  };

  const deleteTable = async (table: RestaurantTable) => {
    if (!window.confirm(`Delete table ${table.id}?`)) return;
    try {
      await api.delete(`/tables/${table.id}`);
      setNotice(`Table ${table.id} deleted.`);
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not delete this table.'));
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
    const table = isWaiter ? tables.find((candidate) => candidate.id === Number(selectedTableId)) : undefined;
    if (isWaiter && !table) {
      setError('Select an available table or one of your active table orders.');
      return;
    }
    const existingOrder = table?.activeOrderId
      ? orders.find((order) => order.id === table.activeOrderId && order.waiterUsername === user?.username)
      : undefined;
    if (table?.activeOrderId && !existingOrder) {
      setError('This occupied table is not linked to one of your active orders.');
      return;
    }

    setSaving(true);
    setError('');
    setNotice('');
    try {
      if (existingOrder) {
        await api.post(`/orders/${existingOrder.id}/items`, { items });
        setNotice(`Items added to table ${table?.id}'s active order.`);
      } else {
        await api.post('/orders', { customerName, items, ...(table ? { tableId: table.id } : {}) });
        setNotice(table ? `Order created for table ${table.id}.` : 'Order created.');
      }
      setCart({});
      setCustomerName('');
      await refreshData();
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not create this order.'));
    } finally {
      setSaving(false);
    }
  };

  const updateItemStatus = async (orderId: number, itemId: number, status: OrderStatus) => {
    setError('');
    setSaving(true);
    try {
      await api.patch(`/orders/${orderId}/items/${itemId}/status`, { status });
      if (isWaiter && status === 'SERVED') {
        await refreshData();
      } else {
        await refreshOrders();
      }
    } catch (requestError: unknown) {
      if (handleExpiredSession(requestError)) return;
      setError(getErrorMessage(requestError, 'Could not update the item.'));
    } finally {
      setSaving(false);
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
      : activeSection === 'staff' ? 'Staff accounts' : activeSection === 'tables' ? 'Tables' : 'Reports';
  const hasSampleOrders = orders.some((order) => order.customerName.startsWith('Sample · '));
  const maxDailyRevenue = Math.max(...report.dailySales.map((day) => day.revenue), 1);
  const totalCart = menu.reduce((sum, item) => sum + item.price * (cart[item.id] ?? 0), 0);
  const visibleMenu = menu.filter((item) => `${item.name} ${item.category ?? ''}`.toLocaleLowerCase().includes(menuSearch.trim().toLocaleLowerCase()));

  if (loading || !user) {
    return <main className="loading-screen">Loading your workspace…</main>;
  }

  const assignedTableNumbers = [...new Set(orders.flatMap((order) => order.tableId == null ? [] : [order.tableId]))]
    .sort((first, second) => first - second);
  const visibleOrders = isWaiter && progressTable
    ? orders.filter((order) => (order.tableId?.toString() ?? 'pickup') === progressTable)
    : orders;

  const renderOrders = () => (
    <div className="orders-layout">
      {(isAdmin || isWaiter) && <section className="panel order-builder">
        <div className="section-heading">
          <div><p className="eyebrow">New sale</p><h2>Create an order</h2></div>
          <span className="muted">{menu.filter((item) => item.available).length} items available</span>
        </div>
        <form onSubmit={submitOrder}>
          {isWaiter && <>
            <label className="field-label" htmlFor="order-table">Table</label>
            <select id="order-table" required value={selectedTableId} onChange={(event) => setSelectedTableId(event.target.value)}>
              <option value="">Select a table</option>
              {tables.map((table) => {
                const hasOwnOrder = table.activeOrderId !== null && orders.some((order) => order.id === table.activeOrderId && order.waiterUsername === user.username);
                const disabled = table.status === 'RESERVED' || (table.status === 'OCCUPIED' && !hasOwnOrder);
                return <option key={table.id} value={table.id} disabled={disabled}>Table {table.id} · {hasOwnOrder ? 'Add to active order' : table.status.toLowerCase()}</option>;
              })}
            </select>
          </>}
          <label className="field-label" htmlFor="customer-name">Customer name <span>(optional)</span></label>
          <input id="customer-name" value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="Walk-in customer" maxLength={120} />
          <label className="field-label" htmlFor="order-menu-search">Search menu by name or category</label>
          <input id="order-menu-search" type="search" value={menuSearch} onChange={(event) => setMenuSearch(event.target.value)} placeholder="Search menu" />
          {menu.filter((item) => item.available).length === 0 ? (
            <p className="empty-state">Add an available item in the Menu section to start an order.</p>
          ) : (
            <div className="order-menu-list">
              {visibleMenu.filter((item) => item.available).map((item) => (
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
      </section>}

      <section className="panel">
        <div className="section-heading"><div><p className="eyebrow">Live tickets</p><h2>Recent orders</h2></div></div>
        {isWaiter && <>
          <label className="field-label" htmlFor="progress-table">Filter order progress by table</label>
          <select id="progress-table" value={progressTable} onChange={(event) => setProgressTable(event.target.value)}>
            <option value="">All assigned tables and pickup</option>
            {assignedTableNumbers.map((id) => <option key={id} value={id}>Table {id}</option>)}
            <option value="pickup">Pickup</option>
          </select>
        </>}
        {visibleOrders.length === 0 ? <p className="empty-state">No orders to show.</p> : (
          <div className="order-list">
            {visibleOrders.map((order) => (
              <article className="order-card" key={order.id}>
                <div className="order-card-header"><div><strong>Order #{order.id}{order.tableId ? ` · Table ${order.tableId}` : ''}</strong><span>{order.customerName}{order.waiterUsername ? ` · Waiter ${order.waiterUsername}` : ''}</span></div><strong>{formatPrice(order.total)}</strong></div>
                <ul className="kitchen-items">
                  {order.items.map((item) => (
                    <li key={item.id} className="order-item-progress">
                      <span>{item.quantity} × {item.itemName}</span>
                      <span className={`status-badge status-${item.status.toLowerCase()}`}>{item.status}</span>
                      {isWaiter && item.status === 'READY' && (
                        <button className="text-button" type="button" disabled={saving}
                          aria-label={`Serve ${item.itemName} for order ${order.id}`}
                          onClick={() => updateItemStatus(order.id, item.id, 'SERVED')}>Serve</button>
                      )}
                    </li>
                  ))}
                </ul>
                <div className="order-card-footer"><span>{formatDate(order.createdAt)}</span><div className="row-actions">
                  <span className={`status-badge status-${order.status.toLowerCase()}`}>{order.status}</span>
                  {isCashier && <button className="text-button" type="button" onClick={() => setBillOrder(order)}>View bill</button>}
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
    const queue = orders.flatMap((order) => order.items
      .filter((item) => item.status === 'NEW' || item.status === 'PREPARING')
      .map((item) => ({ order, item })))
      .sort((first, second) => first.item.createdAt.localeCompare(second.item.createdAt) || first.item.id - second.item.id);

    return (
      <section className="panel">
        <div className="section-heading">
          <h2>Kitchen queue</h2><span className="muted">{queue.length} items</span>
        </div>
        {queue.length === 0 ? <p className="empty-state">New items will appear here.</p> : (
          <div className="table-scroll">
            <table className="data-table">
              <thead><tr><th>Order</th><th>Table</th><th>Item</th><th>Qty</th><th>Status</th><th>Time received</th><th>Action</th></tr></thead>
              <tbody>
                {queue.map(({ order, item }) => (
                  <tr key={item.id}>
                    <td>#{order.id}</td>
                    <td>{order.tableId == null ? 'Pickup' : order.tableId}</td>
                    <td>{item.itemName}</td>
                    <td>{item.quantity}</td>
                    <td><span className={`status-badge status-${item.status.toLowerCase()}`}>{item.status}</span></td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td>
                      <button className="text-button" type="button" disabled={saving}
                        onClick={() => updateItemStatus(order.id, item.id, item.status === 'NEW' ? 'PREPARING' : 'READY')}>
                        {item.status === 'NEW' ? 'Start preparing' : 'Mark ready'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    );
  };

  const renderStaff = () => (
    <div className="menu-layout">
      <section className="panel staff-panel">
        <p className="eyebrow">Staff access</p>
        <h2>{staffForm.id === null ? 'Create staff account' : `Edit ${staffForm.username}`}</h2>
        <p className="muted">Select one or more roles. Leave the password blank when editing to keep the current password.</p>
        <form className="staff-form stacked-form" onSubmit={saveStaff}>
          <label className="field-label" htmlFor="staff-full-name">Full name <span>(optional)</span></label>
          <input id="staff-full-name" maxLength={150} value={staffForm.fullName} onChange={(event) => setStaffForm({ ...staffForm, fullName: event.target.value })} />
          <label className="field-label" htmlFor="staff-username">Username</label>
          <input id="staff-username" required minLength={3} maxLength={100} autoComplete="off" value={staffForm.username} onChange={(event) => setStaffForm({ ...staffForm, username: event.target.value })} />
          <label className="field-label" htmlFor="staff-password">Password {staffForm.id === null ? '(required, at least 12 characters)' : '(optional)'}</label>
          <input id="staff-password" type="password" minLength={staffForm.id === null ? 12 : undefined} maxLength={72} required={staffForm.id === null} autoComplete="new-password" value={staffForm.password} onChange={(event) => setStaffForm({ ...staffForm, password: event.target.value })} />
          <fieldset className="stacked-form"><legend>Roles</legend>
            {(['WAITER', 'CASHIER', 'KITCHEN', 'CHEF'] as Role[]).map((role) => <label key={role} className="field-label"><input type="checkbox" checked={staffForm.roles.includes(role)} onChange={(event) => setStaffForm({ ...staffForm, roles: event.target.checked ? [...staffForm.roles, role] : staffForm.roles.filter((item) => item !== role) })} /> {role === 'KITCHEN' ? 'Kitchen' : role === 'CHEF' ? 'Chef' : role[0] + role.slice(1).toLowerCase()}</label>)}
          </fieldset>
          <div className="editor-actions">
            <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Saving…' : staffForm.id === null ? 'Create account' : 'Save changes'}</button>
            {staffForm.id !== null && <button className="secondary-button" type="button" onClick={() => setStaffForm(emptyStaffForm)}>Cancel</button>}
          </div>
        </form>
      </section>
      <section className="panel">
        <div className="section-heading"><div><p className="eyebrow">Current profiles</p><h2>Staff accounts</h2></div><span className="muted">{staff.length} profiles</span></div>
        {staff.length === 0 ? <p className="empty-state">No staff accounts have been created.</p> : <div className="order-list">
          {staff.map((member) => <article className="order-card" key={member.id}>
            <div className="order-card-header"><div><strong>{member.fullName || member.username}</strong><span>@{member.username} · {member.roles.join(', ')}</span></div><span>{member.enabled ? 'Enabled' : 'Disabled'}</span></div>
            <div className="row-actions"><button className="text-button" type="button" onClick={() => setStaffForm({ id: member.id, username: member.username, fullName: member.fullName ?? '', password: '', roles: member.roles.filter((role) => role !== 'ADMIN') })}>Edit</button><button className="text-button danger" type="button" onClick={() => deleteStaff(member)}>Delete</button></div>
          </article>)}
        </div>}
      </section>
    </div>
  );

  const renderTables = () => (
    <div className="menu-layout">
      <section className="panel staff-panel">
        <p className="eyebrow">Dining areas</p><h2>Add a table</h2>
        <form className="stacked-form" onSubmit={createTable}>
          <label className="field-label" htmlFor="table-number">Table number</label>
          <input id="table-number" type="number" min="1" step="1" required value={tableNumberInput} onChange={(event) => setTableNumberInput(event.target.value)} />
          <label className="field-label" htmlFor="new-table-status">Initial status</label>
          <select id="new-table-status" value={tableStatusInput} onChange={(event) => setTableStatusInput(event.target.value as TableStatus)}><option value="AVAILABLE">Available</option><option value="OCCUPIED">Occupied</option><option value="RESERVED">Reserved</option></select>
          <button className="primary-button" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Add table'}</button>
        </form>
      </section>
      <section className="panel">
        <div className="section-heading"><div><p className="eyebrow">Floor status</p><h2>Tables</h2></div><span className="muted">{tables.length} total</span></div>
        {tables.length === 0 ? <p className="empty-state">No tables have been configured.</p> : <div className="order-list">
          {tables.map((table) => <article className="order-card" key={table.id}>
            <div className="order-card-header"><strong>Table {table.id}</strong>{table.activeOrderId && <span>Active order #{table.activeOrderId}</span>}</div>
            <div className="row-actions"><select aria-label={`Status for table ${table.id}`} disabled={Boolean(table.activeOrderId)} value={table.status} onChange={(event) => updateTableStatus(table, event.target.value as TableStatus)}><option value="AVAILABLE">Available</option><option value="OCCUPIED">Occupied</option><option value="RESERVED">Reserved</option></select><button className="text-button danger" type="button" disabled={Boolean(table.activeOrderId)} onClick={() => deleteTable(table)}>Delete</button></div>
          </article>)}
        </div>}
      </section>
    </div>
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
            <label className="field-label" htmlFor="item-category">Category <span>(optional)</span></label>
            <input id="item-category" maxLength={80} value={menuForm.category} onChange={(event) => setMenuForm({ ...menuForm, category: event.target.value })} placeholder="e.g. Mains" />
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
        <label className="field-label" htmlFor="menu-search">Search by item or category</label>
        <input id="menu-search" type="search" value={menuSearch} onChange={(event) => setMenuSearch(event.target.value)} placeholder="Search menu" />
        {menu.length === 0 ? <p className="empty-state">No menu items yet{isAdmin ? ' — add the first one to get started.' : '.'}</p> : (
          <div className="menu-grid">
            {visibleMenu.map((item) => (
              <article className="menu-card" key={item.id}>
                <img
                  className="item-image"
                  src={item.imageUrl || undefined}
                  alt={item.name}
                  onLoad={(event) => { event.currentTarget.style.visibility = 'visible'; }}
                  onError={(event) => { event.currentTarget.style.visibility = 'hidden'; }}
                />
                <div className="menu-card-content"><div className="menu-item-heading"><h3>{item.name}</h3><span className={item.available ? 'availability available' : 'availability unavailable'}>{item.available ? 'Available' : 'Unavailable'}</span></div>
                  {item.category && <p className="eyebrow">{item.category}</p>}
                  {item.description && <p className="muted item-description">{item.description}</p>}
                  <div className="menu-card-footer"><strong>{formatPrice(item.price)}</strong>
                    {isAdmin && <div className="row-actions"><button className="text-button" type="button" onClick={() => { setEditingItemId(item.id); setMenuForm({ name: item.name, description: item.description ?? '', category: item.category ?? '', price: String(item.price), imageUrl: item.imageUrl ?? '' }); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Edit</button><button className="text-button danger" type="button" onClick={() => deleteMenuItem(item)}>Delete</button></div>}
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
            ['menu', 'Menu'],
          ] as [Section, string][] : isCashier && !isAdmin ? [
            ['orders', 'Order monitoring'],
            ['menu', 'Menu'],
            ['reports', 'Billing & reports'],
          ] as [Section, string][] : isWaiter && !isAdmin ? [
            ['orders', 'Table orders'],
            ['menu', 'Menu'],
          ] as [Section, string][] : [
            ['overview', 'Overview'],
            ['orders', 'Orders'],
            ['menu', 'Menu'],
            ...(isAdmin ? [['staff', 'Staff accounts'] as [Section, string], ['tables', 'Tables'] as [Section, string]] : []),
            ...((isAdmin || user.roles.includes('CASHIER')) ? [['reports', 'Reports'] as [Section, string]] : []),
          ] as [Section, string][]).map(([section, label]) => (
            <button
              key={section}
              type="button"
              className={`nav-item ${activeSection === section ? 'active' : ''}`}
              aria-current={activeSection === section ? 'page' : undefined}
              onClick={() => { setActiveSection(section); setError(''); setNotice(''); }}
            >
              {sectionIcons[section]}<span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="user-avatar">{(user.fullName || user.username).charAt(0).toUpperCase()}</div>
            <div><strong>{user.fullName || user.username}</strong><span>{user.roles.map((role) => role.toLowerCase()).join(' · ')}</span></div>
          </div>
          <button className="logout-button" type="button" onClick={logout}><LogOut size={16} aria-hidden="true" /><span>Log out</span></button>
        </div>
      </aside>

      <main className="content-shell">
        <header className="topbar">
          <div><p className="eyebrow">{title}</p><h1>{sectionName}</h1><p className="workspace-greeting">Welcome back, {user.fullName || user.username}</p></div>
          <button className="secondary-button refresh-button" type="button" onClick={() => { setError(''); refreshData().catch((requestError: unknown) => { if (!handleExpiredSession(requestError)) setError(getErrorMessage(requestError, 'Could not refresh data.')); }); }}><RefreshCw size={16} aria-hidden="true" /> Refresh data</button>
        </header>
        {error && <div className="alert-error" role="alert">{error}</div>}
        {notice && <div className="alert-success" role="status">{notice}</div>}
        {!isKitchen && activeSection === 'overview' && renderOverview()}
        {activeSection === 'orders' && (isKitchen ? renderKitchenOrders() : renderOrders())}
        {activeSection === 'menu' && renderMenu()}
        {!isKitchen && isAdmin && activeSection === 'staff' && renderStaff()}
        {!isKitchen && isAdmin && activeSection === 'tables' && renderTables()}
        {!isKitchen && (isAdmin || user.roles.includes('CASHIER')) && activeSection === 'reports' && renderReports()}
        {billOrder && <section className="panel" role="dialog" aria-modal="true" aria-labelledby="bill-title">
          <div className="section-heading"><div><p className="eyebrow">Billing</p><h2 id="bill-title">Bill for order #{billOrder.id}</h2></div><button className="text-button" type="button" onClick={() => setBillOrder(null)}>Close</button></div>
          <p>{billOrder.customerName}{billOrder.tableId ? ` · Table ${billOrder.tableId}` : ''} · {formatDate(billOrder.createdAt)}</p>
          <div className="table-scroll"><table className="data-table"><thead><tr><th>Item</th><th>Qty</th><th>Unit price</th><th>Line total</th></tr></thead><tbody>
            {billOrder.items.map((item) => <tr key={item.id}><td>{item.itemName}</td><td>{item.quantity}</td><td>{formatPrice(item.unitPrice)}</td><td>{formatPrice(item.lineTotal)}</td></tr>)}
          </tbody><tfoot><tr><th colSpan={3}>Total</th><th>{formatPrice(billOrder.total)}</th></tr></tfoot></table></div>
          <button className="primary-button" type="button" onClick={() => window.print()}>Print bill</button>
        </section>}
      </main>
    </div>
  );
}
