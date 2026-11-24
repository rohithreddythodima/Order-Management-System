export type Role = 'ADMIN' | 'KITCHEN' | 'WAITER' | 'CHEF' | 'CASHIER';

export interface UserDto {
  id: number;
  username: string;
  fullName: string;
  roles: Role[];
  enabled: boolean;
  createdAt: string;
}

export interface AuthResponse {
  message: string;
  user: UserDto | null;
}

export interface MenuItem {
  id: number;
  name: string;
  description: string;
  category: string | null;
  price: number;
  imageUrl: string | null;
  available: boolean;
}

export type OrderStatus = 'NEW' | 'PREPARING' | 'READY' | 'COMPLETED';
export type TableStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED';

export interface RestaurantTable {
  id: number;
  status: TableStatus;
  activeOrderId: number | null;
}

export interface OrderLineItem {
  menuItemId: number;
  itemName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  customerName: string;
  status: OrderStatus;
  total: number;
  createdAt: string;
  items: OrderLineItem[];
  trackingCode: string | null;
  tableId?: number | null;
  waiterUsername?: string | null;
}

export type TrackedOrder = Omit<Order, 'trackingCode'>;

export interface DailySales {
  date: string;
  orderCount: number;
  revenue: number;
}

export interface SalesReport {
  ordersToday: number;
  revenueToday: number;
  ordersThisMonth: number;
  revenueThisMonth: number;
  dailySales: DailySales[];
}
