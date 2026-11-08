# Order Desk

Order Desk is a full-stack restaurant ordering application with public customer ordering and tracking, an administrator menu and staff workspace, and a dedicated kitchen order queue backed by PostgreSQL.

## Project structure

- `backend/` — Java 25 + Spring Boot 3 backend
- `frontend/` — React + TypeScript + Vite frontend
- `database/migrations/` — PostgreSQL migration scripts
- `docker-compose.yml` — Local orchestration for PostgreSQL, backend, and frontend
- `.env.example` — Environment variable template

## Features

- Separate customer ordering, administrator sign-in, and kitchen staff sign-in
- Administrator and kitchen staff account creation from their respective sign-in pages
- Customers can browse the public menu and place an order without creating an account
- Order tracking through a private, unguessable tracking code
- Administrator-only menu item creation, editing, and deletion (including uploaded product photos)
- Kitchen queue with incoming/preparing/completed counts and order status controls
- Administrator-provisioned kitchen staff accounts with BCrypt-hashed passwords
- Order creation from available menu items, order status updates, and deletion
- Daily and monthly order/revenue totals calculated from saved orders
- Role-based dashboard routing; menu changes require an administrator

## Local setup

1. Start PostgreSQL and create the `restaurant_management` database.
2. Update the datasource credentials in `backend/src/main/resources/application.yml` if needed.
3. Use Java 25 and start the backend:

```bash
cd backend
mvn spring-boot:run
```

4. Start the frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173` and proxies `/api` calls to the backend on `http://localhost:8080`.

### Accounts and roles

1. Open `http://localhost:5173/login` and choose **Administrator sign in** or **Kitchen staff sign in**. Each sign-in page includes a matching account-creation form. Use a unique username and a password of at least 12 characters.
2. Administrators can manage menu items, orders, reports, and kitchen accounts. Kitchen staff see the kitchen queue and can update order status; they cannot edit the menu or delete orders.
3. Customers use **Customer order** or `/customer` without logging in. A new order receives a random tracking code. It is saved in that browser and can also be entered on another device to check order status. Treat the code like a private order link.

**Security note:** Account creation is open from the administrator and kitchen sign-in pages as requested. Anyone who can reach the app can create an administrator account. Do not expose this configuration to the public internet; for production, restrict account creation to authorized administrators and use HTTPS.

The backend creates tables automatically with Hibernate. `database/migrations/` contains the equivalent schema evolution for deployments that run SQL migrations themselves; apply `V3__kitchen_roles_and_order_tracking.sql` to add the kitchen role and tracking-code index to an existing installation. Reports show actual saved orders; an empty database correctly reports zero sales.

## Optional demo data

To try the application with sample Canadian-inspired menu items and 30 days of clearly labeled example orders, run `database/demo_seed.sql` against `restaurant_management` after starting the backend once. The seed can be rerun safely; the Reports and Overview pages identify the sample totals, and the sample orders can be deleted from Orders.

The sample menu photos are served locally from `frontend/public/images/`.

## Docker

```bash
docker compose up --build
```

## Notes

This is a production-style foundation and a staged implementation aligned to the university requirement document. The project is structured to continue into Sprint 1 and Sprint 2 modules in a clean and extensible way.
