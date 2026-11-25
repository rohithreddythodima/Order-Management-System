# Order Desk

Order Desk is a full-stack restaurant ordering application with public customer ordering and tracking, an administrator menu and staff workspace, and a dedicated kitchen order queue with a local H2 database and optional PostgreSQL support.

## Project structure

- `backend/` — Java 17+ and Spring Boot 3 backend
- `frontend/` — React + TypeScript + Vite frontend
- `database/migrations/` — PostgreSQL migration scripts
- `.env.example` — Environment variable template

## Features

- Separate customer ordering, administrator sign-in, and kitchen staff sign-in
- Administrator and kitchen staff account creation from their respective sign-in pages
- Customers can browse the public menu and place an order without creating an account
- Order tracking through a private, unguessable tracking code
- Administrator-only menu item creation, editing, and deletion (including uploaded product photos)
- Oldest-first kitchen queue with item timestamps and preparing/ready controls
- Waiters can filter to their tables and serve ready items. Admins and cashiers can monitor progress and cashiers can view and print bills.
- Administrator-provisioned kitchen staff accounts with BCrypt-hashed passwords
- Order creation from available menu items, automatic served status, and administrator deletion
- Daily and monthly order/revenue totals calculated from saved orders
- Role-based dashboard routing; menu changes require an administrator

## Local setup

Install JDK 17 or later, Maven, and Node.js 20 or later. The default backend stores data in `backend/data/` using H2.

1. Start the backend:

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --order-desk.auth.admin-self-signup=true"
```

2. Start the frontend in a second terminal:

```bash
cd frontend
npm ci
npm run dev -- --host 127.0.0.1
```

The frontend runs on `http://localhost:5173` and proxies `/api` calls to the backend on `http://localhost:8080`.

To build and test locally, run `mvn verify` in `backend/` and `npm run build` in `frontend/`.

### Accounts and roles

1. Open `http://localhost:5173/login` and choose a staff workspace. Choose **Sign up** for Administrator, Kitchen, Waiter, or Cashier to create an account, then sign in to that workspace. Use a unique username and a password of at least 12 characters.
2. Administrators can manage menu items, tables, staff, orders, and reports. Each staff workspace can browse the menu. Kitchen staff prepare items and mark them ready. Waiters can serve ready items from their table orders and shared pickup orders. When the last item is served, the order becomes served and its table becomes available.
3. Customers use **Customer order** or `/customer` without logging in. A new order receives a random tracking code. It is saved in that browser and can also be entered on another device to check order status. Treat the code like a private order link.

**Local demo access:** The commands above bind both services to this computer and enable direct administrator signup. Without the demo flag, anonymous administrator signup is limited to first-time setup. For production, restrict staff signup to authorized administrators and use HTTPS.

### Optional PostgreSQL setup

Create a PostgreSQL database and export the `SPRING_*` variables in `.env.example` before starting the backend. Hibernate manages the schema by default. For installations that apply SQL migrations directly, apply the scripts in `database/migrations/` in version order. V6 backfills legacy item progress and timestamps.

## Optional demo data

The backend inserts 53 sample Canadian-inspired menu items when they are missing. To add 30 days of clearly labeled example orders, run `database/demo_seed.sql` against a PostgreSQL installation after starting the backend once. The seed can be rerun safely; the Reports and Overview pages identify the sample totals, and the sample orders can be deleted from Orders.

The sample menu photos are served locally from `frontend/public/images/`.
