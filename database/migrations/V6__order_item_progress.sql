ALTER TABLE order_line_items ADD COLUMN IF NOT EXISTS status VARCHAR(20);
ALTER TABLE order_line_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

UPDATE order_line_items AS item
SET status = COALESCE(item.status, CASE WHEN orders.status = 'COMPLETED' THEN 'SERVED' ELSE orders.status END),
    created_at = COALESCE(item.created_at, orders.created_at)
FROM customer_orders AS orders
WHERE item.order_id = orders.id AND (item.status IS NULL OR item.created_at IS NULL);
