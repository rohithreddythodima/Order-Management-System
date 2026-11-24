CREATE TABLE IF NOT EXISTS restaurant_tables (
    id BIGINT PRIMARY KEY CHECK (id >= 1),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED'))
);

ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS table_id BIGINT;
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS table_number BIGINT;
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT;
ALTER TABLE customer_orders ADD COLUMN IF NOT EXISTS created_by_username VARCHAR(100);

DO $$ BEGIN
    ALTER TABLE customer_orders ADD CONSTRAINT fk_customer_orders_table
        FOREIGN KEY (table_id) REFERENCES restaurant_tables(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE customer_orders ADD CONSTRAINT fk_customer_orders_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_customer_orders_table_status ON customer_orders (table_id, status);
CREATE INDEX IF NOT EXISTS idx_customer_orders_created_by_status ON customer_orders (created_by_user_id, status);
