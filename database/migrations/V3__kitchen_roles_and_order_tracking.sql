INSERT INTO roles (name) VALUES ('KITCHEN') ON CONFLICT (name) DO NOTHING;

ALTER TABLE customer_orders
    ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(36);

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_orders_tracking_code
    ON customer_orders (tracking_code)
    WHERE tracking_code IS NOT NULL;
